package com.atlassync.session.service;

import com.atlassync.session.dto.*;
import com.atlassync.session.entity.*;
import com.atlassync.session.exception.*;
import com.atlassync.session.integration.CartSnapshotClient;
import com.atlassync.session.payment.StripeCustomerService;
import com.atlassync.session.payment.StripeService;
import com.atlassync.session.repository.QrTokenRepository;
import com.atlassync.session.repository.SessionLineItemRepository;
import com.atlassync.session.repository.SessionRepository;
import com.atlassync.session.repository.SessionStateTransitionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionStateTransitionRepository transitionRepository;
    private final SessionLineItemRepository lineItemRepository;
    private final QrTokenRepository qrTokenRepository;
    private final QrSigningService qrSigningService;
    private final SessionEventProducer eventProducer;
    private final CartSnapshotClient cartSnapshotClient;
    private final ObjectMapper objectMapper;
    private final StripeService stripeService;
    private final StripeCustomerService stripeCustomerService;

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

    /**
     * Creates a Stripe PaymentIntent for the session's current cart total.
     * Transitions the session into PAYING. The mobile confirms the intent
     * directly against Stripe; we wait for the webhook to mark the session
     * COMPLETED.
     *
     * <p>Amount is computed server-side from the live cart — never from the
     * client — so a tampered client can't pay 1 MAD for 200 MAD of stuff.
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntent(UUID sessionId, Long userId, String email) {
        ShoppingSession session = findSessionOrThrow(sessionId);
        verifyOwnership(session, userId);

        // CartSnapshotClient.fetch returns a CartSnapshot record with a
        // `total` (BigDecimal) field — use that directly.
        BigDecimal amount = cartSnapshotClient.fetch(sessionId).total();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalStateException(
                    "Refusing to create payment intent for empty cart");
        }

        String customerId;
        try {
            customerId = stripeCustomerService.getOrCreate(userId, email);
        } catch (StripeException e) {
            log.error("[stripe] failed to get/create customer for user={}", userId, e);
            throw new RuntimeException("Could not get Stripe customer: " + e.getMessage(), e);
        }

        SessionStatus fromState = session.getStatus();
        if (fromState != SessionStatus.PAYING) {
            session.transitionTo(SessionStatus.PAYING);
            sessionRepository.save(session);
            recordTransition(session, fromState, SessionStatus.PAYING,
                    "user:" + userId, null);
        }

        try {
            PaymentIntent intent = stripeService.createPaymentIntent(sessionId, amount, customerId);
            return new PaymentIntentResponse(
                    sessionId,
                    intent.getId(),
                    intent.getClientSecret(),
                    amount,
                    intent.getCurrency().toUpperCase());
        } catch (StripeException e) {
            log.error("[stripe] failed to create paymentIntent session={}", sessionId, e);
            throw new RuntimeException("Could not create payment intent: " + e.getMessage(), e);
        }
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
        session.setCompletedAt(Instant.now());
        snapshotCartIntoSession(session);
        sessionRepository.save(session);

        recordTransition(session, fromState, SessionStatus.COMPLETED, "payment-service", null);

        QrData exitQr = generateQr(session, QrTokenType.EXIT);

        eventProducer.publishSessionPaid(session.getId(), session.getUserId());
        eventProducer.publishSessionCompleted(session.getId());

        log.info("Payment completed for session={}, exit QR generated", sessionId);
        return new PaymentResponse(session.getId(), session.getStatus().name(), exitQr);
    }

    /**
     * Mark a session COMPLETED from the Stripe webhook. Idempotent — re-delivery
     * is a noop. Snapshots the cart, generates the exit QR, publishes the
     * session-paid / session-completed events.
     *
     * <p>This is the source of truth for "payment cleared". Do NOT trust the
     * mobile to mark sessions paid — anyone can fake an HTTP call. The webhook
     * is signature-verified by {@link StripeService#constructEvent}.
     */
    @Transactional
    public void markPaidFromWebhook(UUID sessionId) {
        ShoppingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Session not found: " + sessionId));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.info("[stripe-webhook] session={} already COMPLETED, skipping",
                    sessionId);
            return;
        }

        SessionStatus fromState = session.getStatus();
        session.transitionTo(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        snapshotCartIntoSession(session);
        sessionRepository.save(session);

        recordTransition(session, fromState, SessionStatus.COMPLETED,
                "stripe-webhook", null);

        generateQr(session, QrTokenType.EXIT);

        eventProducer.publishSessionPaid(session.getId(), session.getUserId());
        eventProducer.publishSessionCompleted(session.getId());

        log.info("[stripe-webhook] session={} marked COMPLETED, exit QR generated",
                sessionId);
    }

    /**
     * Pulls the cart for this session and freezes it into {@code session_line_items}.
     * Cart-service holds carts in Redis with a TTL, so without this snapshot the
     * receipt would vanish once the cart key expires.
     */
    private void snapshotCartIntoSession(ShoppingSession session) {
        CartSnapshotClient.CartSnapshot cart = cartSnapshotClient.fetch(session.getId());
        List<SessionLineItem> lineItems = cart.items().stream()
                .map((item) -> {
                    SessionLineItem entity = new SessionLineItem();
                    entity.setSession(session);
                    entity.setBarcode(item.productId());
                    entity.setProductName(item.productName());
                    entity.setQuantity(item.quantity());
                    entity.setUnitPrice(item.priceAtAddition());
                    entity.setLineTotal(item.priceAtAddition().multiply(BigDecimal.valueOf(item.quantity())));
                    entity.setImageUrl(item.imageUrl());
                    entity.setAddedAt(item.addedAt());
                    return entity;
                })
                .toList();
        if (!lineItems.isEmpty()) {
            lineItemRepository.saveAll(lineItems);
        }
        session.setTotalAmount(cart.total());
        session.setItemCount(cart.itemCount());
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
     * Receipt view — the session plus its frozen line items. Used by Order
     * Detail / Receipt on the mobile app.
     */
    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(UUID sessionId, Long userId) {
        ShoppingSession session = findSessionOrThrow(sessionId);
        verifyOwnership(session, userId);

        List<ReceiptResponse.LineItem> items = lineItemRepository
                .findBySessionIdOrderByIdAsc(sessionId)
                .stream()
                .map((entity) -> new ReceiptResponse.LineItem(
                        entity.getBarcode(),
                        entity.getProductName(),
                        entity.getQuantity(),
                        entity.getUnitPrice(),
                        entity.getLineTotal(),
                        entity.getImageUrl(),
                        entity.getAddedAt()
                ))
                .toList();

        return new ReceiptResponse(
                session.getId(),
                session.getUserId(),
                session.getStoreId(),
                session.getStatus().name(),
                session.getCreatedAt(),
                session.getCompletedAt(),
                session.getTotalAmount(),
                session.getItemCount(),
                items
        );
    }

    /**
     * History list for the logged-in user. Newest sessions first. Pageable so
     * the Orders tab can ask for "all" or fall back to the most recent N.
     */
    @Transactional(readOnly = true)
    public List<SessionHistoryItem> getHistory(Long userId, int limit) {
        return sessionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map((session) -> new SessionHistoryItem(
                        session.getId(),
                        session.getStoreId(),
                        session.getStatus().name(),
                        session.getCreatedAt(),
                        session.getCompletedAt(),
                        session.getTotalAmount(),
                        session.getItemCount()
                ))
                .toList();
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
        QrData exitQr = null;
        if (session.getStatus() == SessionStatus.COMPLETED) {
            exitQr = qrTokenRepository
                    .findFirstBySessionIdAndTokenTypeOrderByCreatedAtDesc(
                            session.getId(), QrTokenType.EXIT)
                    .map(token -> new QrData(
                            token.getCorrelationId(),
                            token.getPayload(),
                            token.getVaultSignature(),
                            token.getExpiresAt()))
                    .orElse(null);
        }
        return new SessionResponse(
                session.getId(),
                session.getUserId(),
                session.getStatus().name(),
                session.getCreatedAt(),
                exitQr);
    }
}
