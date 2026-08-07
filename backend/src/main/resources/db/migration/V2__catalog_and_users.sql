DROP TABLE schema_marker;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER', 'OPERATOR', 'ADMIN')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    sale_starts_at TIMESTAMPTZ NOT NULL,
    sale_ends_at TIMESTAMPTZ NOT NULL,
    event_starts_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (sale_starts_at < sale_ends_at),
    CHECK (sale_ends_at <= event_starts_at)
);

CREATE TABLE ticket_grades (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    price NUMERIC(19, 0) NOT NULL CHECK (price >= 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'KRW'),
    UNIQUE (event_id, code)
);

CREATE TABLE inventories (
    ticket_grade_id UUID PRIMARY KEY REFERENCES ticket_grades(id),
    total INTEGER NOT NULL CHECK (total >= 0),
    available INTEGER NOT NULL CHECK (available >= 0),
    held INTEGER NOT NULL DEFAULT 0 CHECK (held >= 0),
    sold INTEGER NOT NULL DEFAULT 0 CHECK (sold >= 0),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CHECK (available + held + sold = total)
);

INSERT INTO users (id, email, display_name, role) VALUES
    ('10000000-0000-0000-0000-000000000001', 'customer@festa.local', '데모 고객', 'CUSTOMER'),
    ('10000000-0000-0000-0000-000000000002', 'operator@festa.local', '현장 운영자', 'OPERATOR'),
    ('10000000-0000-0000-0000-000000000003', 'admin@festa.local', '관리자', 'ADMIN');

INSERT INTO events (id, name, description, sale_starts_at, sale_ends_at, event_starts_at) VALUES
    ('20000000-0000-0000-0000-000000000001', '올리브영 페스타 2026', '뷰티와 웰니스 브랜드를 한자리에서 만나는 페스타입니다.', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '31 days');

INSERT INTO ticket_grades (id, event_id, code, name, price, currency) VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'GENERAL', '일반', 30000, 'KRW'),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'VIP', 'VIP', 50000, 'KRW');

INSERT INTO inventories (ticket_grade_id, total, available) VALUES
    ('30000000-0000-0000-0000-000000000001', 1, 1),
    ('30000000-0000-0000-0000-000000000002', 1, 1);
