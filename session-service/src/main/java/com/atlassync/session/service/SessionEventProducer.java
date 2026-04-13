package com.atlassync.session.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
}
