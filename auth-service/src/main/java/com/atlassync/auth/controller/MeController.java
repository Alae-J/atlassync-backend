package com.atlassync.auth.controller;

import com.atlassync.auth.dto.AuthResponse;
import com.atlassync.auth.dto.ChangePasswordRequest;
import com.atlassync.auth.dto.EmailVerificationSentResponse;
import com.atlassync.auth.dto.PhoneLinkRequest;
import com.atlassync.auth.dto.PhoneVerifyRequest;
import com.atlassync.auth.dto.UpdateUsernameRequest;
import com.atlassync.auth.service.AuthService;
import com.atlassync.auth.service.PhoneLinkService;
import com.atlassync.auth.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Personal-details endpoints reached from Account → Profile tab. All routes
 * require an authenticated caller — the gateway validates the JWT and
 * forwards the resolved {@code X-User-Id} header, which this controller
 * trusts as the identity.
 */
@RestController
@RequestMapping("/api/auth/me")
public class MeController {

    private final UserAccountService userAccountService;
    private final PhoneLinkService phoneLinkService;
    private final AuthService authService;

    public MeController(UserAccountService userAccountService,
                        PhoneLinkService phoneLinkService,
                        AuthService authService) {
        this.userAccountService = userAccountService;
        this.phoneLinkService = phoneLinkService;
        this.authService = authService;
    }

    @PatchMapping("/username")
    public ResponseEntity<AuthResponse> updateUsername(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid UpdateUsernameRequest request) {
        var user = userAccountService.updateUsername(userId, request.username());
        // Reissue tokens so the JWT and AuthResponse carry the fresh username.
        return ResponseEntity.ok(authService.issueTokensFor(user));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid ChangePasswordRequest request) {
        userAccountService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/phone/request")
    public ResponseEntity<EmailVerificationSentResponse> requestPhoneCode(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid PhoneLinkRequest request) {
        var user = userAccountService.load(userId);
        var result = phoneLinkService.requestCode(user, request.phone());
        return ResponseEntity.accepted().body(new EmailVerificationSentResponse(
                result.resendCooldownSeconds(),
                result.expiresInSeconds()
        ));
    }

    @PostMapping("/phone/verify")
    public ResponseEntity<AuthResponse> verifyPhone(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid PhoneVerifyRequest request) {
        var user = userAccountService.load(userId);
        var linked = phoneLinkService.verifyAndLink(user, request.phone(), request.code());
        return ResponseEntity.ok(authService.issueTokensFor(linked));
    }
}
