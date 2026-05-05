package com.atlassync.auth.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "atlassync.otp.sms.provider",
        havingValue = "log",
        matchIfMissing = true
)
public class LogSmsSender implements SmsSender {

    @Override
    public void send(String phone, String message) {
        log.info("[SMS:dev] to={} message={}", phone, message);
    }
}
