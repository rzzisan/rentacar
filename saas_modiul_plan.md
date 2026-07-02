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
**Status: ⬜ বাকি**

**লক্ষ্য:** বিদ্যমান সব data-কে tenant-aware করা

**কাজের তালিকা:**
- [ ] `tenants` টেবিল তৈরি
- [ ] `super_admins` টেবিল তৈরি
- [ ] বিদ্যমান সব টেবিলে `tenant_id` column যোগ
- [ ] বিদ্যমান data-এ tenant_id সেট (migration: একটি default tenant তৈরি করে সব পুরানো data সেই tenant-এ assign)
- [ ] `api/auth/login.php` — superadmin check যোগ + tenant_id session
- [ ] `api/_helpers.php` — `get_tenant_id()`, `require_superadmin()`, `require_active_tenant()` functions
- [ ] সব `api/admin/*` endpoint-এ tenant_id filter
- [ ] সব `api/vehicles/*` endpoint-এ tenant_id filter
- [ ] সব `api/manager/*` endpoint-এ tenant_id filter (manager-এর tenant inherit)
- [ ] সব `api/driver/*` endpoint-এ tenant_id filter (driver-এর tenant inherit)
- [ ] `api/admin/reports.php` — সব ৫টি section-এ tenant_id
- [ ] প্রথম superadmin account তৈরি (DB-তে manually insert)
- [ ] Test: দুটি tenant তৈরি করে data isolation verify

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
**Status: ⬜ বাকি**

**লক্ষ্য:** billing schema তৈরি এবং manual invoice management

**কাজের তালিকা:**
- [ ] `subscription_plans` টেবিল তৈরি + প্রথম plan insert
- [ ] `subscriptions` টেবিল তৈরি
- [ ] `subscription_invoices` টেবিল তৈরি
- [ ] `api/cron/generate-invoices.php` — monthly invoice generator
- [ ] `api/cron/check-overdue.php` — overdue check + tenant suspend
- [ ] Server-এ cron job সেট: `0 6 1 * * php /var/www/html/car.zisan.me/api/cron/generate-invoices.php`
- [ ] Server-এ cron job সেট: `0 8 * * * php /var/www/html/car.zisan.me/api/cron/check-overdue.php`
- [ ] Suspended tenant login block (`require_active_tenant()`)

**এই phase সম্পন্ন হলে:** মাসে মাসে invoice তৈরি হবে, overdue-তে tenant suspend হবে।

---

### Phase 3: SuperAdmin Panel
**Status: ⬜ বাকি**

**লক্ষ্য:** আমরা (rzzisan) সব tenant পরিচালনা করতে পারব

**কাজের তালিকা:**
- [ ] `api/superadmin/tenants/` — CRUD endpoints
- [ ] `api/superadmin/billing/invoices.php` — invoice তালিকা + mark-paid
- [ ] `api/superadmin/billing/stats.php` — revenue stats
- [ ] Frontend: `SuperAdminDashboard.tsx`
- [ ] Frontend: `SuperAdminTenants.tsx` — tenant list, edit, suspend/activate
- [ ] Frontend: `SuperAdminBilling.tsx` — invoice list, manual mark-paid
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

**লক্ষ্য:** automatic payment collection

**প্রস্তাবিত gateway:** SSLCommerz (বাংলাদেশে bKash/Nagad/card সব সাপোর্ট করে)

**কাজের তালিকা:**
- [ ] SSLCommerz merchant account নেওয়া
- [ ] `api/payment/initiate.php` — payment শুরু
- [ ] `api/payment/success.php` — payment callback, invoice mark-paid
- [ ] `api/payment/fail.php` — failure handle
- [ ] Frontend: invoice page-এ "পেমেন্ট করুন" button
- [ ] Payment confirmation-এ tenant status re-activate

**বিকল্প — Manual Payment (আপাতত):**
SuperAdmin billing panel থেকে invoice manually "Paid" mark করা।
Payment নেওয়া হবে bKash-এ, reference number invoice-এ লেখা হবে।

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

## Billing মূল্য নির্ধারণ (প্রস্তাবিত)

| Plan | গাড়ি প্রতি মাসিক মূল্য | সর্বোচ্চ গাড়ি | সর্বোচ্চ ড্রাইভার |
|---|---|---|---|
| Basic | ৳XXX | ৫ | ১০ |
| Standard | ৳XXX | ২০ | ৪০ |
| Enterprise | ৳XXX | Unlimited | Unlimited |

*মূল্য নির্ধারণ pending — ব্যবসার মালিক (rzzisan) সিদ্ধান্ত নেবেন।*

---

## Implementation শুরু করার আগে চেকলিস্ট

- [ ] প্রতি গাড়ি মাসিক মূল্য নির্ধারণ করা হয়েছে?
- [ ] Trial period কত দিন সিদ্ধান্ত হয়েছে? (default: ৩০ দিন)
- [ ] Payment method: Manual first? নাকি gateway থেকেই শুরু?
- [ ] SSLCommerz/bKash merchant account আছে? (Phase 5-এর জন্য)
- [ ] Server-এ cron job চালানো যাবে?
- [ ] বিদ্যমান একটি tenant-এর data migration করার পরিকল্পনা আছে?

---

## Phase সম্পন্নের tracking

| Phase | নাম | Status | সম্পন্নের তারিখ |
|---|---|---|---|
| 1 | মাল্টি-টেনেন্সি ভিত্তি | ⬜ বাকি | — |
| 2 | Subscription & Billing | ⬜ বাকি | — |
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
│   └── billing/
│       ├── invoices.php   ← GET list, POST mark-paid
│       └── stats.php      ← GET revenue dashboard
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
│   └── Billing.tsx
├── Register.tsx           ← নতুন
└── (বিদ্যমান pages অপরিবর্তিত থাকবে)
```

---

*এই ডকুমেন্ট সর্বশেষ আপডেট: 2026-07-02*
*পরবর্তী কাজ: Phase 1 শুরু করার আগে "Implementation শুরু করার আগে চেকলিস্ট" সম্পন্ন করুন।*
