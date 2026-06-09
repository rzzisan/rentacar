## PROJECT: car.zisan.me — Rent-A-Car Management System

### Stack
- Frontend: Vite + React + TypeScript + Tailwind CSS v4
- Backend: PHP REST API (session-based auth)
- DB: MySQL (car_rental_db, user: carapp)
- Routing: .htaccess → /api/* PHP, rest → React SPA
- Build output: frontend/ → public/app/

### Roles: admin | employee | customer

### Folder Structure
car.zisan.me/
├── api/
│   ├── _helpers.php          — json_response(), require_auth(), require_role(), input()
│   ├── auth/                 — login.php, logout.php, me.php
│   └── admin/
│       ├── stats.php
│       ├── vehicles/         — index, show, update, destroy
│       ├── drivers/          — index (CRUD + profile pic upload)
│       ├── rentals/          — index, show, update_status, expenses, expenses_destroy
│       └── settlements/      — index, show, update, collect-payment, payment-history
├── config/
│   ├── config.php            — DB creds, session, TAX_RATE=15%, DEBUG_MODE=true
│   └── Database.php
├── frontend/src/
│   ├── App.tsx               — routes + ProtectedRoute
│   ├── api/client.ts         — fetch wrapper (credentials: include)
│   ├── types/index.ts        — User, Vehicle, Rental, TripExpense, Driver, Settlement, SettlementPayment
│   ├── layouts/AppLayout.tsx — Header + Sidebar + Outlet
│   ├── components/layout/    — Header.tsx, Sidebar.tsx
│   └── pages/admin/
│       ├── Dashboard.tsx
│       ├── Vehicles.tsx
│       ├── Drivers.tsx
│       ├── Rentals.tsx
│       └── Settlements.tsx   ← LATEST WORK
├── database/
│   ├── migrate_rentals.sql
│   └── migrate_payment_collection.sql   ← EXECUTED
└── public/app/               — Vite build output (committed)

### DB Schema (key tables)
- users: id, username, email, password, role, status
- customers: id, user_id, first_name, last_name, phone, nid, license_number
- vehicles: id, registration_number, brand, model, year, vehicle_type, status, daily_rent_price
- drivers: id, name, mobile, status, commission_percent
- rentals: id, customer_id, vehicle_id, driver_id, start_date, end_date(nullable),
           trip_type(one_way/round_trip), agreed_amount, rental_status, payment_status
- trip_expenses: id, rental_id, expense_type, amount, receipt_image, created_at
- settlements: id, rental_id, driver_id, agreed_amount, total_expenses, net_amount,
               commission_percent, driver_commission, amount_to_collect,
               paid_amount, remaining_amount,   ← ADDED THIS SESSION
               payment_status, payment_method, payment_notes
- settlement_payments: id, settlement_id, amount, payment_method, payment_date,
                       recorded_by, notes, created_at   ← CREATED THIS SESSION

### This Session: Payment Collection Feature (COMPLETE ✓)
1. DB migration: Added paid_amount, remaining_amount to settlements; created settlement_payments table
2. API: collect-payment.php (POST, transaction-based, auto-status=paid when remaining<=0)
3. API: payment-history.php (GET, joins users for recorded_by_name)
4. Frontend Settlements.tsx: payment summary grid, history viewer (toggleable), collection form
5. Fixed: blank screen bug → TypeError: paid_amount.toFixed not a function
   → Root cause: MySQL DECIMAL returned as string, missing (float) cast in PHP
   → Fixed in settlements/index.php and settlements/show.php

### Conventions (must follow)
- All PHP numeric fields MUST be explicitly cast: (int)/(float) before json_response()
- Prepared statements only — NO string interpolation in SQL
- Response format: { success: bool, data: any, message: string }
- UI text: বাংলা
- Mobile-first Tailwind
- Build: cd frontend && npm run build → public/app/
- API auth: require_role('admin') at top of every admin endpoint

### Known Issues
- DEBUG_MODE = true in config.php (set false for production)
- settlements/show.php missing rental_start_date in SELECT (not breaking, just incomplete)

### Completed Pages (admin)
✓ Dashboard, Vehicles, Drivers, Rentals, Settlements (with payment collection)

### Placeholder Pages (not yet built)
admin: customers, payments, employees, maintenance, reports, settings
employee: dashboard, vehicles, rentals, customers
customer: dashboard, vehicles, bookings, invoices, profile

### Next Session Candidates
- Admin: Customers page (list, view details, edit)
- Admin: Employees page
- Employee dashboard
- Customer dashboard
- Fix DEBUG_MODE=false before production
