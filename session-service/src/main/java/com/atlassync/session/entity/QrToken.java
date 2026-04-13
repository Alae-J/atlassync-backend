package com.atlassync.session.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "qr_tokens")
@Getter
@Setter
@NoArgsConstructor
public class QrToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 36)
    private String correlationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ShoppingSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 10)
    private QrTokenType tokenType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "vault_signature", nullable = false, columnDefinition = "TEXT")
    private String vaultSignature;

    @Column(name = "key_version", nullable = false)
    private Integer keyVersion = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private QrStatus status = QrStatus.ACTIVE;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QrToken qrToken = (QrToken) o;
        return id != null && Objects.equals(id, qrToken.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
