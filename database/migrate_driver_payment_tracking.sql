-- Driver Payment Tracking Migration
-- Add column to track which driver submitted each payment collection

ALTER TABLE settlement_payments
ADD COLUMN paid_by_driver_id INT NULL AFTER recorded_by,
ADD CONSTRAINT fk_spay_driver FOREIGN KEY (paid_by_driver_id) REFERENCES drivers(id) ON DELETE SET NULL,
ADD INDEX idx_paid_by_driver (paid_by_driver_id);

-- Update recorded_by column name to notes (if not already done)
-- Note: migration_payment_collection.sql uses 'notes' but we're keeping payment_notes in the column for clarity
