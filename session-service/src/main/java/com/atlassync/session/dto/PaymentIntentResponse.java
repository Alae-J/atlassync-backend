package com.atlassync.session.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Returned from {@code POST /api/sessions/{id}/pay/intent}. The mobile uses
 * {@code clientSecret} to confirm the payment against Stripe directly via
 * the Stripe React Native SDK — raw card details never touch our servers.
 * {@code amount} and {@code currency} are echoed back so the mobile can
 * sanity-check what it's about to charge.
 */
public record PaymentIntentResponse(
        UUID sessionId,
        String paymentIntentId,
        String clientSecret,
        BigDecimal amount,
        String currency
) {}
