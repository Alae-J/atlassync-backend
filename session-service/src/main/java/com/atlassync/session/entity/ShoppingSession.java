package com.atlassync.session.entity;

import com.atlassync.session.exception.IllegalStateTransitionException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "shopping_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SessionStatus status = SessionStatus.CREATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void transitionTo(SessionStatus target) {
        Set<SessionStatus> allowed = switch (this.status) {
            case CREATED -> Set.of(SessionStatus.ACTIVE, SessionStatus.CANCELLED);
            case ACTIVE -> Set.of(SessionStatus.PAYING, SessionStatus.CANCELLED);
            case PAYING -> Set.of(SessionStatus.COMPLETED, SessionStatus.CANCELLED);
            case COMPLETED, CANCELLED -> Set.of();
        };

        if (!allowed.contains(target)) {
            throw new IllegalStateTransitionException(
                    "Cannot transition from " + this.status + " to " + target);
        }
        this.status = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShoppingSession that = (ShoppingSession) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
