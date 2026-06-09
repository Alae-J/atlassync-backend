package com.atlassync.product.service;

import com.atlassync.product.entity.Product;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Thin HTTP wrapper around the Open Food Facts v2 API. Returns a parsed
 * {@link JsonNode} on success; the actual mapping to a {@link Product}
 * lives in {@link ProductEnricher} so this class stays cheap to mock
 * and the policy (price, aisle, dietary mapping) stays in one place.
 *
 * <p>Bounded by a hard 1.5s read timeout so the scan-path stays
 * responsive even if OFF is slow or down.
 */
@Component
public class OpenFoodFactsClient {

    private static final Logger log = LoggerFactory.getLogger(OpenFoodFactsClient.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(1000);
    private static final Duration READ_TIMEOUT = Duration.ofMillis(1500);

    private final RestClient restClient;
    private final ProductEnricher enricher;

    public OpenFoodFactsClient(@Value("${openfoodfacts.base-url}") String baseUrl,
                               ProductEnricher enricher) {
        this.enricher = enricher;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("User-Agent", "AtlasSync/1.0 (learning-project)")
                .build();
    }

    public Optional<Product> fetchProduct(String barcode) {
        JsonNode root = fetchRaw(barcode);
        if (root == null || root.path("status").asInt() != 1) {
            log.debug("Product not found on Open Food Facts: {}", barcode);
            return Optional.empty();
        }
        Product enriched = enricher.enrich(barcode, root.path("product"));
        log.info("Enriched product from OFF: {} ({}, aisle {})",
                enriched.getName(), barcode, enriched.getAisleNumber());
        return Optional.of(enriched);
    }

    private JsonNode fetchRaw(String barcode) {
        try {
            return restClient.get()
                    .uri("/product/{barcode}.json", barcode)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (ResourceAccessException e) {
            // Timeout or connection error. Fail soft — caller treats it as miss.
            log.warn("OFF timed out for barcode {}: {}", barcode, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("OFF call failed for barcode {}: {}", barcode, e.getMessage());
            return null;
        }
    }
}
