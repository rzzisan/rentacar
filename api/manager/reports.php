<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');
$manager_id = require_manager();
$tid = get_tenant_id();
$conn = (new Database())->connect();

$vids = get_manager_vehicle_ids($conn, $manager_id);
if (!$vids) {
    json_response(['success' => true, 'data' => [
        'monthly_revenue'     => [],
        'vehicle_revenue'     => [],
        'expense_breakdown'   => [],
        'driver_performance'  => [],
        'summary'             => ['total_revenue' => 0, 'total_expenses' => 0, 'total_due' => 0, 'total_trips' => 0],
    ]]);
}

$in = manager_vehicle_in_clause($vids);
$t  = str_repeat('i', count($vids));

// ── ১. মাসিক রাজস্ব (শেষ ৬ মাস) ─────────────────────────────────
$mstmt = $conn->prepare(
    "SELECT DATE_FORMAT(r.start_date, '%Y-%m') as month,
            COUNT(*) as trip_count,
            COALESCE(SUM(CASE WHEN r.agreed_amount > 0 THEN r.agreed_amount ELSE 0 END), 0) as revenue,
            COALESCE(SUM(te.expense_total), 0) as expenses
     FROM rentals r
     LEFT JOIN (
         SELECT rental_id, SUM(amount) as expense_total
         FROM trip_expenses GROUP BY rental_id
     ) te ON te.rental_id = r.id
     WHERE r.vehicle_id IN $in
       AND r.tenant_id = ?
       AND r.rental_status != 'cancelled'
       AND r.start_date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
     GROUP BY DATE_FORMAT(r.start_date, '%Y-%m')
     ORDER BY month DESC"
);
$mparams = array_merge($vids, [$tid]);
$mstmt->bind_param($t . 'i', ...$mparams);
$mstmt->execute();
$monthly_rows = $mstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$mstmt->close();

$monthly_revenue = array_map(fn($r) => [
    'month'      => $r['month'],
    'trip_count' => (int)$r['trip_count'],
    'revenue'    => (float)$r['revenue'],
    'expenses'   => (float)$r['expenses'],
    'net'        => (float)$r['revenue'] - (float)$r['expenses'],
], $monthly_rows);

// ── ২. গাড়িভিত্তিক রাজস্ব ───────────────────────────────────────
$vstmt = $conn->prepare(
    "SELECT v.id, v.brand, v.model, v.registration_number, v.status,
            COUNT(r.id) as trip_count,
            COALESCE(SUM(r.agreed_amount), 0) as revenue,
            COALESCE(SUM(te.expense_total), 0) as expenses
     FROM vehicles v
     LEFT JOIN rentals r ON r.vehicle_id = v.id AND r.rental_status != 'cancelled'
     LEFT JOIN (
         SELECT rental_id, SUM(amount) as expense_total
         FROM trip_expenses GROUP BY rental_id
     ) te ON te.rental_id = r.id
     WHERE v.id IN $in AND v.tenant_id = ?
     GROUP BY v.id
     ORDER BY revenue DESC"
);
$vparams = array_merge($vids, [$tid]);
$vstmt->bind_param($t . 'i', ...$vparams);
$vstmt->execute();
$vehicle_rows = $vstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$vstmt->close();

$vehicle_revenue = array_map(fn($r) => [
    'id'                  => (int)$r['id'],
    'brand'               => $r['brand'],
    'model'               => $r['model'],
    'registration_number' => $r['registration_number'],
    'status'              => $r['status'],
    'trip_count'          => (int)$r['trip_count'],
    'revenue'             => (float)$r['revenue'],
    'expenses'            => (float)$r['expenses'],
    'net'                 => (float)$r['revenue'] - (float)$r['expenses'],
], $vehicle_rows);

// ── ৩. খরচের ধরনভিত্তিক ব্রেকডাউন ─────────────────────────────
$estmt = $conn->prepare(
    "SELECT te.expense_type, SUM(te.amount) as total
     FROM trip_expenses te
     JOIN rentals r ON r.id = te.rental_id
     WHERE r.vehicle_id IN $in AND r.tenant_id = ?
     GROUP BY te.expense_type
     ORDER BY total DESC"
);
$eparams = array_merge($vids, [$tid]);
$estmt->bind_param($t . 'i', ...$eparams);
$estmt->execute();
$expense_rows = $estmt->get_result()->fetch_all(MYSQLI_ASSOC);
$estmt->close();

$expense_breakdown = array_map(fn($r) => [
    'expense_type' => $r['expense_type'],
    'total'        => (float)$r['total'],
], $expense_rows);

// ── ৪. ড্রাইভার পারফরম্যান্স ────────────────────────────────────
$dstmt = $conn->prepare(
    "SELECT d.id, d.name, d.mobile, d.commission_rate, d.status,
            COUNT(r.id) as total_trips,
            COUNT(CASE WHEN MONTH(r.start_date) = MONTH(CURDATE()) AND YEAR(r.start_date) = YEAR(CURDATE()) THEN 1 END) as this_month_trips,
            COALESCE(SUM(s.driver_commission), 0) as total_commission,
            COALESCE(SUM(s.paid_amount), 0) as total_paid,
            COALESCE(SUM(s.remaining_amount), 0) as total_due
     FROM drivers d
     JOIN driver_vehicles dv ON d.id = dv.driver_id
     LEFT JOIN rentals r ON r.driver_id = d.id AND r.vehicle_id IN $in AND r.rental_status = 'completed'
     LEFT JOIN settlements s ON s.rental_id = r.id
     WHERE dv.vehicle_id IN $in AND d.tenant_id = ?
     GROUP BY d.id
     ORDER BY total_trips DESC"
);
$params_d = array_merge($vids, $vids, [$tid]);
$types_d  = $t . $t . 'i';
$dstmt->bind_param($types_d, ...$params_d);
$dstmt->execute();
$driver_rows = $dstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$dstmt->close();

$driver_performance = array_map(fn($r) => [
    'id'               => (int)$r['id'],
    'name'             => $r['name'],
    'mobile'           => $r['mobile'],
    'commission_rate'  => (float)$r['commission_rate'],
    'status'           => $r['status'],
    'total_trips'      => (int)$r['total_trips'],
    'this_month_trips' => (int)$r['this_month_trips'],
    'total_commission' => (float)$r['total_commission'],
    'total_paid'       => (float)$r['total_paid'],
    'total_due'        => (float)$r['total_due'],
], $driver_rows);

// ── সারসংক্ষেপ ──────────────────────────────────────────────────
$sumstmt = $conn->prepare(
    "SELECT
        COUNT(r.id) as total_trips,
        COALESCE(SUM(r.agreed_amount), 0) as total_revenue,
        COALESCE(SUM(te.expense_total), 0) as total_expenses,
        COALESCE(SUM(s.remaining_amount), 0) as total_due
     FROM rentals r
     LEFT JOIN (
         SELECT rental_id, SUM(amount) as expense_total
         FROM trip_expenses GROUP BY rental_id
     ) te ON te.rental_id = r.id
     LEFT JOIN settlements s ON s.rental_id = r.id AND s.payment_status != 'paid'
     WHERE r.vehicle_id IN $in AND r.tenant_id = ? AND r.rental_status != 'cancelled'"
);
$sumparams = array_merge($vids, [$tid]);
$sumstmt->bind_param($t . 'i', ...$sumparams);
$sumstmt->execute();
$sum = $sumstmt->get_result()->fetch_assoc();
$sumstmt->close();

json_response([
    'success' => true,
    'data' => [
        'monthly_revenue'    => $monthly_revenue,
        'vehicle_revenue'    => $vehicle_revenue,
        'expense_breakdown'  => $expense_breakdown,
        'driver_performance' => $driver_performance,
        'summary' => [
            'total_trips'   => (int)$sum['total_trips'],
            'total_revenue' => (float)$sum['total_revenue'],
            'total_expenses'=> (float)$sum['total_expenses'],
            'total_due'     => (float)$sum['total_due'],
        ],
    ],
]);
