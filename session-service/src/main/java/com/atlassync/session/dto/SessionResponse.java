package com.atlassync.session.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Session state surfaced to the mobile. {@code exitQr} is populated only
 * once the session is COMPLETED — the mobile polls this endpoint after
 * Stripe confirms payment and uses {@code exitQr} to render the walkout
 * gate pass.
 */
public record SessionResponse(
        UUID sessionId,
        Long userId,
        String status,
        Instant createdAt,
        QrData exitQr
) {}
