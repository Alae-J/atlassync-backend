package com.atlassync.auth.delivery;

/**
 * Delivers an OTP to its recipient. Implementations might use SMS, WhatsApp,
 * email, or just log the code (dev). Selected via {@code atlassync.otp.delivery.provider}.
 */
public interface OtpDeliveryChannel {
    void deliver(OtpDelivery delivery);
}
