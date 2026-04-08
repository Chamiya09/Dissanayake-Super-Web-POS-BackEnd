-- Add dedicated barcode column separate from SKU/Product ID.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(100);

-- Keep active-product barcode values unique when present.
CREATE UNIQUE INDEX IF NOT EXISTS ux_products_active_barcode
    ON products (barcode)
    WHERE is_active = TRUE AND barcode IS NOT NULL;
