<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id    = require_manager();
$tid           = get_tenant_id();
$conn          = (new Database())->connect();
$settlement_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$settlement_id) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট আইডি প্রয়োজন'], 400);
}

$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

// Verify settlement belongs to manager's vehicles
$verify = $conn->prepare(
    "SELECT s.id FROM settlements s JOIN rentals r ON s.rental_id = r.id
     WHERE s.id = ? AND r.vehicle_id IN $in AND r.tenant_id = ?"
);
$params = array_merge([$settlement_id], $vids, [$tid]);
$types  = 'i' . str_repeat('i', count($vids)) . 'i';
$verify->bind_param($types, ...$params);
$verify->execute();
if ($verify->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট পাওয়া যায়নি'], 404);
}
$verify->close();

$stmt = $conn->prepare(
    "SELECT sp.id, sp.amount, sp.payment_method, sp.notes as payment_notes, sp.payment_date,
            sp.recorded_by, u.username as recorded_by_name,
            sp.paid_by_driver_id, d.name as paid_by_driver_name
     FROM settlement_payments sp
     LEFT JOIN users u ON sp.recorded_by = u.id
     LEFT JOIN drivers d ON sp.paid_by_driver_id = d.id
     WHERE sp.settlement_id = ? ORDER BY sp.payment_date DESC"
);
$stmt->bind_param('i', $settlement_id);
$stmt->execute();
$payments = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

$total_collected = 0;
foreach ($payments as &$p) {
    $p['id']     = (int)$p['id'];
    $p['amount'] = (float)$p['amount'];
    $total_collected += $p['amount'];
}

json_response(['success' => true, 'data' => ['total_collected' => (float)$total_collected, 'payment_count' => count($payments), 'payments' => $payments]]);
