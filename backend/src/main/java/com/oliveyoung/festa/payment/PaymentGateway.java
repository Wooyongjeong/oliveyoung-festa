package com.oliveyoung.festa.payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentResult approve(String attemptKey, BigDecimal amount, String currency, String scenario);
    PaymentResult find(String attemptKey);

    record PaymentResult(String attemptKey, BigDecimal amount, String currency, Status status, String transactionId) {}
    enum Status { APPROVED, DECLINED, NOT_FOUND }
}
