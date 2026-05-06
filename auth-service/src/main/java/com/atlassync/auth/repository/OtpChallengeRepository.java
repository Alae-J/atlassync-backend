package com.atlassync.auth.repository;

import com.atlassync.auth.entity.OtpChallenge;
import com.atlassync.auth.entity.OtpChallengeStatus;
import com.atlassync.auth.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findByIdAndStatusAndPurpose(UUID id, OtpChallengeStatus status, OtpPurpose purpose);

    @Modifying
    @Query("update OtpChallenge o " +
           "set o.status = com.atlassync.auth.entity.OtpChallengeStatus.EXPIRED " +
           "where o.recipient = :recipient and o.purpose = :purpose " +
           "and o.status = com.atlassync.auth.entity.OtpChallengeStatus.PENDING")
    int markPendingChallengesExpired(@Param("recipient") String recipient, @Param("purpose") OtpPurpose purpose);

    @Modifying
    @Query("delete from OtpChallenge o where o.expiresAt < :cutoff")
    int deleteExpiredOlderThan(@Param("cutoff") Instant cutoff);
}
