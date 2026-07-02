-- SaaS Phase 2: Subscription & Billing ভিত্তি
-- আগে backup নিয়ে নিন: mysqldump car_rental_db > db/backups/pre_phase2_billing_<date>.sql

-- ── saas_settings: SuperAdmin-configurable global billing settings ─
CREATE TABLE saas_settings (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    key_name    VARCHAR(100) UNIQUE NOT NULL,
    value       VARCHAR(500) NOT NULL,
    description TEXT,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO saas_settings (key_name, value, description) VALUES
('price_per_vehicle',  '500',  'প্রতি গাড়ি মাসিক মূল্য (টাকা)'),
('trial_days',         '30',   'নতুন tenant-এর trial period (দিন)'),
('invoice_due_days',   '7',    'Invoice তৈরির পর payment-এর deadline (দিন)');

-- ── subscriptions: প্রতিটি tenant-এর সাবস্ক্রিপশন state ────────────
-- নোট: subscription_plans টেবিল বাদ দেওয়া হয়েছে (একক global price,
-- saas_settings-এ) — তাই এখানে plan_id নেই, মূল পরিকল্পনার থেকে বিচ্যুতি।
CREATE TABLE subscriptions (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       INT NOT NULL,
    started_at      DATE NOT NULL,
    status          ENUM('trialing','active','past_due','cancelled') DEFAULT 'trialing',
    vehicle_count   INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    INDEX idx_subscriptions_tenant (tenant_id)
);

-- ── subscription_invoices: মাসিক invoice রেকর্ড ─────────────────────
CREATE TABLE subscription_invoices (
    id                  INT PRIMARY KEY AUTO_INCREMENT,
    tenant_id           INT NOT NULL,
    subscription_id     INT NOT NULL,
    invoice_number      VARCHAR(50) UNIQUE NOT NULL,
    invoice_date        DATE NOT NULL,
    due_date            DATE NOT NULL,
    vehicle_count       INT NOT NULL,
    price_per_vehicle   DECIMAL(10,2) NOT NULL,
    total_amount        DECIMAL(10,2) NOT NULL,
    status              ENUM('pending','paid','overdue','waived') DEFAULT 'pending',
    paid_at             TIMESTAMP NULL,
    payment_method      VARCHAR(50),
    payment_ref         VARCHAR(255),
    notes               TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoices_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_invoices_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    INDEX idx_invoices_tenant (tenant_id),
    INDEX idx_invoices_status (status)
);

-- ── বিদ্যমান tenant(s)-এর জন্য subscriptions রো backfill ───────────
-- tenants.status → subscriptions.status ম্যাপিং:
--   trial→trialing, active→active, suspended→past_due, cancelled→cancelled
INSERT INTO subscriptions (tenant_id, started_at, status, vehicle_count)
SELECT
    t.id,
    CURDATE(),
    CASE t.status
        WHEN 'trial'     THEN 'trialing'
        WHEN 'active'    THEN 'active'
        WHEN 'suspended' THEN 'past_due'
        ELSE 'cancelled'
    END,
    (SELECT COUNT(*) FROM vehicles v WHERE v.tenant_id = t.id AND v.status != 'inactive')
FROM tenants t;
