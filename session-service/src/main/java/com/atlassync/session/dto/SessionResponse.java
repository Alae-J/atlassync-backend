package com.atlassync.session.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID sessionId,
        Long userId,
        String status,
        Instant createdAt
) {
}
