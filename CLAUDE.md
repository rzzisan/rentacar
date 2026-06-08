# car.zisan.me — Rent-A-Car Management System

## প্রজেক্ট পরিচিতি
বাংলাদেশ-ভিত্তিক কার রেন্টাল ম্যানেজমেন্ট সিস্টেম। UI সম্পূর্ণ বাংলায়। তিনটি role: `admin`, `employee`, `customer`।

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
    └── admin/
        ├── Dashboard.tsx          — stats cards (real data from API)
        └── Vehicles.tsx           — CRUD: list, add modal, edit modal, delete confirm
```

### তৈরি হয়নি এখনো (placeholder দেখায়)
```
admin:    rentals, customers, payments, employees, maintenance, reports, settings
employee: dashboard, vehicles, rentals, customers
customer: dashboard, vehicles, bookings, invoices, profile
```

---

## PHP API structure

```
api/
├── _helpers.php                   — json_response(), require_auth(), require_role(), input()
├── auth/
│   ├── login.php   POST           — session তৈরি করে
│   ├── logout.php  POST           — session destroy করে JSON রিটার্ন
│   └── me.php      GET            — current user info
├── admin/
│   └── stats.php   GET            — dashboard stats (admin only)
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
4. Role অনুযায়ী redirect: admin→`/admin`, employee→`/employee`, customer→`/customer`
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
| id, customer_id, vehicle_id, employee_id | FK |
| start_date, end_date, pickup_location, dropoff_location | — |
| rental_status | enum: pending, active, completed, cancelled |
| total_days, daily_rate, subtotal, discount, tax, total_amount | decimal |
| payment_status | enum: pending, paid, partial |

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

### Build ও Deploy
```bash
cd frontend
npm run dev      # dev server :5173, /api proxy → localhost
npm run build    # → public/app/ (production)
```

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
