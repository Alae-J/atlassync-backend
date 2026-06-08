package com.atlassync.session.exception;

/**
 * Thrown when {@code createPaymentIntent} is called for a session whose
 * intent already succeeded. The mobile should treat this as a "go to
 * walkout" signal, not a payment failure.
 */
public class SessionAlreadyPaidException extends RuntimeException {
    public SessionAlreadyPaidException(String message) {
        super(message);
    }
}
