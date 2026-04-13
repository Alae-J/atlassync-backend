package com.atlassync.session.dto;

import java.util.UUID;

public record GateValidationResponse(
        boolean valid,
        UUID sessionId,
        String message
) {
}
