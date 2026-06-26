<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
only_method('GET');

$conn = (new Database())->connect();
$rental_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$rental_id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

// Manager শুধু নিজের assigned গাড়ির ট্রিপ দেখতে পারবে
$vids = get_manager_vehicle_ids($conn, $manager_id);
if (!$vids) {
    json_response(['success' => false, 'message' => 'আপনার কোনো গাড়ি নেই'], 403);
}
$in = manager_vehicle_in_clause($vids);

// ট্রিপটি manager-এর গাড়ির কিনা যাচাই করে তথ্য আনো
$sql = "SELECT r.id, r.pickup_location, r.dropoff_location,
               r.rental_status, r.actual_start_time, r.actual_end_time,
               d.name as driver_name, d.mobile as driver_phone,
               v.brand as vehicle_brand, v.model as vehicle_model,
               v.registration_number as vehicle_reg
        FROM rentals r
        LEFT JOIN drivers d ON r.driver_id = d.id
        LEFT JOIN vehicles v ON r.vehicle_id = v.id
        WHERE r.id = ? AND r.vehicle_id IN $in";

$stmt = $conn->prepare($sql);
$types = 'i' . str_repeat('i', count($vids));
$stmt->bind_param($types, $rental_id, ...$vids);
$stmt->execute();
$rental = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$rental) {
    json_response(['success' => false, 'message' => 'ট্রিপ পাওয়া যায়নি বা অনুমতি নেই'], 404);
}

// লোকেশন পয়েন্ট সব
$lstmt = $conn->prepare(
    "SELECT id, latitude, longitude, accuracy, recorded_at
     FROM trip_locations
     WHERE rental_id = ?
     ORDER BY recorded_at ASC"
);
$lstmt->bind_param('i', $rental_id);
$lstmt->execute();
$rows = $lstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$lstmt->close();

$points = array_map(function($r) {
    return [
        'id'          => (int)$r['id'],
        'lat'         => (float)$r['latitude'],
        'lng'         => (float)$r['longitude'],
        'accuracy'    => $r['accuracy'] !== null ? (float)$r['accuracy'] : null,
        'recorded_at' => $r['recorded_at'],
    ];
}, $rows);

json_response([
    'success' => true,
    'data' => [
        'rental_id'        => (int)$rental['id'],
        'pickup_location'  => $rental['pickup_location'],
        'dropoff_location' => $rental['dropoff_location'],
        'rental_status'    => $rental['rental_status'],
        'actual_start_time'=> $rental['actual_start_time'],
        'actual_end_time'  => $rental['actual_end_time'],
        'driver_name'      => $rental['driver_name'],
        'driver_phone'     => $rental['driver_phone'],
        'vehicle'          => trim(($rental['vehicle_brand'] ?? '') . ' ' . ($rental['vehicle_model'] ?? '')),
        'vehicle_reg'      => $rental['vehicle_reg'],
        'total_points'     => count($points),
        'points'           => $points,
    ]
]);
?>
