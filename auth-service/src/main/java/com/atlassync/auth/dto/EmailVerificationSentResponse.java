package com.atlassync.auth.dto;

public record EmailVerificationSentResponse(
        long resendCooldownSeconds,
        long expiresInSeconds
) {}
