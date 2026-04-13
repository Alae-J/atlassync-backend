package com.atlassync.product.service;

import com.atlassync.product.entity.Product;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenFoodFactsClient {

    private static final Logger log = LoggerFactory.getLogger(OpenFoodFactsClient.class);

    private final RestClient restClient;

    public OpenFoodFactsClient(@Value("${openfoodfacts.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "AtlasSync/1.0 (learning-project)")
                .build();
    }

    public Optional<Product> fetchProduct(String barcode) {
        try {
            JsonNode root = restClient.get()
                    .uri("/product/{barcode}.json", barcode)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null || root.path("status").asInt() != 1) {
                log.debug("Product not found on Open Food Facts: {}", barcode);
                return Optional.empty();
            }

            JsonNode productNode = root.path("product");

            Product product = new Product();
            product.setBarcode(barcode);
            product.setName(textOrDefault(productNode, "product_name", "Unknown"));
            product.setBrand(textOrNull(productNode, "brands"));
            product.setImageUrl(textOrNull(productNode, "image_url"));
            product.setNutriscoreGrade(textOrNull(productNode, "nutriscore_grade"));
            product.setPrice(BigDecimal.ZERO);
            product.setStockQuantity(0);
            product.setRfidSecurityRequired(false);

            JsonNode nutriNode = productNode.path("nutriments");
            if (!nutriNode.isMissingNode()) {
                Map<String, Object> nutriments = new HashMap<>();
                nutriments.put("energy_kcal", nutriNode.path("energy-kcal_100g").asDouble(0));
                nutriments.put("fat", nutriNode.path("fat_100g").asDouble(0));
                nutriments.put("carbohydrates", nutriNode.path("carbohydrates_100g").asDouble(0));
                nutriments.put("proteins", nutriNode.path("proteins_100g").asDouble(0));
                nutriments.put("salt", nutriNode.path("salt_100g").asDouble(0));
                product.setNutriments(nutriments);
            }

            log.info("Fetched product from Open Food Facts: {} ({})", product.getName(), barcode);
            return Optional.of(product);

        } catch (Exception e) {
            log.warn("Failed to fetch product from Open Food Facts for barcode {}: {}", barcode, e.getMessage());
            return Optional.empty();
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        String value = textOrNull(node, field);
        return value != null ? value : defaultValue;
    }
}
