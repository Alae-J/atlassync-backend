package com.atlassync.auth.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Delivers OTPs through Meta's WhatsApp Cloud API using a pre-approved Authentication template.
 * <p>
 * Endpoint: {@code POST {base-url}/{phone-number-id}/messages} with bearer token. The OTP code
 * is passed as the single body parameter; recipient is sent in international format with the
 * leading {@code +} stripped (Meta's required shape).
 */
@Slf4j
public class WhatsAppDeliveryChannel implements OtpDeliveryChannel {

    private final RestClient client;
    private final WhatsAppProperties props;

    public WhatsAppDeliveryChannel(RestClient.Builder builder, WhatsAppProperties props) {
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeaders(headers -> headers.setBearerAuth(props.accessToken()))
                .build();
        this.props = props;
    }

    @Override
    public void deliver(OtpDelivery delivery) {
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to",                stripLeadingPlus(delivery.recipient()),
                "type",              "template",
                "template",          template(delivery.code())
        );
        try {
            Map<?, ?> response = client.post()
                    .uri("/{phoneNumberId}/messages", props.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String messageId = extractMessageId(response);
            log.info("[delivery:whatsapp] queued id={} to={}", messageId, delivery.recipient());
        } catch (RestClientException ex) {
            log.error("[delivery:whatsapp] failed for to={}: {}", delivery.recipient(), ex.getMessage());
            throw new DeliveryException("Failed to deliver OTP via WhatsApp Cloud API", ex);
        }
    }

    private Map<String, Object> template(String code) {
        List<Map<String, Object>> components = new ArrayList<>(2);
        components.add(Map.of(
                "type",       "body",
                "parameters", List.of(Map.of("type", "text", "text", code))
        ));
        if (Boolean.TRUE.equals(props.includeOtpButton())) {
            components.add(Map.of(
                    "type",       "button",
                    "sub_type",   "url",
                    "index",      "0",
                    "parameters", List.of(Map.of("type", "text", "text", code))
            ));
        }
        return Map.of(
                "name",       props.templateName(),
                "language",   Map.of("code", props.languageCode()),
                "components", components
        );
    }

    private static String stripLeadingPlus(String phone) {
        return phone.startsWith("+") ? phone.substring(1) : phone;
    }

    @SuppressWarnings("unchecked")
    private static String extractMessageId(Map<?, ?> response) {
        if (response == null) return "?";
        Object messages = response.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object id = first.get("id");
            if (id != null) return id.toString();
        }
        return "?";
    }
}
