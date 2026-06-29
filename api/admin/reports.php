<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');
require_role('admin');
$conn = (new Database())->connect();

$year = isset($_GET['year']) ? (int)$_GET['year'] : (int)date('Y');

// ── ১. মাসিক P&L ─────────────────────────────────────────────────────────────
// rentals + trip_expenses + settlements (maintenance আলাদা query-তে)
$plstmt = $conn->prepare(
    "SELECT DATE_FORMAT(r.start_date, '%Y-%m') as month,
            COUNT(r.id) as trip_count,
            COALESCE(SUM(CASE WHEN r.rental_status='completed' THEN r.agreed_amount ELSE 0 END), 0) as revenue,
            COALESCE(SUM(te.expense_total), 0) as trip_expenses,
            COALESCE(SUM(CASE WHEN r.rental_status='completed' THEN s.driver_commission ELSE 0 END), 0) as commission
     FROM rentals r
     LEFT JOIN (
         SELECT rental_id, SUM(amount) as expense_total FROM trip_expenses GROUP BY rental_id
     ) te ON te.rental_id = r.id
     LEFT JOIN settlements s ON s.rental_id = r.id
     WHERE YEAR(r.start_date) = ? AND r.rental_status != 'cancelled'
     GROUP BY DATE_FORMAT(r.start_date, '%Y-%m')
     ORDER BY month ASC"
);
$plstmt->bind_param('i', $year);
$plstmt->execute();
$pl_rows = $plstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$plstmt->close();

// রক্ষণাবেক্ষণ খরচ — আলাদাভাবে মাসভিত্তিক
$mplstmt = $conn->prepare(
    "SELECT DATE_FORMAT(start_date, '%Y-%m') as month, SUM(cost) as maint_total
     FROM maintenance
     WHERE status='completed' AND YEAR(start_date) = ?
     GROUP BY DATE_FORMAT(start_date, '%Y-%m')"
);
$mplstmt->bind_param('i', $year);
$mplstmt->execute();
$mpl_rows = $mplstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$mplstmt->close();

// month → maintenance_cost map
$maint_by_month = [];
foreach ($mpl_rows as $mr) {
    $maint_by_month[$mr['month']] = (float)$mr['maint_total'];
}

$monthly_pl = array_map(function($r) use ($maint_by_month) {
    $maint = $maint_by_month[$r['month']] ?? 0.0;
    return [
        'month'            => $r['month'],
        'trip_count'       => (int)$r['trip_count'],
        'revenue'          => (float)$r['revenue'],
        'trip_expenses'    => (float)$r['trip_expenses'],
        'commission'       => (float)$r['commission'],
        'maintenance_cost' => $maint,
        'net'              => (float)$r['revenue'] - (float)$r['trip_expenses'] - (float)$r['commission'] - $maint,
    ];
}, $pl_rows);

// ── ২. গাড়িভিত্তিক লাভজনকতা + Utilization ─────────────────────────────────
// maintenance আলাদা subquery দিয়ে pre-aggregate করা — cross-join এড়াতে
$vstmt = $conn->prepare(
    "SELECT v.id, v.brand, v.model, v.registration_number, v.status, v.year,
            COUNT(DISTINCT r.id) as trip_count,
            COALESCE(SUM(CASE WHEN r.rental_status='completed' THEN r.agreed_amount ELSE 0 END), 0) as revenue,
            COALESCE(SUM(te.expense_total), 0) as trip_expenses,
            COALESCE(SUM(CASE WHEN r.rental_status='completed' THEN s.driver_commission ELSE 0 END), 0) as commission,
            COALESCE(m_agg.maint_total, 0) as maintenance_cost,
            COALESCE(SUM(CASE WHEN r.rental_status='completed' THEN r.total_days ELSE 0 END), 0) as rented_days
     FROM vehicles v
     LEFT JOIN rentals r ON r.vehicle_id = v.id AND r.rental_status != 'cancelled' AND YEAR(r.start_date) = ?
     LEFT JOIN (
         SELECT rental_id, SUM(amount) as expense_total FROM trip_expenses GROUP BY rental_id
     ) te ON te.rental_id = r.id
     LEFT JOIN settlements s ON s.rental_id = r.id
     LEFT JOIN (
         SELECT vehicle_id, SUM(cost) as maint_total
         FROM maintenance
         WHERE status = 'completed' AND YEAR(start_date) = ?
         GROUP BY vehicle_id
     ) m_agg ON m_agg.vehicle_id = v.id
     GROUP BY v.id, v.brand, v.model, v.registration_number, v.status, v.year, m_agg.maint_total
     ORDER BY revenue DESC"
);
$vstmt->bind_param('ii', $year, $year);
$vstmt->execute();
$v_rows = $vstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$vstmt->close();

$year_days = ($year == (int)date('Y'))
    ? (int)date('z') + 1
    : (checkdate(2, 29, $year) ? 366 : 365);

$vehicle_profitability = array_map(function($r) use ($year_days) {
    $revenue  = (float)$r['revenue'];
    $expenses = (float)$r['trip_expenses'] + (float)$r['commission'] + (float)$r['maintenance_cost'];
    $rented   = (int)$r['rented_days'];
    return [
        'id'                    => (int)$r['id'],
        'brand'                 => $r['brand'],
        'model'                 => $r['model'],
        'registration_number'   => $r['registration_number'],
        'status'                => $r['status'],
        'year'                  => (int)$r['year'],
        'trip_count'            => (int)$r['trip_count'],
        'revenue'               => $revenue,
        'trip_expenses'         => (float)$r['trip_expenses'],
        'commission'            => (float)$r['commission'],
        'maintenance_cost'      => (float)$r['maintenance_cost'],
        'total_expenses'        => $expenses,
        'net'                   => $revenue - $expenses,
        'rented_days'           => $rented,
        'utilization_rate'      => $year_days > 0 ? round($rented / $year_days * 100, 1) : 0,
    ];
}, $v_rows);

// ── ৩. বকেয়া Aging ─────────────────────────────────────────────────────────
$agestmt = $conn->prepare(
    "SELECT
        s.id as settlement_id,
        r.id as rental_id,
        r.start_date,
        r.end_date,
        d.name as driver_name,
        d.mobile as driver_mobile,
        v.brand, v.model, v.registration_number,
        s.agreed_amount,
        s.driver_commission,
        s.paid_amount,
        s.remaining_amount,
        s.payment_status,
        DATEDIFF(CURDATE(), r.end_date) as days_overdue
     FROM settlements s
     JOIN rentals r ON r.id = s.rental_id
     JOIN vehicles v ON v.id = r.vehicle_id
     LEFT JOIN drivers d ON d.id = s.driver_id
     WHERE s.remaining_amount > 0
     ORDER BY days_overdue DESC"
);
$agestmt->execute();
$age_rows = $agestmt->get_result()->fetch_all(MYSQLI_ASSOC);
$agestmt->close();

$dues_aging = ['current' => [], 'days30' => [], 'days60' => [], 'days90' => [], 'total_due' => 0.0];

foreach ($age_rows as $r) {
    $item = [
        'settlement_id'       => (int)$r['settlement_id'],
        'rental_id'           => (int)$r['rental_id'],
        'start_date'          => $r['start_date'],
        'end_date'            => $r['end_date'],
        'driver_name'         => $r['driver_name'] ?? '—',
        'driver_mobile'       => $r['driver_mobile'] ?? '',
        'vehicle'             => $r['brand'] . ' ' . $r['model'],
        'registration_number' => $r['registration_number'],
        'agreed_amount'       => (float)$r['agreed_amount'],
        'driver_commission'   => (float)$r['driver_commission'],
        'paid_amount'         => (float)$r['paid_amount'],
        'remaining_amount'    => (float)$r['remaining_amount'],
        'payment_status'      => $r['payment_status'],
        'days_overdue'        => (int)$r['days_overdue'],
    ];
    $d = (int)$r['days_overdue'];
    if ($d <= 30)      $dues_aging['current'][] = $item;
    elseif ($d <= 60)  $dues_aging['days30'][]  = $item;
    elseif ($d <= 90)  $dues_aging['days60'][]  = $item;
    else               $dues_aging['days90'][]   = $item;
    $dues_aging['total_due'] += (float)$r['remaining_amount'];
}
$dues_aging['total_due'] = round($dues_aging['total_due'], 2);

// ── ৪. ড্রাইভার পারফরম্যান্স ────────────────────────────────────────────────
$dstmt = $conn->prepare(
    "SELECT d.id, d.name, d.mobile, d.commission_rate, d.status,
            COUNT(DISTINCT r.id) as total_trips,
            COUNT(DISTINCT CASE WHEN YEAR(r.start_date) = ? THEN r.id END) as year_trips,
            COUNT(DISTINCT CASE WHEN MONTH(r.start_date) = MONTH(CURDATE()) AND YEAR(r.start_date) = YEAR(CURDATE()) THEN r.id END) as month_trips,
            COALESCE(SUM(CASE WHEN r.rental_status='completed' THEN r.agreed_amount ELSE 0 END), 0) as total_revenue,
            COALESCE(SUM(s.driver_commission), 0) as total_commission,
            COALESCE(SUM(s.paid_amount), 0) as total_paid,
            COALESCE(SUM(s.remaining_amount), 0) as total_due
     FROM drivers d
     LEFT JOIN rentals r ON r.driver_id = d.id AND r.rental_status = 'completed'
     LEFT JOIN settlements s ON s.rental_id = r.id
     WHERE d.status = 'active'
     GROUP BY d.id
     ORDER BY total_revenue DESC"
);
$dstmt->bind_param('i', $year);
$dstmt->execute();
$d_rows = $dstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$dstmt->close();

$driver_performance = array_map(fn($r) => [
    'id'               => (int)$r['id'],
    'name'             => $r['name'],
    'mobile'           => $r['mobile'],
    'commission_rate'  => (float)$r['commission_rate'],
    'status'           => $r['status'],
    'total_trips'      => (int)$r['total_trips'],
    'year_trips'       => (int)$r['year_trips'],
    'month_trips'      => (int)$r['month_trips'],
    'total_revenue'    => (float)$r['total_revenue'],
    'total_commission' => (float)$r['total_commission'],
    'total_paid'       => (float)$r['total_paid'],
    'total_due'        => (float)$r['total_due'],
    'avg_trip_value'   => $r['total_trips'] > 0 ? round((float)$r['total_revenue'] / (int)$r['total_trips'], 0) : 0,
], $d_rows);

// ── ৫. গ্রাহক বিশ্লেষণ ─────────────────────────────────────────────────────
$cstmt = $conn->prepare(
    "SELECT c.id, c.first_name, c.last_name, c.phone,
            COUNT(r.id) as total_trips,
            COUNT(CASE WHEN r.rental_status='completed' THEN 1 END) as completed_trips,
            COALESCE(SUM(CASE WHEN r.rental_status='completed' THEN r.agreed_amount ELSE 0 END), 0) as total_spent,
            MAX(r.start_date) as last_trip_date,
            MIN(r.start_date) as first_trip_date,
            DATEDIFF(CURDATE(), MAX(r.start_date)) as days_since_last
     FROM customers c
     JOIN rentals r ON r.customer_id = c.id AND r.rental_status != 'cancelled'
     GROUP BY c.id
     HAVING total_trips >= 1
     ORDER BY total_spent DESC
     LIMIT 50"
);
$cstmt->execute();
$c_rows = $cstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$cstmt->close();

$customer_analytics = array_map(fn($r) => [
    'id'              => (int)$r['id'],
    'name'            => trim($r['first_name'] . ' ' . $r['last_name']),
    'phone'           => $r['phone'],
    'total_trips'     => (int)$r['total_trips'],
    'completed_trips' => (int)$r['completed_trips'],
    'total_spent'     => (float)$r['total_spent'],
    'last_trip_date'  => $r['last_trip_date'],
    'first_trip_date' => $r['first_trip_date'],
    'days_since_last' => (int)$r['days_since_last'],
    'is_repeat'       => (int)($r['total_trips'] >= 3),
    'is_inactive'     => (int)($r['days_since_last'] >= 60),
], $c_rows);

// ── বার্ষিক সারসংক্ষেপ ─────────────────────────────────────────────────────
$sumstmt = $conn->prepare(
    "SELECT
        COUNT(DISTINCT CASE WHEN r.rental_status='completed' THEN r.id END) as completed_trips,
        COUNT(DISTINCT r.id) as total_trips,
        COALESCE(SUM(CASE WHEN r.rental_status='completed' THEN r.agreed_amount ELSE 0 END), 0) as total_revenue,
        COALESCE(SUM(te.expense_total), 0) as total_trip_expenses,
        COALESCE(SUM(CASE WHEN r.rental_status='completed' THEN s.driver_commission ELSE 0 END), 0) as total_commission,
        COUNT(DISTINCT r.customer_id) as unique_customers
     FROM rentals r
     LEFT JOIN (
         SELECT rental_id, SUM(amount) as expense_total FROM trip_expenses GROUP BY rental_id
     ) te ON te.rental_id = r.id
     LEFT JOIN settlements s ON s.rental_id = r.id
     WHERE YEAR(r.start_date) = ? AND r.rental_status != 'cancelled'"
);
$sumstmt->bind_param('i', $year);
$sumstmt->execute();
$sum = $sumstmt->get_result()->fetch_assoc();
$sumstmt->close();

// মোট রক্ষণাবেক্ষণ খরচ — আলাদা query, কোনো JOIN নেই
$msum = $conn->prepare(
    "SELECT COALESCE(SUM(cost), 0) as total FROM maintenance WHERE status='completed' AND YEAR(start_date)=?"
);
$msum->bind_param('i', $year);
$msum->execute();
$mc = $msum->get_result()->fetch_assoc();
$msum->close();

$total_rev  = (float)$sum['total_revenue'];
$total_exp  = (float)$sum['total_trip_expenses'] + (float)$sum['total_commission'] + (float)$mc['total'];
$net        = $total_rev - $total_exp;

json_response([
    'success' => true,
    'data' => [
        'year'                  => $year,
        'monthly_pl'            => $monthly_pl,
        'vehicle_profitability' => $vehicle_profitability,
        'dues_aging'            => $dues_aging,
        'driver_performance'    => $driver_performance,
        'customer_analytics'    => $customer_analytics,
        'summary' => [
            'total_trips'         => (int)$sum['total_trips'],
            'completed_trips'     => (int)$sum['completed_trips'],
            'unique_customers'    => (int)$sum['unique_customers'],
            'total_revenue'       => $total_rev,
            'total_trip_expenses' => (float)$sum['total_trip_expenses'],
            'total_commission'    => (float)$sum['total_commission'],
            'total_maintenance'   => (float)$mc['total'],
            'total_expenses'      => $total_exp,
            'net_profit'          => $net,
            'total_due'           => $dues_aging['total_due'],
        ],
    ],
]);
