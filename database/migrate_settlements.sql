-- Settlements table for payment tracking
CREATE TABLE IF NOT EXISTS settlements (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    rental_id       INT NOT NULL UNIQUE,
    driver_id       INT,
    agreed_amount   DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_expenses  DECIMAL(10,2) DEFAULT 0,
    net_amount      DECIMAL(10,2) DEFAULT 0,
    commission_percent DECIMAL(5,2) DEFAULT 0,
    driver_commission  DECIMAL(10,2) DEFAULT 0,
    amount_to_collect  DECIMAL(10,2) DEFAULT 0,
    payment_status  ENUM('pending', 'paid', 'partial', 'refunded') DEFAULT 'pending',
    payment_method  VARCHAR(50),
    payment_notes   TEXT,
    paid_date       DATETIME,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE,
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE SET NULL,
    INDEX idx_rental (rental_id),
    INDEX idx_driver (driver_id),
    INDEX idx_status (payment_status),
    INDEX idx_paid_date (paid_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alter rentals table to track settlement reference (optional but useful)
ALTER TABLE rentals ADD COLUMN settlement_id INT DEFAULT NULL AFTER agreed_amount;
