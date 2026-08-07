CREATE TABLE purchase_rights (
    user_id UUID NOT NULL REFERENCES users(id),
    event_id UUID NOT NULL REFERENCES events(id),
    status VARCHAR(30) NOT NULL CHECK (status IN ('AVAILABLE', 'HELD')),
    order_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, event_id),
    CHECK ((status = 'AVAILABLE' AND order_id IS NULL) OR (status = 'HELD' AND order_id IS NOT NULL))
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    event_id UUID NOT NULL REFERENCES events(id),
    ticket_grade_id UUID NOT NULL REFERENCES ticket_grades(id),
    status VARCHAR(30) NOT NULL CHECK (status IN ('HELD', 'EXPIRED', 'PAYMENT_PROCESSING', 'PAYMENT_FAILED', 'PAID', 'COMPLETED', 'REFUND_PROCESSING', 'REFUNDED')),
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    unit_price NUMERIC(19, 0) NOT NULL CHECK (unit_price >= 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'KRW'),
    grade_name_snapshot VARCHAR(100) NOT NULL,
    event_name_snapshot VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, idempotency_key)
);

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'RELEASED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_status_histories (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE purchase_rights
    ADD CONSTRAINT purchase_right_order_fk FOREIGN KEY (order_id) REFERENCES orders(id);

CREATE INDEX orders_user_created_idx ON orders(user_id, created_at DESC);
CREATE INDEX reservations_active_expiry_idx ON reservations(expires_at) WHERE status = 'ACTIVE';
