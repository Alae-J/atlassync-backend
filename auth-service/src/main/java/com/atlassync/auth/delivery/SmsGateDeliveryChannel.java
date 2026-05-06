package com.atlassync.auth.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Delivers OTPs as plain SMS through sms-gate.app, which relays them via an Android phone
 * running the SMSGate app on a real SIM (free P2P SMS within the same carrier network).
 */
@Slf4j
public class SmsGateDeliveryChannel implements OtpDeliveryChannel {

    private final RestClient client;

    public SmsGateDeliveryChannel(RestClient.Builder builder, SmsGateProperties props) {
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(props.username(), props.password()))
                .build();
    }

    @Override
    public void deliver(OtpDelivery delivery) {
        var body = Map.of(
                "message", delivery.displayMessage(),
                "phoneNumbers", List.of(delivery.recipient())
        );
        try {
            Map<?, ?> response = client.post()
                    .uri("/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String messageId = response != null ? String.valueOf(response.get("id")) : "?";
            log.info("[delivery:smsgate] queued id={} to={}", messageId, delivery.recipient());
        } catch (RestClientException ex) {
            log.error("[delivery:smsgate] failed for to={}: {}", delivery.recipient(), ex.getMessage());
            throw new DeliveryException("Failed to relay SMS via sms-gate.app", ex);
        }
    }
}
