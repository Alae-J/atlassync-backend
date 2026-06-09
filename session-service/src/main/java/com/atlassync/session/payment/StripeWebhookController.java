package com.atlassync.session.payment;

import com.atlassync.session.exception.IllegalStateTransitionException;
import com.atlassync.session.service.SessionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Dispute;
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
 * <p>Handled events: {@code payment_intent.succeeded}, {@code charge.refunded},
 * {@code charge.dispute.created}, {@code charge.dispute.closed}. Other events
 * get logged and acknowledged (200) but don't drive state.
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

        switch (event.getType()) {
            case "payment_intent.succeeded":
                return handlePaymentIntentSucceeded(event);
            case "charge.refunded":
                return handleChargeRefunded(event);
            case "charge.dispute.created":
                return handleDisputeCreated(event);
            case "charge.dispute.closed":
                return handleDisputeClosed(event);
            default:
                // Unknown event type — ack so Stripe stops retrying.
                return ResponseEntity.ok("ok");
        }
    }

    private ResponseEntity<String> handlePaymentIntentSucceeded(Event event) {
        PaymentIntent intent = unwrap(event, PaymentIntent.class);
        if (intent == null) {
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
        return ResponseEntity.ok("ok");
    }

    private ResponseEntity<String> handleChargeRefunded(Event event) {
        Charge charge = unwrap(event, Charge.class);
        if (charge == null) {
            log.warn("[stripe-webhook] charge.refunded with no charge payload");
            return ResponseEntity.ok("ignored");
        }
        try {
            sessionService.markRefundedFromWebhook(
                    charge.getPaymentIntent(),
                    charge.getAmountRefunded(),
                    charge.getAmount());
        } catch (IllegalStateException e) {
            log.warn("[stripe-webhook] refund could not be applied: {}", e.getMessage());
            return ResponseEntity.ok("not_applicable");
        } catch (IllegalStateTransitionException e) {
            log.error("[stripe-webhook] session state conflict on refund, acking: {}", e.getMessage());
            return ResponseEntity.ok("state_conflict");
        } catch (Exception e) {
            log.error("[stripe-webhook] failed to apply refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("retry");
        }
        return ResponseEntity.ok("ok");
    }

    private ResponseEntity<String> handleDisputeCreated(Event event) {
        Dispute dispute = unwrap(event, Dispute.class);
        if (dispute == null) {
            log.warn("[stripe-webhook] charge.dispute.created with no dispute payload");
            return ResponseEntity.ok("ignored");
        }
        try {
            sessionService.markDisputedFromWebhook(dispute.getPaymentIntent());
        } catch (IllegalStateException e) {
            log.warn("[stripe-webhook] dispute could not be applied: {}", e.getMessage());
            return ResponseEntity.ok("not_applicable");
        } catch (IllegalStateTransitionException e) {
            log.error("[stripe-webhook] session state conflict on dispute, acking: {}", e.getMessage());
            return ResponseEntity.ok("state_conflict");
        } catch (Exception e) {
            log.error("[stripe-webhook] failed to apply dispute", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("retry");
        }
        return ResponseEntity.ok("ok");
    }

    private ResponseEntity<String> handleDisputeClosed(Event event) {
        Dispute dispute = unwrap(event, Dispute.class);
        if (dispute == null) {
            log.warn("[stripe-webhook] charge.dispute.closed with no dispute payload");
            return ResponseEntity.ok("ignored");
        }
        boolean won = "won".equals(dispute.getStatus());
        try {
            sessionService.resolveDisputeFromWebhook(dispute.getPaymentIntent(), won);
        } catch (IllegalStateException e) {
            log.warn("[stripe-webhook] dispute resolution could not be applied: {}", e.getMessage());
            return ResponseEntity.ok("not_applicable");
        } catch (IllegalStateTransitionException e) {
            log.error("[stripe-webhook] session state conflict on dispute close, acking: {}", e.getMessage());
            return ResponseEntity.ok("state_conflict");
        } catch (Exception e) {
            log.error("[stripe-webhook] failed to resolve dispute", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("retry");
        }
        return ResponseEntity.ok("ok");
    }

    @SuppressWarnings("unchecked")
    private <T extends StripeObject> T unwrap(Event event, Class<T> type) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject obj = deserializer.getObject().orElse(null);
        if (obj == null) {
            // API version skew between Stripe events and stripe-java SDK.
            // The strict deserializer refuses unknown schemas; fall back to
            // unsafe mapping which preserves the fields we care about.
            try {
                obj = deserializer.deserializeUnsafe();
            } catch (Exception e) {
                log.warn("[stripe-webhook] could not deserialize event {} type={}: {}",
                        event.getId(), event.getType(), e.getMessage());
                return null;
            }
        }
        return type.isInstance(obj) ? (T) obj : null;
    }
}
