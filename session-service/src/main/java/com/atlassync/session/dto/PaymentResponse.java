package com.atlassync.session.dto;

import java.util.UUID;

public record PaymentResponse(
        UUID sessionId,
        String status,
        QrData exitQr
) {
}
