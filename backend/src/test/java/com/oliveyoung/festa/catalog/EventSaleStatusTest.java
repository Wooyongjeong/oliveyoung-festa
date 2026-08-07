package com.oliveyoung.festa.catalog;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventSaleStatusTest {

    private final Instant saleStartsAt = Instant.parse("2026-08-10T00:00:00Z");
    private final Instant saleEndsAt = Instant.parse("2026-08-20T00:00:00Z");

    @Test
    void upcomingBeforeSaleStarts() {
        assertThat(EventSaleStatus.at(saleStartsAt.minusMillis(1), saleStartsAt, saleEndsAt))
                .isEqualTo(EventSaleStatus.UPCOMING);
    }

    @Test
    void onSaleAtStartAndBeforeEnd() {
        assertThat(EventSaleStatus.at(saleStartsAt, saleStartsAt, saleEndsAt))
                .isEqualTo(EventSaleStatus.ON_SALE);
        assertThat(EventSaleStatus.at(saleEndsAt.minusMillis(1), saleStartsAt, saleEndsAt))
                .isEqualTo(EventSaleStatus.ON_SALE);
    }

    @Test
    void endedAtSaleEnd() {
        assertThat(EventSaleStatus.at(saleEndsAt, saleStartsAt, saleEndsAt))
                .isEqualTo(EventSaleStatus.ENDED);
    }
}
