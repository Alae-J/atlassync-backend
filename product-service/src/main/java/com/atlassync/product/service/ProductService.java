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

        // Cameras (especially iOS) return EAN-13 codes starting with '0' as their
        // UPC-A 12-digit equivalent — same code, just with the leading zero
        // stripped. Try every plausible form against the DB before falling back
        // to OFF.
        for (String candidate : normaliseCandidates(barcode)) {
            Optional<Product> hit = productRepository.findByBarcode(candidate);
            if (hit.isPresent()) return hit;
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
        // Cart refresh path — normalise each barcode so UPC-A / EAN-13 equivalents
        // both resolve. Done eagerly so the IN query hits all plausible forms.
        List<String> expanded = barcodes.stream()
                .flatMap(bc -> normaliseCandidates(bc).stream())
                .distinct()
                .toList();
        return productRepository.findByBarcodeIn(expanded);
    }

    /**
     * Returns every barcode form worth checking against the DB. UPC-A (12) and
     * EAN-13 (13 with leading 0) are mathematically the same code, so a scan of
     * either form should resolve to the same product.
     */
    private static List<String> normaliseCandidates(String barcode) {
        if (barcode == null || barcode.isBlank()) return List.of();
        String trimmed = barcode.trim();
        if (!trimmed.matches("\\d+")) return List.of(trimmed);

        if (trimmed.length() == 12) {
            // UPC-A scanned -> also try the EAN-13 form with a leading 0.
            return List.of(trimmed, "0" + trimmed);
        }
        if (trimmed.length() == 13 && trimmed.startsWith("0")) {
            // EAN-13 with leading 0 -> also try the UPC-A form.
            return List.of(trimmed, trimmed.substring(1));
        }
        return List.of(trimmed);
    }

    public List<Product> searchByName(String query, int limit) {
        return productRepository.searchByNameContainingIgnoreCase(query, PageRequest.of(0, limit));
    }

    public List<Product> searchByNameInAisle(String query, Integer aisle, int limit) {
        if (query == null || query.isBlank()) {
            return productRepository.findByAisleNumber(aisle, PageRequest.of(0, limit));
        }
        return productRepository.searchByNameAndAisle(query, aisle, PageRequest.of(0, limit));
    }
}
