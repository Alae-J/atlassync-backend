package com.atlassync.session.exception;

/**
 * Caller tried to start (or otherwise act on) a shopping session before confirming
 * their email. Surfaced to the mobile app as 403 so the UI can prompt the user to
 * verify before retrying.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
