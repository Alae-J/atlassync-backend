package com.atlassync.auth.sms;

public interface SmsSender {
    void send(String phone, String message);
}
