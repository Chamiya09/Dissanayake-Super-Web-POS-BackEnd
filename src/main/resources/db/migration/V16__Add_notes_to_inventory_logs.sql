-- Add optional notes column to inventory_logs for manual adjustment reasons
ALTER TABLE inventory_logs ADD COLUMN notes VARCHAR(500);
