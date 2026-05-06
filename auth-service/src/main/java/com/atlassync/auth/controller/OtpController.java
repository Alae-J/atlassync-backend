package com.atlassync.auth.controller;

import com.atlassync.auth.dto.AuthResponse;
import com.atlassync.auth.dto.OtpEmailRequestRequest;
import com.atlassync.auth.dto.OtpRequestRequest;
import com.atlassync.auth.dto.OtpRequestResponse;
import com.atlassync.auth.dto.OtpVerifyRequest;
import com.atlassync.auth.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/request")
    public ResponseEntity<OtpRequestResponse> request(@RequestBody @Valid OtpRequestRequest request) {
        OtpRequestResponse response = otpService.requestForPhone(request.phone());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/email/request")
    public ResponseEntity<OtpRequestResponse> requestForEmail(@RequestBody @Valid OtpEmailRequestRequest request) {
        OtpRequestResponse response = otpService.requestForEmail(request.email().trim().toLowerCase());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping({"/verify", "/email/verify"})
    public ResponseEntity<AuthResponse> verify(@RequestBody @Valid OtpVerifyRequest request) {
        AuthResponse response = otpService.verify(request.correlationId(), request.code());
        return ResponseEntity.ok(response);
    }
}
