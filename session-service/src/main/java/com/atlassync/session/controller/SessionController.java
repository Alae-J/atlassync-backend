package com.atlassync.session.controller;

import com.atlassync.session.dto.*;
import com.atlassync.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<StartSessionResponse> startSession(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestBody(required = false) StartSessionRequest request) {

        Long userId = headerUserId != null ? headerUserId : paramUserId;
        if (userId == null) {
            throw new IllegalArgumentException("userId is required via X-User-Id header or userId param");
        }

        Long storeId = request != null ? request.storeId() : null;
        StartSessionResponse response = sessionService.startSession(userId, storeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable UUID id) {
        return ResponseEntity.ok(sessionService.getSession(id));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponse> completePayment(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId) {

        Long userId = headerUserId != null ? headerUserId : paramUserId;
        if (userId == null) {
            throw new IllegalArgumentException("userId is required via X-User-Id header or userId param");
        }

        sessionService.initiatePayment(id, userId);
        PaymentResponse response = sessionService.completePayment(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelSession(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId) {

        Long userId = headerUserId != null ? headerUserId : paramUserId;
        if (userId == null) {
            throw new IllegalArgumentException("userId is required via X-User-Id header or userId param");
        }

        sessionService.cancelSession(id, userId);
    }
}
