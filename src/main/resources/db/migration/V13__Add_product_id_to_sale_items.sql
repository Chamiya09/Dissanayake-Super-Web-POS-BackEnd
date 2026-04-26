-- V13: Add product_id FK to sale_items so SaleService can deduct inventory by product ID.
--      Column is nullable (BIGINT) because older rows won't have it populated.

ALTER TABLE sale_items
    ADD COLUMN IF NOT EXISTS product_id BIGINT REFERENCES products(id) ON DELETE SET NULL;
