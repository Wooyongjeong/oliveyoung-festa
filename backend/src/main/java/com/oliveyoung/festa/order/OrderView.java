package com.oliveyoung.festa.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderView(
        UUID id,
        UUID eventId,
        String eventName,
        String gradeCode,
        String gradeName,
        BigDecimal unitPrice,
        String currency,
        OrderStatus status,
        Instant reservationExpiresAt,
        Instant createdAt
) {
}
