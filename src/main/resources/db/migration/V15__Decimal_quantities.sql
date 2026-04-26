-- V15: Support decimal quantities (e.g. 1.500 kg, 0.750 l) in sale_items and reorder_items.
--      Widens the INTEGER quantity column to NUMERIC(10,3) — existing whole-number rows are
--      automatically cast by PostgreSQL (e.g. 2 -> 2.000).

-- Sale items
ALTER TABLE sale_items
    ALTER COLUMN quantity TYPE NUMERIC(10, 3);

-- Reorder items (has a CHECK constraint that must be recreated)
ALTER TABLE reorder_items
    DROP CONSTRAINT IF EXISTS reorder_items_quantity_check;

ALTER TABLE reorder_items
    ALTER COLUMN quantity TYPE NUMERIC(10, 3);

ALTER TABLE reorder_items
    ADD CONSTRAINT reorder_items_quantity_check CHECK (quantity > 0);
