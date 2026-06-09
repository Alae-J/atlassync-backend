package com.atlassync.session.service;

import com.atlassync.session.entity.SessionLineItem;
import com.atlassync.session.entity.ShoppingSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSessionPaid(UUID sessionId, Long userId) {
        log.info("Publishing session.paid event for session={}", sessionId);
        kafkaTemplate.send("session.paid", sessionId.toString(),
                Map.of("sessionId", sessionId.toString(),
                        "userId", userId,
                        "paidAt", Instant.now().toString()));
    }

    public void publishSessionCompleted(UUID sessionId) {
        log.info("Publishing session.completed event for session={}", sessionId);
        kafkaTemplate.send("session.completed", sessionId.toString(),
                Map.of("sessionId", sessionId.toString(),
                        "completedAt", Instant.now().toString()));
    }

    /**
     * Publish the analytics-grade event with full purchase context.
     * Schema is intentionally self-contained except for {@code categoryId}, which
     * the silver Spark job joins against product-service.
     */
    public void publishPurchasesCompleted(ShoppingSession session,
                                          List<SessionLineItem> items,
                                          String currency) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventTime", Instant.now().toString());
        payload.put("sessionId", session.getId().toString());
        payload.put("userId", session.getUserId());
        payload.put("storeId", session.getStoreId());
        payload.put("currency", currency.toUpperCase());
        payload.put("total", session.getTotalAmount());
        payload.put("items", items.stream().map(this::toItemMap).toList());

        kafkaTemplate.send("purchases.completed", session.getId().toString(), payload);
        log.info("[kafka] purchases.completed sent for session={} items={} total={}",
                session.getId(), items.size(), session.getTotalAmount());
    }

    private Map<String, Object> toItemMap(SessionLineItem it) {
        Map<String, Object> m = new HashMap<>();
        m.put("barcode", it.getBarcode());
        m.put("name", it.getProductName());
        m.put("unitPrice", it.getUnitPrice());
        m.put("qty", it.getQuantity());
        return m;
    }
}
