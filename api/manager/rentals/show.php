<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
$tid = get_tenant_id();
only_method('GET');
$conn      = (new Database())->connect();
$rental_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$rental_id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

$sql = "SELECT r.*,
        c.first_name as customer_first_name, c.last_name as customer_last_name, c.phone as customer_phone,
        v.brand as vehicle_brand, v.model as vehicle_model, v.registration_number as vehicle_registration_number,
        d.name as driver_name
        FROM rentals r
        LEFT JOIN customers c ON r.customer_id = c.id
        LEFT JOIN vehicles v ON r.vehicle_id = v.id
        LEFT JOIN drivers d ON r.driver_id = d.id
        WHERE r.id = ? AND r.vehicle_id IN $in AND r.tenant_id = ?";

$stmt = $conn->prepare($sql);
$params = array_merge([$rental_id], $vids, [$tid]);
$types  = 'i' . str_repeat('i', count($vids)) . 'i';
$stmt->bind_param($types, ...$params);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি'], 404);
}

$rental = $result->fetch_assoc();
$stmt->close();

$rental['id']            = (int)$rental['id'];
$rental['customer_id']   = (int)$rental['customer_id'];
$rental['vehicle_id']    = (int)$rental['vehicle_id'];
$rental['employee_id']   = $rental['employee_id'] ? (int)$rental['employee_id'] : null;
$rental['driver_id']     = $rental['driver_id'] ? (int)$rental['driver_id'] : null;
$rental['agreed_amount'] = (float)$rental['agreed_amount'];
$rental['total_days']    = $rental['total_days'] ? (int)$rental['total_days'] : null;
$rental['daily_rate']    = $rental['daily_rate'] ? (float)$rental['daily_rate'] : null;
$rental['subtotal']      = $rental['subtotal'] ? (float)$rental['subtotal'] : null;
$rental['discount']      = $rental['discount'] ? (float)$rental['discount'] : null;
$rental['tax']           = $rental['tax'] ? (float)$rental['tax'] : null;
$rental['total_amount']  = $rental['total_amount'] ? (float)$rental['total_amount'] : null;

$estmt = $conn->prepare(
    "SELECT id, rental_id, expense_type, description, amount, receipt_image,
            location_name, latitude, longitude, created_at
     FROM trip_expenses WHERE rental_id = ? ORDER BY created_at DESC"
);
$estmt->bind_param('i', $rental_id);
$estmt->execute();
$expenses = $estmt->get_result()->fetch_all(MYSQLI_ASSOC);
$estmt->close();

foreach ($expenses as &$exp) {
    $exp['id']        = (int)$exp['id'];
    $exp['rental_id'] = (int)$exp['rental_id'];
    $exp['amount']    = (float)$exp['amount'];
    $exp['latitude']  = $exp['latitude']  !== null ? (float)$exp['latitude']  : null;
    $exp['longitude'] = $exp['longitude'] !== null ? (float)$exp['longitude'] : null;
}

$rental['expenses'] = $expenses;
json_response(['success' => true, 'data' => $rental]);
