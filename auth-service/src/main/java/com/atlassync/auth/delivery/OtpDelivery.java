package com.atlassync.auth.delivery;

/**
 * Everything a delivery channel needs to send an OTP.
 *
 * @param recipient      either an E.164 phone number ({@code +14155551234}) for SMS/WhatsApp
 *                       channels, or an email address ({@code user@example.com}) for email
 *                       channels. The active channel decides how to interpret it.
 * @param code           the raw numeric OTP, useful for template-based channels
 * @param displayMessage a fully-formatted human message, useful for free-form channels
 */
public record OtpDelivery(String recipient, String code, String displayMessage) {

    public OtpDelivery {
        requireNonBlank(recipient, "recipient");
        requireNonBlank(code, "code");
        requireNonBlank(displayMessage, "displayMessage");
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
    }
}
