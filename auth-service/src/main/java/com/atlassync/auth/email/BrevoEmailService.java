package com.atlassync.auth.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional emails via the Brevo HTTP API. Async — register etc. don't wait.
 * If the Brevo API key isn't configured, calls are no-ops with a warning log so dev
 * environments don't crash on missing creds.
 */
@Slf4j
@Service
public class BrevoEmailService implements EmailService {

    private static final String EMAIL_PATH = "/v3/smtp/email";

    private final RestClient client;
    private final BrevoEmailProperties props;

    public BrevoEmailService(RestClient.Builder builder, BrevoEmailProperties props) {
        this.props = props;
        this.client = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("api-key", props.apiKey() != null ? props.apiKey() : "")
                .build();
    }

    @Async("emailExecutor")
    @Override
    public void sendWelcome(String to, String name) {
        if (!props.isConfigured()) {
            log.warn("[email:brevo] welcome email skipped for to={} (BREVO_API_KEY not set)", to);
            return;
        }
        sendEmail(
                to,
                name,
                "Welcome to AtlasSync",
                buildWelcomeText(name),
                buildWelcomeHtml(name)
        );
    }

    private void sendEmail(String to, String name, String subject, String text, String html) {
        Map<String, Object> body = Map.of(
                "sender",      Map.of("name", props.fromName(), "email", props.fromEmail()),
                "to",          List.of(toRecipient(to, name)),
                "subject",     subject,
                "textContent", text,
                "htmlContent", html
        );
        try {
            Map<?, ?> response = client.post()
                    .uri(EMAIL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String messageId = response != null ? String.valueOf(response.get("messageId")) : "?";
            log.info("[email:brevo] sent id={} subject=\"{}\" to={}", messageId, subject, to);
        } catch (RestClientException ex) {
            log.error("[email:brevo] failed to={} subject=\"{}\": {}", to, subject, ex.getMessage());
        }
    }

    private static Map<String, String> toRecipient(String email, String name) {
        return name != null && !name.isBlank()
                ? Map.of("email", email, "name", name)
                : Map.of("email", email);
    }

    private static String buildWelcomeText(String name) {
        return """
                Hi %s,

                Your AtlasSync account is ready. Walk into a partner store, scan
                what you grab, walk out — your phone is the checkout line.

                If you didn't sign up, ignore this email and your account will
                stay dormant.

                — The AtlasSync team
                """.formatted(name);
    }

    private static String buildWelcomeHtml(String name) {
        return """
                <!doctype html>
                <html><body style="font-family:-apple-system,Segoe UI,Helvetica,Arial,sans-serif;\
                color:#15140f;background:#f4ede0;padding:32px 0;">
                <table style="max-width:480px;margin:0 auto;background:#fffdf8;\
                border-radius:14px;padding:32px;">
                <tr><td>
                <p style="font-size:13px;letter-spacing:1.4px;color:#7a7163;margin:0 0 12px;\
                text-transform:uppercase">Welcome aboard</p>
                <h1 style="font-family:Georgia,serif;font-size:32px;letter-spacing:-0.6px;\
                margin:0 0 16px;color:#15140f;">Hi <em style="color:#c87a3a;">%s</em>.</h1>
                <p style="font-size:14px;line-height:1.55;color:#5a5448;margin:0 0 12px;">\
                Your AtlasSync account is ready. Walk into a partner store, scan what you grab,\
                walk out — your phone is the checkout line.</p>
                <p style="font-size:12px;color:#7a7163;margin:24px 0 0;">\
                If you didn't sign up, ignore this email and your account will stay dormant.</p>
                </td></tr>
                </table>
                </body></html>
                """.formatted(name);
    }
}
