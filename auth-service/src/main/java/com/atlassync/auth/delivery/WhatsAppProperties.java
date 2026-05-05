package com.atlassync.auth.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the WhatsApp Cloud API (Meta).
 * <p>
 * The flow uses pre-approved <strong>Authentication templates</strong>: the OTP code is passed
 * as a single body parameter. Create the template via Meta Business Suite and reference it by
 * name in {@link #templateName()}.
 */
@ConfigurationProperties(prefix = "atlassync.otp.delivery.whatsapp")
public record WhatsAppProperties(
        String baseUrl,
        String phoneNumberId,
        String accessToken,
        String templateName,
        String languageCode
) {

    public WhatsAppProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://graph.facebook.com/v20.0";
        }
        if (languageCode == null || languageCode.isBlank()) {
            languageCode = "en";
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
