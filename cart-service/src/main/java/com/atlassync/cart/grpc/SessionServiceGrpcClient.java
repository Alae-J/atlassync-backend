package com.atlassync.cart.grpc;

import com.atlassync.cart.exception.InvalidSessionException;
import com.atlassync.proto.session.SessionResponse;
import com.atlassync.proto.session.SessionServiceGrpc;
import com.atlassync.proto.session.ValidateSessionRequest;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class SessionServiceGrpcClient {

    @GrpcClient("session-service")
    private SessionServiceGrpc.SessionServiceBlockingStub stub;

    public SessionResponse validateSession(String sessionId, Long userId) {
        try {
            SessionResponse response = stub.validateSession(
                    ValidateSessionRequest.newBuilder()
                            .setSessionId(sessionId)
                            .setUserId(userId == null ? "" : String.valueOf(userId))
                            .build());
            if (!response.getValid()) {
                throw new InvalidSessionException("Session is not valid: " + sessionId);
            }
            return response;
        } catch (StatusRuntimeException ex) {
            throw new InvalidSessionException("Unable to validate session: " + ex.getStatus().getDescription());
        }
    }
}
