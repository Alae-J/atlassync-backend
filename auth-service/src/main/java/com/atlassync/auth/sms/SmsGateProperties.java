package com.atlassync.auth.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the sms-gate.app cloud gateway.
 * <p>
 * The gateway is a self-hosted Android relay that exposes a REST API; this app installed on a
 * phone with an Inwi SIM (or any other carrier) sends the actual SMS over the cellular network.
 * <p>
 * Credentials come from the SMSGate Android app's "Cloud Server" tab.
 */
@ConfigurationProperties(prefix = "atlassync.otp.sms.smsgate")
public record SmsGateProperties(
        String baseUrl,
        String username,
        String password
) {

    public SmsGateProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.sms-gate.app/3rdparty/v1";
        }
        requireNonBlank(username, "atlassync.otp.sms.smsgate.username is required");
        requireNonBlank(password, "atlassync.otp.sms.smsgate.password is required");
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
