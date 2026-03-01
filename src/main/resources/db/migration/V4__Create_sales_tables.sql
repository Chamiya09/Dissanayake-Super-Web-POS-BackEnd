-- V4: Create sales and sale_items tables

CREATE TABLE sales (
    id             BIGSERIAL    PRIMARY KEY,
    receipt_no     VARCHAR(20)  NOT NULL UNIQUE,
    sale_date      TIMESTAMP    NOT NULL DEFAULT NOW(),
    payment_method VARCHAR(50)  NOT NULL,
    total_amount   DOUBLE PRECISION NOT NULL,
    status         VARCHAR(20)  NOT NULL
);

CREATE TABLE sale_items (
    id           BIGSERIAL        PRIMARY KEY,
    sale_id      BIGINT           NOT NULL REFERENCES sales(id),
    product_name VARCHAR(255)     NOT NULL,
    quantity     INTEGER          NOT NULL,
    unit_price   DOUBLE PRECISION NOT NULL,
    line_total   DOUBLE PRECISION NOT NULL
);
