package com.atlassync.auth.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "atlassync.otp.delivery.provider",
        havingValue = "log",
        matchIfMissing = true
)
public class LogDeliveryChannel implements OtpDeliveryChannel {

    @Override
    public void deliver(OtpDelivery delivery) {
        log.info("[delivery:log] to={} message={}", delivery.phone(), delivery.displayMessage());
    }
}
