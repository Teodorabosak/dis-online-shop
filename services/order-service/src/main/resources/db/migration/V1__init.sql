-- Pocetna sema baze order-service-a.

CREATE TABLE orders (
    id              VARCHAR(36)    PRIMARY KEY,
    customer_email  VARCHAR(255)   NOT NULL,
    status          VARCHAR(32)    NOT NULL,
    total           NUMERIC(12, 2) NOT NULL,
    status_reason   VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE order_items (
    id            BIGSERIAL      PRIMARY KEY,
    order_id      VARCHAR(36)    NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id    VARCHAR(64)    NOT NULL,
    product_name  VARCHAR(255)   NOT NULL,
    unit_price    NUMERIC(12, 2) NOT NULL,
    quantity      INT            NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_orders_customer_email ON orders (customer_email);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
