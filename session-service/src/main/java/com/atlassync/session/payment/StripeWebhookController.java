package com.atlassync.session.payment;

import com.atlassync.session.exception.IllegalStateTransitionException;
import com.atlassync.session.service.SessionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Receives signed events from Stripe and routes them into the session
 * lifecycle. This endpoint is open at the gateway (no JWT) — Stripe doesn't
 * carry our bearer token. Trust comes from the {@code Stripe-Signature}
 * header, verified against {@code atlassync.stripe.webhook-secret}.
 *
 * <p>For local dev: run {@code stripe listen --forward-to
 * localhost:8080/api/sessions/webhooks/stripe} and feed the printed
 * {@code whsec_...} secret into the env var. In production this is a real
 * webhook endpoint configured on the Stripe dashboard.
 *
 * <p>We only act on {@code payment_intent.succeeded}. Other events
 * (created, processing, canceled, etc.) get logged and acknowledged but
 * don't drive state — we don't need them yet.
 */
@RestController
@RequestMapping("/api/sessions/webhooks")
@Slf4j
public class StripeWebhookController {

    private final StripeService stripeService;
    private final SessionService sessionService;

    public StripeWebhookController(StripeService stripeService, SessionService sessionService) {
        this.stripeService = stripeService;
        this.sessionService = sessionService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handle(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        Event event;
        try {
            event = stripeService.constructEvent(payload, signature);
        } catch (SignatureVerificationException e) {
            log.warn("[stripe-webhook] bad signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
        }

        log.info("[stripe-webhook] received type={} id={}", event.getType(), event.getId());

        if ("payment_intent.succeeded".equals(event.getType())) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject obj = deserializer.getObject().orElse(null);
            if (!(obj instanceof PaymentIntent intent)) {
                log.warn("[stripe-webhook] payment_intent.succeeded with no intent payload");
                return ResponseEntity.ok("ignored");
            }
            String sessionIdStr = intent.getMetadata() != null
                    ? intent.getMetadata().get("session_id")
                    : null;
            if (sessionIdStr == null || sessionIdStr.isBlank()) {
                log.warn("[stripe-webhook] paymentIntent={} missing session_id metadata",
                        intent.getId());
                return ResponseEntity.ok("missing metadata");
            }
            UUID sessionId;
            try {
                sessionId = UUID.fromString(sessionIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("[stripe-webhook] paymentIntent={} malformed session_id={}",
                        intent.getId(), sessionIdStr);
                return ResponseEntity.ok("bad_metadata");
            }

            try {
                sessionService.markPaidFromWebhook(sessionId);
            } catch (IllegalStateTransitionException e) {
                // The session is in an unexpected state (e.g. CANCELLED). Retrying
                // won't help — ack to stop Stripe's retry storm. Investigate via the
                // warn log instead.
                log.error("[stripe-webhook] session={} state conflict, acking to stop retries: {}",
                        sessionId, e.getMessage());
                return ResponseEntity.ok("state_conflict");
            } catch (Exception e) {
                log.error("[stripe-webhook] failed to complete session={}", sessionId, e);
                // Return 500 so Stripe retries — webhook re-delivery is fine,
                // markPaidFromWebhook is idempotent.
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("retry");
            }
        }

        return ResponseEntity.ok("ok");
    }
}
