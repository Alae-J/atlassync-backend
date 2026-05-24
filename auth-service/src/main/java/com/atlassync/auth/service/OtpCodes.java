package com.atlassync.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Code-generation primitives shared by every OTP flow (login OTP, email verification,
 * future password resets). Kept in one place so the hashing scheme and the
 * constant-time comparison live together — easy to audit, hard to mismatch.
 */
public final class OtpCodes {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpCodes() {}

    public static String generate(int length) {
        if (length < 4 || length > 10) {
            throw new IllegalArgumentException("OTP code length must be between 4 and 10");
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    public static String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
