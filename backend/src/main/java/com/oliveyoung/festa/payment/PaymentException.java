package com.oliveyoung.festa.payment;

import com.oliveyoung.festa.api.ApiStatus;

public class PaymentException extends RuntimeException {
    private final ApiStatus status;
    public PaymentException(ApiStatus status) { super(status.message()); this.status = status; }
    public ApiStatus status() { return status; }
}
