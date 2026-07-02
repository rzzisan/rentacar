# Car Rental SaaS রূপান্তর — সম্পূর্ণ পরিকল্পনা

## এই ডকুমেন্টের উদ্দেশ্য
এই ফাইলটি একটি AI agent এবং ডেভেলপারের জন্য reference হিসেবে লেখা হয়েছে।
প্রতিটি phase implement করার আগে এই ফাইল পড়তে হবে যাতে context বোঝা যায়।
কোনো কাজ সম্পন্ন হলে সেই phase-এর status আপডেট করতে হবে।

---

## প্রজেক্ট প্রেক্ষাপট

### বর্তমান অবস্থা
`car.zisan.me` একটি single-tenant রেন্ট-এ-কার ম্যানেজমেন্ট সিস্টেম।
- **একটিমাত্র admin** অ্যাকাউন্ট আছে যে সব গাড়ি, ড্রাইভার, ম্যানেজার এবং ট্রিপ দেখতে ও পরিচালনা করতে পারে।
- Driver এবং Manager আলাদা টেবিলে থাকে (`drivers`, `managers`) — `users` টেবিলে নয়।
- সব ডেটা একটাই global pool-এ — কোনো tenant isolation নেই।

### রূপান্তরের লক্ষ্য
এই সিস্টেমটিকে **SaaS (Software as a Service) প্রোডাক্ট** হিসেবে রূপান্তরিত করতে হবে যেখানে:
- একাধিক রেন্ট-এ-কার ব্যবসা (tenant) এই সিস্টেম ব্যবহার করবে
- প্রতিটি ব্যবসার **নিজস্ব এবং সম্পূর্ণ আলাদা** গাড়ি, ড্রাইভার, ম্যানেজার এবং ট্রিপ থাকবে
- **Billing:** প্রতি গাড়ি × মাসিক রেট — মাসিক invoice তৈরি হবে
- **rzzisan** (আমরা) হব SuperAdmin — সব tenant দেখতে ও পরিচালনা করতে পারব
- Payment: বাংলাদেশে প্রথমে manual confirmation, পরে gateway

---

## নতুন Role hierarchy

```
SuperAdmin (rzzisan/আমরা)
    │── সব tenant দেখা, billing manage, suspend/activate
    │
    ├── Tenant ১ (রহিম কার রেন্টাল)
    │       ├── Admin (ব্যবসার মালিক)
    │       ├── Manager(s) — শুধু নিজ tenant-এর গাড়ি
    │       └── Driver(s)  — শুধু নিজ tenant-এর ট্রিপ
    │
    ├── Tenant ২ (করিম রেন্টাল)
    │       ├── Admin
    │       ├── Manager(s)
    │       └── Driver(s)
    │
    └── Tenant N ...
```

---

## সম্পূর্ণ Database Schema পরিবর্তন

### নতুন টেবিল ১: `tenants`
```sql
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
```
**কেন:** প্রতিটি ব্যবসা একটি tenant। সব entity এই tenant-এর সাথে যুক্ত থাকবে।

### নতুন টেবিল ২: `subscription_plans`
```sql
CREATE TABLE subscription_plans (
    id                  INT PRIMARY KEY AUTO_INCREMENT,
    name                VARCHAR(100) NOT NULL,
    price_per_vehicle   DECIMAL(10,2) NOT NULL,
    max_vehicles        INT DEFAULT NULL,
    max_drivers         INT DEFAULT NULL,
    features            JSON,
    is_active           TINYINT(1) DEFAULT 1,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
**কেন:** ভবিষ্যতে বিভিন্ন plan (Basic/Pro/Enterprise) রাখার সুবিধার্থে।

### নতুন টেবিল ৩: `subscriptions`
```sql
CREATE TABLE subscriptions (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    tenant_id       INT NOT NULL REFERENCES tenants(id),
    plan_id         INT NOT NULL REFERENCES subscription_plans(id),
    started_at      DATE NOT NULL,
    status          ENUM('trialing','active','past_due','cancelled') DEFAULT 'trialing',
    vehicle_count   INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### নতুন টেবিল ৪: `subscription_invoices`
```sql
CREATE TABLE subscription_invoices (
    id                  INT PRIMARY KEY AUTO_INCREMENT,
    tenant_id           INT NOT NULL REFERENCES tenants(id),
    subscription_id     INT NOT NULL REFERENCES subscriptions(id),
    invoice_number      VARCHAR(50) UNIQUE,
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
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
**কেন:** প্রতি মাসে প্রতিটি tenant-এর জন্য একটি invoice তৈরি হবে। গাড়ির সংখ্যা × মূল্য = মোট বিল।

### `super_admins` টেবিল (আলাদা — users থেকে না)
```sql
CREATE TABLE super_admins (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
**কেন:** SuperAdmin tenant-এর অংশ নয়, সম্পূর্ণ আলাদা entity। `users` টেবিলে রাখলে tenant_id nullable হতে হবে — বিপজ্জনক।

### বিদ্যমান টেবিলে `tenant_id` যোগ
```sql
-- সব entity-তে tenant_id foreign key
ALTER TABLE users       ADD COLUMN tenant_id INT REFERENCES tenants(id);
ALTER TABLE vehicles    ADD COLUMN tenant_id INT REFERENCES tenants(id);
ALTER TABLE drivers     ADD COLUMN tenant_id INT REFERENCES tenants(id);
ALTER TABLE managers    ADD COLUMN tenant_id INT REFERENCES tenants(id);
ALTER TABLE rentals     ADD COLUMN tenant_id INT REFERENCES tenants(id);
ALTER TABLE maintenance ADD COLUMN tenant_id INT REFERENCES tenants(id);
ALTER TABLE customers   ADD COLUMN tenant_id INT REFERENCES tenants(id);

-- trip_expenses, settlements: rental_id থেকে tenant derive হয় — আলাদা FK লাগবে না
-- কিন্তু performance-এর জন্য settlements-এ রাখা যেতে পারে (Phase 2-তে সিদ্ধান্ত)
```
**কেন:** প্রতিটি query-তে `WHERE tenant_id = ?` দিয়ে data isolation নিশ্চিত হবে।

---

## Authentication পরিবর্তন

### বর্তমান Login Flow
```
POST /api/auth/login.php
  → users টেবিল চেক (admin/employee/customer)
  → managers টেবিল চেক (manager)
  → drivers টেবিল চেক (driver)
  → $_SESSION['user_id'], $_SESSION['role'] সেট
```

### নতুন Login Flow (তিন স্তর)
```
POST /api/auth/login.php
  1. super_admins টেবিল চেক → role='superadmin', tenant_id=NULL
  2. users টেবিল চেক (admin) → role='admin', tenant_id সেট
  3. managers টেবিল চেক → role='manager', tenant_id সেট
  4. drivers টেবিল চেক → role='driver', tenant_id সেট

Session-এ যোগ হবে: $_SESSION['tenant_id']
```

### `_helpers.php` নতুন functions
```php
// বর্তমান tenant_id session থেকে নেওয়া
function get_tenant_id() {
    if (!isset($_SESSION['tenant_id'])) json_response(['success'=>false,'message'=>'Tenant not found'], 403);
    return (int)$_SESSION['tenant_id'];
}

// SuperAdmin check
function require_superadmin() {
    require_auth();
    if ($_SESSION['role'] !== 'superadmin') json_response(['success'=>false,'message'=>'Access denied'], 403);
}

// Tenant active check (suspended হলে block)
function require_active_tenant() {
    $tid = get_tenant_id();
    // tenants টেবিল থেকে status চেক — suspended হলে 403
}
```

---

## API পরিবর্তন (Phase ১ — সবচেয়ে বড় কাজ)

প্রতিটি existing API endpoint-এ নিচের পরিবর্তন করতে হবে:

### Pattern (প্রতিটি endpoint-এ)
```php
// আগে:
$stmt = $conn->prepare("SELECT * FROM vehicles WHERE status = ?");

// পরে:
$tid = get_tenant_id();
$stmt = $conn->prepare("SELECT * FROM vehicles WHERE tenant_id = ? AND status = ?");
$stmt->bind_param('is', $tid, $status);
```

### প্রভাবিত API files
| File | পরিবর্তন |
|---|---|
| `api/vehicles/index.php` | WHERE tenant_id + INSERT-এ tenant_id |
| `api/vehicles/show.php` | WHERE tenant_id |
| `api/vehicles/update.php` | WHERE tenant_id (ownership verify) |
| `api/vehicles/destroy.php` | WHERE tenant_id |
| `api/admin/stats.php` | WHERE tenant_id |
| `api/admin/rentals/index.php` | WHERE tenant_id |
| `api/admin/rentals/show.php` | JOIN-এ tenant_id |
| `api/admin/rentals/update.php` | WHERE tenant_id |
| `api/admin/rentals/update_status.php` | WHERE tenant_id |
| `api/admin/rentals/expenses.php` | JOIN-এ tenant_id |
| `api/admin/settlements/index.php` | WHERE tenant_id |
| `api/admin/settlements/collect-payment.php` | WHERE tenant_id |
| `api/admin/drivers/index.php` | WHERE tenant_id |
| `api/admin/drivers/dues.php` | WHERE tenant_id |
| `api/admin/drivers/collect.php` | WHERE tenant_id |
| `api/admin/managers/index.php` | WHERE tenant_id |
| `api/admin/reports.php` | WHERE tenant_id (সব ৫ section) |
| `api/admin/maintenance/index.php` | WHERE tenant_id |
| `api/admin/documents/index.php` | WHERE tenant_id |
| `api/manager/*` | tenant_id filter (manager-এর assigned vehicles-এর মধ্যে) |
| `api/driver/*` | tenant_id via driver record |
| `api/auth/login.php` | tenant_id session-এ সেট |

---

## নতুন API endpoints

### SuperAdmin API (`api/superadmin/`)
```
GET    api/superadmin/tenants/index.php         → সব tenant তালিকা
POST   api/superadmin/tenants/create.php        → নতুন tenant তৈরি
PUT    api/superadmin/tenants/update.php?id=    → tenant info আপডেট
PUT    api/superadmin/tenants/status.php?id=    → active/suspended toggle
GET    api/superadmin/billing/invoices.php      → সব invoice (filter: tenant, status, month)
POST   api/superadmin/billing/mark-paid.php     → invoice paid mark করা
GET    api/superadmin/billing/stats.php         → revenue dashboard
GET    api/superadmin/tenants/impersonate.php?id= → কোনো tenant-এর admin হিসেবে login (debug/support)
```

### Tenant Registration API (`api/auth/`)
```
POST   api/auth/register.php     → নতুন tenant + admin user তৈরি; trial শুরু
GET    api/auth/check-email.php  → email availability check
```

### Billing Cron Script (`api/cron/`)
```
GET    api/cron/generate-invoices.php   → মাসের ১ তারিখে invoice তৈরি
GET    api/cron/check-overdue.php       → due_date পার হলে tenant suspend
```

---

## Frontend পরিবর্তন

### নতুন Pages

#### SuperAdmin Panel (`frontend/src/pages/superadmin/`)
```
Dashboard.tsx     → tenant count, monthly revenue, overdue invoices
Tenants.tsx       → tenant তালিকা, status badge, edit, suspend, impersonate
Billing.tsx       → invoice তালিকা, filter by month/tenant/status, mark-paid
Revenue.tsx       → মাসিক আয় chart, vehicle count trend
```

#### Tenant Onboarding (`frontend/src/pages/`)
```
Register.tsx      → নতুন tenant sign-up form
Trial.tsx         → trial status + upgrade prompt (যখন trial শেষ হবে)
```

### Route পরিবর্তন (`App.tsx`)
```typescript
// নতুন route
<Route path="/superadmin/*" element={<ProtectedRoute roles={['superadmin']} />}>
  <Route path="dashboard" element={<SuperAdminDashboard />} />
  <Route path="tenants" element={<SuperAdminTenants />} />
  <Route path="billing" element={<SuperAdminBilling />} />
</Route>

// Registration page (public)
<Route path="/register" element={<Register />} />
```

### Login পরিবর্তন
- Superadmin login করলে → `/superadmin/dashboard`
- Admin login করলে → `/admin/dashboard` (আগের মতো)
- Suspended tenant login করলে → error বার্তা দেখাবে

---

## Billing Logic বিস্তারিত

### Invoice তৈরির নিয়ম (Cron — মাসের ১ তারিখ)
```
প্রতিটি active/trialing tenant-এর জন্য:
  1. সেই tenant-এর active vehicles গণনা করো
     (status != 'inactive' AND tenant_id = ?)
  2. invoice তৈরি করো:
     - vehicle_count = গণনা
     - price_per_vehicle = plan-এর মূল্য
     - total_amount = vehicle_count × price_per_vehicle
     - invoice_date = আজ (১ তারিখ)
     - due_date = ৭ দিন পরে
     - status = 'pending'
  3. tenant-কে email notification (ভবিষ্যতে)
```

### Overdue Check (Cron — প্রতিদিন)
```
যেসব invoice due_date পার হয়েছে কিন্তু status='pending':
  1. invoice status → 'overdue'
  2. tenant status → 'suspended'
  3. suspended tenant login করতে পারবে না
  4. API-তে require_active_tenant() block করবে
```

### Trial Logic
```
Tenant তৈরির সময়:
  - trial_ends_at = আজ + ৩০ দিন
  - subscription status = 'trialing'

Trial শেষ হলে (Cron):
  - subscription/trial শেষের কথা জানানো হবে
  - Payment না হলে → suspended
```

---

## Implementation Phases

---

### Phase 1: মাল্টি-টেনেন্সি ভিত্তি
**Status: ✅ সম্পন্ন (2026-07-02)**

**লক্ষ্য:** বিদ্যমান সব data-কে tenant-aware করা

**কাজের তালিকা:**
- [x] `tenants` টেবিল তৈরি
- [x] `super_admins` টেবিল তৈরি
- [x] বিদ্যমান সব টেবিলে `tenant_id` column যোগ (users, vehicles, drivers, managers, rentals, maintenance, customers — NOT NULL, no default, FK+index; api_tokens-এও nullable tenant_id)
- [x] বিদ্যমান data-এ tenant_id সেট (default tenant id=1 তৈরি করে সব পুরানো data সেখানে backfill)
- [x] `api/auth/login.php` — superadmin check সবার আগে যোগ + tenant_id session (users/drivers/managers তিন fallback-এই)
- [x] `api/_helpers.php` — `get_tenant_id()`, `require_superadmin()`, `require_active_tenant()` functions
- [x] সব `api/admin/*` endpoint-এ tenant_id filter (rentals, settlements, drivers, managers, customers, maintenance, documents, reports, stats)
- [x] সব `api/vehicles/*` endpoint-এ tenant_id filter
- [x] সব `api/manager/*` endpoint-এ tenant_id filter (manager-এর tenant inherit)
- [x] সব `api/driver/*` endpoint-এ tenant_id filter (driver-এর tenant inherit)
- [x] `api/admin/reports.php` — সব ৫টি section-এ tenant_id
- [x] প্রথম superadmin account তৈরি (rzzisan@gmail.com, DB-তে manually insert)
- [x] Test: দুটি tenant তৈরি করে data isolation + IDOR protection verify (read ও write দুটোতেই), টেস্ট tenant পরে মুছে ফেলা হয়েছে

**Implementation নোট:**
- DB migration: `db/migrations/001_multi_tenancy.sql` (আগে `db/backups/`-এ mysqldump backup নেওয়া হয়েছে)
- ~60টি API ফাইল আপডেট হয়েছে একই প্যাটার্নে (`$tid = get_tenant_id();` + WHERE/INSERT-এ tenant_id) — 4টি সমান্তরাল সাব-এজেন্ট দিয়ে (admin rentals/settlements/reports/documents, admin drivers/managers/customers/maintenance, manager panel, driver panel), তারপর manual lint+grep+curl দিয়ে verify করা হয়েছে
- Verification-এর সময় ২টি **পুরনো (migration-অসম্পর্কিত) bug** ধরা পড়েছে ও ঠিক করা হয়েছে: `api/manager/drivers/index.php` ও `api/manager/customers/index.php`-এ `$in` (vehicle placeholder list) একই SQL-এ একাধিকবার ব্যবহৃত হচ্ছিল কিন্তু params array-তে একবারই দেওয়া ছিল — `bind_param()` ArgumentCountError দিয়ে fatal করত। এই bug production-এ live ছিল (manager panel-এর ড্রাইভার/গ্রাহক তালিকা কখনো কাজ করেনি) — এখন ঠিক হয়েছে।
- `trip_expenses`, `settlements`, `driver_vehicles`, `manager_vehicles`, `vehicle_documents`-এ tenant_id column নেই — parent (`rentals`/`vehicles`) join করে tenant verify করা হয়
- IDOR প্রতিরোধ সর্বত্র প্রাধান্য পেয়েছে: client-supplied ID দিয়ে যেকোনো lookup/update/delete/assign-এ tenant ownership check বাধ্যতামূলক করা হয়েছে

**Migration Script:**
```sql
-- বিদ্যমান সিস্টেমের জন্য: একটি default tenant তৈরি
INSERT INTO tenants (name, email, status, trial_ends_at)
VALUES ('Default Tenant', 'admin@car.zisan.me', 'active', '2099-12-31');

-- সব পুরানো data এই tenant-এ assign
UPDATE vehicles  SET tenant_id = 1;
UPDATE drivers   SET tenant_id = 1;
UPDATE managers  SET tenant_id = 1;
UPDATE rentals   SET tenant_id = 1;
UPDATE users     SET tenant_id = 1 WHERE role = 'admin';
-- ইত্যাদি
```

**এই phase সম্পন্ন হলে:** একাধিক সম্পূর্ণ আলাদা tenant চলতে পারবে।

---

### Phase 2: Subscription & Billing ভিত্তি
**Status: ✅ সম্পন্ন (2026-07-02)**

**লক্ষ্য:** billing schema তৈরি এবং manual invoice management

**কাজের তালিকা:**
- [x] `saas_settings` টেবিল তৈরি + default values insert (price_per_vehicle=৳৫০০, trial_days=৩০, invoice_due_days=৭)
- [x] `subscriptions` টেবিল তৈরি (plan_id নেই — নিচে নোট দেখুন)
- [x] `subscription_invoices` টেবিল তৈরি
- [x] `api/cron/generate-invoices.php` — monthly invoice generator (saas_settings থেকে price পড়ে, idempotent — একই মাসে দ্বিতীয়বার চললে duplicate invoice হয় না)
- [x] `api/cron/check-overdue.php` — overdue check + tenant suspend
- [x] Server-এ cron job সেট: `0 6 1 * * php8.3 .../generate-invoices.php`
- [x] Server-এ cron job সেট: `0 8 * * * php8.3 .../check-overdue.php`
- [x] Suspended tenant login block (`require_active_tenant()`) — **এবং mid-session enforcement**-ও যোগ হয়েছে (নিচে দেখুন)

**সিদ্ধান্ত (2026-07-02, ব্যবহারকারীর সাথে confirm করা):** trial ও active — দুই ধরনের tenant-এরই মাসিক invoice তৈরি হয় (শুধু active নয়)। তাই trial চলাকালীন invoice unpaid থেকে গেলে `check-overdue.php` সেই tenant-কে suspend করে দিতে পারে — trial period শেষ হওয়ার অপেক্ষা করে না।

**নোট:** `subscription_plans` টেবিল বাদ দেওয়া হয়েছে। একটিমাত্র global `price_per_vehicle` থাকবে `saas_settings`-এ — SuperAdmin যেকোনো সময় পরিবর্তন করতে পারবে। তাই `subscriptions` টেবিলে মূল doc-এর `plan_id` column নেই (deviation)। Multi-plan support ভবিষ্যতে দরকার হলে যোগ করা যাবে।

**Implementation নোট:**
- `db/migrations/002_subscription_billing.sql` — বিদ্যমান tenant(s)-এর জন্য `subscriptions` রো auto-backfill করে (tenants.status অনুযায়ী ম্যাপ করে)
- Cron script দুটো **CLI-only** (`php_sapi_name() !== 'cli'` চেক করে 403 দেয়) — মূল doc-এ এগুলোকে HTTP `GET` endpoint হিসেবে লেখা ছিল, কিন্তু auth ছাড়া HTTP-accessible cron endpoint রাখা একটা real vulnerability (যে কেউ hit করে বিলিং সাইকেল/tenant suspend জোর করে ট্রিগার করতে পারত) — তাই deviation করে CLI-only করা হয়েছে, root crontab দিয়ে সরাসরি PHP CLI চালানো হয়
- **গুরুত্বপূর্ণ:** সার্ভারে `/usr/bin/php` symlink PHP 8.5-এ পয়েন্ট করে যেখানে `mysqli` extension নেই — Apache আসলে **PHP 8.3-fpm** ব্যবহার করে (mysqli আছে)। তাই cron entry-তে সুনির্দিষ্টভাবে `/usr/bin/php8.3` ব্যবহার করা হয়েছে।
- **`require_active_tenant()` mid-session enforcement:** আগে (Phase 1-এ) এই ফাংশন শুধু login-এ কল হতো — মানে suspend হওয়ার পরেও আগে থেকে লগইন করা সেশন কাজ করতে থাকত যতক্ষণ না ইউজার আবার লগইন করে। এখন `require_auth()`-এর ভেতরেই (superadmin বাদে) কল হয় — তাই প্রতিটি API রিকোয়েস্টে suspend/cancel চেক হয়, একবার cron দিয়ে suspend হলে সাথে সাথেই সব চলমান সেশন ব্লক হয়ে যায়। Live curl টেস্ট দিয়ে verify করা হয়েছে (login করার পর tenant suspend করে একই session cookie দিয়ে ফের কল করলে 403 আসে)।
- Log ফাইল: `storage/logs/cron-invoices.log`, `storage/logs/cron-overdue.log` (gitignored)
- প্রথম বাস্তব invoice তৈরি হয়েছে test করার সময়: `INV-202607-T1`, tenant 1 (Default Tenant, real live business), ৳১০০০ (২টি গাড়ি × ৳৫০০), due 2026-07-09 — এটা মুছে ফেলা হয়নি, প্রকৃত জুলাই ২০২৬-এর বিল হিসেবেই থাকছে

**এই phase সম্পন্ন হলে:** মাসে মাসে invoice তৈরি হবে, overdue-তে tenant suspend হবে (login ও চলমান session দুটোতেই)।

---

### Phase 3: SuperAdmin Panel
**Status: ⬜ বাকি**

**লক্ষ্য:** আমরা (rzzisan) সব tenant পরিচালনা করতে পারব

**কাজের তালিকা:**
- [ ] `api/superadmin/tenants/` — CRUD endpoints
- [ ] `api/superadmin/billing/invoices.php` — invoice তালিকা + mark-paid (manual)
- [ ] `api/superadmin/billing/stats.php` — revenue stats
- [ ] `api/superadmin/settings/index.php` — GET/PUT saas_settings (price, trial_days, due_days)
- [ ] Frontend: `SuperAdminDashboard.tsx`
- [ ] Frontend: `SuperAdminTenants.tsx` — tenant list, edit, suspend/activate
- [ ] Frontend: `SuperAdminBilling.tsx` — invoice list, manual mark-paid (bKash ref লেখার field)
- [ ] Frontend: `SuperAdminSettings.tsx` — price/trial/due_days পরিবর্তন
- [ ] App.tsx-এ superadmin routes
- [ ] Login redirect: superadmin → `/superadmin/dashboard`

**এই phase সম্পন্ন হলে:** web থেকে সব tenant ও billing পরিচালনা করা যাবে।

---

### Phase 4: Tenant Onboarding & Registration
**Status: ⬜ বাকি**

**লক্ষ্য:** নতুন ব্যবসা নিজে sign-up করতে পারবে

**কাজের তালিকা:**
- [ ] `api/auth/register.php` — tenant + admin user একসাথে তৈরি
- [ ] Frontend: `Register.tsx` — registration form
- [ ] Trial শেষের warning (admin dashboard-এ banner)
- [ ] Suspended account-এ login করলে বিশেষ বার্তা + payment instruction
- [ ] `/register` route (public)

**এই phase সম্পন্ন হলে:** নতুন ব্যবসা স্বয়ংক্রিয়ভাবে sign-up করতে পারবে।

---

### Phase 5: Payment Gateway Integration
**Status: ⬜ বাকি (Phase 3/4 সম্পন্নের পরে)**

**সিদ্ধান্ত (2026-07-02):** আপাতত Manual payment। Phase 3-এ SuperAdmin billing panel থেকে invoice manually "Paid" mark করবে। bKash-এ payment নিয়ে TrxID invoice-এ লেখা হবে। Gateway পরবর্তীতে যোগ হবে।

**লক্ষ্য:** automatic payment collection (gateway integration)

**প্রস্তাবিত gateway:** SSLCommerz (বাংলাদেশে bKash/Nagad/card সব সাপোর্ট করে)

**কাজের তালিকা:**
- [ ] SSLCommerz merchant account নেওয়া
- [ ] `api/payment/initiate.php` — payment শুরু করা
- [ ] `api/payment/success.php` — SSLCommerz callback, invoice auto mark-paid
- [ ] `api/payment/fail.php` — failure handle
- [ ] Frontend: tenant-এর invoice page-এ "পেমেন্ট করুন" button
- [ ] Payment confirmation-এ tenant status re-activate (suspended → active)

**এই phase শুরু করার আগে দরকার:** SSLCommerz merchant account।

---

### Phase 6: Android App Tenant Support
**Status: ⬜ বাকি**

**লক্ষ্য:** Android driver app-এ multi-tenant support

**কাজের তালিকা:**
- [ ] Login response-এ tenant_id Android-এ store করা
- [ ] API calls-এ tenant context — server session থেকে automatic (extra params লাগবে না)
- [ ] Suspended tenant login করলে error screen দেখানো
- [ ] Build + deploy নতুন APK

**নোট:** Android app driver/manager হিসেবে login করে। tenant_id server session-এ থাকে, তাই API calls-এ extra change লাগবে না। শুধু error handling করতে হবে।

---

## গুরুত্বপূর্ণ সিদ্ধান্ত ও কারণ

### কেন `super_admins` আলাদা টেবিল?
`users` টেবিলে superadmin রাখলে `tenant_id` NULL হতে হবে। তাহলে সব query-তে `tenant_id IS NULL OR tenant_id = ?` দিতে হবে — বিপজ্জনক এবং ভুলের সম্ভাবনা বেশি। আলাদা টেবিলে রাখলে login logic একটু জটিল কিন্তু data isolation নিরাপদ।

### কেন `trip_expenses`/`settlements`-এ tenant_id দেওয়া হয়নি?
এই টেবিলগুলো `rental_id` এর মাধ্যমে `rentals` এ যুক্ত। `rentals`-এ `tenant_id` থাকলে JOIN করে tenant verify করা যায়। আলাদা column redundant এবং sync-এর ঝামেলা তৈরি করতে পারে। তবে performance issue হলে Phase 2-তে যোগ করা যাবে।

### কেন vehicle-based billing?
গাড়ির সংখ্যা সহজে গণনাযোগ্য এবং ব্যবসার আকার বোঝার সবচেয়ে প্রাসঙ্গিক metric। ড্রাইভার বা ট্রিপ সংখ্যা দিয়ে billing করলে ব্যবসায়ীরা ড্রাইভার কম রাখার চেষ্টা করতে পারে।

### কেন trial 30 দিন?
ব্যবসায়ী প্রথমে ব্যবহার করে দেখবে, তারপর payment করবে। এটি standard SaaS practice।

### Tenant URL structure
Subdomain (`rahimcars.car.zisan.me`) না করে single domain রাখা হচ্ছে। কারণ: SSL wildcard certificate দরকার, DNS setup জটিল। Session-based tenant identification সহজ এবং যথেষ্ট।

---

## Billing মূল্য ও Trial নির্ধারণ

**সিদ্ধান্ত (2026-07-02):** মূল্য এবং trial period hardcode করা হবে না। SuperAdmin যেকোনো সময় পরিবর্তন করতে পারবে।

### `saas_settings` টেবিল (নতুন — Phase 2-এ তৈরি হবে)
```sql
CREATE TABLE saas_settings (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    key_name    VARCHAR(100) UNIQUE NOT NULL,
    value       VARCHAR(500) NOT NULL,
    description TEXT,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Default values (SuperAdmin পরিবর্তন করবে)
INSERT INTO saas_settings (key_name, value, description) VALUES
('price_per_vehicle',  '500',  'প্রতি গাড়ি মাসিক মূল্য (টাকা)'),
('trial_days',         '30',   'নতুন tenant-এর trial period (দিন)'),
('invoice_due_days',   '7',    'Invoice তৈরির পর payment-এর deadline (দিন)');
```

### SuperAdmin Settings Panel (Phase 3-এ যোগ হবে)
SuperAdmin panel থেকে `saas_settings` এর মান পরিবর্তন করা যাবে:
- প্রতি গাড়ি মাসিক মূল্য (টাকা)
- Trial period (দিন)
- Invoice due date (দিন)

নতুন tenant তৈরির সময় সেই মুহূর্তের `trial_days` ব্যবহার হবে।
Invoice তৈরির সময় সেই মুহূর্তের `price_per_vehicle` ব্যবহার হবে।

---

## Implementation শুরু করার আগে চেকলিস্ট

- [x] প্রতি গাড়ি মাসিক মূল্য → SuperAdmin settings থেকে configurable (default: ৳৫০০)
- [x] Trial period → SuperAdmin settings থেকে configurable (default: ৩০ দিন)
- [x] Payment method → আপাতত Manual; পরবর্তীতে SSLCommerz gateway যোগ হবে
- [ ] SSLCommerz/bKash merchant account → Phase 5-এর সময় নেওয়া হবে
- [ ] Server-এ cron job চালানো যাবে কিনা verify করা
- [ ] বিদ্যমান data-এর migration script test করা

---

## Phase সম্পন্নের tracking

| Phase | নাম | Status | সম্পন্নের তারিখ |
|---|---|---|---|
| 1 | মাল্টি-টেনেন্সি ভিত্তি | ✅ সম্পন্ন | 2026-07-02 |
| 2 | Subscription & Billing | ✅ সম্পন্ন | 2026-07-02 |
| 3 | SuperAdmin Panel | ⬜ বাকি | — |
| 4 | Tenant Onboarding | ⬜ বাকি | — |
| 5 | Payment Gateway | ⬜ বাকি | — |
| 6 | Android Support | ⬜ বাকি | — |

---

## নতুন ফাইল structure (সম্পন্নের পর)

```
api/
├── _helpers.php           ← get_tenant_id(), require_superadmin(), require_active_tenant() যোগ হবে
├── auth/
│   ├── login.php          ← super_admins চেক + tenant_id session যোগ হবে
│   └── register.php       ← নতুন: tenant self-registration
├── superadmin/            ← নতুন directory
│   ├── tenants/
│   │   ├── index.php      ← GET list / POST create
│   │   ├── update.php     ← PUT
│   │   └── status.php     ← PUT: active/suspended toggle
│   ├── billing/
│   │   ├── invoices.php   ← GET list, POST mark-paid (manual, bKash ref সহ)
│   │   └── stats.php      ← GET revenue dashboard
│   └── settings/
│       └── index.php      ← GET/PUT: price_per_vehicle, trial_days, invoice_due_days
├── cron/                  ← নতুন directory
│   ├── generate-invoices.php
│   └── check-overdue.php
├── admin/   ← বিদ্যমান, tenant_id filter যোগ হবে
├── manager/ ← বিদ্যমান, tenant_id filter যোগ হবে
├── driver/  ← বিদ্যমান, tenant_id filter যোগ হবে
└── vehicles/ ← বিদ্যমান, tenant_id filter যোগ হবে

frontend/src/pages/
├── superadmin/            ← নতুন directory
│   ├── Dashboard.tsx
│   ├── Tenants.tsx
│   ├── Billing.tsx        ← manual mark-paid, bKash TrxID field
│   └── Settings.tsx       ← price/trial/due_days configurable
├── Register.tsx           ← নতুন
└── (বিদ্যমান pages অপরিবর্তিত থাকবে)
```

---

*এই ডকুমেন্ট সর্বশেষ আপডেট: 2026-07-02*
*সিদ্ধান্ত লগ: মূল্য ও trial period SuperAdmin-configurable; payment আপাতত manual (bKash), gateway Phase 5-এ।*
*পরবর্তী কাজ: Phase 1 implementation শুরু।*
