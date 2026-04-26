ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS products_status_check;

ALTER TABLE products
    ADD CONSTRAINT products_status_check CHECK (status IN ('ACTIVE', 'DISCONTINUED'));

UPDATE suppliers
SET is_active = TRUE
WHERE is_active IS NULL;

UPDATE products
SET status = 'ACTIVE'
WHERE status IS NULL;
