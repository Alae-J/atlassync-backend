package com.atlassync.auth.delivery;

/**
 * Everything a delivery channel needs to send an OTP.
 *
 * @param phone          E.164 phone number (e.g. {@code +14155551234})
 * @param code           the raw numeric OTP, useful for template-based channels
 * @param displayMessage a fully-formatted human message, useful for free-form channels
 */
public record OtpDelivery(String phone, String code, String displayMessage) {

    public OtpDelivery {
        requireNonBlank(phone, "phone");
        requireNonBlank(code, "code");
        requireNonBlank(displayMessage, "displayMessage");
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
    }
}
