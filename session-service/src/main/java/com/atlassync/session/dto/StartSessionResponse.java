package com.atlassync.session.dto;

import java.util.UUID;

public record StartSessionResponse(
        UUID sessionId,
        String status,
        QrData entryQr
) {
}
