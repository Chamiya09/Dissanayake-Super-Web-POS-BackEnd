-- V17: Add supplier email-accept tracking for reorder workflow
-- - supplier_accept_token: public one-time token embedded in supplier email
-- - accepted_at: when supplier confirmed by clicking the accept link

ALTER TABLE reorders
    ADD COLUMN IF NOT EXISTS supplier_accept_token VARCHAR(120),
    ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_reorders_supplier_accept_token
    ON reorders (supplier_accept_token)
    WHERE supplier_accept_token IS NOT NULL;
