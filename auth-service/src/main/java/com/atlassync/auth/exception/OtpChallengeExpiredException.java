package com.atlassync.auth.exception;

public class OtpChallengeExpiredException extends OtpException {
    public OtpChallengeExpiredException(String message) {
        super(message);
    }
}
