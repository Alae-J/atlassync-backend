package com.atlassync.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CartItemDto(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal priceAtAddition,
        String imageUrl,
        Instant addedAt
) {
}
