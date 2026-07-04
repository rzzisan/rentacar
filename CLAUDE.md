# car.zisan.me — Rent-A-Car Management System

## প্রজেক্ট পরিচিতি
বাংলাদেশ-ভিত্তিক কার রেন্টাল ম্যানেজমেন্ট সিস্টেম। প্রোডাক্ট ব্র্যান্ড নাম **ZyroTrip** (Zyro Tech, zyrotechbd.com) — 2026-07-03 থেকে "CarRental" নাম থেকে রিব্র্যান্ড করা হয়েছে (app label, launcher icon, ওয়েব favicon/title/লগইন-সাইডবার লোগো)। **শুধু display-level রিব্র্যান্ড** — Android `applicationId` (`com.rzzisan.carrental`) ও internal Kotlin প্যাকেজ স্ট্রাকচার ইচ্ছাকৃতভাবে অপরিবর্তিত রাখা হয়েছে যাতে বর্তমান ইনস্টল করা অ্যাপগুলো স্বাভাবিকভাবে in-app আপডেট পেতে থাকে (পুরো applicationId রিব্র্যান্ড করলে সব ইউজারকে uninstall+reinstall করতে হতো)। সোর্স লোগো (সব ফরম্যাট) `branding/` ফোল্ডারে। UI সম্পূর্ণ বাংলায়। Role: `superadmin` (SaaS-ব্যাপী, tenant-agnostic), `admin`, `manager`, `employee`, `customer`, `driver` (সব tenant-scoped)।

**মাল্টি-টেনেন্ট SaaS (2026-07-02 থেকে):** সিস্টেমটি একাধিক আলাদা রেন্ট-এ-কার ব্যবসা (tenant) হোস্ট করে — প্রতিটির ডেটা সম্পূর্ণ isolated। বিস্তারিত পরিকল্পনা ও phase tracking: `saas_modiul_plan.md`। এই ফাইলে multi-tenancy-সংক্রান্ত conventions নিচে "Multi-Tenancy" সেকশনে।

---

## আর্কিটেকচার

### Frontend — Vite + React (TypeScript)
- **অবস্থান:** `frontend/`
- **Build output:** `public/app/` (Vite `base: '/public/app/'` production-এ)
- **Styling:** Tailwind CSS v4 (`@tailwindcss/vite` plugin, `@import "tailwindcss"`)
- **Routing:** react-router-dom v6, `BrowserRouter` (no basename)
- **HTTP client:** `src/api/client.ts` — fetch wrapper, `credentials: 'include'`

### Backend — PHP REST API
- **API base path:** `/api/`
- **Auth:** PHP session-based (`config/config.php` এ session timeout ১ ঘণ্টা)
- **DB:** MySQL — database: `car_rental_db`, user: `carapp`
- **Response format:** সব endpoint থেকে `{ success: bool, data: any, message: string }`

### Routing — `.htaccess`
1. `/api/*` → PHP API (pass-through)
2. Actual files (`-f`) → direct serve (static assets)
3. বাকি সব → `/public/app/index.html` (React SPA)

---

## ডিজাইন নির্দেশনা (অবশ্যই মেনে চলতে হবে)

### ১. Mobile-First
- Tailwind breakpoints: `sm` (640px), `md` (768px), `lg` (1024px), `xl` (1280px)
- সব layout এবং component mobile-first

### ২. React Component Design
- Reusable components → `frontend/src/components/`
- Page-level → `frontend/src/pages/`
- Shared layouts → `frontend/src/layouts/`
- TypeScript সবসময়, functional components + hooks

### ৩. Visual Design Standards
- Primary: Indigo `#4f46e5` | Dark sidebar `#0f172a` | White header
- Card-based content layout
- বাংলা ফন্ট সাপোর্ট (Segoe UI, system-ui fallback)
- Custom theme variables → `frontend/src/index.css` এর `@theme {}` block

---

## Frontend ফাইল structure

```
frontend/src/
├── App.tsx                        — routes, auth state, ProtectedRoute
├── index.css                      — Tailwind import + @theme variables
├── main.tsx
├── api/
│   └── client.ts                  — api.get/post/put/delete wrapper
├── types/
│   └── index.ts                   — User, Vehicle, Rental, ApiResponse<T>, Tenant, SubscriptionInvoice, SaasSettings, SuperAdminStats
├── lib/
│   └── roleHome.ts                — role → home route ম্যাপিং; App.tsx ও Login.tsx দুটোতেই ব্যবহৃত (redirect logic এক জায়গায়)
├── hooks/
│   └── useAuth.ts                 — (সংরক্ষিত, App.tsx এ inline ব্যবহার হচ্ছে)
├── layouts/
│   └── AppLayout.tsx              — Header + Sidebar + <Outlet>
├── components/
│   └── layout/
│       ├── Header.tsx             — white topbar, user dropdown, logout
│       └── Sidebar.tsx            — dark sidebar, role-based nav
└── pages/
    ├── Login.tsx                  — login form (ইমেইল/মোবাইল + পাসওয়ার্ড), POST /api/auth/login.php
    ├── Register.tsx                — নতুন tenant self-registration (public), POST /api/auth/register.php, সফল হলে অটো-লগইন
    ├── superadmin/
    │   ├── Dashboard.tsx          — stat cards (tenant count/status), ৬ মাসের আয় ট্রেন্ড bar chart, সাম্প্রতিক invoice তালিকা
    │   ├── Tenants.tsx            — tenant CRUD: তালিকা, নতুন tenant তৈরি (+প্রথম admin একসাথে), edit, status পরিবর্তন
    │   ├── Billing.tsx            — সব tenant-এর invoice তালিকা (filter), manual mark-paid (bKash TrxID field)
    │   └── Settings.tsx           — saas_settings ফর্ম (price_per_vehicle/trial_days/invoice_due_days)
    ├── admin/
    │   ├── Dashboard.tsx          — চলমান+আপকামিং ট্রিপ হাইলাইট কার্ড (লাইভ টাইমার/কাউন্টডাউন), ৬ stats কার্ড (বকেয়া, আজকের ট্রিপ সহ), quick links
    │   ├── Vehicles.tsx           — CRUD: list, add modal, edit modal, delete confirm
    │   ├── Rentals.tsx            — ট্রিপ ম্যানেজমেন্ট: তালিকা, তৈরি, স্ট্যাটাস, খরচ, লাইভ টাইমার; ?open=<id> এলে ডিটেইল মডাল অটো-খোলে
    │   ├── Settlements.tsx        — ট্রিপ সেটেলমেন্ট: কমিশন হিসাব, পেমেন্ট সংগ্রহ ও ইতিহাস
    │   ├── Drivers.tsx            — ড্রাইভার CRUD + গাড়ি অ্যাসাইনমেন্ট (driver_vehicles)
    │   ├── DriverCollections.tsx  — ড্রাইভার বকেয়া জমা (FIFO bulk collection)
    │   └── Managers.tsx           — ম্যানেজার CRUD + গাড়ি অ্যাসাইনমেন্ট (একটি গাড়ি ↔ একজন ম্যানেজার)
    ├── manager/
    │   ├── Dashboard.tsx          — ম্যানেজার ড্যাশবোর্ড: assigned গাড়ির stats, active/upcoming trips
    │   ├── Vehicles.tsx           — assigned গাড়ির তালিকা + স্ট্যাটাস পরিবর্তন (available/maintenance/inactive)
    │   ├── Rentals.tsx            — ট্রিপ ম্যানেজমেন্ট (assigned গাড়ির জন্য, admin Rentals-এর মতো)
    │   ├── Settlements.tsx        — সেটেলমেন্ট ম্যানেজমেন্ট (assigned গাড়ির জন্য)
    │   ├── Drivers.tsx            — ড্রাইভার ম্যানেজমেন্ট + পারফরম্যান্স stats (এই মাস ট্রিপ, মোট, বকেয়া)
    │   ├── DriverCollections.tsx  — ড্রাইভার বকেয়া জমা (assigned গাড়ির ড্রাইভার)
    │   └── Reports.tsx            — রিপোর্ট: মাসিক রাজস্ব, গাড়িভিত্তিক, খরচ ব্রেকডাউন, ড্রাইভার পারফরম্যান্স
    └── driver/
        ├── Dashboard.tsx          — লেজার (কমিশন/পেমেন্ট) + মাসিক আয়ের সারসংক্ষেপ (৬ মাস) + expense breakdown per trip + লাইভ ট্রিপ কার্ড
        ├── Rentals.tsx            — নিজের ট্রিপ: তৈরি/শুরু/সম্পন্ন (বাতিল নয়), খরচ + রসিদ আপলোড; date range ফিল্টার; ?open=<id> এলে ডিটেইল মডাল অটো-খোলে
        └── Profile.tsx            — প্রোফাইল: নাম/মোবাইল/ছবি আপডেট, পাসওয়ার্ড পরিবর্তন, assigned গাড়ি ও ট্রিপ stats
```

### তৈরি হয়নি এখনো (placeholder দেখায়)
```
admin:    customers, payments, employees, maintenance, reports, settings
employee: dashboard, vehicles, rentals, customers
customer: dashboard, vehicles, bookings, invoices, profile
```

### Manager role বিশেষ নোট
- `managers` টেবিল আলাদা (drivers টেবিলের মতো), `users` টেবিলে নয়
- Login fallback: `api/auth/login.php` → managers টেবিল চেক করে
- `require_manager()` in `_helpers.php` — manager_id রিটার্ন করে
- `get_manager_vehicle_ids()` — manager-এর assigned vehicle IDs অ্যারে রিটার্ন
- `manager_vehicle_in_clause()` — SQL IN() clause string তৈরি করে
- Manager শুধু তার assigned vehicles-এর ডেটা দেখতে/পরিবর্তন করতে পারবে

---

## PHP API structure

```
api/
├── _helpers.php                   — json_response(), require_auth(), require_role(), require_driver(), input(),
│                                    create_settlement_for_rental() — idempotent settlement তৈরি (হিসাবের লজিক এক জায়গায়)
├── auth/
│   ├── login.php   POST           — session তৈরি করে; body key `identifier` (ইমেইল বা মোবাইল, পুরনো `email` key-ও চলে)
│   ├── register.php POST          — public, auth লাগে না: নতুন tenant + প্রথম admin একসাথে তৈরি (trial শুরু), সফল হলে অটো-লগইন
│   ├── logout.php  POST           — session destroy করে JSON রিটার্ন
│   ├── me.php      GET            — current user info + tenant_status/trial_ends_at (admin/manager/driver-এর জন্য; superadmin-এর জন্য null)
│   └── update_profile.php POST    — নিজের প্রোফাইল আপডেট (superadmin-এর জন্য block করা — নিচে "Multi-Tenancy" সেকশনে কারণ)
├── superadmin/                     — সব endpoint require_superadmin() দিয়ে গার্ড
│   ├── tenants/                    — index (GET list+POST create tenant+প্রথম admin একসাথে), update (PUT তথ্য), status (PUT trial/active/suspended/cancelled, subscriptions.status sync করে)
│   ├── billing/                    — invoices.php (GET তালিকা+filter, POST mark-paid — suspended tenant হলে auto-reactivate করে), stats.php (GET revenue dashboard + ৬ মাসের ট্রেন্ড)
│   └── settings/                   — index.php GET/PUT saas_settings (price_per_vehicle/trial_days/invoice_due_days)
├── admin/
│   ├── stats.php   GET            — dashboard stats (admin only): counts, monthly_revenue (agreed_amount-ভিত্তিক), total_dues, today_trips + active_trips/upcoming_trips তালিকা
│   ├── rentals/                   — index (GET/POST), show (expenses এ location_name/lat/lng সহ), update, update_status (completed হলে settlement অটো-তৈরি), expenses (POST: location_name/lat/lng সহ), expenses_destroy
│   ├── settlements/               — index (GET; POST এখন পুরনো ট্রিপের fallback), show, update, collect-payment, payment-history
│   ├── drivers/                   — index (GET/POST), update, destroy, dues (বকেয়া overview), collect (FIFO bulk)
│   └── managers/                  — index (GET/POST), update, destroy — ম্যানেজার CRUD + vehicle assignment
├── manager/                       — সব endpoint require_manager() দিয়ে গার্ড; শুধু assigned গাড়ির ডেটা দেখায়
│   ├── stats.php   GET            — admin stats-এর মতো কিন্তু manager-এর assigned vehicles filter করা
│   ├── vehicles.php GET/PUT       — assigned গাড়ির তালিকা; PUT ?id= দিয়ে status পরিবর্তন (rented ছাড়া)
│   ├── reports.php GET            — মাসিক রাজস্ব, গাড়িভিত্তিক revenue, খরচ breakdown, ড্রাইভার performance
│   ├── rentals/                   — index (GET/POST), show, update, update_status, expenses, expenses_destroy
│   ├── settlements/               — index, show, update, collect-payment, payment-history
│   └── drivers/                   — index (GET — total_trips/this_month_trips/total_due সহ), dues (বকেয়া), collect (FIFO bulk)
├── driver/                        — সব endpoint require_driver() দিয়ে গার্ড করা
│   ├── ledger.php  GET            — settlements + monthly_breakdown (৬ মাস) + expense_breakdown per trip
│   ├── profile.php GET/POST       — প্রোফাইল তথ্য + assigned vehicles + stats; POST: নাম/মোবাইল/ছবি/পাসওয়ার্ড আপডেট
│   ├── vehicles.php GET           — নিজেকে অ্যাসাইন করা গাড়ির তালিকা
│   └── rentals/                   — index (GET: status/search/date_from/date_to filter; POST create), show, update_status, expenses
├── vehicles/
│   ├── index.php   GET/POST        — list (filter: status, vehicle_type, search) / create
│   ├── show.php    GET ?id=        — single vehicle
│   ├── update.php  PUT ?id=        — update fields
│   └── destroy.php DELETE ?id=    — delete (active rental থাকলে block করে)
└── cron/                           — CLI-only (HTTP-এ 403), root crontab দিয়ে চালানো হয় (php8.3 দিয়ে, নিচে নোট দেখুন)
    ├── generate-invoices.php       — মাসের ১ তারিখ: প্রতিটি trial/active tenant-এর মাসিক subscription invoice তৈরি করে
    └── check-overdue.php           — প্রতিদিন: overdue invoice mark + tenant suspend

config/
├── config.php                     — DB creds, session config, TAX_RATE=15%
└── Database.php                   — MySQLi connection class

includes/                          — ভবিষ্যতের API-তে ব্যবহার হবে
├── User.php                       — login(), register(), getCurrentUser()
├── Rental.php                     — createRental(), getAllRentals()
├── Customer.php
└── Payment.php
```

---

## Authentication Flow

1. `App.tsx` mount → `GET /api/auth/me.php`
2. 401 response → redirect to `/login`
3. Login form → `POST /api/auth/login.php` (body: `identifier` — **ইমেইল অথবা মোবাইল নম্বর**, `password`) → PHP session set (৪-স্তর fallback: `super_admins` → `users` → `drivers` → `managers`)
4. Role অনুযায়ী redirect: `frontend/src/lib/roleHome.ts`-এর `roleHome()` ফাংশন কেন্দ্রীয়ভাবে ঠিক করে (superadmin→`/superadmin`, admin→`/admin`, manager→`/manager`, employee→`/employee`, customer→`/customer`, driver→`/driver`)
5. Logout → `POST /api/auth/logout.php` → session destroy → redirect to `/login`
6. `ProtectedRoute` component role-check করে, ভুল role হলে নিজের dashboard-এ redirect
7. **নতুন tenant registration:** `/register` (public) → `POST /api/auth/register.php` → tenant + admin একসাথে তৈরি + trial শুরু + **অটো-লগইন** (আলাদা করে লগইন করতে হয় না)

### ইমেইল/মোবাইল দিয়ে লগইন
`users.phone`, `drivers.mobile`, `managers.mobile` — সব **গ্লোবাল UNIQUE** (email-এর মতোই)। Login lookup সব জায়গায় `WHERE email = ? OR phone/mobile = ?`। `super_admins`-এর মোবাইল কলাম নেই — শুধু ইমেইল দিয়ে লগইন করে।

---

## Multi-Tenancy (SaaS)

- **`$_SESSION['tenant_id']`**: প্রতিটি admin/manager/driver session-এ থাকে (login-এই সেট হয়)। `superadmin`-এর tenant_id নেই (tenant-agnostic — সব tenant দেখতে পারে; SuperAdmin panel Phase 3-এ তৈরি হয়েছে)।
- **`get_tenant_id()`** (`api/_helpers.php`) — session থেকে tenant_id রিটার্ন করে, না থাকলে 403। **প্রতিটি tenant-scoped endpoint-এ `require_role()`/`require_manager()`/`require_driver()`-এর ঠিক পরেই কল করতে হবে** (`$tid = get_tenant_id();`)।
- **`require_superadmin()`**, **`require_active_tenant()`** — superadmin guard ও suspended/cancelled tenant block। `require_active_tenant()` **`require_auth()`-এর ভেতরেই কল হয়** (superadmin বাদে) — মানে প্রতিটি authenticated API রিকোয়েস্টে চেক হয়, শুধু login-এ নয়। tenant suspend হলে আগে থেকে লগইন করা session-ও সাথে সাথে block হয়ে যায় (নতুন করে লগইন করার অপেক্ষা করতে হয় না)।
- **tenant_id column আছে:** `users`, `vehicles`, `drivers`, `managers`, `rentals`, `maintenance`, `customers` (সব NOT NULL, কোনো DEFAULT নেই — tenant_id ছাড়া INSERT করলে সশব্দে ব্যর্থ হবে, চুপচাপ ভুল tenant-এ leak হবে না)। `api_tokens`-এও আছে (nullable, mobile bearer-token flow-এর জন্য)।
- **tenant_id column নেই:** `trip_expenses`, `settlements`, `driver_vehicles`, `manager_vehicles`, `vehicle_documents` — এগুলো parent (`rentals`/`vehicles`) join করে tenant verify করতে হয়।
- **নতুন endpoint লেখার নিয়ম (বাধ্যতামূলক):**
  1. Auth guard-এর পর `$tid = get_tenant_id();`
  2. tenant_id-ওয়ালা টেবিলের প্রতিটি SELECT/UPDATE/DELETE-এ `tenant_id = ?` (বা aliased `x.tenant_id = ?`) WHERE-এ যোগ করো
  3. প্রতিটি INSERT-এ `tenant_id` column + bind
  4. **IDOR সতর্কতা:** client-supplied ID (`$_GET['id']`, body-তে আসা vehicle_id/driver_id/rental_id ইত্যাদি) দিয়ে যেকোনো lookup/update/delete/assign-এ tenant ownership যাচাই বাধ্যতামূলক — নাহলে এক tenant-এর user অন্য tenant-এর ডেটা ID guess করে access/মুছতে/পরিবর্তন করতে পারবে
  5. **⚠️ ownership-check কখনো `if ($vids)`/`if ($some_array)`-এর মতো conditional-এর ভেতরে রাখা যাবে না** — খালি array (যেমন manager-এর assigned vehicle ০টা, একটা স্বাভাবিক reachable state) হলে পুরো চেকটাই স্কিপ হয়ে যায় এবং client-supplied ID সরাসরি ব্যবহার হয়ে যায় কোনো tenant filter ছাড়াই। `api/manager/settlements/index.php`-এ এই বাগে যেকোনো manager (assigned vehicle ০টা থাকলে) অন্য tenant-এর completed rental-এর জন্য settlement তৈরি করতে পারত — ঠিক করা হয়েছে 2026-07-05। ownership-check সবসময় unconditionally চালাতে হবে (`manager_vehicle_in_clause([])` নিরাপদে `'(0)'` রিটার্ন করে বলে খালি থাকলেও ০ rows ম্যাচ করবে)।
- **Manager/Driver panel:** এরা ইতিমধ্যে `manager_id`/`driver_id` দিয়ে scoped (via `get_manager_vehicle_ids()`/`driver_vehicles`), tenant_id filter সেখানে **defense-in-depth** হিসেবে অতিরিক্ত যোগ হয়েছে।
- **`$in` (manager_vehicle_in_clause) সতর্কতা:** কোনো একটি SQL-এ `$in` একাধিকবার ব্যবহার করলে (যেমন SELECT-এর subquery + WHERE-এ) প্রতিটি ব্যবহারের জন্য `$vids` আলাদাভাবে params array-তে **SQL টেক্সটে placeholder-এর ক্রম অনুযায়ী** যোগ করতে হবে — নাহলে `bind_param()` ArgumentCountError দিয়ে fatal করবে (`api/manager/drivers/index.php`, `api/manager/customers/index.php`-এ এই বাগ পাওয়া গিয়েছিল, ঠিক করা হয়েছে)।
- **DB migration ফাইল:** `db/migrations/001_multi_tenancy.sql`, `002_subscription_billing.sql`, `003_mobile_login.sql` (backup রাখা হয় `db/backups/`-এ, mysqldump দিয়ে, যেকোনো schema change-এর আগে)
- **প্রথম SuperAdmin:** `super_admins` টেবিলে manual insert, email `rzzisan@gmail.com`
- **⚠️ superadmin session-এর `$_SESSION['user_id']` আসলে `super_admins.id`, `users.id` নয়** — যেকোনো নতুন endpoint লেখার সময় এই দুটো গুলিয়ে ফেলা যাবে না (একবার `api/auth/update_profile.php`-এ এই বাগ হয়েছিল — superadmin profile আপডেট করতে গেলে ভুলবশত একই numeric id-ওয়ালা কোনো tenant admin-এর ডেটা বদলে যেতে পারত; এখন block করা আছে)।
- বিস্তারিত phase-ভিত্তিক পরিকল্পনা: `saas_modiul_plan.md` (Phase 1+2+3+4 সম্পন্ন 2026-07-02; Phase 5+ = payment gateway, Android tenant support)। **⚠️ Phase 5 (payment gateway) ব্যবহারকারীর সিদ্ধান্তে ইচ্ছাকৃতভাবে স্থগিত** — Phase 6 (Android tenant support) Phase 5-এর আগেই সম্পন্ন হয়েছে (ম্যানুয়াল payment-ই আপাতত যথেষ্ট)

### Billing (Phase 2)
- **`saas_settings`**: key-value config (`price_per_vehicle`, `trial_days`, `invoice_due_days`) — SuperAdmin panel তৈরি হলে (Phase 3) সেখান থেকে পরিবর্তনযোগ্য হবে
- **`subscriptions`**: প্রতিটি tenant-এর billing state (trialing/active/past_due/cancelled) — `subscription_plans` টেবিল বাদ দেওয়া হয়েছে, তাই এখানে `plan_id` নেই (single global price)
- **`subscription_invoices`**: মাসিক invoice — `invoice_number` ফরম্যাট `INV-YYYYMM-T<tenant_id>` (তাই cron দুবার চললেও duplicate হয় না)
- **`api/cron/generate-invoices.php`** (মাসের ১ তারিখ) — trial ও active দুই ধরনের tenant-এরই invoice তৈরি করে (সিদ্ধান্ত 2026-07-02); saas_settings থেকে price/due_days পড়ে
- **`api/cron/check-overdue.php`** (প্রতিদিন) — due_date পার হওয়া pending invoice → overdue mark + tenant suspend (idempotent, ইতিমধ্যে suspended/cancelled tenant ছোঁয় না)
- **cron script দুটো CLI-only** (`php_sapi_name() !== 'cli'` চেক, HTTP-এ 403) — অথেন্টিকেশন ছাড়া publicly-triggerable billing endpoint রাখা হয়নি ইচ্ছাকৃতভাবে
- **⚠️ PHP ভার্সন সতর্কতা:** সার্ভারে `/usr/bin/php` → PHP 8.5 (mysqli নেই)। Apache আসলে চালায় **PHP 8.3-fpm**। তাই cron/CLI script চালাতে **সবসময় `/usr/bin/php8.3`** ব্যবহার করবে, শুধু `php` নয়
- crontab (root): `0 6 1 * * php8.3 .../generate-invoices.php`, `0 8 * * * php8.3 .../check-overdue.php`; log → `storage/logs/*.log` (gitignored)

## Database Schema

### tenants / super_admins (SaaS)
- **tenants**: id, name, email (UNI), phone, address, logo_path, status (enum: trial/active/suspended/cancelled), trial_ends_at
- **super_admins**: id, email (UNI), password (bcrypt), name — `users` থেকে সম্পূর্ণ আলাদা টেবিল, tenant-agnostic

### vehicles
| column | type |
|---|---|
| id, tenant_id (FK), registration_number (UNI), brand, model | — |
| year | YEAR |
| vehicle_type | enum: sedan, suv, van, truck, premium |
| color, fuel_type | enum: petrol, diesel, hybrid, electric |
| seating_capacity, mileage, daily_rent_price | — |
| status | enum: available, rented, maintenance, inactive |
| image_path, created_at, updated_at | — |

### rentals
| column | type |
|---|---|
| id, tenant_id, customer_id, vehicle_id, employee_id, driver_id | FK |
| start_date, end_date, pickup_location, dropoff_location | — |
| trip_type | enum: one_way, round_trip |
| agreed_amount | decimal — চুক্তির টাকা |
| actual_start_time, actual_end_time | datetime — প্রকৃত শুরু/শেষ (লাইভ টাইমারে ব্যবহৃত) |
| rental_status | enum: pending, active, completed, cancelled |
| total_days, daily_rate, subtotal, discount, tax, total_amount | decimal |
| payment_status | enum: pending, paid, partial |

### ট্রিপ/সেটেলমেন্ট সংক্রান্ত টেবিল
- **drivers**: id, user_id, name, phone, commission_percent ইত্যাদি
- **driver_vehicles**: driver_id + vehicle_id — কোন ড্রাইভারকে কোন গাড়ি অ্যাসাইন করা
- **managers**: id, name, mobile, email, password (bcrypt), profile_picture, status — ম্যানেজার অ্যাকাউন্ট (drivers টেবিলের মতো structure)
- **manager_vehicles**: manager_id + vehicle_id, UNIQUE(vehicle_id) — একটি গাড়ি সর্বোচ্চ একজন ম্যানেজারকে অ্যাসাইন
- **trip_expenses**: rental_id, expense_type (toll/fuel/parking/repair/driver_allowance/other), amount, description, receipt_image, location_name, latitude (DECIMAL 10,7), longitude (DECIMAL 10,7), created_at
- **settlements**: rental_id, driver_id, agreed_amount, total_expenses, driver_commission, paid_amount, remaining_amount, payment_status
- **settlement_payments**: settlement_id, amount, payment_method, payment_date, payment_notes

### অন্যান্য টেবিল
- **users**: id, username, email, password (bcrypt), role, status
- **customers**: id, user_id, first_name, last_name, phone, nid, license_number, address
- **payments**: id, rental_id, amount, payment_method, status
- **maintenance**, **damage_reports**, **reviews**, **settings**

TAX_RATE = 15% | Timezone: Asia/Dhaka

---

## Development Conventions

### নতুন page তৈরির নিয়ম
1. `frontend/src/pages/<role>/<PageName>.tsx` — React component
2. `App.tsx`-এ route যোগ করো (PlaceholderPage সরিয়ে)
3. দরকার হলে নতুন `api/<resource>/` endpoint তৈরি করো
4. UI text বাংলায়
5. Loading skeleton + error state সবসময় দেখাবে

### নতুন API endpoint তৈরির নিয়ম
1. `api/_helpers.php` এর `require_auth()` বা `require_role('admin')` দিয়ে শুরু
2. Prepared statements বাধ্যতামূলক (string interpolation নিষিদ্ধ)
3. Response: `json_response(['success' => true, 'data' => ..., 'message' => '...'])`
4. `only_method('GET')` / `only_method('POST')` দিয়ে method guard
5. **Numeric cast বাধ্যতামূলক:** MySQL DECIMAL string হিসেবে আসে — response-এর আগে `(float)`/`(int)` cast করতে হবে, নাহলে frontend-এ `.toFixed()` ভেঙে যায়

### Build ও Deploy
```bash
cd frontend
npm run dev      # dev server :5173, /api proxy → localhost
npm run build    # → public/app/ (production)
```

---

## Android Driver App

- **অবস্থান:** `android/` — Jetpack Compose, Kotlin, Moshi, Retrofit
- **Multi-tenant (Phase 6, 2026-07-02):** `LoginData.tenantId` (`data/network/Models.kt`) → `AuthTokenStore.saveUserInfo(..., tenantId=...)` (`data/auth/AuthStorage.kt`, `SharedPreferences`-এ `-1` sentinel দিয়ে nullable Int)। tenant context বাকি সব API call-এ **automatic** — Bearer token নিজেই tenant_id বহন করে (`api_tokens.tenant_id`), সার্ভার-সাইডে `require_active_tenant()` (`api/_helpers.php`) প্রতিটি রিকোয়েস্টে চেক করে, Android-এ আলাদা কোনো param/হেডার লাগে না
- **⚠️ `HttpException` catch করার সময় সবসময় response body থেকে JSON parse করে `message` দেখাতে হবে** (শুধু `BuildConfig.DEBUG`-এ না) — নাহলে suspended-tenant-এর মতো গুরুত্বপূর্ণ backend error message দেখা যাবে না। এই বাগ প্রথমে `LoginScreen.kt`-এ পাওয়া গিয়েছিল (Phase 6), পরে দেখা গেছে বাকি ~১৮টা স্ক্রিনেও একই সমস্যা ছিল (initial-load `catch (e: Exception)` জেনেরিক `e.message` দেখাত) — এখন `util/ApiErrors.kt`-এর `errorMessageOf(e, fallback)` শেয়ারড হেল্পার আছে, **নতুন যেকোনো স্ক্রিনের ডেটা-লোড catch ব্লকে এটাই ব্যবহার করতে হবে**, প্যাটার্ন কপি-পেস্ট না করে (2026-07-05)
- **APK serve directory:** `/var/www/car-apk/`
- **APK index script:** `/var/www/car-apk/gen-index.sh` — চালালে `/apk/` page + `latest.apk` symlink + `version.json` আপডেট হয়
- **Download page:** `https://car.zisan.me/apk/`
- **Stable latest URL:** `https://car.zisan.me/apk/latest.apk`
- **In-app update check (Phase 7, 2026-07-02):** Play Store-এ নেই বলে নিজস্ব version-check — app চালু হলে `https://car.zisan.me/apk/version.json` (static ফাইল, `/api/` এর বাইরে, `ApiService.getAppVersion()` `@Url` দিয়ে) ফেচ করে `BuildConfig.VERSION_CODE`-এর সাথে তুলনা করে (`MainActivity.kt`-এর `UpdateCheckDialog`), নতুন থাকলে ডায়ালগ দেখায় ("ডাউনলোড করুন" → ব্রাউজারে `apk_url` খোলে, ইউজার নিজে ইনস্টল করে — silent/automatic install না)।
- **⚠️ `versionCode` এখন ম্যানুয়াল না — `android/app/build.gradle.kts`-এর `gitCommitCount()` স্বয়ংক্রিয়ভাবে `git rev-list --count HEAD` থেকে বসায়।** কারণ: ম্যানুয়াল বাম্প ভুলে যাওয়ার ইতিহাস আছে (Phase 6 পর্যন্ত `versionCode` কখনো বাড়েইনি)। commit সংখ্যা কখনো কমে না, তাই monotonic guarantee automatic — **নতুন কিছু commit না করে শুধু build করলে versionCode বদলাবে না**, তাই deploy করার আগে সব পরিবর্তন commit করা আবশ্যক (build → commit ক্রম উল্টো হলে APK-এর versionCode আর `version.json`-এর সংখ্যা mismatch হতে পারে)।

### APK নামকরণ নিয়ম (বাধ্যতামূলক)
**প্রতিটি নতুন APK-এর নামে datetime stamp থাকবে — version number নয়।**

```bash
# ক্রম গুরুত্বপূর্ণ: আগে সব কোড পরিবর্তন commit করো, তারপর build করো, তারপর deploy করো —
# versionCode build-এর মুহূর্তে HEAD থেকে fix হয়ে যায়, commit-build উল্টো ক্রমে করলে APK আর version.json-এর সংখ্যা মিলবে না
cp android/app/build/outputs/apk/debug/app-debug.apk \
   /var/www/car-apk/car-rental-$(date +%Y%m%d-%H%M%S).apk
bash /var/www/car-apk/gen-index.sh "এই রিলিজের changelog লিখো এখানে" false
```

- সঠিক নাম: `car-rental-20260628-133500.apk`
- ভুল নাম: `car-rental-v17.apk`, `app-debug.apk`, `car-rental.apk`
- `gen-index.sh` সর্বশেষ APK (mtime অনুযায়ী) থেকে `latest.apk` symlink বানায়, এবং **একই `git rev-list --count HEAD` কমান্ড** (build.gradle.kts যেটা ব্যবহার করে, ঠিক সেটাই) দিয়ে `version.json` লেখে — যাতে APK-তে বেক করা versionCode আর version.json-এর সংখ্যা কখনো ভিন্ন না হয়; নতুন version_code আগেরটার চেয়ে বড় না হলে script জোরালো সতর্কতা দেখায় (২য় আর্গুমেন্ট `true` দিলে `force_update` — ডায়ালগ dismiss করা যাবে না, শুধু critical fix-এর জন্য)

---

## Known Issues
- `DEBUG_MODE = true` — `config/config.php`-এ false করতে হবে production-এ
- `includes/Vehicle.php` ডিলিট করা হয়েছে (SQL injection ছিল) — নতুন API তে direct prepared statements ব্যবহার হচ্ছে
- Employee ও Customer dashboard এখনো placeholder

---

## Git
- Branch: `main`
- User: rzzisan
- Commit message বাংলায় বা English-এ, consistent থাকবে
