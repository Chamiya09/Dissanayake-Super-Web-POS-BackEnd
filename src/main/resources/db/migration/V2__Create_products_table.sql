CREATE TABLE products (
    id              BIGSERIAL        PRIMARY KEY,
    product_name    VARCHAR(255)     NOT NULL,
    sku             VARCHAR(100)     NOT NULL UNIQUE,
    category        VARCHAR(100)     NOT NULL,
    buying_price    NUMERIC(10, 2)   NOT NULL CHECK (buying_price >= 0),
    selling_price   NUMERIC(10, 2)   NOT NULL CHECK (selling_price >= 0),
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
