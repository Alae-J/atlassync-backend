package com.atlassync.session.dto;

import jakarta.validation.constraints.Positive;

/**
 * Admin refund request. {@code amount} is in major currency units (MAD).
 * Null = full refund. Otherwise partial refund of the specified amount.
 * {@code reason} maps to Stripe's RefundReason enum (DUPLICATE, FRAUDULENT,
 * REQUESTED_BY_CUSTOMER) — unknown values are silently dropped on the
 * Stripe side.
 */
public record RefundRequest(
        @Positive Double amount,
        String reason
) {}
