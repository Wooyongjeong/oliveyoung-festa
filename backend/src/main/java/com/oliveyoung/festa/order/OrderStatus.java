package com.oliveyoung.festa.order;

import java.util.EnumSet;

public enum OrderStatus {
    HELD,
    EXPIRED,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    PAID,
    COMPLETED,
    REFUND_PROCESSING,
    REFUNDED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case HELD -> EnumSet.of(PAYMENT_PROCESSING, EXPIRED).contains(target);
            case PAYMENT_PROCESSING -> EnumSet.of(HELD, PAYMENT_FAILED, PAID).contains(target);
            case PAID -> EnumSet.of(COMPLETED, REFUND_PROCESSING).contains(target);
            case COMPLETED -> target == REFUND_PROCESSING;
            case REFUND_PROCESSING -> target == REFUNDED;
            case EXPIRED, PAYMENT_FAILED, REFUNDED -> false;
        };
    }
}
