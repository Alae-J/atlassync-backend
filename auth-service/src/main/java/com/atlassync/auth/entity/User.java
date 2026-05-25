package com.atlassync.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 50)
    private String username;

    @Column
    private String password;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    // ── Preferences (Account → Preferences tab) ─────────────────────────
    // All nullable / default-empty so existing rows pre-V6 read as "no
    // preference" with sensible mobile-side defaults (USD, no flagged
    // allergens, etc.). Language lives elsewhere — surfaced as "Coming
    // soon" until the i18n setup lands.

    @Column(name = "default_store_id")
    private Long defaultStoreId;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dietary_prefs", columnDefinition = "text[]")
    private List<String> dietaryPrefs = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allergens", columnDefinition = "text[]")
    private List<String> allergens = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_prefs", columnDefinition = "jsonb")
    private Map<String, Boolean> notificationPrefs = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
