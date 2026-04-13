package com.atlassync.session.repository;

import com.atlassync.session.entity.SessionStatus;
import com.atlassync.session.entity.ShoppingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<ShoppingSession, UUID> {

    List<ShoppingSession> findByUserIdAndStatus(Long userId, SessionStatus status);
}
