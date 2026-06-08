package com.atlassync.session.payment;

import com.atlassync.session.dto.RefundRequest;
import com.atlassync.session.dto.RefundResponse;
import com.atlassync.session.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only refund endpoint. Authorization is by {@code X-User-Role} forwarded
 * from the gateway — only ROLE_ADMIN or ROLE_STAFF can refund. Bypasses the
 * user-ownership check; admins refund on behalf of any user.
 *
 * <p>This kicks off the refund on Stripe; the {@code charge.refunded} webhook
 * (handled by {@link StripeWebhookController}) flips the session status to
 * REFUNDED / PARTIAL_REFUND.
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class RefundController {

    private final SessionService sessionService;

    @PostMapping("/{id}/refund")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody @Valid RefundRequest request) {
        if (!"ROLE_ADMIN".equals(role) && !"ROLE_STAFF".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(sessionService.issueRefund(id, request.amount(), request.reason()));
    }
}
