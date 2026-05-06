package com.atlassync.auth.dto;

import java.util.UUID;

public record OtpRequestResponse(
        UUID correlationId,
        long resendInSeconds,
        long expiresInSeconds
) {}
