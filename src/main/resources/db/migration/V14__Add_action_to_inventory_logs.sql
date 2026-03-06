-- V14: Add 'action' column to inventory_logs to distinguish the reason for each stock change.
--      e.g. 'SALE_REDUCTION', 'MANUAL_ADDITION', 'MANUAL_REDUCTION', 'ADJUSTMENT'
--      Existing rows default to 'MANUAL_ADDITION' (all prior entries were add-stock operations).

ALTER TABLE inventory_logs
    ADD COLUMN IF NOT EXISTS action VARCHAR(50) NOT NULL DEFAULT 'MANUAL_ADDITION';
