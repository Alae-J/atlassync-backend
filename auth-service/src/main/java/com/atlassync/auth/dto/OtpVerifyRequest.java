package com.atlassync.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record OtpVerifyRequest(
        @NotNull
        UUID correlationId,

        @NotNull
        @Pattern(regexp = "^\\d{4,8}$", message = "code must be 4-8 digits")
        String code
) {}
