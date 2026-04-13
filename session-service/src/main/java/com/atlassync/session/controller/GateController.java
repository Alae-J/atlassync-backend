package com.atlassync.session.controller;

import com.atlassync.session.dto.GateValidationRequest;
import com.atlassync.session.dto.GateValidationResponse;
import com.atlassync.session.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
public class GateController {

    private final SessionService sessionService;

    @PostMapping("/entry")
    public ResponseEntity<GateValidationResponse> validateEntry(
            @Valid @RequestBody GateValidationRequest request) {
        var response = sessionService.activateSession(request.correlationId());
        return ResponseEntity.ok(new GateValidationResponse(
                true, response.sessionId(), "Entry authorized"));
    }

    @PostMapping("/exit")
    public ResponseEntity<GateValidationResponse> validateExit(
            @Valid @RequestBody GateValidationRequest request) {
        GateValidationResponse response = sessionService.validateExitQr(request.correlationId());
        return ResponseEntity.ok(response);
    }
}
