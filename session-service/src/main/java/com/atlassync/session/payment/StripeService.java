package com.atlassync.session.payment;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

/**
 * Thin wrapper around the Stripe SDK. All outbound calls flow through here so
 * tests and the rest of the codebase don't depend directly on {@code Stripe.*}
 * static state. Stripe's Java SDK is built around a process-wide
 * {@code Stripe.apiKey}, so we set it once at startup from
 * {@link StripeProperties}.
 *
 * <p>Two responsibilities:
 * <ul>
 *   <li>{@link #createPaymentIntent} — server-authoritative amount, returns
 *       the {@code client_secret} the mobile uses to confirm against
 *       Stripe directly.</li>
 *   <li>{@link #constructEvent} — verifies the signature on an inbound
 *       webhook payload so we can trust the event's source.</li>
 * </ul>
 */
@Service
@Slf4j
public class StripeService {

    private final StripeProperties properties;

    public StripeService(StripeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (!properties.isConfigured()) {
            log.warn("[stripe] no API key configured — payment intents will fail");
            return;
        }
        Stripe.apiKey = properties.apiKey();
        log.info("[stripe] initialised with key prefix={}", properties.apiKey().substring(0, 8));
    }

    /**
     * Creates a PaymentIntent for the given session. Amount is in the smallest
     * currency unit (centimes for MAD). The {@code session_id} is stamped on
     * {@code metadata} so the webhook can route the event back to the right
     * session without trusting the client.
     *
     * <p>{@code customerId} is the Stripe {@code cus_…} id. Pass {@code null}
     * for users without a customer record yet; the intent will still be created
     * but won't be attached to a Stripe Customer.
     */
    public PaymentIntent createPaymentIntent(
            UUID sessionId,
            BigDecimal amount,
            String customerId,
            String idempotencyKey) throws StripeException {

        long amountMinor = amount.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(amountMinor)
                .setCurrency(properties.currency())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .putExtraParam("automatic_tax", Map.of("enabled", true))
                .putMetadata("session_id", sessionId.toString())
                .setDescription("AtlasSync · session " + sessionId);

        if (customerId != null && !customerId.isBlank()) {
            builder.setCustomer(customerId);
        }

        RequestOptions options = idempotencyKey != null && !idempotencyKey.isBlank()
                ? RequestOptions.builder().setIdempotencyKey(idempotencyKey).build()
                : RequestOptions.getDefault();

        PaymentIntent intent = PaymentIntent.create(builder.build(), options);
        log.info("[stripe] paymentIntent created session={} customer={} idempotency={} amount={} {} id={}",
                sessionId, customerId, idempotencyKey, amountMinor, properties.currency(), intent.getId());
        return intent;
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public Refund refund(String paymentIntentId, Long amountMinor, String reason) throws StripeException {
        RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId);
        if (amountMinor != null) {
            builder.setAmount(amountMinor);
        }
        if (reason != null && !reason.isBlank()) {
            try {
                builder.setReason(RefundCreateParams.Reason.valueOf(reason.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Stripe's reason enum is narrow — fall back to no reason on unknown values.
            }
        }
        return Refund.create(builder.build());
    }

    /**
     * Verifies the {@code Stripe-Signature} header against the configured
     * webhook secret. Throws if the signature is missing or wrong — DO NOT
     * fall back to trusting unsigned events.
     */
    public Event constructEvent(String payload, String sigHeader) throws SignatureVerificationException {
        if (properties.webhookSecret() == null || properties.webhookSecret().isBlank()) {
            throw new IllegalStateException("Stripe webhook secret not configured");
        }
        return Webhook.constructEvent(payload, sigHeader, properties.webhookSecret());
    }
}
