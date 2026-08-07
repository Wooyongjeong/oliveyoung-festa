ALTER TABLE purchase_rights DROP CONSTRAINT purchase_rights_status_check;
ALTER TABLE purchase_rights ADD CONSTRAINT purchase_rights_status_check
    CHECK (status IN ('AVAILABLE', 'HELD', 'PURCHASED', 'REFUND_PROCESSING'));
ALTER TABLE purchase_rights DROP CONSTRAINT purchase_rights_check;
ALTER TABLE purchase_rights ADD CONSTRAINT purchase_rights_check CHECK (
    (status = 'AVAILABLE' AND order_id IS NULL) OR
    (status IN ('HELD', 'PURCHASED', 'REFUND_PROCESSING') AND order_id IS NOT NULL)
);

CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    attempt_key VARCHAR(100) NOT NULL UNIQUE,
    idempotency_key VARCHAR(255) NOT NULL,
    pg_provider VARCHAR(30) NOT NULL,
    routing_version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PROCESSING', 'APPROVED', 'DECLINED', 'UNKNOWN', 'REVIEW_REQUIRED')),
    amount NUMERIC(19, 0) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    pg_transaction_id VARCHAR(100),
    failure_reason VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (order_id, idempotency_key)
);

CREATE UNIQUE INDEX payment_attempts_active_order_idx ON payment_attempts(order_id)
    WHERE status IN ('PROCESSING', 'UNKNOWN', 'REVIEW_REQUIRED');

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE INDEX outbox_events_unpublished_idx ON outbox_events(created_at) WHERE published_at IS NULL;
