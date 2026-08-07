package com.oliveyoung.festa.catalog;

import java.time.Instant;
import java.util.UUID;

public record EventSummary(
        UUID id,
        String name,
        Instant saleStartsAt,
        Instant saleEndsAt,
        Instant eventStartsAt,
        EventSaleStatus status
) {
}
