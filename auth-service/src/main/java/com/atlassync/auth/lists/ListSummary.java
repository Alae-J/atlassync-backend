package com.atlassync.auth.lists;

import java.time.Instant;

/**
 * Lightweight list row returned by {@code GET /api/lists}. {@code totalEstimate}
 * is always null for now — the mobile computes it by looking up each barcode
 * against the product-service once that integration is in place.
 */
public record ListSummary(
        Long id,
        String name,
        int itemCount,
        Object totalEstimate,
        Instant updatedAt
) {}
