package com.atlassync.auth.exception;

import java.time.Duration;

public class OtpRateLimitedException extends OtpException {

    private final Duration retryAfter;

    public OtpRateLimitedException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
