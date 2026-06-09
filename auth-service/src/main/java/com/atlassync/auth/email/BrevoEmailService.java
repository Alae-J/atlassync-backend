package com.atlassync.auth.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends transactional emails via the Brevo HTTP API. Async — register / verify-resend
 * etc. don't wait. If the Brevo API key isn't configured, calls are no-ops with a
 * warning log so dev environments don't crash on missing creds.
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
    public void sendWelcome(String to, String name, String verificationCode, Duration codeTtl) {
        send(to, name, WelcomeEmail.render(name, verificationCode, codeTtl));
    }

    @Async("emailExecutor")
    @Override
    public void sendVerificationCode(String to, String name, String verificationCode, Duration codeTtl) {
        send(to, name, VerificationEmail.render(name, verificationCode, codeTtl));
    }

    @Async("emailExecutor")
    @Override
    public void sendPasswordResetCode(String to, String name, String resetCode, Duration codeTtl) {
        send(to, name, PasswordResetEmail.render(name, resetCode, codeTtl));
    }

    private void send(String to, String name, EmailTemplate template) {
        if (!props.isConfigured()) {
            log.warn("[email:brevo] skipped to={} subject=\"{}\" (BREVO_API_KEY not set)",
                    to, template.subject());
            return;
        }
        Map<String, Object> body = Map.of(
                "sender",      Map.of("name", props.fromName(), "email", props.fromEmail()),
                "to",          List.of(toRecipient(to, name)),
                "subject",     template.subject(),
                "textContent", template.text(),
                "htmlContent", template.html()
        );
        try {
            Map<?, ?> response = client.post()
                    .uri(EMAIL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String messageId = response != null ? String.valueOf(response.get("messageId")) : "?";
            log.info("[email:brevo] sent id={} subject=\"{}\" to={}", messageId, template.subject(), to);
        } catch (RestClientException ex) {
            log.error("[email:brevo] failed to={} subject=\"{}\": {}",
                    to, template.subject(), ex.getMessage());
        }
    }

    private static Map<String, String> toRecipient(String email, String name) {
        return name != null && !name.isBlank()
                ? Map.of("email", email, "name", name)
                : Map.of("email", email);
    }
}
