<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$driver_id = require_driver();
$tid = get_tenant_id();
only_method('POST');

$conn = (new Database())->connect();
$rental_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$data = input();
$new_status      = $data['status'] ?? '';
$location_name   = trim($data['location_name'] ?? '') ?: null;
$latitude        = isset($data['latitude'])  && $data['latitude']  !== '' ? (float)$data['latitude']  : null;
$longitude       = isset($data['longitude']) && $data['longitude'] !== '' ? (float)$data['longitude'] : null;

if (!$rental_id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

// ড্রাইভার শুধু ট্রিপ শুরু ও সম্পন্ন করতে পারবে — বাতিল করতে পারবে না
$valid_statuses = ['active', 'completed'];
if (!in_array($new_status, $valid_statuses)) {
    json_response(['success' => false, 'message' => 'অবৈধ স্ট্যাটাস'], 400);
}

// নিজের ট্রিপ কিনা যাচাই করে বর্তমান স্ট্যাটাস আনা
$stmt = $conn->prepare("SELECT rental_status FROM rentals WHERE id = ? AND driver_id = ? AND tenant_id = ?");
$stmt->bind_param('iii', $rental_id, $driver_id, $tid);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    json_response(['success' => false, 'message' => 'ট্রিপ পাওয়া যায়নি'], 404);
}

$current = $result->fetch_assoc();
$current_status = $current['rental_status'];
$stmt->close();

// Validate status transitions
$allowed_transitions = [
    'pending'   => ['active'],
    'active'    => ['completed'],
    'completed' => [],
    'cancelled' => []
];

if (!in_array($new_status, $allowed_transitions[$current_status] ?? [])) {
    json_response([
        'success' => false,
        'message' => "'{$current_status}' থেকে '{$new_status}' এ যাওয়া যায় না"
    ], 400);
}

// Update status; record actual trip start/end times + GPS location
if ($new_status === 'active') {
    $ustmt = $conn->prepare(
        "UPDATE rentals SET rental_status = ?,
         actual_start_time = COALESCE(actual_start_time, NOW()),
         start_location_name = ?, start_latitude = ?, start_longitude = ?
         WHERE id = ? AND driver_id = ? AND tenant_id = ?"
    );
    $ustmt->bind_param('ssddiii', $new_status, $location_name, $latitude, $longitude, $rental_id, $driver_id, $tid);
} else {
    $ustmt = $conn->prepare(
        "UPDATE rentals SET rental_status = ?, end_date = NOW(), actual_end_time = NOW(),
         end_location_name = ?, end_latitude = ?, end_longitude = ?
         WHERE id = ? AND driver_id = ? AND tenant_id = ?"
    );
    $ustmt->bind_param('ssddiii', $new_status, $location_name, $latitude, $longitude, $rental_id, $driver_id, $tid);
}

if (!$ustmt->execute()) {
    json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $ustmt->error], 500);
}
$ustmt->close();

// ট্রিপ সম্পন্ন হলে সেটেলমেন্ট স্বয়ংক্রিয়ভাবে তৈরি হয়
if ($new_status === 'completed') {
    $settlement = create_settlement_for_rental($conn, $rental_id);
    json_response([
        'success' => true,
        'message' => $settlement
            ? 'ট্রিপ সম্পন্ন হয়েছে এবং সেটেলমেন্ট তৈরি হয়েছে'
            : 'ট্রিপ সম্পন্ন হয়েছে, কিন্তু সেটেলমেন্ট তৈরি করা যায়নি',
        'data' => ['settlement_id' => $settlement['id'] ?? null]
    ]);
}

json_response(['success' => true, 'message' => 'স্ট্যাটাস সফলভাবে আপডেট হয়েছে']);
?>
