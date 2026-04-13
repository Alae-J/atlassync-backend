package com.atlassync.auth.exception;

public class TokenReuseException extends RuntimeException {

    public TokenReuseException(String message) {
        super(message);
    }
}
