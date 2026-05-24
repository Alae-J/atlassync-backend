package com.atlassync.session.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Summary row for the history list (Orders tab, "RECENT SHOPS" on Home).
 * Compact intentionally — heavy detail is only fetched when the user taps in.
 */
public record SessionHistoryItem(
        UUID sessionId,
        Long storeId,
        String status,
        Instant createdAt,
        Instant completedAt,
        BigDecimal totalAmount,
        Integer itemCount
) {}
