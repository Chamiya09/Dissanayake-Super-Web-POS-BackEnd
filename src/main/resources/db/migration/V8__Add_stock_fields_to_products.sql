-- V8: Add stock_quantity and reorder_level columns to products

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS stock_quantity FLOAT        NOT NULL DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS reorder_level  FLOAT        NULL;
