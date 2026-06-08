-- ===================================
-- Rent-A-Car Management System Database Schema
-- ===================================

-- Users Table
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    role ENUM('admin', 'employee', 'customer') NOT NULL DEFAULT 'customer',
    status ENUM('active', 'inactive') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role)
);

-- Customers Table
CREATE TABLE customers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    nid VARCHAR(20) UNIQUE,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(100),
    address TEXT,
    city VARCHAR(50),
    postal_code VARCHAR(10),
    license_number VARCHAR(20) UNIQUE,
    license_expiry DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Vehicles Table
CREATE TABLE vehicles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    registration_number VARCHAR(20) UNIQUE NOT NULL,
    model VARCHAR(100) NOT NULL,
    brand VARCHAR(50) NOT NULL,
    year YEAR,
    vehicle_type ENUM('sedan', 'suv', 'van', 'truck', 'premium') NOT NULL,
    color VARCHAR(30),
    fuel_type ENUM('petrol', 'diesel', 'hybrid', 'electric') NOT NULL,
    seating_capacity INT,
    mileage INT DEFAULT 0,
    daily_rent_price DECIMAL(10, 2) NOT NULL,
    status ENUM('available', 'rented', 'maintenance', 'inactive') DEFAULT 'available',
    image_path VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_brand (brand)
);

-- Rentals Table
CREATE TABLE rentals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    employee_id INT,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    pickup_location VARCHAR(100),
    dropoff_location VARCHAR(100),
    rental_status ENUM('pending', 'active', 'completed', 'cancelled') DEFAULT 'pending',
    total_days INT,
    daily_rate DECIMAL(10, 2),
    subtotal DECIMAL(10, 2),
    discount DECIMAL(10, 2) DEFAULT 0,
    tax DECIMAL(10, 2) DEFAULT 0,
    total_amount DECIMAL(10, 2),
    payment_status ENUM('pending', 'paid', 'partial') DEFAULT 'pending',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_customer (customer_id),
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_status (rental_status),
    INDEX idx_payment_status (payment_status)
);

-- Payments Table
CREATE TABLE payments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    rental_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method ENUM('cash', 'card', 'mobile_banking', 'bank_transfer') NOT NULL,
    transaction_id VARCHAR(100),
    status ENUM('pending', 'completed', 'failed', 'refunded') DEFAULT 'pending',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE,
    INDEX idx_rental (rental_id),
    INDEX idx_status (status)
);

-- Vehicle Maintenance Table
CREATE TABLE maintenance (
    id INT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id INT NOT NULL,
    maintenance_type VARCHAR(100) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    cost DECIMAL(10, 2),
    status ENUM('pending', 'in_progress', 'completed') DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    INDEX idx_vehicle (vehicle_id)
);

-- Damage Reports Table
CREATE TABLE damage_reports (
    id INT PRIMARY KEY AUTO_INCREMENT,
    rental_id INT NOT NULL,
    damage_description TEXT,
    damage_severity ENUM('minor', 'moderate', 'severe') NOT NULL,
    damage_cost DECIMAL(10, 2),
    reported_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('reported', 'under_review', 'approved', 'rejected') DEFAULT 'reported',
    FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE,
    INDEX idx_rental (rental_id)
);

-- Reviews Table
CREATE TABLE reviews (
    id INT PRIMARY KEY AUTO_INCREMENT,
    rental_id INT NOT NULL,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    rating INT CHECK(rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
);

-- Settings Table
CREATE TABLE settings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create Indexes for better performance
CREATE INDEX idx_rentals_dates ON rentals(start_date, end_date);
CREATE INDEX idx_vehicles_type ON vehicles(vehicle_type);
