package com.atlassync.session.repository;

import com.atlassync.session.entity.SessionStateTransition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionStateTransitionRepository extends JpaRepository<SessionStateTransition, Long> {
}
