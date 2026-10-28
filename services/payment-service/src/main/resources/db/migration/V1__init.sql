-- Pocetna sema baze payment-service-a.

CREATE TABLE payments (
    id              BIGSERIAL      PRIMARY KEY,
    order_id        VARCHAR(36)    NOT NULL UNIQUE,
    customer_email  VARCHAR(255)   NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(16)    NOT NULL,
    reason          VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
