-- Pocetna sema baze inventory-service-a.
-- Flyway izvrsava ovu skriptu jednom, pri prvom pokretanju, i belezi da je izvrsena.

CREATE TABLE inventory_items (
    id          BIGSERIAL    PRIMARY KEY,
    product_id  VARCHAR(64)  NOT NULL UNIQUE,
    quantity    INT          NOT NULL CHECK (quantity >= 0),
    reserved    INT          NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    id          BIGSERIAL    PRIMARY KEY,
    order_id    VARCHAR(64)  NOT NULL,
    product_id  VARCHAR(64)  NOT NULL,
    quantity    INT          NOT NULL CHECK (quantity > 0),
    status      VARCHAR(16)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_reservations_order_id ON reservations (order_id);
