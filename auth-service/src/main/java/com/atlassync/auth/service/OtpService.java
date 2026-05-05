package com.atlassync.auth.service;

import com.atlassync.auth.config.OtpProperties;
import com.atlassync.auth.delivery.OtpDelivery;
import com.atlassync.auth.delivery.OtpDeliveryChannel;
import com.atlassync.auth.dto.AuthResponse;
import com.atlassync.auth.dto.OtpRequestResponse;
import com.atlassync.auth.entity.OtpChallenge;
import com.atlassync.auth.entity.OtpChallengeStatus;
import com.atlassync.auth.entity.User;
import com.atlassync.auth.exception.OtpChallengeExpiredException;
import com.atlassync.auth.exception.OtpInvalidCodeException;
import com.atlassync.auth.exception.OtpRateLimitedException;
import com.atlassync.auth.ratelimit.RateLimiter;
import com.atlassync.auth.repository.OtpChallengeRepository;
import com.atlassync.auth.repository.RoleRepository;
import com.atlassync.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
public class OtpService {

    private static final String RATE_LIMIT_PREFIX = "otp:request:";
    private static final String DEFAULT_ROLE = "ROLE_CUSTOMER";

    private final OtpChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpDeliveryChannel deliveryChannel;
    private final RateLimiter rateLimiter;
    private final AuthService authService;
    private final OtpProperties properties;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpChallengeRepository challengeRepository,
                      UserRepository userRepository,
                      RoleRepository roleRepository,
                      OtpDeliveryChannel deliveryChannel,
                      RateLimiter rateLimiter,
                      AuthService authService,
                      OtpProperties properties) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.deliveryChannel = deliveryChannel;
        this.rateLimiter = rateLimiter;
        this.authService = authService;
        this.properties = properties;
    }

    @Transactional
    public OtpRequestResponse request(String phone) {
        enforceRateLimit(phone);
        challengeRepository.markPendingChallengesExpired(phone);

        String code = generateCode(properties.codeLength());
        Instant now = Instant.now();

        var challenge = new OtpChallenge();
        challenge.setPhone(phone);
        challenge.setCodeHash(hash(code));
        challenge.setStatus(OtpChallengeStatus.PENDING);
        challenge.setExpiresAt(now.plus(properties.ttl()));
        challenge = challengeRepository.save(challenge);

        deliveryChannel.deliver(new OtpDelivery(phone, code, formatSms(code)));

        return new OtpRequestResponse(
                challenge.getId(),
                properties.resendCooldown().toSeconds(),
                properties.ttl().toSeconds()
        );
    }

    @Transactional
    public AuthResponse verify(UUID correlationId, String code) {
        var challenge = challengeRepository.findByIdAndStatus(correlationId, OtpChallengeStatus.PENDING)
                .orElseThrow(() -> new OtpInvalidCodeException("OTP challenge not found"));

        Instant now = Instant.now();
        if (challenge.isExpired(now)) {
            challenge.setStatus(OtpChallengeStatus.EXPIRED);
            challengeRepository.save(challenge);
            throw new OtpChallengeExpiredException("OTP code has expired -- request a new one");
        }

        if (challenge.getAttempts() >= properties.maxAttempts()) {
            challenge.setStatus(OtpChallengeStatus.FAILED);
            challengeRepository.save(challenge);
            throw new OtpInvalidCodeException("Too many incorrect attempts -- request a new code");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);
        if (!constantTimeEquals(hash(code), challenge.getCodeHash())) {
            challengeRepository.save(challenge);
            throw new OtpInvalidCodeException("Invalid OTP code");
        }

        challenge.setStatus(OtpChallengeStatus.CONSUMED);
        challenge.setConsumedAt(now);
        challengeRepository.save(challenge);

        User user = findOrCreateUserByPhone(challenge.getPhone());
        return authService.issueTokensFor(user);
    }

    private void enforceRateLimit(String phone) {
        var limit = properties.rateLimit();
        boolean ok = rateLimiter.tryAcquire(RATE_LIMIT_PREFIX + phone, limit.max(), limit.window());
        if (!ok) {
            throw new OtpRateLimitedException(
                    "Too many OTP requests for this phone -- try again later",
                    limit.window()
            );
        }
    }

    private User findOrCreateUserByPhone(String phone) {
        return userRepository.findByPhone(phone).orElseGet(() -> {
            var role = roleRepository.findByName(DEFAULT_ROLE)
                    .orElseThrow(() -> new IllegalStateException("Default role " + DEFAULT_ROLE + " missing"));
            var user = new User();
            user.setPhone(phone);
            user.setRoles(Set.of(role));
            return userRepository.save(user);
        });
    }

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private String formatSms(String code) {
        Duration ttl = properties.ttl();
        return "Your AtlasSync code is " + code + ". Expires in " + ttl.toMinutes() + " minutes.";
    }
}
