package com.atlassync.auth.email;

/**
 * Outbound transactional emails (welcome, password reset, etc.). Distinct from
 * OTP delivery — OTPs go through {@code OtpDeliveryChannel}.
 */
public interface EmailService {

    /**
     * Welcomes a freshly-registered user. Fire-and-forget; failures are logged, never thrown.
     */
    void sendWelcome(String to, String name);
}
