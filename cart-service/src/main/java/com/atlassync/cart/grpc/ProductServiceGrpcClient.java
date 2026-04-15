package com.atlassync.cart.grpc;

import com.atlassync.cart.exception.ProductNotFoundException;
import com.atlassync.proto.product.GetProductRequest;
import com.atlassync.proto.product.ProductResponse;
import com.atlassync.proto.product.ProductServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceGrpcClient {

    @GrpcClient("product-service")
    private ProductServiceGrpc.ProductServiceBlockingStub stub;

    public ProductResponse getProduct(String barcode) {
        try {
            return stub.getProduct(GetProductRequest.newBuilder().setBarcode(barcode).build());
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new ProductNotFoundException("Product not found for barcode: " + barcode);
            }
            throw ex;
        }
    }
}
