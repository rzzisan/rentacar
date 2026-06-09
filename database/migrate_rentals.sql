-- Rental Management System Migration
-- Add trip-specific columns to rentals table
-- Create trip_expenses table for expense tracking

-- Make end_date nullable (not known at trip creation)
ALTER TABLE rentals MODIFY COLUMN end_date DATETIME NULL;

-- Add trip-specific columns
ALTER TABLE rentals
  ADD COLUMN trip_type ENUM('one_way','round_trip') DEFAULT 'one_way' AFTER dropoff_location,
  ADD COLUMN agreed_amount DECIMAL(10,2) DEFAULT 0 AFTER trip_type,
  ADD COLUMN driver_id INT DEFAULT NULL AFTER employee_id;

-- Add foreign key for driver
ALTER TABLE rentals
  ADD CONSTRAINT fk_rental_driver
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE SET NULL;

-- Trip expenses table
CREATE TABLE IF NOT EXISTS trip_expenses (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    rental_id     INT NOT NULL,
    expense_type  ENUM('toll','fuel','parking','repair','driver_allowance','other') NOT NULL,
    description   VARCHAR(255),
    amount        DECIMAL(10,2) NOT NULL DEFAULT 0,
    receipt_image VARCHAR(255),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE,
    INDEX idx_rental (rental_id)
);
