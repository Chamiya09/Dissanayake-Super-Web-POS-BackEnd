-- Add soft-delete support flag for products.
-- Existing rows are marked active by default.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Backfill safety for databases that might already have nullable rows.
UPDATE products
SET is_active = TRUE
WHERE is_active IS NULL;
