package com.atlassync.auth.delivery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "atlassync.otp.delivery.provider", havingValue = "whatsapp")
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppConfig {

    @Bean
    public OtpDeliveryChannel whatsAppDeliveryChannel(WhatsAppProperties props) {
        return new WhatsAppDeliveryChannel(RestClient.builder(), props);
    }
}
