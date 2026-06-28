# car.zisan.me — Rent-A-Car Management System

## প্রজেক্ট পরিচিতি
বাংলাদেশ-ভিত্তিক কার রেন্টাল ম্যানেজমেন্ট সিস্টেম। UI সম্পূর্ণ বাংলায়। চারটি role: `admin`, `employee`, `customer`, `driver`।

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
│   └── index.ts                   — User, Vehicle, Rental, ApiResponse<T>
├── hooks/
│   └── useAuth.ts                 — (সংরক্ষিত, App.tsx এ inline ব্যবহার হচ্ছে)
├── layouts/
│   └── AppLayout.tsx              — Header + Sidebar + <Outlet>
├── components/
│   └── layout/
│       ├── Header.tsx             — white topbar, user dropdown, logout
│       └── Sidebar.tsx            — dark sidebar, role-based nav
└── pages/
    ├── Login.tsx                  — login form, POST /api/auth/login.php
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
│   ├── login.php   POST           — session তৈরি করে
│   ├── logout.php  POST           — session destroy করে JSON রিটার্ন
│   ├── me.php      GET            — current user info
│   └── update_profile.php POST    — নিজের প্রোফাইল আপডেট
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
└── vehicles/
    ├── index.php   GET/POST        — list (filter: status, vehicle_type, search) / create
    ├── show.php    GET ?id=        — single vehicle
    ├── update.php  PUT ?id=        — update fields
    └── destroy.php DELETE ?id=    — delete (active rental থাকলে block করে)

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
3. Login form → `POST /api/auth/login.php` → PHP session set
4. Role অনুযায়ী redirect: admin→`/admin`, manager→`/manager`, employee→`/employee`, customer→`/customer`, driver→`/driver`
5. Logout → `POST /api/auth/logout.php` → session destroy → redirect to `/login`
6. `ProtectedRoute` component role-check করে, ভুল role হলে নিজের dashboard-এ redirect

---

## Database Schema

### vehicles
| column | type |
|---|---|
| id, registration_number (UNI), brand, model | — |
| year | YEAR |
| vehicle_type | enum: sedan, suv, van, truck, premium |
| color, fuel_type | enum: petrol, diesel, hybrid, electric |
| seating_capacity, mileage, daily_rent_price | — |
| status | enum: available, rented, maintenance, inactive |
| image_path, created_at, updated_at | — |

### rentals
| column | type |
|---|---|
| id, customer_id, vehicle_id, employee_id, driver_id | FK |
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
- **APK serve directory:** `/var/www/car-apk/`
- **APK index script:** `/var/www/car-apk/gen-index.sh` — চালালে `/apk/` page + `latest.apk` symlink আপডেট হয়
- **Download page:** `https://car.zisan.me/apk/`
- **Stable latest URL:** `https://car.zisan.me/apk/latest.apk`

### APK নামকরণ নিয়ম (বাধ্যতামূলক)
**প্রতিটি নতুন APK-এর নামে datetime stamp থাকবে — version number নয়।**

```bash
# Build করার পর:
cp android/app/build/outputs/apk/debug/app-debug.apk \
   /var/www/car-apk/car-rental-$(date +%Y%m%d-%H%M%S).apk
bash /var/www/car-apk/gen-index.sh
```

- সঠিক নাম: `car-rental-20260628-133500.apk`
- ভুল নাম: `car-rental-v17.apk`, `app-debug.apk`, `car-rental.apk`
- `gen-index.sh` সর্বশেষ APK (mtime অনুযায়ী) থেকে `latest.apk` symlink বানায়

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
