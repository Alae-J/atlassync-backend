package com.atlassync.auth.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Delivers OTPs via the Brevo transactional-email API. The recipient is treated as an email
 * address; phone numbers passed here will be rejected by Brevo.
 */
@Slf4j
public class BrevoEmailDeliveryChannel implements OtpDeliveryChannel {

    private final RestClient client;
    private final BrevoProperties props;

    public BrevoEmailDeliveryChannel(RestClient.Builder builder, BrevoProperties props) {
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("api-key", props.apiKey())
                .build();
        this.props = props;
    }

    @Override
    public void deliver(OtpDelivery delivery) {
        Map<String, Object> body = Map.of(
                "sender",      Map.of("name", props.fromName(), "email", props.fromEmail()),
                "to",          List.of(Map.of("email", delivery.recipient())),
                "subject",     props.subject(),
                "textContent", buildTextBody(delivery),
                "htmlContent", buildHtmlBody(delivery)
        );
        try {
            Map<?, ?> response = client.post()
                    .uri("/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String messageId = response != null ? String.valueOf(response.get("messageId")) : "?";
            log.info("[delivery:brevo] queued id={} to={}", messageId, delivery.recipient());
        } catch (RestClientException ex) {
            log.error("[delivery:brevo] failed for to={}: {}", delivery.recipient(), ex.getMessage());
            throw new DeliveryException("Failed to deliver OTP via Brevo", ex);
        }
    }

    private String buildTextBody(OtpDelivery delivery) {
        return """
                Your AtlasSync verification code is:

                %s

                %s

                If you didn't request this code, you can ignore this email.
                """.formatted(delivery.code(), delivery.displayMessage());
    }

    private String buildHtmlBody(OtpDelivery delivery) {
        return """
                <!doctype html>
                <html><body style="font-family:-apple-system,Segoe UI,Helvetica,Arial,sans-serif;\
                color:#15140f;background:#f4ede0;padding:32px 0;">
                <table style="max-width:480px;margin:0 auto;background:#fffdf8;\
                border-radius:14px;padding:32px;">
                <tr><td>
                <p style="font-size:13px;letter-spacing:1.4px;color:#7a7163;margin:0 0 12px;\
                text-transform:uppercase">Your code</p>
                <p style="font-family:Georgia,serif;font-size:48px;letter-spacing:6px;\
                margin:0 0 24px;color:#15140f;">%s</p>
                <p style="font-size:13px;color:#5a5448;margin:0 0 8px;">%s</p>
                <p style="font-size:12px;color:#7a7163;margin:24px 0 0;">\
                If you didn't request this code, you can ignore this email.</p>
                </td></tr>
                </table>
                </body></html>
                """.formatted(delivery.code(), delivery.displayMessage());
    }
}
