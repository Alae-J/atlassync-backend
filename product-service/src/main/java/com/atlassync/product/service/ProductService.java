package com.atlassync.product.service;

import com.atlassync.product.entity.Product;
import com.atlassync.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final OpenFoodFactsClient openFoodFactsClient;

    public ProductService(ProductRepository productRepository, OpenFoodFactsClient openFoodFactsClient) {
        this.productRepository = productRepository;
        this.openFoodFactsClient = openFoodFactsClient;
    }

    @Cacheable(value = "products", key = "#barcode")
    public Optional<Product> findByBarcode(String barcode) {
        log.debug("Cache miss for barcode: {}", barcode);

        Optional<Product> product = productRepository.findByBarcode(barcode);
        if (product.isPresent()) {
            return product;
        }

        log.info("Product not in DB, trying Open Food Facts: {}", barcode);
        Optional<Product> offProduct = openFoodFactsClient.fetchProduct(barcode);

        if (offProduct.isPresent()) {
            Product saved = productRepository.save(offProduct.get());
            log.info("Saved product from Open Food Facts: {}", saved.getBarcode());
            return Optional.of(saved);
        }

        return Optional.empty();
    }

    public List<Product> findByBarcodes(List<String> barcodes) {
        return productRepository.findByBarcodeIn(barcodes);
    }

    public List<Product> searchByName(String query, int limit) {
        return productRepository.searchByNameContainingIgnoreCase(query, PageRequest.of(0, limit));
    }
}
