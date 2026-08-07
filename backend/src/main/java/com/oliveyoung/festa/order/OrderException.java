package com.oliveyoung.festa.order;

import com.oliveyoung.festa.api.ApiStatus;

public class OrderException extends RuntimeException {

    private final ApiStatus status;

    public OrderException(ApiStatus status) {
        super(status.message());
        this.status = status;
    }

    public ApiStatus status() {
        return status;
    }
}
