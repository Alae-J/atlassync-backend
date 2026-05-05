package com.atlassync.auth.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends SMS through the sms-gate.app cloud relay. Requires the SMSGate Android app (with an
 * active SIM) running in Cloud Server mode and reachable through Firebase push.
 */
@Slf4j
public class SmsGateSender implements SmsSender {

    private final RestClient client;

    public SmsGateSender(RestClient.Builder builder, SmsGateProperties props) {
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(props.username(), props.password()))
                .build();
    }

    @Override
    public void send(String phone, String message) {
        var body = Map.of(
                "message", message,
                "phoneNumbers", List.of(phone)
        );
        try {
            Map<?, ?> response = client.post()
                    .uri("/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String messageId = response != null ? String.valueOf(response.get("id")) : "?";
            log.info("[sms-gate] queued message id={} to phone={}", messageId, phone);
        } catch (RestClientException ex) {
            log.error("[sms-gate] failed to relay SMS to phone={}: {}", phone, ex.getMessage());
            throw new SmsDeliveryException("Failed to relay SMS via sms-gate.app", ex);
        }
    }
}
