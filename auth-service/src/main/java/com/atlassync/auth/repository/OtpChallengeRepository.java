package com.atlassync.auth.repository;

import com.atlassync.auth.entity.OtpChallenge;
import com.atlassync.auth.entity.OtpChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findByIdAndStatus(UUID id, OtpChallengeStatus status);

    @Modifying
    @Query("update OtpChallenge o " +
           "set o.status = com.atlassync.auth.entity.OtpChallengeStatus.EXPIRED " +
           "where o.phone = :phone and o.status = com.atlassync.auth.entity.OtpChallengeStatus.PENDING")
    int markPendingChallengesExpired(@Param("phone") String phone);

    @Modifying
    @Query("delete from OtpChallenge o where o.expiresAt < :cutoff")
    int deleteExpiredOlderThan(@Param("cutoff") Instant cutoff);
}
