package com.atlassync.auth.email;

import java.time.Duration;

/**
 * Outbound transactional emails. Distinct from OTP delivery — login OTPs go through
 * {@code OtpDeliveryChannel}, this is for branded one-shot mails like welcome &
 * verification reminders.
 */
public interface EmailService {

    /**
     * Welcomes a freshly-registered user and bundles the first verification code so
     * the recipient gets one email, not two. Fire-and-forget; failures are logged.
     */
    void sendWelcome(String to, String name, String verificationCode, Duration codeTtl);

    /**
     * Re-sends a verification code from the "Verify now" path in the app. Same code,
     * different copy than the welcome email so users don't get the same message twice.
     */
    void sendVerificationCode(String to, String name, String verificationCode, Duration codeTtl);

    /**
     * Sends a one-time code for the forgot-password flow. The code is short-lived
     * and single-use; the recipient pastes it into the reset screen along with a
     * new password.
     */
    void sendPasswordResetCode(String to, String name, String resetCode, Duration codeTtl);
}
