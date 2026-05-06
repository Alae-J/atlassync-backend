package com.atlassync.auth.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Brevo (formerly Sendinblue) configuration for transactional emails. Uses the same
 * Brevo account as the OTP delivery channel; that's fine — Brevo doesn't care which
 * client uses the API key.
 */
@ConfigurationProperties(prefix = "atlassync.email.brevo")
public record BrevoEmailProperties(
        String baseUrl,
        String apiKey,
        String fromEmail,
        String fromName
) {

    public BrevoEmailProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.brevo.com";
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = "AtlasSync";
        }
    }

    /** {@code true} when the API key is configured and emails can actually be delivered. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && fromEmail != null && !fromEmail.isBlank();
    }
}
