-- V12: Link products to their primary supplier
-- supplier_id is nullable — existing products have no supplier assigned yet.
ALTER TABLE products
    ADD COLUMN supplier_id BIGINT,
    ADD CONSTRAINT fk_products_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL;

CREATE INDEX idx_products_supplier_id ON products(supplier_id);
