package com.atlassync.session.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Reads the cart for a session straight from cart-service over HTTP. Used at
 * payment completion to snapshot line items into the session record before
 * the cart's Redis TTL evicts them.
 *
 * <p>Kept intentionally thin — no retries, no circuit breaker. If the cart
 * service can't be reached the session still completes; the receipt just has
 * empty line items, which the mobile renders gracefully.
 */
@Slf4j
@Component
public class CartSnapshotClient {

    private final RestClient client;

    public CartSnapshotClient(RestClient.Builder builder,
                              @Value("${atlassync.cart-service.base-url:http://localhost:8083}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public CartSnapshot fetch(UUID sessionId) {
        try {
            CartSnapshot snapshot = client.get()
                    .uri("/api/cart/{id}", sessionId)
                    .retrieve()
                    .body(CartSnapshot.class);

            if (snapshot == null) return empty(sessionId);
            return snapshot;
        } catch (RestClientException ex) {
            log.warn("[cart-snapshot] failed for session={}: {}", sessionId, ex.getMessage());
            return empty(sessionId);
        }
    }

    private static CartSnapshot empty(UUID sessionId) {
        return new CartSnapshot(sessionId.toString(), Collections.emptyList(), BigDecimal.ZERO, 0);
    }

    public record CartSnapshot(
            String sessionId,
            List<CartItem> items,
            BigDecimal total,
            int itemCount
    ) {}

    public record CartItem(
            String productId,
            String productName,
            Integer quantity,
            BigDecimal priceAtAddition,
            String imageUrl,
            Instant addedAt
    ) {}
}
