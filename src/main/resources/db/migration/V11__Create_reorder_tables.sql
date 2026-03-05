-- V11: Create reorder tables
-- Stores purchase orders placed through the Reorder Management module and
-- their individual line items.

CREATE TABLE reorders (
    id             BIGSERIAL       PRIMARY KEY,
    order_ref      VARCHAR(50)     NOT NULL UNIQUE,
    supplier_email VARCHAR(255)    NOT NULL,
    status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING','CONFIRMED','CANCELLED','RECEIVED')),
    total_amount   DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reorders_status     ON reorders (status);
CREATE INDEX idx_reorders_created_at ON reorders (created_at DESC);

CREATE TABLE reorder_items (
    id           BIGSERIAL        PRIMARY KEY,
    reorder_id   BIGINT           NOT NULL REFERENCES reorders(id) ON DELETE CASCADE,
    product_name VARCHAR(255)     NOT NULL,
    product_id   BIGINT           NULL,      -- soft link; nullable so orders survive product deletion
    quantity     INTEGER          NOT NULL CHECK (quantity > 0),
    unit_price   DOUBLE PRECISION NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX idx_reorder_items_reorder_id ON reorder_items (reorder_id);
CREATE INDEX idx_reorder_items_product_id ON reorder_items (product_id);
