package com.atlassync.auth.service;

import com.atlassync.auth.config.OtpProperties;
import com.atlassync.auth.delivery.OtpDelivery;
import com.atlassync.auth.delivery.OtpDeliveryChannel;
import com.atlassync.auth.entity.OtpChallenge;
import com.atlassync.auth.entity.OtpChallengeStatus;
import com.atlassync.auth.entity.OtpPurpose;
import com.atlassync.auth.entity.User;
import com.atlassync.auth.exception.DuplicateResourceException;
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
 * Links a phone number to an already-authenticated user account. Shares the
 * OTP table + delivery channel with login-OTP, but tags challenges with
 * {@code purpose=PHONE_VERIFICATION} so the lifecycles don't collide —
 * a successful verify attaches the phone to the user instead of minting
 * session tokens.
 */
@Service
public class PhoneLinkService {

    private static final String RATE_LIMIT_PREFIX = "phone-link:request:";

    private final OtpChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final OtpDeliveryChannel deliveryChannel;
    private final RateLimiter rateLimiter;
    private final OtpProperties properties;

    public PhoneLinkService(OtpChallengeRepository challengeRepository,
                            UserRepository userRepository,
                            OtpDeliveryChannel deliveryChannel,
                            RateLimiter rateLimiter,
                            OtpProperties properties) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.deliveryChannel = deliveryChannel;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Transactional
    public RequestResult requestCode(User user, String phone) {
        // Phone numbers are unique per user — bail before sending a code if
        // someone else has already claimed it.
        userRepository.findByPhone(phone).ifPresent((existing) -> {
            if (!existing.getId().equals(user.getId())) {
                throw new DuplicateResourceException("Phone number already linked to another account");
            }
        });

        enforceRateLimit(phone);
        challengeRepository.markPendingChallengesExpired(phone, OtpPurpose.PHONE_VERIFICATION);

        String code = OtpCodes.generate(properties.codeLength());
        var challenge = new OtpChallenge();
        challenge.setRecipient(phone);
        challenge.setCodeHash(OtpCodes.hash(code));
        challenge.setStatus(OtpChallengeStatus.PENDING);
        challenge.setPurpose(OtpPurpose.PHONE_VERIFICATION);
        challenge.setExpiresAt(Instant.now().plus(properties.ttl()));
        challengeRepository.save(challenge);

        String message = "Your AtlasSync code is " + code + ". Expires in "
                + properties.ttl().toMinutes() + " minutes.";
        deliveryChannel.deliver(new OtpDelivery(phone, code, message));

        return new RequestResult(
                properties.resendCooldown().toSeconds(),
                properties.ttl().toSeconds()
        );
    }

    @Transactional
    public User verifyAndLink(User user, String phone, String code) {
        OtpChallenge challenge = latestPendingFor(phone)
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

        user.setPhone(phone);
        return userRepository.save(user);
    }

    private void enforceRateLimit(String phone) {
        var limit = properties.rateLimit();
        boolean ok = rateLimiter.tryAcquire(RATE_LIMIT_PREFIX + phone, limit.max(), limit.window());
        if (!ok) {
            throw new OtpRateLimitedException(
                    "Too many code requests for this phone -- try again later",
                    limit.window()
            );
        }
    }

    private Optional<OtpChallenge> latestPendingFor(String phone) {
        return challengeRepository.findFirstByRecipientAndStatusAndPurposeOrderByCreatedAtDesc(
                phone, OtpChallengeStatus.PENDING, OtpPurpose.PHONE_VERIFICATION);
    }

    public record RequestResult(long resendCooldownSeconds, long expiresInSeconds) {}
}
