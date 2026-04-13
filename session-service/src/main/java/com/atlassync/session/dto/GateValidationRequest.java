package com.atlassync.session.dto;

import jakarta.validation.constraints.NotBlank;

public record GateValidationRequest(
        @NotBlank String correlationId
) {
}
