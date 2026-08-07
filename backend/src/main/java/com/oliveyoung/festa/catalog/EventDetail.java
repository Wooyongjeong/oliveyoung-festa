package com.oliveyoung.festa.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventDetail(
        UUID id,
        String name,
        String description,
        Instant saleStartsAt,
        Instant saleEndsAt,
        Instant eventStartsAt,
        EventSaleStatus status,
        List<TicketGrade> grades
) {
    public record TicketGrade(
            UUID id,
            String code,
            String name,
            BigDecimal price,
            String currency,
            int total,
            int available
    ) {
    }
}
