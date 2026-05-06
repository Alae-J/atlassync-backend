package com.atlassync.auth.exception;

/**
 * Base type for OTP failures so the controller advice can group them.
 */
public abstract class OtpException extends RuntimeException {
    protected OtpException(String message) {
        super(message);
    }
}
