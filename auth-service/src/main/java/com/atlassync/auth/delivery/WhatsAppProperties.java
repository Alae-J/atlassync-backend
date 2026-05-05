package com.atlassync.auth.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the WhatsApp Cloud API (Meta).
 * <p>
 * Sends OTPs via a pre-approved template. Meta supports two relevant template categories:
 * <ul>
 *   <li><strong>Authentication</strong> — purpose-built for OTP, includes an auto-fill / copy
 *       button that consumes the same parameter as the body. Set {@link #includeOtpButton()}
 *       to {@code true} (the default).</li>
 *   <li><strong>Utility</strong> — a generic template with one body parameter; no auto-fill
 *       button. Useful as a fallback when running on a Test WABA, which cannot create
 *       custom Authentication templates. Set {@link #includeOtpButton()} to {@code false}.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "atlassync.otp.delivery.whatsapp")
public record WhatsAppProperties(
        String baseUrl,
        String phoneNumberId,
        String accessToken,
        String templateName,
        String languageCode,
        Boolean includeOtpButton
) {

    public WhatsAppProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://graph.facebook.com/v20.0";
        }
        if (languageCode == null || languageCode.isBlank()) {
            languageCode = "en";
        }
        if (includeOtpButton == null) {
            includeOtpButton = Boolean.TRUE;
        }
        requireNonBlank(phoneNumberId, "atlassync.otp.delivery.whatsapp.phone-number-id is required");
        requireNonBlank(accessToken,   "atlassync.otp.delivery.whatsapp.access-token is required");
        requireNonBlank(templateName,  "atlassync.otp.delivery.whatsapp.template-name is required");
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
