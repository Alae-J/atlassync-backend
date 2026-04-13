package com.atlassync.session.service;

import com.atlassync.proto.session.*;
import com.atlassync.session.dto.QrData;
import com.atlassync.session.entity.QrTokenType;
import com.atlassync.session.entity.SessionStatus;
import com.atlassync.session.entity.ShoppingSession;
import com.atlassync.session.exception.SessionNotFoundException;
import com.atlassync.session.repository.SessionRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class SessionGrpcService extends SessionServiceGrpc.SessionServiceImplBase {

    private final SessionRepository sessionRepository;
    private final SessionService sessionService;

    @Override
    public void validateSession(ValidateSessionRequest request,
                                StreamObserver<com.atlassync.proto.session.SessionResponse> responseObserver) {
        try {
            UUID sessionId = UUID.fromString(request.getSessionId());
            Long userId = Long.parseLong(request.getUserId());

            ShoppingSession session = sessionRepository.findById(sessionId)
                    .orElse(null);

            if (session == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Session not found: " + sessionId)
                        .asRuntimeException());
                return;
            }

            if (!session.getUserId().equals(userId)) {
                responseObserver.onError(Status.PERMISSION_DENIED
                        .withDescription("Session does not belong to user")
                        .asRuntimeException());
                return;
            }

            boolean valid = session.getStatus() == SessionStatus.ACTIVE;

            com.atlassync.proto.session.SessionResponse response =
                    com.atlassync.proto.session.SessionResponse.newBuilder()
                            .setSessionId(session.getId().toString())
                            .setStatus(session.getStatus().name())
                            .setValid(valid)
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid session ID or user ID format")
                    .asRuntimeException());
        }
    }

    @Override
    public void createExitToken(ExitTokenRequest request,
                                StreamObserver<ExitTokenResponse> responseObserver) {
        try {
            UUID sessionId = UUID.fromString(request.getSessionId());

            ShoppingSession session = sessionRepository.findById(sessionId)
                    .orElse(null);

            if (session == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Session not found: " + sessionId)
                        .asRuntimeException());
                return;
            }

            if (session.getStatus() != SessionStatus.COMPLETED) {
                responseObserver.onError(Status.FAILED_PRECONDITION
                        .withDescription("Session must be in COMPLETED state to generate exit token")
                        .asRuntimeException());
                return;
            }

            // Use SessionService's internal QR generation via completePayment flow
            // For gRPC, we generate a standalone exit QR
            QrData qrData = sessionService.generateExitQrForGrpc(session);

            ExitTokenResponse response = ExitTokenResponse.newBuilder()
                    .setQrPayload(qrData.payload())
                    .setSignature(qrData.signature())
                    .setCorrelationId(qrData.correlationId())
                    .setExpiresAtEpoch(qrData.expiresAt().toEpochMilli())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid session ID format")
                    .asRuntimeException());
        }
    }
}
