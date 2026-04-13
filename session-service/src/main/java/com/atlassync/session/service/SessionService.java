package com.atlassync.session.service;

import com.atlassync.session.dto.*;
import com.atlassync.session.entity.*;
import com.atlassync.session.exception.*;
import com.atlassync.session.repository.QrTokenRepository;
import com.atlassync.session.repository.SessionRepository;
import com.atlassync.session.repository.SessionStateTransitionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionStateTransitionRepository transitionRepository;
    private final QrTokenRepository qrTokenRepository;
    private final QrSigningService qrSigningService;
    private final SessionEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Transactional
    public StartSessionResponse startSession(Long userId, Long storeId) {
        ShoppingSession session = new ShoppingSession();
        session.setUserId(userId);
        session.setStoreId(storeId != null ? storeId : 1L);
        session.setStatus(SessionStatus.CREATED);
        session.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        session = sessionRepository.save(session);

        QrData entryQr = generateQr(session, QrTokenType.ENTRY);

        log.info("Started session={} for user={}", session.getId(), userId);
        return new StartSessionResponse(session.getId(), session.getStatus().name(), entryQr);
    }

    @Transactional
    public SessionResponse activateSession(String correlationId) {
        QrToken qrToken = findAndValidateQrToken(correlationId);
        ShoppingSession session = qrToken.getSession();

        qrToken.setUsedAt(Instant.now());
        qrToken.setStatus(QrStatus.USED);
        qrTokenRepository.save(qrToken);

        SessionStatus fromState = session.getStatus();
        session.transitionTo(SessionStatus.ACTIVE);
        sessionRepository.save(session);

        recordTransition(session, fromState, SessionStatus.ACTIVE, "gate-entry", null);

        log.info("Activated session={}", session.getId());
        return toSessionResponse(session);
    }

    @Transactional
    public SessionResponse initiatePayment(UUID sessionId, Long userId) {
        ShoppingSession session = findSessionOrThrow(sessionId);
        verifyOwnership(session, userId);

        SessionStatus fromState = session.getStatus();
        session.transitionTo(SessionStatus.PAYING);
        sessionRepository.save(session);

        recordTransition(session, fromState, SessionStatus.PAYING, "user:" + userId, null);

        log.info("Payment initiated for session={}", sessionId);
        return toSessionResponse(session);
    }

    @Transactional
    public PaymentResponse completePayment(UUID sessionId, Long userId) {
        ShoppingSession session = findSessionOrThrow(sessionId);
        verifyOwnership(session, userId);

        SessionStatus fromState = session.getStatus();
        session.transitionTo(SessionStatus.COMPLETED);
        sessionRepository.save(session);

        recordTransition(session, fromState, SessionStatus.COMPLETED, "payment-service", null);

        QrData exitQr = generateQr(session, QrTokenType.EXIT);

        eventProducer.publishSessionPaid(session.getId(), session.getUserId());
        eventProducer.publishSessionCompleted(session.getId());

        log.info("Payment completed for session={}, exit QR generated", sessionId);
        return new PaymentResponse(session.getId(), session.getStatus().name(), exitQr);
    }

    @Transactional
    public GateValidationResponse validateExitQr(String correlationId) {
        try {
            QrToken qrToken = findAndValidateQrToken(correlationId);
            ShoppingSession session = qrToken.getSession();

            if (session.getStatus() != SessionStatus.COMPLETED) {
                return new GateValidationResponse(false, session.getId(),
                        "Session is not in COMPLETED state");
            }

            qrToken.setUsedAt(Instant.now());
            qrToken.setStatus(QrStatus.USED);
            qrTokenRepository.save(qrToken);

            log.info("Exit QR validated for session={}", session.getId());
            return new GateValidationResponse(true, session.getId(), "Exit authorized");
        } catch (InvalidQrTokenException | QrTokenExpiredException e) {
            return new GateValidationResponse(false, null, e.getMessage());
        }
    }

    @Transactional
    public SessionResponse cancelSession(UUID sessionId, Long userId) {
        ShoppingSession session = findSessionOrThrow(sessionId);
        verifyOwnership(session, userId);

        SessionStatus fromState = session.getStatus();
        session.transitionTo(SessionStatus.CANCELLED);
        sessionRepository.save(session);

        recordTransition(session, fromState, SessionStatus.CANCELLED, "user:" + userId, "User cancelled");

        log.info("Session={} cancelled by user={}", sessionId, userId);
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId) {
        ShoppingSession session = findSessionOrThrow(sessionId);
        return toSessionResponse(session);
    }

    /**
     * Generate an exit QR token for a completed session (used by gRPC service).
     */
    @Transactional
    public QrData generateExitQrForGrpc(ShoppingSession session) {
        return generateQr(session, QrTokenType.EXIT);
    }

    // --- Internal helpers ---

    private QrData generateQr(ShoppingSession session, QrTokenType type) {
        String correlationId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

        String payload = buildQrPayload(session.getId(), session.getUserId(), type, expiresAt);
        String signature = qrSigningService.sign(payload);

        QrToken qrToken = new QrToken();
        qrToken.setCorrelationId(correlationId);
        qrToken.setSession(session);
        qrToken.setTokenType(type);
        qrToken.setPayload(payload);
        qrToken.setVaultSignature(signature);
        qrToken.setKeyVersion(1);
        qrToken.setExpiresAt(expiresAt);
        qrToken.setStatus(QrStatus.ACTIVE);
        qrTokenRepository.save(qrToken);

        return new QrData(correlationId, payload, signature, expiresAt);
    }

    private String buildQrPayload(UUID sessionId, Long userId, QrTokenType type, Instant expiresAt) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "sessionId", sessionId.toString(),
                    "userId", userId,
                    "type", type.name(),
                    "expiresAt", expiresAt.toString()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize QR payload", e);
        }
    }

    private QrToken findAndValidateQrToken(String correlationId) {
        QrToken qrToken = qrTokenRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new InvalidQrTokenException(
                        "QR token not found for correlationId=" + correlationId));

        if (qrToken.getStatus() != QrStatus.ACTIVE) {
            throw new InvalidQrTokenException("QR token is not active, status=" + qrToken.getStatus());
        }

        if (Instant.now().isAfter(qrToken.getExpiresAt())) {
            qrToken.setStatus(QrStatus.EXPIRED);
            qrTokenRepository.save(qrToken);
            throw new QrTokenExpiredException("QR token has expired");
        }

        if (!qrSigningService.verify(qrToken.getPayload(), qrToken.getVaultSignature())) {
            throw new InvalidQrTokenException("QR token signature verification failed");
        }

        return qrToken;
    }

    private ShoppingSession findSessionOrThrow(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(
                        "Session not found: " + sessionId));
    }

    private void verifyOwnership(ShoppingSession session, Long userId) {
        if (!session.getUserId().equals(userId)) {
            throw new SessionNotFoundException("Session not found: " + session.getId());
        }
    }

    private void recordTransition(ShoppingSession session, SessionStatus from, SessionStatus to,
                                  String triggeredBy, String reason) {
        SessionStateTransition transition = new SessionStateTransition();
        transition.setSession(session);
        transition.setFromState(from);
        transition.setToState(to);
        transition.setTriggeredBy(triggeredBy);
        transition.setReason(reason);
        transitionRepository.save(transition);
    }

    private SessionResponse toSessionResponse(ShoppingSession session) {
        return new SessionResponse(
                session.getId(),
                session.getUserId(),
                session.getStatus().name(),
                session.getCreatedAt()
        );
    }
}
