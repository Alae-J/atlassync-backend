package com.atlassync.auth.delivery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "atlassync.otp.delivery.provider", havingValue = "brevo")
@EnableConfigurationProperties(BrevoProperties.class)
public class BrevoConfig {

    @Bean
    public OtpDeliveryChannel brevoEmailDeliveryChannel(BrevoProperties props) {
        return new BrevoEmailDeliveryChannel(RestClient.builder(), props);
    }
}
