package com.atlassync.cart.dto;

public record CartUpdateEvent(
        String sessionId,
        String eventType,
        CartSnapshot snapshot
) {
}
