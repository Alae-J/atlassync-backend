package com.atlassync.product.grpc;

import com.atlassync.product.entity.Product;
import com.atlassync.product.service.ProductService;
import com.atlassync.proto.product.GetProductRequest;
import com.atlassync.proto.product.GetProductsRequest;
import com.atlassync.proto.product.GetProductsResponse;
import com.atlassync.proto.product.ProductResponse;
import com.atlassync.proto.product.ProductServiceGrpc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@GrpcService
public class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ProductGrpcService.class);

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public ProductGrpcService(ProductService productService, ObjectMapper objectMapper) {
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void getProduct(GetProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        String barcode = request.getBarcode();
        Optional<Product> product = productService.findByBarcode(barcode);

        if (product.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Product not found: " + barcode)
                            .asRuntimeException());
            return;
        }

        responseObserver.onNext(toProto(product.get()));
        responseObserver.onCompleted();
    }

    @Override
    public void getProducts(GetProductsRequest request, StreamObserver<GetProductsResponse> responseObserver) {
        List<String> barcodes = request.getBarcodesList();
        List<Product> products = productService.findByBarcodes(barcodes);

        GetProductsResponse response = GetProductsResponse.newBuilder()
                .addAllProducts(products.stream().map(this::toProto).toList())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private ProductResponse toProto(Product p) {
        ProductResponse.Builder builder = ProductResponse.newBuilder()
                .setBarcode(p.getBarcode())
                .setName(p.getName())
                .setPrice(p.getPrice().toPlainString())
                .setCurrencyCode(orEmpty(p.getCurrencyCode()))
                .setAisleNumber(p.getAisleNumber() != null ? p.getAisleNumber() : 0)
                .setRfidFlag(Boolean.TRUE.equals(p.getRfidSecurityRequired()));

        if (p.getBrand() != null) builder.setBrand(p.getBrand());
        if (p.getImageUrl() != null) builder.setImageUrl(p.getImageUrl());
        if (p.getNutriscoreGrade() != null) builder.setNutriscoreGrade(p.getNutriscoreGrade());
        if (p.getAttributes() != null) builder.setAttributesJson(toJson(p.getAttributes()));

        return builder.build();
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize attributes to JSON", e);
            return "{}";
        }
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }
}
