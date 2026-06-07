package com.atlassync.auth.entity;

/**
 * What an OTP challenge is for. Both purposes share the same hashing, expiry, and
 * rate-limit machinery, but their lifecycles diverge: a LOGIN code mints session
 * tokens, an EMAIL_VERIFICATION code flips the user's verified flag.
 */
public enum OtpPurpose {
    LOGIN,
    EMAIL_VERIFICATION,
    PHONE_VERIFICATION,
    PASSWORD_RESET
}
