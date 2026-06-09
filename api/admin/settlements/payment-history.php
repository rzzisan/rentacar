<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');

$conn = (new Database())->connect();
$settlement_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$settlement_id) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট আইডি প্রয়োজন'], 400);
}

// Verify settlement exists
$verify_stmt = $conn->prepare("SELECT id FROM settlements WHERE id = ?");
$verify_stmt->bind_param('i', $settlement_id);
$verify_stmt->execute();
if ($verify_stmt->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট পাওয়া যায়নি'], 404);
}
$verify_stmt->close();

// Get payment history
$stmt = $conn->prepare(
    "SELECT sp.id,
            sp.amount,
            sp.payment_method,
            sp.payment_notes,
            sp.payment_date,
            sp.recorded_by,
            u.username as recorded_by_name
     FROM settlement_payments sp
     LEFT JOIN users u ON sp.recorded_by = u.id
     WHERE sp.settlement_id = ?
     ORDER BY sp.payment_date DESC"
);
$stmt->bind_param('i', $settlement_id);
$stmt->execute();
$payments = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

// Cast numeric fields
foreach ($payments as &$payment) {
    $payment['id'] = (int)$payment['id'];
    $payment['amount'] = (float)$payment['amount'];
}

// Calculate totals
$total_collected = 0;
foreach ($payments as $payment) {
    $total_collected += $payment['amount'];
}

json_response([
    'success' => true,
    'data' => [
        'total_collected' => (float)$total_collected,
        'payment_count' => count($payments),
        'payments' => $payments
    ]
]);
?>
