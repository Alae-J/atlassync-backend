package com.atlassync.auth.exception;

/** The user tried to (re)verify an email that's already confirmed. */
public class EmailAlreadyVerifiedException extends RuntimeException {
    public EmailAlreadyVerifiedException(String message) {
        super(message);
    }
}
