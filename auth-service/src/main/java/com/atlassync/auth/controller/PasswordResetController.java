package com.atlassync.auth.controller;

import com.atlassync.auth.dto.AuthResponse;
import com.atlassync.auth.dto.PasswordResetConfirmRequest;
import com.atlassync.auth.dto.PasswordResetRequest;
import com.atlassync.auth.dto.PasswordResetSentResponse;
import com.atlassync.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Forgot-password endpoints. Both routes are open at the gateway — the user is
 * unauthenticated by definition while resetting. The {@code request} endpoint
 * returns the same response shape whether or not the email exists, to avoid
 * leaking which emails are registered.
 */
@RestController
@RequestMapping("/api/auth/password/reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/request")
    public ResponseEntity<PasswordResetSentResponse> request(
            @RequestBody @Valid PasswordResetRequest request) {
        var result = passwordResetService.request(request.email());
        return ResponseEntity.accepted().body(new PasswordResetSentResponse(
                result.resendCooldownSeconds(),
                result.expiresInSeconds()
        ));
    }

    @PostMapping("/confirm")
    public ResponseEntity<AuthResponse> confirm(
            @RequestBody @Valid PasswordResetConfirmRequest request) {
        AuthResponse response = passwordResetService.confirm(
                request.email(), request.code(), request.newPassword());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
