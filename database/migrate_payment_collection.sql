-- Payment Collection Feature Migration
-- Add payment tracking to settlements

-- Add columns to settlements table
ALTER TABLE settlements
ADD COLUMN paid_amount DECIMAL(10,2) DEFAULT 0 AFTER amount_to_collect,
ADD COLUMN remaining_amount DECIMAL(10,2) DEFAULT 0 AFTER paid_amount;

-- Create payment logs table
CREATE TABLE IF NOT EXISTS settlement_payments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    settlement_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(100),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    recorded_by INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (settlement_id) REFERENCES settlements(id) ON DELETE CASCADE,
    FOREIGN KEY (recorded_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_settlement (settlement_id),
    INDEX idx_settlement_date (settlement_id, payment_date)
);

-- Update existing settlements to set initial paid_amount and remaining_amount
UPDATE settlements
SET paid_amount = 0,
    remaining_amount = amount_to_collect
WHERE paid_amount IS NULL;
