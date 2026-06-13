<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
only_method('POST');
$conn      = (new Database())->connect();
$rental_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$data      = input();
$new_status = $data['status'] ?? '';

if (!$rental_id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

$valid_statuses = ['pending', 'active', 'completed', 'cancelled'];
if (!in_array($new_status, $valid_statuses)) {
    json_response(['success' => false, 'message' => 'অবৈধ স্ট্যাটাস'], 400);
}

$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

$chkStmt = $conn->prepare("SELECT rental_status FROM rentals WHERE id = ? AND vehicle_id IN $in");
$params  = array_merge([$rental_id], $vids);
$types   = 'i' . str_repeat('i', count($vids));
$chkStmt->bind_param($types, ...$params);
$chkStmt->execute();
$result = $chkStmt->get_result();

if ($result->num_rows === 0) {
    json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি'], 404);
}

$current_status = $result->fetch_assoc()['rental_status'];
$chkStmt->close();

$allowed_transitions = [
    'pending'   => ['active', 'cancelled'],
    'active'    => ['completed', 'cancelled'],
    'completed' => [],
    'cancelled' => []
];

if (!in_array($new_status, $allowed_transitions[$current_status] ?? [])) {
    json_response(['success' => false, 'message' => "'{$current_status}' থেকে '{$new_status}' এ যাওয়া যায় না"], 400);
}

if ($new_status === 'active') {
    $ustmt = $conn->prepare("UPDATE rentals SET rental_status = ?, actual_start_time = COALESCE(actual_start_time, NOW()) WHERE id = ?");
    $ustmt->bind_param('si', $new_status, $rental_id);
} elseif ($new_status === 'completed') {
    $ustmt = $conn->prepare("UPDATE rentals SET rental_status = ?, end_date = NOW(), actual_end_time = NOW() WHERE id = ?");
    $ustmt->bind_param('si', $new_status, $rental_id);
} else {
    $ustmt = $conn->prepare("UPDATE rentals SET rental_status = ? WHERE id = ?");
    $ustmt->bind_param('si', $new_status, $rental_id);
}

if (!$ustmt->execute()) {
    json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $ustmt->error], 500);
}
$ustmt->close();

if ($new_status === 'completed') {
    $settlement = create_settlement_for_rental($conn, $rental_id);
    json_response([
        'success' => true,
        'message' => $settlement ? 'ট্রিপ সম্পন্ন হয়েছে এবং সেটেলমেন্ট তৈরি হয়েছে' : 'ট্রিপ সম্পন্ন হয়েছে, কিন্তু সেটেলমেন্ট তৈরি করা যায়নি',
        'data'    => ['settlement_id' => $settlement['id'] ?? null]
    ]);
}

json_response(['success' => true, 'message' => 'স্ট্যাটাস সফলভাবে আপডেট হয়েছে']);
