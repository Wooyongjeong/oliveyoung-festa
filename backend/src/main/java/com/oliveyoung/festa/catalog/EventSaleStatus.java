package com.oliveyoung.festa.catalog;

import java.time.Instant;

public enum EventSaleStatus {
    UPCOMING,
    ON_SALE,
    ENDED;

    public static EventSaleStatus at(Instant now, Instant saleStartsAt, Instant saleEndsAt) {
        if (now.isBefore(saleStartsAt)) {
            return UPCOMING;
        }
        if (!now.isBefore(saleEndsAt)) {
            return ENDED;
        }
        return ON_SALE;
    }
}
