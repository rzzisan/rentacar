<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id    = require_manager();
$conn          = (new Database())->connect();
$settlement_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$settlement_id) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট আইডি প্রয়োজন'], 400);
}

$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

$stmt = $conn->prepare(
    "SELECT s.*,
            c.first_name as customer_first_name, c.last_name as customer_last_name, c.phone as customer_phone,
            v.brand as vehicle_brand, v.model as vehicle_model, v.registration_number as vehicle_registration_number,
            d.name as driver_name, d.mobile as driver_mobile,
            r.pickup_location, r.dropoff_location, r.trip_type, r.notes as rental_notes
     FROM settlements s
     LEFT JOIN rentals r ON s.rental_id = r.id
     LEFT JOIN customers c ON r.customer_id = c.id
     LEFT JOIN vehicles v ON r.vehicle_id = v.id
     LEFT JOIN drivers d ON s.driver_id = d.id
     WHERE s.id = ? AND r.vehicle_id IN $in"
);
$params = array_merge([$settlement_id], $vids);
$types  = 'i' . str_repeat('i', count($vids));
$stmt->bind_param($types, ...$params);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট পাওয়া যায়নি'], 404);
}

$settlement = $result->fetch_assoc();
$stmt->close();

$estmt = $conn->prepare(
    "SELECT id, expense_type, description, amount, receipt_image, created_at
     FROM trip_expenses WHERE rental_id = ? ORDER BY created_at DESC"
);
$estmt->bind_param('i', $settlement['rental_id']);
$estmt->execute();
$expenses = $estmt->get_result()->fetch_all(MYSQLI_ASSOC);
$estmt->close();

$settlement['id']                 = (int)$settlement['id'];
$settlement['rental_id']          = (int)$settlement['rental_id'];
$settlement['driver_id']          = $settlement['driver_id'] ? (int)$settlement['driver_id'] : null;
$settlement['agreed_amount']      = (float)$settlement['agreed_amount'];
$settlement['total_expenses']     = (float)$settlement['total_expenses'];
$settlement['net_amount']         = (float)$settlement['net_amount'];
$settlement['commission_percent'] = (float)$settlement['commission_percent'];
$settlement['driver_commission']  = (float)$settlement['driver_commission'];
$settlement['amount_to_collect']  = (float)$settlement['amount_to_collect'];
$settlement['paid_amount']        = (float)$settlement['paid_amount'];
$settlement['remaining_amount']   = (float)$settlement['remaining_amount'];

foreach ($expenses as &$exp) {
    $exp['id']     = (int)$exp['id'];
    $exp['amount'] = (float)$exp['amount'];
}

$settlement['expenses'] = $expenses;
json_response(['success' => true, 'data' => $settlement]);
