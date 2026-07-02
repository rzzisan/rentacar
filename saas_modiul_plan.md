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
**Status: ✅ সম্পন্ন (2026-07-02)**

**লক্ষ্য:** আমরা (rzzisan) সব tenant পরিচালনা করতে পারব

**কাজের তালিকা:**
- [x] `api/superadmin/tenants/` — CRUD endpoints (index GET/POST, update PUT, status PUT)
- [x] `api/superadmin/billing/invoices.php` — invoice তালিকা + mark-paid (manual)
- [x] `api/superadmin/billing/stats.php` — revenue stats
- [x] `api/superadmin/settings/index.php` — GET/PUT saas_settings (price, trial_days, due_days)
- [x] Frontend: `SuperAdminDashboard.tsx` — stat cards, ৬ মাসের আয় ট্রেন্ড bar chart, সাম্প্রতিক invoice তালিকা
- [x] Frontend: `SuperAdminTenants.tsx` — tenant list, নতুন tenant তৈরি (+ প্রথম admin একসাথে), edit, status পরিবর্তন (trial/active/suspended/cancelled)
- [x] Frontend: `SuperAdminBilling.tsx` — invoice list (tenant/status/month filter), manual mark-paid (bKash ref field)
- [x] Frontend: `SuperAdminSettings.tsx` — price/trial/due_days পরিবর্তন
- [x] App.tsx-এ superadmin routes (`roleHome()` helper দিয়ে redirect ternary-গুলো centralize করা হয়েছে, `frontend/src/lib/roleHome.ts`)
- [x] Login redirect: superadmin → `/superadmin`

**Scope note:** Impersonate ফিচার (tenant-এর admin হিসেবে login) এই phase-এ নেই — checklist-এ ছিল না, আলাদা নিরাপত্তা বিবেচনা দরকার।

**Implementation নোট:**
- Tenant তৈরির সাথেই প্রথম admin user + subscription row (status='trialing') একসাথে তৈরি হয় (transaction-এ) — trial_ends_at সেই মুহূর্তের `saas_settings.trial_days` থেকে গণনা হয়
- Mark-paid করলে, tenant সেই মুহূর্তে suspended থাকলে **স্বয়ংক্রিয়ভাবে active-এ ফিরে যায়** (tenant + subscriptions দুটোই) — manual bKash payment confirm করার মূল উদ্দেশ্য এটাই
- **আবিষ্কৃত ও ঠিক করা বাগ:** `saas_settings.description`-এর বাংলা টেক্সট Phase 2 migration-এ (`mysql ... < file.sql`, charset flag ছাড়া CLI import) double-encoded হয়ে গিয়েছিল — mysqli (charset utf8mb4 সেট করা) দিয়ে সরাসরি UPDATE করে ঠিক করা হয়েছে
- **আবিষ্কৃত ও ঠিক করা বাগ:** `api/auth/update_profile.php` কোনো role-check ছাড়াই `$_SESSION['user_id']` দিয়ে `users` টেবিল আপডেট করত — superadmin session-এর `user_id` আসলে `super_admins.id` (আলাদা টেবিল), তাই superadmin প্রোফাইল বদলাতে গেলে **ভুলবশত একই numeric id-ওয়ালা কোনো tenant-এর admin-এর ডেটা বদলে যেতে পারত**। এখন superadmin-কে explicit block করা হয়েছে (backend + Header.tsx-এ "অ্যাকাউন্ট সেটিংস" মেনু আইটেমও লুকানো)।
- Frontend যাচাই: `npm run build` (tsc + vite, ত্রুটিমুক্ত) + Playwright দিয়ে লাইভ প্রোডাকশনে (https://car.zisan.me) পূর্ণ flow টেস্ট — login→dashboard→tenants→billing→settings→logout, সব পেজে সঠিক ডেটা রেন্ডার + কোনো real console error নেই

**এই phase সম্পন্ন হলে:** web থেকে সব tenant ও billing পরিচালনা করা যাবে।

---

### Phase 4: Tenant Onboarding & Registration
**Status: ✅ সম্পন্ন (2026-07-02)**

**লক্ষ্য:** নতুন ব্যবসা নিজে sign-up করতে পারবে

**কাজের তালিকা:**
- [x] `api/auth/register.php` — tenant + admin user একসাথে তৈরি (transaction-এ, auto-login সহ)
- [x] Frontend: `Register.tsx` — registration form
- [x] Trial শেষের warning (admin dashboard-এ banner) — ≤৭ দিন বাকি থাকলে amber, মেয়াদ পার হলে red
- [x] Suspended account-এ login করলে বিশেষ বার্তা + payment instruction (`require_active_tenant()`-এর message আপডেট)
- [x] `/register` route (public)

**বাড়তি কাজ (ব্যবহারকারীর অনুরোধে, Phase 4-এর সাথেই):**
- [x] **মোবাইল নম্বর দিয়ে লগইন** — admin/manager/driver তিনটাতেই ইমেইল অথবা মোবাইল দিয়ে লগইন করা যায় এখন
  - `users.phone`, `drivers.mobile`, `managers.mobile` — গ্লোবাল UNIQUE constraint যোগ (migration `003_mobile_login.sql`)
  - `api/auth/login.php` request body key `email` → `identifier` (backward-compat: পুরনো `email` key-ও গ্রহণ করে)
  - `includes/User.php::login()` ও drivers/managers query — `WHERE email = ? OR phone/mobile = ?`
  - Registration flow-এ `admin_phone` **বাধ্যতামূলক** (মোবাইল-লগইন কাজে লাগানোর জন্য); SuperAdmin-এর ম্যানুয়াল tenant-creation flow-এ ঐচ্ছিক

**Implementation নোট:**
- `api/auth/register.php` লজিক Phase 3-এর `api/superadmin/tenants/index.php` POST-এর অনুরূপ (একই প্যাটার্ন পুনর্ব্যবহার) — পার্থক্য: public (no auth), admin_phone বাধ্যতামূলক, সফল হলে session সেট করে অটো-লগইন করে দেয়
- `api/auth/me.php` এখন admin/manager/driver response-এ `tenant_status` ও `trial_ends_at` রিটার্ন করে (trial banner-এর ডেটা সোর্স); superadmin-এর জন্য দুটোই `null`
- DB migration আগে duplicate phone/mobile চেক করে নিশ্চিত হওয়া হয়েছে যে বিদ্যমান ডেটায় কোনো conflict নেই, তারপর UNIQUE যোগ হয়েছে
- Verification: curl দিয়ে email+phone উভয় লগইন (admin/manager/driver), register→auto-login→isolation→duplicate-reject সব টেস্ট, তারপর Playwright দিয়ে ব্রাউজারে পূর্ণ flow (register ফর্ম পূরণ → dashboard → DB-তে trial_ends_at কাছাকাছি সেট করে banner visually confirm → logout → মোবাইল নম্বর দিয়ে লগইন) — সব টেস্ট ডেটা পরিষ্কার করা হয়েছে

**এই phase সম্পন্ন হলে:** নতুন ব্যবসা স্বয়ংক্রিয়ভাবে sign-up করতে পারবে, এবং সবাই ইমেইল বা মোবাইল যেকোনো একটা দিয়ে লগইন করতে পারবে।

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

**সিদ্ধান্ত (2026-07-02):** ব্যবহারকারী ইচ্ছাকৃতভাবে Phase 5 আপাতত বাদ দিয়ে সরাসরি Phase 6-এ চলে গেছেন — ম্যানুয়াল payment (SuperAdmin billing panel-এ mark-paid) আপাতত যথেষ্ট। merchant account নেওয়ার পর ভবিষ্যতে ফিরে আসা যাবে।

---

### Phase 6: Android App Tenant Support
**Status: ✅ সম্পন্ন (2026-07-02)**

**লক্ষ্য:** Android driver app-এ multi-tenant support

**কাজের তালিকা:**
- [x] Login response-এ tenant_id Android-এ store করা (`AuthStorage`-এ `SharedPreferences`, `-1` sentinel দিয়ে nullable Int)
- [x] API calls-এ tenant context — server session থেকে automatic (extra params লাগেনি — Phase 1/2-এই হয়ে গিয়েছিল, নিচে দেখুন)
- [x] Suspended tenant login করলে error screen দেখানো — **একটা real bug ধরা পড়ে ঠিক হয়েছে** (নিচে দেখুন)
- [x] Build + deploy নতুন APK

**আবিষ্কার — backend-এর কাজ Phase 1/2-এই হয়ে গিয়েছিল:** mobile bearer-token flow (`_generate_api_token()`/`_validate_bearer_token()`, `api/_helpers.php`) ইতিমধ্যে token-এ `tenant_id` সংরক্ষণ করে এবং প্রতি রিকোয়েস্টে session-এ restore করে; `require_active_tenant()` (Phase 2-এর mid-session enforcement, `require_auth()`-এর ভেতরে) token-ভিত্তিক সেশনেও কাজ করে। তাই "API calls-এ tenant context" আসলে Android-সাইডে কোনো নতুন কোড ছাড়াই ইতিমধ্যে কাজ করছিল — মূল doc-এর নোট সঠিক ছিল।

**আবিষ্কৃত ও ঠিক করা বাগ:** `LoginScreen.kt`-এ `HttpException` (403/401 ইত্যাদি non-2xx status) catch হলে backend-এর প্রকৃত error message (যেমন suspended-tenant-এর বিলের বার্তা) **শুধু `BuildConfig.DEBUG` build-এ** দেখানো হতো — production/release APK-তে সবসময় generic "লগইন ব্যর্থ হয়েছে" দেখাত। মানে suspended tenant-এর driver/manager আসল কারণ (বকেয়া বিল) কখনো জানতেই পারত না release APK দিয়ে। এখন error body-র JSON parse করে `message` ফিল্ড **DEBUG/release নির্বিশেষে সবসময়** দেখানো হয় (parse ব্যর্থ হলে generic ফলব্যাক)। curl দিয়ে verify করা হয়েছে: suspended tenant-এর driver login করলে backend ঠিক এই JSON shape রিটার্ন করে যেটা নতুন কোড parse করে।

**স্কোপ নোট:** mid-session suspension (login-পরবর্তী কোনো API call 403 দিলে) প্রতিটি স্ক্রিনে এখনো `catch (_: Exception) {}` দিয়ে নিরবে গেলা হয় (pre-existing pattern, শুধু login স্ক্রিনে না) — এটা এই phase-এর checklist-এ ছিল না (checklist শুধু login-সময়ের কথা বলে), তাই ইচ্ছাকৃতভাবে বাদ রাখা হয়েছে। ভবিষ্যতে দরকার হলে আলাদা কাজ।

---

### Phase 7: Android In-App Update Notification
**Status: ✅ সম্পন্ন (2026-07-02)**

**লক্ষ্য:** APK Play Store-এ যাচ্ছে না (আপাতত সাইডলোড দিয়ে বিতরণ), তাই Play Store-এর automatic update notification পাওয়া যাবে না। নিজস্ব version-check মেকানিজম বানাতে হবে যাতে পুরনো APK ব্যবহারকারীরা নতুন ভার্সন আছে জানতে পারে এবং সহজে আপডেট করতে পারে।

**সিদ্ধান্ত:** সম্পূর্ণ automatic/silent install না — ব্যবহারকারীকে ডায়ালগ দেখিয়ে ট্যাপ করে ডাউনলোড+ইনস্টল করতে হবে (Android নিজেই "unknown source" অনুমতি চাইবে)। Silent background install করতে `REQUEST_INSTALL_PACKAGES` + `FileProvider` লাগত, এই স্কেলের internal business app-এর জন্য অতিরিক্ত জটিলতা মনে হয়েছে।

**কাজের তালিকা:**
- [x] `android/app/build.gradle.kts`-এ প্রকৃত version bump প্রথা চালু করা (versionCode/versionName এখন পর্যন্ত সবসময় `1`/`1.0.0` স্থির ছিল — কখনো বাড়েনি; এখন `2`/`1.1.0`-এ বাম্প করা হয়েছে, প্রতিটি ভবিষ্যৎ রিলিজে বাড়াতে হবে)
- [x] `/var/www/car-apk/version.json` (static ফাইল, সরাসরি Apache সার্ভ করে — নতুন PHP endpoint লাগেনি) — `{version_code, version_name, apk_url, changelog, force_update}`
- [x] `gen-index.sh` স্ক্রিপ্ট আপডেট: `build.gradle.kts` থেকে versionCode/versionName পড়ে `version.json` অটো-জেনারেট করে (ঐচ্ছিক আর্গুমেন্ট: changelog, force_update)
- [x] Android: app চালু হলে `https://car.zisan.me/apk/version.json` ফেচ করে `BuildConfig.VERSION_CODE`-এর সাথে তুলনা (`MainActivity.kt`-এর `UpdateCheckDialog`)
- [x] নতুন ভার্সন থাকলে ডায়ালগ: changelog টেক্সট + "ডাউনলোড করুন" বাটন → `apk_url` ব্রাউজারে খোলে (Intent.ACTION_VIEW)
- [x] `force_update = true` হলে ডায়ালগ dismiss করা যাবে না (ভবিষ্যতে critical security fix-এর জন্য সংরক্ষিত)
- [x] CLAUDE.md-এ APK deploy রুটিনে version.json আপডেটের ধাপ যোগ করা

**Implementation নোট:**
- `ApiService.getAppVersion()` `@GET` + `@Url` (fixed default) ব্যবহার করে যেহেতু endpoint `/api/` prefix-এর বাইরে (`/apk/version.json`) এবং response `ApiResponse<T>` wrapper-এ না (plain static JSON) — বাকি সব endpoint থেকে ভিন্ন প্যাটার্ন, তাই আলাদাভাবে handle করা হয়েছে
- Network/parse ব্যর্থ হলে (version.json এখনো নেই এমন পুরনো deploy, বা connectivity সমস্যা) নীরবে উপেক্ষা করে — silent catch, কোনো error UI দেখায় না, যাতে এই non-critical ফিচার মূল app flow-কে ব্লক না করে
- Verified: gradle build সফল, `version.json` সঠিক শেপে generate হয় (`python3 -m json.tool` দিয়ে valid JSON যাচাই), Apache-এ সরাসরি serve হচ্ছে (curl দিয়ে HTTP 200 + বাংলা changelog UTF-8 ঠিকভাবে দেখা যাচ্ছে) confirm করা হয়েছে। **সীমাবদ্ধতা:** এই সার্ভারে Android emulator নেই, তাই dialog-টা device-এ visually চালিয়ে দেখা যায়নি — শুধু compile + backend/JSON-shape যাচাই করা হয়েছে।
- এই ফিচারটা মূল SaaS multi-tenancy রূপান্তরের অংশ না, কিন্তু Android app maintenance-এর সাথে সরাসরি সম্পর্কিত (Phase 6-এর ধারাবাহিকতায়), তাই এই ডকুমেন্টেই ট্র্যাক করা হয়েছে

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
| 3 | SuperAdmin Panel | ✅ সম্পন্ন | 2026-07-02 |
| 4 | Tenant Onboarding | ✅ সম্পন্ন | 2026-07-02 |
| 5 | Payment Gateway | ⬜ বাকি | — |
| 6 | Android Support | ✅ সম্পন্ন | 2026-07-02 |
| 7 | Android Update Notification | ✅ সম্পন্ন | 2026-07-02 |

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
*বর্তমান অবস্থা: Phase 1, 2, 3, 4, 6 সম্পন্ন। Phase 5 (payment gateway) ইচ্ছাকৃতভাবে স্থগিত — merchant account নেওয়ার পর করা হবে।*
