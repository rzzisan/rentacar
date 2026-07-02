<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

$driver_id = require_driver();
$tid = get_tenant_id();
only_method('GET');

$conn = (new Database())->connect();

// ড্রাইভারকে অ্যাসাইন করা গাড়ির তালিকা
$stmt = $conn->prepare(
    "SELECT v.id, v.registration_number, v.brand, v.model, v.year,
            v.vehicle_type, v.color, v.fuel_type, v.seating_capacity,
            v.daily_rent_price, v.status AS vehicle_status
     FROM driver_vehicles dv
     JOIN vehicles v ON v.id = dv.vehicle_id
     WHERE dv.driver_id = ? AND v.tenant_id = ?
     ORDER BY dv.assigned_at DESC"
);
$stmt->bind_param('ii', $driver_id, $tid);
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

foreach ($rows as &$row) {
    $row['id']               = (int)$row['id'];
    $row['year']             = $row['year'] ? (int)$row['year'] : null;
    $row['seating_capacity'] = $row['seating_capacity'] ? (int)$row['seating_capacity'] : null;
    $row['daily_rent_price'] = (float)$row['daily_rent_price'];
}

json_response(['success' => true, 'data' => $rows]);
?>
