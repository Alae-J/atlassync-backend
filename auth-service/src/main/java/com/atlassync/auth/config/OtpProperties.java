package com.atlassync.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "atlassync.otp")
public record OtpProperties(
        int codeLength,
        Duration ttl,
        Duration resendCooldown,
        int maxAttempts,
        RateLimit rateLimit
) {

    public OtpProperties {
        if (codeLength <= 0) codeLength = 6;
        if (ttl == null) ttl = Duration.ofMinutes(5);
        if (resendCooldown == null) resendCooldown = Duration.ofSeconds(30);
        if (maxAttempts <= 0) maxAttempts = 5;
        if (rateLimit == null) rateLimit = new RateLimit(3, Duration.ofMinutes(15));
    }

    public record RateLimit(int max, Duration window) {
        public RateLimit {
            if (max <= 0) max = 3;
            if (window == null) window = Duration.ofMinutes(15);
        }
    }
}
