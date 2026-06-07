package com.atlassync.auth.dto;

/**
 * Response from {@code POST /api/auth/password/reset/request}. Returned with
 * the same shape regardless of whether the email belongs to a real account —
 * the request endpoint is intentionally non-revealing.
 */
public record PasswordResetSentResponse(
        long resendCooldownSeconds,
        long expiresInSeconds
) {}
