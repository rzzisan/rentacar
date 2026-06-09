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

// Get driver's settlements with expenses and payments
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
     WHERE s.driver_id = ?
     ORDER BY r.start_date DESC"
);
$stmt->bind_param('i', $driver_id);
$stmt->execute();
$result = $stmt->get_result();
$settlements = $result->fetch_all(MYSQLI_ASSOC);
$stmt->close();

// Cast numeric fields for each settlement
foreach ($settlements as &$settlement) {
    $settlement['id'] = (int)$settlement['id'];
    $settlement['rental_id'] = (int)$settlement['rental_id'];
    $settlement['agreed_amount'] = (float)$settlement['agreed_amount'];
    $settlement['total_expenses'] = (float)$settlement['total_expenses'];
    $settlement['net_amount'] = (float)$settlement['net_amount'];
    $settlement['commission_percent'] = (float)$settlement['commission_percent'];
    $settlement['driver_commission'] = (float)$settlement['driver_commission'];
    $settlement['amount_to_collect'] = (float)$settlement['amount_to_collect'];
    $settlement['paid_amount'] = (float)$settlement['paid_amount'];
    $settlement['remaining_amount'] = (float)$settlement['remaining_amount'];

    // Get payment history for this settlement
    $pstmt = $conn->prepare(
        "SELECT id, amount, payment_method, payment_notes, payment_date
         FROM settlement_payments
         WHERE settlement_id = ?
         ORDER BY payment_date DESC"
    );
    $pstmt->bind_param('i', $settlement['id']);
    $pstmt->execute();
    $payments = $pstmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $pstmt->close();

    foreach ($payments as &$payment) {
        $payment['id'] = (int)$payment['id'];
        $payment['amount'] = (float)$payment['amount'];
    }

    $settlement['payments'] = $payments;
}

// Calculate totals
$total_earned = 0;
$total_pending = 0;

foreach ($settlements as $settlement) {
    $total_earned += (float)$settlement['driver_commission'];
    $total_pending += (float)$settlement['remaining_amount'];
}

json_response([
    'success' => true,
    'data' => [
        'settlements' => $settlements,
        'summary' => [
            'total_trips' => count($settlements),
            'total_earned' => (float)$total_earned,
            'total_paid' => (float)($total_earned - $total_pending),
            'total_pending' => (float)$total_pending,
        ],
    ],
]);
?>
