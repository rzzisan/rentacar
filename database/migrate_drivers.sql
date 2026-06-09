-- Drivers table
CREATE TABLE IF NOT EXISTS drivers (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  name             VARCHAR(100) NOT NULL,
  mobile           VARCHAR(20)  NOT NULL,
  profile_picture  VARCHAR(255) DEFAULT NULL,
  email            VARCHAR(100) NOT NULL UNIQUE,
  password         VARCHAR(255) NOT NULL,
  commission_rate  DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  status           ENUM('active','inactive') DEFAULT 'active',
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Driver → Vehicle assignments (many-to-many)
CREATE TABLE IF NOT EXISTS driver_vehicles (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  driver_id   INT NOT NULL,
  vehicle_id  INT NOT NULL,
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY  uq_driver_vehicle (driver_id, vehicle_id),
  FOREIGN KEY (driver_id)  REFERENCES drivers(id)  ON DELETE CASCADE,
  FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
);
