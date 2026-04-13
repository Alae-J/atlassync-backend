package com.atlassync.session.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "session_state_transitions")
@Getter
@Setter
@NoArgsConstructor
public class SessionStateTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ShoppingSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", nullable = false, length = 16)
    private SessionStatus fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 16)
    private SessionStatus toState;

    @Column(name = "transitioned_at", nullable = false)
    private Instant transitionedAt;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    @Column(name = "reason", length = 512)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (this.transitionedAt == null) {
            this.transitionedAt = Instant.now();
        }
    }
}
