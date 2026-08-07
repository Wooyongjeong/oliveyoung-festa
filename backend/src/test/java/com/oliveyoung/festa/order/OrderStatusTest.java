package com.oliveyoung.festa.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void allowsOnlyDefinedOrderTransitions() {
        assertThat(OrderStatus.HELD.canTransitionTo(OrderStatus.PAYMENT_PROCESSING)).isTrue();
        assertThat(OrderStatus.HELD.canTransitionTo(OrderStatus.EXPIRED)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.COMPLETED)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.REFUND_PROCESSING)).isTrue();
        assertThat(OrderStatus.HELD.canTransitionTo(OrderStatus.PAID)).isFalse();
        assertThat(OrderStatus.EXPIRED.canTransitionTo(OrderStatus.HELD)).isFalse();
    }
}
