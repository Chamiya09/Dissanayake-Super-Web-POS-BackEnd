CREATE TABLE suppliers (
    id                      BIGSERIAL       PRIMARY KEY,
    company_name            VARCHAR(255)    NOT NULL,
    contact_person          VARCHAR(255)    NOT NULL,
    email                   VARCHAR(255)    NOT NULL UNIQUE,
    phone                   VARCHAR(20)     NOT NULL,
    lead_time               INTEGER         NOT NULL CHECK (lead_time >= 1),
    is_auto_reorder_enabled BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);