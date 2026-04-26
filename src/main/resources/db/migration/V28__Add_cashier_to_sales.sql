ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS cashier_id BIGINT,
    ADD COLUMN IF NOT EXISTS cashier_username VARCHAR(50),
    ADD COLUMN IF NOT EXISTS cashier_name VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_sales_cashier_id_sale_date ON sales (cashier_id, sale_date DESC);
