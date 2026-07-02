<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

$driver_id = require_driver();
$tid = get_tenant_id();
only_method('POST');

$conn = (new Database())->connect();
$data = input();

$rental_id = isset($data['rental_id']) ? (int)$data['rental_id'] : 0;
$latitude  = isset($data['latitude'])  && $data['latitude']  !== '' ? (float)$data['latitude']  : null;
$longitude = isset($data['longitude']) && $data['longitude'] !== '' ? (float)$data['longitude'] : null;
$accuracy  = isset($data['accuracy'])  && $data['accuracy']  !== '' ? (float)$data['accuracy']  : null;

if (!$rental_id || $latitude === null || $longitude === null) {
    json_response(['success' => false, 'message' => 'rental_id, latitude, longitude প্রয়োজন'], 400);
}

// ড্রাইভারের active ট্রিপ কিনা যাচাই
$stmt = $conn->prepare(
    "SELECT id FROM rentals WHERE id = ? AND driver_id = ? AND tenant_id = ? AND rental_status = 'active'"
);
$stmt->bind_param('iii', $rental_id, $driver_id, $tid);
$stmt->execute();
$found = $stmt->get_result()->num_rows > 0;
$stmt->close();

if (!$found) {
    json_response(['success' => false, 'message' => 'সক্রিয় ট্রিপ পাওয়া যায়নি'], 404);
}

// লোকেশন সংরক্ষণ
$ins = $conn->prepare(
    "INSERT INTO trip_locations (rental_id, latitude, longitude, accuracy, recorded_at)
     VALUES (?, ?, ?, ?, NOW())"
);
$ins->bind_param('iddd', $rental_id, $latitude, $longitude, $accuracy);

if (!$ins->execute()) {
    json_response(['success' => false, 'message' => 'লোকেশন সংরক্ষণ ব্যর্থ'], 500);
}
$ins->close();

json_response(['success' => true, 'message' => 'লোকেশন সংরক্ষিত']);
?>
