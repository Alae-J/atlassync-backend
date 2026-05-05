package com.atlassync.auth.delivery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "atlassync.otp.delivery.provider", havingValue = "smsgate")
@EnableConfigurationProperties(SmsGateProperties.class)
public class SmsGateConfig {

    @Bean
    public OtpDeliveryChannel smsGateDeliveryChannel(SmsGateProperties props) {
        return new SmsGateDeliveryChannel(RestClient.builder(), props);
    }
}
