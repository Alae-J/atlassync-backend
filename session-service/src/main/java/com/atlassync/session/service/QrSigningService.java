package com.atlassync.session.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class QrSigningService {

    private final RestClient vaultClient;
    private final String fallbackSecret;

    public QrSigningService(@Value("${vault.uri:http://localhost:8200}") String vaultUri,
                            @Value("${vault.token:root}") String vaultToken,
                            @Value("${jwt.secret:fallback-key}") String fallbackSecret) {
        this.vaultClient = RestClient.builder()
                .baseUrl(vaultUri)
                .defaultHeader("X-Vault-Token", vaultToken)
                .build();
        this.fallbackSecret = fallbackSecret;
    }

    @SuppressWarnings("unchecked")
    public String sign(String payload) {
        try {
            String b64 = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            Map<String, Object> body = Map.of("input", b64);
            Map<String, Object> response = vaultClient.post()
                    .uri("/v1/transit/sign/qr-signing-key")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return (String) data.get("signature");
        } catch (Exception e) {
            log.warn("Vault unavailable, using HMAC fallback: {}", e.getMessage());
            return hmacSign(payload);
        }
    }

    @SuppressWarnings("unchecked")
    public boolean verify(String payload, String signature) {
        if (signature.startsWith("vault:")) {
            try {
                String b64 = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
                Map<String, Object> body = Map.of("input", b64, "signature", signature);
                Map<String, Object> response = vaultClient.post()
                        .uri("/v1/transit/verify/qr-signing-key")
                        .body(body)
                        .retrieve()
                        .body(Map.class);
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return (boolean) data.get("valid");
            } catch (Exception e) {
                log.warn("Vault verify failed: {}", e.getMessage());
                return false;
            }
        }
        return hmacVerify(payload, signature);
    }

    private String hmacSign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    fallbackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "hmac:" + Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC signing failed", e);
        }
    }

    private boolean hmacVerify(String payload, String signature) {
        if (!signature.startsWith("hmac:")) {
            return false;
        }
        String expected = hmacSign(payload);
        return expected.equals(signature);
    }
}
