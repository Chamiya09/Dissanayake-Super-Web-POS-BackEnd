-- Ensure SKU is always present at the database level even if application payloads omit it.
-- This protects all insert/update paths, not only the main ProductService flow.

CREATE OR REPLACE FUNCTION set_products_sku_if_missing()
RETURNS trigger AS $$
BEGIN
    IF NEW.sku IS NULL OR btrim(NEW.sku) = '' THEN
        LOOP
            NEW.sku := 'PI' || upper(substr(md5(random()::text || clock_timestamp()::text), 1, 12));
            EXIT WHEN NOT EXISTS (
                SELECT 1
                FROM products p
                WHERE p.sku = NEW.sku
                  AND (TG_OP = 'INSERT' OR p.id <> NEW.id)
            );
        END LOOP;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_products_set_sku_if_missing ON products;

CREATE TRIGGER trg_products_set_sku_if_missing
BEFORE INSERT OR UPDATE OF sku ON products
FOR EACH ROW
EXECUTE FUNCTION set_products_sku_if_missing();

UPDATE products
SET sku = 'PI' || upper(substr(md5(random()::text || clock_timestamp()::text || id::text), 1, 12))
WHERE sku IS NULL OR btrim(sku) = '';
