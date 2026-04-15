package com.atlassync.cart.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class CartEventProducer {

    private static final String TOPIC_ITEM_ADDED = "cart.item.added";
    private static final String TOPIC_ITEM_REMOVED = "cart.item.removed";
    private static final String TOPIC_HELP_REQUESTED = "help.requested";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CartEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishItemAdded(String sessionId, String barcode, String productName,
                                 BigDecimal price, Integer quantity) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("barcode", barcode);
        payload.put("productName", productName);
        payload.put("price", price);
        payload.put("quantity", quantity);
        payload.put("timestamp", Instant.now().toString());
        kafkaTemplate.send(TOPIC_ITEM_ADDED, sessionId, payload);
    }

    public void publishItemRemoved(String sessionId, String barcode) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("barcode", barcode);
        payload.put("timestamp", Instant.now().toString());
        kafkaTemplate.send(TOPIC_ITEM_REMOVED, sessionId, payload);
    }

    public void publishHelpRequested(String sessionId, int aisleNumber, Long userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("aisleNumber", aisleNumber);
        payload.put("userId", userId);
        payload.put("timestamp", Instant.now().toString());
        kafkaTemplate.send(TOPIC_HELP_REQUESTED, sessionId, payload);
    }
}
