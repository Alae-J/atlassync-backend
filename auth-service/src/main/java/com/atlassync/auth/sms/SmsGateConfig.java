package com.atlassync.auth.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "atlassync.otp.sms.provider", havingValue = "smsgate")
@EnableConfigurationProperties(SmsGateProperties.class)
public class SmsGateConfig {

    @Bean
    public SmsSender smsGateSender(SmsGateProperties props) {
        return new SmsGateSender(RestClient.builder(), props);
    }
}
