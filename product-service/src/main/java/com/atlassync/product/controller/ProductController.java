package com.atlassync.product.controller;

import com.atlassync.product.dto.ProductDto;
import com.atlassync.product.dto.ProductMapper;
import com.atlassync.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{barcode}")
    public ResponseEntity<ProductDto> getByBarcode(@PathVariable String barcode) {
        return productService.findByBarcode(barcode)
                .map(ProductMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<ProductDto> search(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) Integer aisle,
            @RequestParam(defaultValue = "20") int limit) {
        var products = aisle != null
                ? productService.searchByNameInAisle(query, aisle, limit)
                : productService.searchByName(query, limit);
        return products.stream().map(ProductMapper::toDto).toList();
    }

    @GetMapping("/batch")
    public List<ProductDto> batch(@RequestParam List<String> barcodes) {
        return productService.findByBarcodes(barcodes)
                .stream()
                .map(ProductMapper::toDto)
                .toList();
    }
}
