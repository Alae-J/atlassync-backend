package com.atlassync.auth.service;

import com.atlassync.auth.config.OtpProperties;
import com.atlassync.auth.delivery.OtpDelivery;
import com.atlassync.auth.delivery.OtpDeliveryChannel;
import com.atlassync.auth.dto.AuthResponse;
import com.atlassync.auth.dto.OtpRequestResponse;
import com.atlassync.auth.entity.OtpChallenge;
import com.atlassync.auth.entity.OtpChallengeStatus;
import com.atlassync.auth.entity.Role;
import com.atlassync.auth.entity.User;
import com.atlassync.auth.exception.OtpChallengeExpiredException;
import com.atlassync.auth.exception.OtpInvalidCodeException;
import com.atlassync.auth.exception.OtpRateLimitedException;
import com.atlassync.auth.ratelimit.InMemoryRateLimiter;
import com.atlassync.auth.ratelimit.RateLimiter;
import com.atlassync.auth.repository.OtpChallengeRepository;
import com.atlassync.auth.repository.RoleRepository;
import com.atlassync.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceTest {

    private OtpProperties properties;
    private FakeChallengeRepo challengeRepo;
    private FakeUserRepo userRepo;
    private RoleRepository roleRepo;
    private RateLimiter rateLimiter;
    private RecordingDeliveryChannel deliveryChannel;
    private AuthService authService;
    private OtpService service;

    @BeforeEach
    void setUp() {
        properties = new OtpProperties(
                6,
                Duration.ofMinutes(5),
                Duration.ofSeconds(30),
                3,
                new OtpProperties.RateLimit(3, Duration.ofMinutes(15))
        );
        challengeRepo = new FakeChallengeRepo();
        userRepo = new FakeUserRepo();
        rateLimiter = new InMemoryRateLimiter();
        deliveryChannel = new RecordingDeliveryChannel();

        roleRepo = mock(RoleRepository.class);
        Role customer = new Role();
        customer.setName("ROLE_CUSTOMER");
        when(roleRepo.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customer));

        authService = mock(AuthService.class);
        when(authService.issueTokensFor(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    return new AuthResponse("a", "r", u.getId(), u.getEmail(), "ROLE_CUSTOMER");
                });

        service = new OtpService(
                challengeRepo, userRepo, roleRepo, deliveryChannel, rateLimiter, authService, properties
        );
    }

    @Test
    void requestCreatesChallengeAndSendsSms() {
        OtpRequestResponse response = service.requestForPhone("+14155551111");

        assertThat(response.correlationId()).isNotNull();
        assertThat(response.expiresInSeconds()).isEqualTo(300L);
        assertThat(deliveryChannel.sentTo).containsExactly("+14155551111");
        assertThat(deliveryChannel.lastDelivery.code()).hasSize(6);
        assertThat(deliveryChannel.lastDelivery.displayMessage()).containsPattern("\\d{6}");
        assertThat(challengeRepo.findById(response.correlationId()))
                .hasValueSatisfying(c -> {
                    assertThat(c.getStatus()).isEqualTo(OtpChallengeStatus.PENDING);
                    assertThat(c.getCodeHash()).isNotBlank();
                });
    }

    @Test
    void requestExpiresPriorPendingChallengesForSamePhone() {
        OtpRequestResponse first = service.requestForPhone("+14155551111");
        service.requestForPhone("+14155551111");

        OtpChallenge stale = challengeRepo.findById(first.correlationId()).orElseThrow();
        assertThat(stale.getStatus()).isEqualTo(OtpChallengeStatus.EXPIRED);
    }

    @Test
    void requestRateLimitsRepeatedCalls() {
        for (int i = 0; i < 3; i++) service.requestForPhone("+14155552222");
        assertThatThrownBy(() -> service.requestForPhone("+14155552222"))
                .isInstanceOf(OtpRateLimitedException.class);
    }

    @Test
    void verifyAcceptsCorrectCodeAndCreatesUser() {
        OtpRequestResponse req = service.requestForPhone("+14155553333");
        String code = deliveryChannel.lastCode();

        AuthResponse auth = service.verify(req.correlationId(), code);

        assertThat(auth.accessToken()).isEqualTo("a");
        OtpChallenge consumed = challengeRepo.findById(req.correlationId()).orElseThrow();
        assertThat(consumed.getStatus()).isEqualTo(OtpChallengeStatus.CONSUMED);
        assertThat(userRepo.findByPhone("+14155553333")).isPresent();
        verify(authService).issueTokensFor(any(User.class));
    }

    @Test
    void verifyReusesExistingUserOnRepeatLogin() {
        User existing = new User();
        existing.setPhone("+14155554444");
        existing.setRoles(Set.of(roleRepo.findByName("ROLE_CUSTOMER").orElseThrow()));
        userRepo.save(existing);

        OtpRequestResponse req = service.requestForPhone("+14155554444");
        service.verify(req.correlationId(), deliveryChannel.lastCode());

        assertThat(userRepo.savedCount).isEqualTo(1);
    }

    @Test
    void verifyRejectsBadCode() {
        OtpRequestResponse req = service.requestForPhone("+14155555555");

        assertThatThrownBy(() -> service.verify(req.correlationId(), "000000"))
                .isInstanceOf(OtpInvalidCodeException.class);

        OtpChallenge stillPending = challengeRepo.findById(req.correlationId()).orElseThrow();
        assertThat(stillPending.getStatus()).isEqualTo(OtpChallengeStatus.PENDING);
        assertThat(stillPending.getAttempts()).isEqualTo(1);
    }

    @Test
    void verifyFailsAfterMaxAttempts() {
        OtpRequestResponse req = service.requestForPhone("+14155556666");
        for (int i = 0; i < properties.maxAttempts(); i++) {
            try { service.verify(req.correlationId(), "000000"); } catch (OtpInvalidCodeException ignored) {}
        }
        assertThatThrownBy(() -> service.verify(req.correlationId(), "000000"))
                .isInstanceOf(OtpInvalidCodeException.class);

        OtpChallenge ch = challengeRepo.findById(req.correlationId()).orElseThrow();
        assertThat(ch.getStatus()).isEqualTo(OtpChallengeStatus.FAILED);
    }

    @Test
    void verifyRejectsExpiredChallenge() {
        OtpRequestResponse req = service.requestForPhone("+14155557777");
        OtpChallenge ch = challengeRepo.findById(req.correlationId()).orElseThrow();
        ch.setExpiresAt(Instant.now().minusSeconds(1));
        challengeRepo.save(ch);

        assertThatThrownBy(() -> service.verify(req.correlationId(), deliveryChannel.lastCode()))
                .isInstanceOf(OtpChallengeExpiredException.class);

        OtpChallenge after = challengeRepo.findById(req.correlationId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OtpChallengeStatus.EXPIRED);
    }

    @Test
    void verifyRejectsUnknownCorrelationId() {
        assertThatThrownBy(() -> service.verify(UUID.randomUUID(), "123456"))
                .isInstanceOf(OtpInvalidCodeException.class);
    }

    // -- minimal in-memory repo fakes -----------------------------------------------------------

    private static class FakeChallengeRepo implements OtpChallengeRepository {
        private final Map<UUID, OtpChallenge> store = new HashMap<>();

        @Override public Optional<OtpChallenge> findByIdAndStatus(UUID id, OtpChallengeStatus status) {
            return Optional.ofNullable(store.get(id))
                    .filter(c -> c.getStatus() == status);
        }
        @Override public int markPendingChallengesExpired(String recipient) {
            int count = 0;
            for (OtpChallenge c : store.values()) {
                if (c.getRecipient().equals(recipient) && c.getStatus() == OtpChallengeStatus.PENDING) {
                    c.setStatus(OtpChallengeStatus.EXPIRED);
                    count++;
                }
            }
            return count;
        }
        @Override public int deleteExpiredOlderThan(Instant cutoff) { return 0; }

        @Override public <S extends OtpChallenge> S save(S entity) {
            if (entity.getId() == null) entity.setId(UUID.randomUUID());
            store.put(entity.getId(), entity);
            return entity;
        }
        @Override public Optional<OtpChallenge> findById(UUID id) { return Optional.ofNullable(store.get(id)); }

        // unused JpaRepository methods --------------------------------------
        @Override public List<OtpChallenge> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<OtpChallenge> findAll(org.springframework.data.domain.Sort sort) { return findAll(); }
        @Override public org.springframework.data.domain.Page<OtpChallenge> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public List<OtpChallenge> findAllById(Iterable<UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public <S extends OtpChallenge> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(UUID id) { return store.containsKey(id); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(UUID id) { store.remove(id); }
        @Override public void delete(OtpChallenge entity) { store.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends OtpChallenge> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override public <S extends OtpChallenge> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends OtpChallenge> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<OtpChallenge> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public OtpChallenge getOne(UUID id) { return store.get(id); }
        @Override public OtpChallenge getById(UUID id) { return store.get(id); }
        @Override public OtpChallenge getReferenceById(UUID id) { return store.get(id); }
        @Override public <S extends OtpChallenge> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends OtpChallenge> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends OtpChallenge> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends OtpChallenge> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends OtpChallenge> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends OtpChallenge> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends OtpChallenge, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }

    private static class FakeUserRepo implements UserRepository {
        private final Map<Long, User> byId = new HashMap<>();
        private final AtomicLong sequence = new AtomicLong();
        int savedCount = 0;

        @Override public Optional<User> findByEmail(String email) {
            return byId.values().stream().filter(u -> email.equals(u.getEmail())).findFirst();
        }
        @Override public Optional<User> findByPhone(String phone) {
            return byId.values().stream().filter(u -> phone.equals(u.getPhone())).findFirst();
        }
        @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
        @Override public boolean existsByUsername(String username) {
            return byId.values().stream().anyMatch(u -> username.equals(u.getUsername()));
        }
        @Override public boolean existsByPhone(String phone) { return findByPhone(phone).isPresent(); }
        @Override public <S extends User> S save(S entity) {
            if (entity.getId() == null) entity.setId(sequence.incrementAndGet());
            byId.put(entity.getId(), entity);
            savedCount++;
            return entity;
        }
        @Override public Optional<User> findById(Long id) { return Optional.ofNullable(byId.get(id)); }

        // unused JpaRepository methods --------------------------------------
        @Override public List<User> findAll() { return new ArrayList<>(byId.values()); }
        @Override public List<User> findAll(org.springframework.data.domain.Sort sort) { return findAll(); }
        @Override public org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public List<User> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(Long id) { return byId.containsKey(id); }
        @Override public long count() { return byId.size(); }
        @Override public void deleteById(Long id) { byId.remove(id); }
        @Override public void delete(User entity) { byId.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends User> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { byId.clear(); }
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<User> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { byId.clear(); }
        @Override public User getOne(Long id) { return byId.get(id); }
        @Override public User getById(Long id) { return byId.get(id); }
        @Override public User getReferenceById(Long id) { return byId.get(id); }
        @Override public <S extends User> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends User> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends User> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends User, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }

    private static class RecordingDeliveryChannel implements OtpDeliveryChannel {
        final List<String> sentTo = new ArrayList<>();
        OtpDelivery lastDelivery;

        @Override public void deliver(OtpDelivery delivery) {
            sentTo.add(delivery.recipient());
            lastDelivery = delivery;
        }

        String lastCode() {
            if (lastDelivery == null) throw new IllegalStateException("No delivery captured");
            return lastDelivery.code();
        }
    }
}
