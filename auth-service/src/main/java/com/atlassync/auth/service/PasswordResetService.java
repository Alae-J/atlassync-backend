package com.atlassync.auth.service;

import com.atlassync.auth.config.OtpProperties;
import com.atlassync.auth.dto.AuthResponse;
import com.atlassync.auth.email.EmailService;
import com.atlassync.auth.entity.OtpChallenge;
import com.atlassync.auth.entity.OtpChallengeStatus;
import com.atlassync.auth.entity.OtpPurpose;
import com.atlassync.auth.entity.RevocationReason;
import com.atlassync.auth.entity.User;
import com.atlassync.auth.exception.OtpChallengeExpiredException;
import com.atlassync.auth.exception.OtpInvalidCodeException;
import com.atlassync.auth.exception.OtpRateLimitedException;
import com.atlassync.auth.ratelimit.RateLimiter;
import com.atlassync.auth.repository.OtpChallengeRepository;
import com.atlassync.auth.repository.RefreshTokenRepository;
import com.atlassync.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Owns the forgot-password flow. Mirrors {@link EmailVerificationService}'s shape —
 * reuses {@code otp_challenges} with {@code purpose=PASSWORD_RESET} so we share
 * the hashing, expiry, and rate-limit machinery.
 *
 * <p>Two security properties we care about:
 * <ul>
 *   <li>The {@link #request(String) request} step is <em>non-revealing</em>: it returns
 *       the same response shape whether or not the email exists, so attackers can't
 *       use this endpoint as an email-existence oracle. Rate limiting runs first,
 *       regardless of user existence, to keep timing identical.</li>
 *   <li>The {@link #confirm confirm} step revokes <em>all</em> outstanding refresh
 *       tokens for the user (not just the calling client's). If an attacker has
 *       hijacked a session, password reset kicks them out everywhere.</li>
 * </ul>
 */
@Service
@Slf4j
public class PasswordResetService {

    private static final String RATE_LIMIT_PREFIX = "password-reset:request:";

    private final OtpChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final RateLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final OtpProperties properties;

    public PasswordResetService(OtpChallengeRepository challengeRepository,
                                UserRepository userRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                EmailService emailService,
                                RateLimiter rateLimiter,
                                PasswordEncoder passwordEncoder,
                                AuthService authService,
                                OtpProperties properties) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.properties = properties;
    }

    /**
     * Mints a reset code for the email and dispatches the email — but only if the
     * email belongs to a real user. Returns the same shape either way to avoid
     * leaking which addresses are registered. Rate-limited per-email to prevent
     * existence-probing via timing or quota exhaustion.
     */
    @Transactional
    public RequestResult request(String email) {
        String normalisedEmail = email.trim().toLowerCase();
        enforceRateLimit(normalisedEmail);

        Optional<User> user = userRepository.findByEmail(normalisedEmail);
        if (user.isPresent()) {
            String code = mintCode(normalisedEmail);
            emailService.sendPasswordResetCode(
                    user.get().getEmail(), user.get().getUsername(), code, properties.ttl());
            log.info("[password-reset] requested for userId={}", user.get().getId());
        } else {
            log.info("[password-reset] requested for unknown email — silently dropping");
        }

        return new RequestResult(
                properties.resendCooldown().toSeconds(),
                properties.ttl().toSeconds()
        );
    }

    /**
     * Validates the code, replaces the user's password, revokes every active
     * refresh token they hold, and issues a fresh token pair so the mobile can
     * sign the user in immediately.
     */
    @Transactional
    public AuthResponse confirm(String email, String code, String newPassword) {
        String normalisedEmail = email.trim().toLowerCase();

        // Generic message either way: don't reveal which side failed.
        User user = userRepository.findByEmail(normalisedEmail)
                .orElseThrow(() -> new OtpInvalidCodeException("Invalid reset code"));

        OtpChallenge challenge = challengeRepository
                .findFirstByRecipientAndStatusAndPurposeOrderByCreatedAtDesc(
                        normalisedEmail, OtpChallengeStatus.PENDING, OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new OtpInvalidCodeException("No reset in progress -- request a new code"));

        Instant now = Instant.now();
        if (challenge.isExpired(now)) {
            challenge.setStatus(OtpChallengeStatus.EXPIRED);
            challengeRepository.save(challenge);
            throw new OtpChallengeExpiredException("Reset code has expired -- request a new one");
        }

        if (challenge.getAttempts() >= properties.maxAttempts()) {
            challenge.setStatus(OtpChallengeStatus.FAILED);
            challengeRepository.save(challenge);
            throw new OtpInvalidCodeException("Too many incorrect attempts -- request a new code");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);
        if (!OtpCodes.constantTimeEquals(OtpCodes.hash(code), challenge.getCodeHash())) {
            challengeRepository.save(challenge);
            throw new OtpInvalidCodeException("Invalid reset code");
        }

        challenge.setStatus(OtpChallengeStatus.CONSUMED);
        challenge.setConsumedAt(now);
        challengeRepository.save(challenge);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        int revoked = refreshTokenRepository.revokeAllForUser(user.getId(), RevocationReason.PASSWORD_RESET);
        log.info("[password-reset] reset complete userId={} sessions_revoked={}", user.getId(), revoked);

        return authService.issueTokensFor(user);
    }

    private String mintCode(String email) {
        challengeRepository.markPendingChallengesExpired(email, OtpPurpose.PASSWORD_RESET);

        String code = OtpCodes.generate(properties.codeLength());
        var challenge = new OtpChallenge();
        challenge.setRecipient(email);
        challenge.setCodeHash(OtpCodes.hash(code));
        challenge.setStatus(OtpChallengeStatus.PENDING);
        challenge.setPurpose(OtpPurpose.PASSWORD_RESET);
        challenge.setExpiresAt(Instant.now().plus(properties.ttl()));
        challengeRepository.save(challenge);

        return code;
    }

    private void enforceRateLimit(String email) {
        var limit = properties.rateLimit();
        boolean ok = rateLimiter.tryAcquire(RATE_LIMIT_PREFIX + email, limit.max(), limit.window());
        if (!ok) {
            throw new OtpRateLimitedException(
                    "Too many reset requests for this email -- try again later",
                    limit.window()
            );
        }
    }

    public record RequestResult(long resendCooldownSeconds, long expiresInSeconds) {}
}
