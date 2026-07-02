-- SaaS Phase 1: মাল্টি-টেনেন্সি ভিত্তি
-- আগে backup নিয়ে নিন: mysqldump car_rental_db > db/backups/pre_tenant_migration_<date>.sql

-- ── নতুন টেবিল: tenants ─────────────────────────────────────────
CREATE TABLE tenants (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    phone           VARCHAR(20),
    address         TEXT,
    logo_path       VARCHAR(500),
    status          ENUM('trial','active','suspended','cancelled') DEFAULT 'trial',
    trial_ends_at   DATE NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ── নতুন টেবিল: super_admins (users থেকে সম্পূর্ণ আলাদা) ──────────
CREATE TABLE super_admins (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Default tenant (বিদ্যমান সব ডেটা এখানে assign হবে) ────────────
INSERT INTO tenants (id, name, email, status, trial_ends_at)
VALUES (1, 'Default Tenant', 'admin@car.zisan.me', 'active', '2099-12-31');

-- ── বিদ্যমান টেবিলে tenant_id যোগ ──────────────────────────────────
-- প্যাটার্ন: nullable যোগ → backfill → NOT NULL করা (DEFAULT ছাড়া, যাতে
-- ভবিষ্যতে tenant_id ছাড়া INSERT করলে সশব্দে ব্যর্থ হয়, চুপচাপ leak না হয়)

ALTER TABLE users ADD COLUMN tenant_id INT NULL AFTER id;
UPDATE users SET tenant_id = 1;
ALTER TABLE users MODIFY tenant_id INT NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id), ADD INDEX idx_users_tenant (tenant_id);

ALTER TABLE vehicles ADD COLUMN tenant_id INT NULL AFTER id;
UPDATE vehicles SET tenant_id = 1;
ALTER TABLE vehicles MODIFY tenant_id INT NOT NULL;
ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id), ADD INDEX idx_vehicles_tenant (tenant_id);

ALTER TABLE drivers ADD COLUMN tenant_id INT NULL AFTER id;
UPDATE drivers SET tenant_id = 1;
ALTER TABLE drivers MODIFY tenant_id INT NOT NULL;
ALTER TABLE drivers ADD CONSTRAINT fk_drivers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id), ADD INDEX idx_drivers_tenant (tenant_id);

ALTER TABLE managers ADD COLUMN tenant_id INT NULL AFTER id;
UPDATE managers SET tenant_id = 1;
ALTER TABLE managers MODIFY tenant_id INT NOT NULL;
ALTER TABLE managers ADD CONSTRAINT fk_managers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id), ADD INDEX idx_managers_tenant (tenant_id);

ALTER TABLE rentals ADD COLUMN tenant_id INT NULL AFTER id;
UPDATE rentals SET tenant_id = 1;
ALTER TABLE rentals MODIFY tenant_id INT NOT NULL;
ALTER TABLE rentals ADD CONSTRAINT fk_rentals_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id), ADD INDEX idx_rentals_tenant (tenant_id);

ALTER TABLE maintenance ADD COLUMN tenant_id INT NULL AFTER id;
UPDATE maintenance SET tenant_id = 1;
ALTER TABLE maintenance MODIFY tenant_id INT NOT NULL;
ALTER TABLE maintenance ADD CONSTRAINT fk_maintenance_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id), ADD INDEX idx_maintenance_tenant (tenant_id);

ALTER TABLE customers ADD COLUMN tenant_id INT NULL AFTER id;
UPDATE customers SET tenant_id = 1;
ALTER TABLE customers MODIFY tenant_id INT NOT NULL;
ALTER TABLE customers ADD CONSTRAINT fk_customers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id), ADD INDEX idx_customers_tenant (tenant_id);

-- ── api_tokens-এও tenant_id (mobile bearer-token flow-এর জন্য, nullable — superadmin token-এ NULL) ─
ALTER TABLE api_tokens ADD COLUMN tenant_id INT NULL AFTER role;

-- trip_expenses, settlements, driver_vehicles, manager_vehicles, vehicle_documents:
-- tenant_id লাগবে না — এগুলো rental_id/vehicle_id/driver_id/manager_id দিয়ে
-- parent টেবিলে tenant_id-তে derive হয়, আলাদা column redundant।
