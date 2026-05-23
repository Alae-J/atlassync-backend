package com.atlassync.session.repository;

import com.atlassync.session.entity.SessionLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionLineItemRepository extends JpaRepository<SessionLineItem, Long> {

    /**
     * Receipt order — by insertion order, which mirrors the order the user
     * scanned items in the store.
     */
    List<SessionLineItem> findBySessionIdOrderByIdAsc(UUID sessionId);
}
