<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

require_auth();
only_method('GET');

if (!isset($_SESSION['driver_id'])) {
    json_response(['success' => false, 'message' => 'চালক সেশন প্রয়োজন'], 401);
}

$conn = (new Database())->connect();
$driver_id = (int)$_SESSION['driver_id'];
$tid = get_tenant_id();

// ── settlements with expense breakdown ───────────────────────────
$stmt = $conn->prepare(
    "SELECT s.id,
            s.rental_id,
            s.agreed_amount,
            s.total_expenses,
            s.net_amount,
            s.commission_percent,
            s.driver_commission,
            s.amount_to_collect,
            s.paid_amount,
            s.remaining_amount,
            s.payment_status,
            r.start_date,
            r.pickup_location,
            r.dropoff_location,
            r.trip_type,
            c.first_name as customer_first_name,
            c.last_name as customer_last_name,
            c.phone as customer_phone,
            v.brand as vehicle_brand,
            v.model as vehicle_model,
            v.registration_number
     FROM settlements s
     LEFT JOIN rentals r ON s.rental_id = r.id
     LEFT JOIN customers c ON r.customer_id = c.id
     LEFT JOIN vehicles v ON r.vehicle_id = v.id
     WHERE s.driver_id = ? AND r.tenant_id = ?
     ORDER BY r.start_date DESC"
);
$stmt->bind_param('ii', $driver_id, $tid);
$stmt->execute();
$result = $stmt->get_result();
$settlements = $result->fetch_all(MYSQLI_ASSOC);
$stmt->close();

foreach ($settlements as &$settlement) {
    $settlement['id']              = (int)$settlement['id'];
    $settlement['rental_id']       = (int)$settlement['rental_id'];
    $settlement['agreed_amount']   = (float)$settlement['agreed_amount'];
    $settlement['total_expenses']  = (float)$settlement['total_expenses'];
    $settlement['net_amount']      = (float)$settlement['net_amount'];
    $settlement['commission_percent'] = (float)$settlement['commission_percent'];
    $settlement['driver_commission']  = (float)$settlement['driver_commission'];
    $settlement['amount_to_collect']  = (float)$settlement['amount_to_collect'];
    $settlement['paid_amount']     = (float)$settlement['paid_amount'];
    $settlement['remaining_amount']= (float)$settlement['remaining_amount'];

    // payment history
    $pstmt = $conn->prepare(
        "SELECT id, amount, payment_method, notes as payment_notes, payment_date
         FROM settlement_payments
         WHERE settlement_id = ?
         ORDER BY payment_date DESC"
    );
    $pstmt->bind_param('i', $settlement['id']);
    $pstmt->execute();
    $payments = $pstmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $pstmt->close();
    foreach ($payments as &$payment) {
        $payment['id']     = (int)$payment['id'];
        $payment['amount'] = (float)$payment['amount'];
    }
    $settlement['payments'] = $payments;

    // expense breakdown per trip
    $estmt = $conn->prepare(
        "SELECT expense_type, SUM(amount) as total
         FROM trip_expenses WHERE rental_id = ?
         GROUP BY expense_type"
    );
    $estmt->bind_param('i', $settlement['rental_id']);
    $estmt->execute();
    $exp_rows = $estmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $estmt->close();
    $expense_breakdown = [];
    foreach ($exp_rows as $er) {
        $expense_breakdown[$er['expense_type']] = (float)$er['total'];
    }
    $settlement['expense_breakdown'] = $expense_breakdown;
}

// ── totals ───────────────────────────────────────────────────────
$total_earned  = 0;
$total_paid    = 0;
$total_pending = 0;
foreach ($settlements as $s) {
    $total_earned  += $s['driver_commission'];
    $total_paid    += $s['paid_amount'];
    $total_pending += $s['remaining_amount'];
}

// ── monthly breakdown (last 6 months) ────────────────────────────
$mstmt = $conn->prepare(
    "SELECT
        DATE_FORMAT(r.start_date, '%Y-%m') as month,
        COUNT(*)                           as trip_count,
        COALESCE(SUM(s.driver_commission), 0) as earned,
        COALESCE(SUM(s.paid_amount), 0)       as paid,
        COALESCE(SUM(s.remaining_amount), 0)  as pending
     FROM settlements s
     JOIN rentals r ON s.rental_id = r.id
     WHERE s.driver_id = ? AND r.tenant_id = ?
       AND r.start_date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
     GROUP BY DATE_FORMAT(r.start_date, '%Y-%m')
     ORDER BY month DESC"
);
$mstmt->bind_param('ii', $driver_id, $tid);
$mstmt->execute();
$monthly_rows = $mstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$mstmt->close();

$monthly_breakdown = array_map(function ($row) {
    return [
        'month'      => $row['month'],
        'trip_count' => (int)$row['trip_count'],
        'earned'     => (float)$row['earned'],
        'paid'       => (float)$row['paid'],
        'pending'    => (float)$row['pending'],
    ];
}, $monthly_rows);

// ── this month stats (dashboard quick view) ──────────────────────
$this_month = $monthly_breakdown[0] ?? null;
$last_month = $monthly_breakdown[1] ?? null;

json_response([
    'success' => true,
    'data' => [
        'settlements'       => $settlements,
        'summary' => [
            'total_trips'   => count($settlements),
            'total_earned'  => round($total_earned, 2),
            'total_paid'    => round($total_paid, 2),
            'total_pending' => round($total_pending, 2),
        ],
        'monthly_breakdown' => $monthly_breakdown,
        'this_month'        => $this_month,
        'last_month'        => $last_month,
    ],
]);
