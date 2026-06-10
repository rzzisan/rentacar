<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('POST');

$conn = (new Database())->connect();
$rental_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$data = input();
$new_status = $data['status'] ?? '';

if (!$rental_id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

$valid_statuses = ['pending', 'active', 'completed', 'cancelled'];
if (!in_array($new_status, $valid_statuses)) {
    json_response(['success' => false, 'message' => 'অবৈধ স্ট্যাটাস'], 400);
}

// Get current status
$stmt = $conn->prepare("SELECT rental_status FROM rentals WHERE id = ?");
$stmt->bind_param('i', $rental_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি'], 404);
}

$current = $result->fetch_assoc();
$current_status = $current['rental_status'];
$stmt->close();

// Validate status transitions
$allowed_transitions = [
    'pending'   => ['active', 'cancelled'],
    'active'    => ['completed', 'cancelled'],
    'completed' => [],
    'cancelled' => []
];

if (!in_array($new_status, $allowed_transitions[$current_status] ?? [])) {
    json_response([
        'success' => false,
        'message' => "'{$current_status}' থেকে '{$new_status}' এ যাওয়া যায় না"
    ], 400);
}

// Update status; record actual trip start/end times
if ($new_status === 'active') {
    // ট্রিপ শুরু — প্রকৃত শুরুর সময় রেকর্ড (আগে সেট থাকলে অপরিবর্তিত)
    $ustmt = $conn->prepare(
        "UPDATE rentals SET rental_status = ?, actual_start_time = COALESCE(actual_start_time, NOW()) WHERE id = ?"
    );
    $ustmt->bind_param('si', $new_status, $rental_id);
} elseif ($new_status === 'completed') {
    // ট্রিপ সম্পন্ন — প্রকৃত শেষের সময় রেকর্ড
    $ustmt = $conn->prepare(
        "UPDATE rentals SET rental_status = ?, end_date = NOW(), actual_end_time = NOW() WHERE id = ?"
    );
    $ustmt->bind_param('si', $new_status, $rental_id);
} else {
    $ustmt = $conn->prepare(
        "UPDATE rentals SET rental_status = ? WHERE id = ?"
    );
    $ustmt->bind_param('si', $new_status, $rental_id);
}

if (!$ustmt->execute()) {
    json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $ustmt->error], 500);
}
$ustmt->close();

json_response(['success' => true, 'message' => 'স্ট্যাটাস সফলভাবে আপডেট হয়েছে']);
?>
