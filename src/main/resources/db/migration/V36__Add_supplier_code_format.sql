-- Supplier IDs are stored as supplier_code using SI + zero-padded database id.
-- Examples: id=1 -> SI0001, id=12 -> SI0012

ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS supplier_code VARCHAR(100);

CREATE OR REPLACE FUNCTION set_suppliers_code()
RETURNS trigger AS $$
BEGIN
    IF NEW.id IS NULL THEN
        NEW.id := nextval(pg_get_serial_sequence('suppliers', 'id'));
    END IF;

    NEW.supplier_code := 'SI' || lpad(NEW.id::text, 4, '0');

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_suppliers_set_code ON suppliers;

CREATE TRIGGER trg_suppliers_set_code
BEFORE INSERT OR UPDATE OF supplier_code ON suppliers
FOR EACH ROW
EXECUTE FUNCTION set_suppliers_code();

UPDATE suppliers
SET supplier_code = 'SI' || lpad(id::text, 4, '0');

CREATE UNIQUE INDEX IF NOT EXISTS uq_suppliers_supplier_code
    ON suppliers (supplier_code);

SELECT setval(
    pg_get_serial_sequence('suppliers', 'id'),
    COALESCE((SELECT MAX(id) FROM suppliers), 0) + 1,
    false
);
