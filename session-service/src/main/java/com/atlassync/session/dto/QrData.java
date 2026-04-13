package com.atlassync.session.dto;

import java.time.Instant;

public record QrData(
        String correlationId,
        String payload,
        String signature,
        Instant expiresAt
) {
}
