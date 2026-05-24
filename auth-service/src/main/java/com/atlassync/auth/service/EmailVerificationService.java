package com.atlassync.auth.service;

import com.atlassync.auth.config.OtpProperties;
import com.atlassync.auth.email.EmailService;
import com.atlassync.auth.entity.OtpChallenge;
import com.atlassync.auth.entity.OtpChallengeStatus;
import com.atlassync.auth.entity.OtpPurpose;
import com.atlassync.auth.entity.User;
import com.atlassync.auth.exception.EmailAlreadyVerifiedException;
import com.atlassync.auth.exception.OtpChallengeExpiredException;
import com.atlassync.auth.exception.OtpInvalidCodeException;
import com.atlassync.auth.exception.OtpRateLimitedException;
import com.atlassync.auth.ratelimit.RateLimiter;
import com.atlassync.auth.repository.OtpChallengeRepository;
import com.atlassync.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Owns the email-verification lifecycle: bootstrap-on-register, on-demand resend,
 * and code verification. Reuses the {@code otp_challenges} table (scoped by
 * {@code purpose=EMAIL_VERIFICATION}) so we share the hashing, expiry, and rate-limit
 * machinery with the login OTP flow.
 *
 * <p>Kept separate from {@link OtpService} because the lifecycle differs: a successful
 * verify here mutates the User row instead of minting tokens, so conflating them would
 * make either path cluttered.
 */
@Service
public class EmailVerificationService {

    private static final String RATE_LIMIT_PREFIX = "email-verify:request:";

    private final OtpChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RateLimiter rateLimiter;
    private final OtpProperties properties;

    public EmailVerificationService(OtpChallengeRepository challengeRepository,
                                    UserRepository userRepository,
                                    EmailService emailService,
                                    RateLimiter rateLimiter,
                                    OtpProperties properties) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    /**
     * Mints the first verification code for a freshly-registered user and dispatches
     * the welcome email. Skipped silently if the user has no email (phone-only signup)
     * or is already verified — neither should happen at register time but the guard
     * keeps this method safe to call from anywhere.
     */
    @Transactional
    public void bootstrapForNewUser(User user) {
        if (user.getEmail() == null || user.isEmailVerified()) return;
        String code = mintCode(user.getEmail());
        emailService.sendWelcome(user.getEmail(), user.getUsername(), code, properties.ttl());
    }

    /**
     * Re-sends a verification code from the "Verify now" path. Throws if the user is
     * already verified or has no email on file.
     */
    @Transactional
    public ResendResult resend(User user) {
        requireUnverifiedEmail(user);
        String code = mintCode(user.getEmail());
        emailService.sendVerificationCode(user.getEmail(), user.getUsername(), code, properties.ttl());
        return new ResendResult(
                properties.resendCooldown().toSeconds(),
                properties.ttl().toSeconds()
        );
    }

    /**
     * Verifies the supplied code against the user's most recent pending challenge.
     * On success, flips {@code email_verified=true} and persists the timestamp.
     */
    @Transactional
    public User verify(User user, String code) {
        requireUnverifiedEmail(user);

        OtpChallenge challenge = latestPendingFor(user.getEmail())
                .orElseThrow(() -> new OtpInvalidCodeException("No verification in progress -- request a new code"));

        Instant now = Instant.now();
        if (challenge.isExpired(now)) {
            challenge.setStatus(OtpChallengeStatus.EXPIRED);
            challengeRepository.save(challenge);
            throw new OtpChallengeExpiredException("Verification code has expired -- request a new one");
        }

        if (challenge.getAttempts() >= properties.maxAttempts()) {
            challenge.setStatus(OtpChallengeStatus.FAILED);
            challengeRepository.save(challenge);
            throw new OtpInvalidCodeException("Too many incorrect attempts -- request a new code");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);
        if (!OtpCodes.constantTimeEquals(OtpCodes.hash(code), challenge.getCodeHash())) {
            challengeRepository.save(challenge);
            throw new OtpInvalidCodeException("Invalid verification code");
        }

        challenge.setStatus(OtpChallengeStatus.CONSUMED);
        challenge.setConsumedAt(now);
        challengeRepository.save(challenge);

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);
        return userRepository.save(user);
    }

    private String mintCode(String email) {
        enforceRateLimit(email);
        challengeRepository.markPendingChallengesExpired(email, OtpPurpose.EMAIL_VERIFICATION);

        String code = OtpCodes.generate(properties.codeLength());
        var challenge = new OtpChallenge();
        challenge.setRecipient(email);
        challenge.setCodeHash(OtpCodes.hash(code));
        challenge.setStatus(OtpChallengeStatus.PENDING);
        challenge.setPurpose(OtpPurpose.EMAIL_VERIFICATION);
        challenge.setExpiresAt(Instant.now().plus(properties.ttl()));
        challengeRepository.save(challenge);

        return code;
    }

    private void enforceRateLimit(String email) {
        var limit = properties.rateLimit();
        boolean ok = rateLimiter.tryAcquire(RATE_LIMIT_PREFIX + email, limit.max(), limit.window());
        if (!ok) {
            throw new OtpRateLimitedException(
                    "Too many verification requests for this email -- try again later",
                    limit.window()
            );
        }
    }

    private Optional<OtpChallenge> latestPendingFor(String email) {
        return challengeRepository.findFirstByRecipientAndStatusAndPurposeOrderByCreatedAtDesc(
                email, OtpChallengeStatus.PENDING, OtpPurpose.EMAIL_VERIFICATION);
    }

    private static void requireUnverifiedEmail(User user) {
        if (user.getEmail() == null) {
            throw new IllegalStateException("Cannot verify email -- user has no email on file");
        }
        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }
    }

    public record ResendResult(long resendCooldownSeconds, long expiresInSeconds) {}
}
