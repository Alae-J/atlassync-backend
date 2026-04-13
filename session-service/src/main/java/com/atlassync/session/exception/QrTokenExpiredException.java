package com.atlassync.session.exception;

public class QrTokenExpiredException extends RuntimeException {

    public QrTokenExpiredException(String message) {
        super(message);
    }
}
