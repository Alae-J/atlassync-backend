package com.atlassync.session.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result of an admin-initiated refund. The session's status flips via the
 * webhook (charge.refunded) — this response just confirms the refund was
 * accepted by Stripe.
 */
public record RefundResponse(
        UUID sessionId,
        String stripeRefundId,
        BigDecimal amountRefunded,
        String currency,
        String status
) {}
