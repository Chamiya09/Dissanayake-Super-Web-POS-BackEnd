-- V9: Create inventory table

CREATE TABLE inventory (
    id             BIGSERIAL      PRIMARY KEY,
    product_id     BIGINT         NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    stock_quantity FLOAT          NOT NULL DEFAULT 0.0,
    reorder_level  FLOAT          NOT NULL DEFAULT 10.0,
    unit           VARCHAR(20),
    last_updated   TIMESTAMP      NOT NULL DEFAULT NOW()
);
