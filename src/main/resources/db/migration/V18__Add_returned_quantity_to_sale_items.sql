-- V18: Track returned quantities per sale item to support partial item-level sale returns.

ALTER TABLE sale_items
    ADD COLUMN IF NOT EXISTS returned_quantity NUMERIC(10, 3) NOT NULL DEFAULT 0;
