package com.atlassync.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartSnapshot(
        String sessionId,
        List<CartItemDto> items,
        BigDecimal total,
        int itemCount
) {
}
