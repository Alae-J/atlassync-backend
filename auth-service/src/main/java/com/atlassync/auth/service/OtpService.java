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

    /**
     * Issues an OTP for a phone number. The user is created on first verify.
     */
    @Transactional
    public OtpRequestResponse requestForPhone(String phone) {
        return issueChallenge(phone);
    }

    /**
     * Issues an OTP for an email address. The user is created on first verify.
     */
    @Transactional
    public OtpRequestResponse requestForEmail(String email) {
        return issueChallenge(email);
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

        User user = findOrCreateUser(challenge.getRecipient());
        return authService.issueTokensFor(user);
    }

    private OtpRequestResponse issueChallenge(String recipient) {
        enforceRateLimit(recipient);
        challengeRepository.markPendingChallengesExpired(recipient);

        String code = generateCode(properties.codeLength());
        Instant now = Instant.now();

        var challenge = new OtpChallenge();
        challenge.setRecipient(recipient);
        challenge.setCodeHash(hash(code));
        challenge.setStatus(OtpChallengeStatus.PENDING);
        challenge.setExpiresAt(now.plus(properties.ttl()));
        challenge = challengeRepository.save(challenge);

        deliveryChannel.deliver(new OtpDelivery(recipient, code, formatMessage(code)));

        return new OtpRequestResponse(
                challenge.getId(),
                properties.resendCooldown().toSeconds(),
                properties.ttl().toSeconds()
        );
    }

    private void enforceRateLimit(String recipient) {
        var limit = properties.rateLimit();
        boolean ok = rateLimiter.tryAcquire(RATE_LIMIT_PREFIX + recipient, limit.max(), limit.window());
        if (!ok) {
            throw new OtpRateLimitedException(
                    "Too many OTP requests for this recipient -- try again later",
                    limit.window()
            );
        }
    }

    private User findOrCreateUser(String recipient) {
        boolean isEmail = recipient.contains("@");
        var existing = isEmail
                ? userRepository.findByEmail(recipient)
                : userRepository.findByPhone(recipient);

        return existing.orElseGet(() -> {
            var role = roleRepository.findByName(DEFAULT_ROLE)
                    .orElseThrow(() -> new IllegalStateException("Default role " + DEFAULT_ROLE + " missing"));
            var user = new User();
            if (isEmail) user.setEmail(recipient);
            else user.setPhone(recipient);
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

    private String formatMessage(String code) {
        Duration ttl = properties.ttl();
        return "Your AtlasSync code is " + code + ". Expires in " + ttl.toMinutes() + " minutes.";
    }
}
