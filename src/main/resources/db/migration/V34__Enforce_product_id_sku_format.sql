-- Product IDs are stored in products.sku as PI + zero-padded database id.
-- Repairs existing rows and prevents request payloads from overriding the format.

CREATE OR REPLACE FUNCTION set_products_sku_if_missing()
RETURNS trigger AS $$
BEGIN
    IF NEW.id IS NULL THEN
        NEW.id := nextval(pg_get_serial_sequence('products', 'id'));
    END IF;

    NEW.sku := 'PI' || lpad(NEW.id::text, 4, '0');

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_products_set_sku_if_missing ON products;

CREATE TRIGGER trg_products_set_sku_if_missing
BEFORE INSERT OR UPDATE OF sku ON products
FOR EACH ROW
EXECUTE FUNCTION set_products_sku_if_missing();

UPDATE products
SET sku = 'PI' || lpad(id::text, 4, '0');
