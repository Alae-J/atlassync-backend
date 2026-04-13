package com.atlassync.auth.repository;

import com.atlassync.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyId(UUID familyId);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revoked = true, rt.revocationReason = :reason
            WHERE rt.familyId = :familyId AND rt.revoked = false
            """)
    int revokeByFamilyId(@Param("familyId") UUID familyId,
                         @Param("reason") com.atlassync.auth.entity.RevocationReason reason);
}
