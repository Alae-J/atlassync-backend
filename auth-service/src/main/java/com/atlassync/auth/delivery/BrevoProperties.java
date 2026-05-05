package com.atlassync.auth.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Brevo (formerly Sendinblue) transactional-email API.
 * <p>
 * Endpoint: {@code POST {base-url}/v3/smtp/email} with header {@code api-key: <key>}.
 * Free tier: 300 emails/day forever.
 */
@ConfigurationProperties(prefix = "atlassync.otp.delivery.brevo")
public record BrevoProperties(
        String baseUrl,
        String apiKey,
        String fromEmail,
        String fromName,
        String subject
) {

    public BrevoProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.brevo.com";
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = "AtlasSync";
        }
        if (subject == null || subject.isBlank()) {
            subject = "Your AtlasSync verification code";
        }
        requireNonBlank(apiKey,    "atlassync.otp.delivery.brevo.api-key is required");
        requireNonBlank(fromEmail, "atlassync.otp.delivery.brevo.from-email is required");
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
