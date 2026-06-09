package com.atlassync.session.entity;

public enum SessionStatus {
    CREATED,
    ACTIVE,
    PAYING,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    PARTIAL_REFUND,
    DISPUTED,
    CHARGEBACK_LOST
}
