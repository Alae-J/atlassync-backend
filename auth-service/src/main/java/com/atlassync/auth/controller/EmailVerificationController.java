package com.atlassync.auth.controller;

import com.atlassync.auth.dto.AuthResponse;
import com.atlassync.auth.dto.EmailVerificationSentResponse;
import com.atlassync.auth.dto.EmailVerifyRequest;
import com.atlassync.auth.entity.User;
import com.atlassync.auth.exception.InvalidTokenException;
import com.atlassync.auth.repository.UserRepository;
import com.atlassync.auth.service.AuthService;
import com.atlassync.auth.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Email verification endpoints. Both routes need an authenticated caller — the gateway
 * validates the JWT and forwards the resolved {@code X-User-Id} header, so this
 * controller treats that header as the trusted identity.
 */
@RestController
@RequestMapping("/api/auth/email")
public class EmailVerificationController {

    private final EmailVerificationService verificationService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public EmailVerificationController(EmailVerificationService verificationService,
                                       AuthService authService,
                                       UserRepository userRepository) {
        this.verificationService = verificationService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/send-verification")
    public ResponseEntity<EmailVerificationSentResponse> sendVerification(
            @RequestHeader("X-User-Id") Long userId) {
        var user = loadUser(userId);
        var result = verificationService.resend(user);
        return ResponseEntity.accepted().body(new EmailVerificationSentResponse(
                result.resendCooldownSeconds(),
                result.expiresInSeconds()
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verify(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid EmailVerifyRequest request) {
        var user = loadUser(userId);
        var verified = verificationService.verify(user, request.code());
        return ResponseEntity.status(HttpStatus.OK).body(authService.issueTokensFor(verified));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));
    }
}
