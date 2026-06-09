package com.atlassync.session.repository;

import com.atlassync.session.entity.QrToken;
import com.atlassync.session.entity.QrTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QrTokenRepository extends JpaRepository<QrToken, UUID> {

    Optional<QrToken> findByCorrelationId(String correlationId);

    Optional<QrToken> findFirstBySessionIdAndTokenTypeOrderByCreatedAtDesc(
            UUID sessionId, QrTokenType tokenType);
}
