package com.atlassync.auth.sms;

public class SmsDeliveryException extends RuntimeException {
    public SmsDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
