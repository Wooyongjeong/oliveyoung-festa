package com.oliveyoung.festa.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentView(UUID orderId, String attemptKey, PaymentStatus status, BigDecimal amount,
                          String currency, String failureReason, Instant updatedAt) {
}
