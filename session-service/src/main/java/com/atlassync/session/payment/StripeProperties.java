package com.atlassync.session.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stripe Test Mode configuration. All three values are env-var driven so we
 * never commit real keys. {@code apiKey} is the server secret key
 * (sk_test_...), used to authenticate outbound Stripe API calls.
 * {@code webhookSecret} (whsec_...) signs incoming webhook payloads — we
 * verify it on every webhook request to be sure the call actually came from
 * Stripe and not an attacker. {@code currency} stays MAD for the Morocco
 * demo.
 */
@ConfigurationProperties(prefix = "atlassync.stripe")
public record StripeProperties(
        String apiKey,
        String webhookSecret,
        String currency
) {
    public StripeProperties {
        if (currency == null || currency.isBlank()) currency = "mad";
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
