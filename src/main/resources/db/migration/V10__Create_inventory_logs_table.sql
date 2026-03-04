-- V10: Create inventory_logs audit table

CREATE TABLE inventory_logs (
    id               BIGSERIAL       PRIMARY KEY,
    product_id       BIGINT          NULL,           -- nullable: log survives product deletion
    product_name     VARCHAR(255)    NOT NULL,
    quantity_changed FLOAT           NOT NULL,
    stock_after      FLOAT           NOT NULL,
    timestamp        TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_logs_product_id ON inventory_logs (product_id);
