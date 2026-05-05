package com.atlassync.auth.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the sms-gate.app cloud relay (Android phone gateway).
 * Credentials come from the SMSGate Android app's "Cloud Server" tab.
 */
@ConfigurationProperties(prefix = "atlassync.otp.delivery.smsgate")
public record SmsGateProperties(
        String baseUrl,
        String username,
        String password
) {

    public SmsGateProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.sms-gate.app/3rdparty/v1";
        }
        requireNonBlank(username, "atlassync.otp.delivery.smsgate.username is required");
        requireNonBlank(password, "atlassync.otp.delivery.smsgate.password is required");
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
