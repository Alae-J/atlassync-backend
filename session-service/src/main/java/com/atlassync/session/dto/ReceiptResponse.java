package com.atlassync.session.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The frozen, self-contained view of a completed session — what the mobile
 * Order Detail / Receipt screen renders. All numeric and time fields are
 * what the receipt would show; line items are insertion-ordered.
 */
public record ReceiptResponse(
        UUID sessionId,
        Long userId,
        Long storeId,
        String status,
        Instant createdAt,
        Instant completedAt,
        BigDecimal totalAmount,
        Integer itemCount,
        List<LineItem> items
) {
    public record LineItem(
            String barcode,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            String imageUrl,
            Instant addedAt
    ) {}
}
