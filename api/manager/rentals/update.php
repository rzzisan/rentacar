<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
only_method('POST');
$conn      = (new Database())->connect();
$rental_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$data      = input();

if (!$rental_id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

$chkStmt = $conn->prepare("SELECT id, rental_status, vehicle_id, driver_id FROM rentals WHERE id = ? AND vehicle_id IN $in");
$params  = array_merge([$rental_id], $vids);
$types   = 'i' . str_repeat('i', count($vids));
$chkStmt->bind_param($types, ...$params);
$chkStmt->execute();
$rental = $chkStmt->get_result()->fetch_assoc();
$chkStmt->close();

if (!$rental) {
    json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি'], 404);
}

if ($rental['rental_status'] !== 'pending') {
    json_response(['success' => false, 'message' => 'ট্রিপ শুরু হয়ে গেলে গাড়ি বা ড্রাইভার পরিবর্তন করা যায় না'], 400);
}

$vehicle_id   = isset($data['vehicle_id']) && $data['vehicle_id'] ? (int)$data['vehicle_id'] : (int)$rental['vehicle_id'];
$driver_given = array_key_exists('driver_id', $data);
$driver_id    = $driver_given && $data['driver_id'] ? (int)$data['driver_id'] : null;

// Manager can only set vehicles from their own list
if (!in_array($vehicle_id, $vids)) {
    json_response(['success' => false, 'message' => 'এই গাড়ি আপনার অধীনে নেই'], 403);
}

// Auto-assign driver if vehicle changed
if (!$driver_given && $vehicle_id !== (int)$rental['vehicle_id']) {
    $astmt = $conn->prepare(
        "SELECT dv.driver_id FROM driver_vehicles dv
         JOIN drivers d ON d.id = dv.driver_id AND d.status = 'active'
         WHERE dv.vehicle_id = ? ORDER BY dv.assigned_at DESC LIMIT 1"
    );
    $astmt->bind_param('i', $vehicle_id);
    $astmt->execute();
    $arow = $astmt->get_result()->fetch_assoc();
    $astmt->close();
    $driver_id = $arow ? (int)$arow['driver_id'] : null;
} elseif (!$driver_given) {
    $driver_id = $rental['driver_id'] ? (int)$rental['driver_id'] : null;
}

if ($driver_id) {
    $dstmt = $conn->prepare("SELECT id FROM drivers WHERE id = ?");
    $dstmt->bind_param('i', $driver_id);
    $dstmt->execute();
    if ($dstmt->get_result()->num_rows === 0) {
        json_response(['success' => false, 'message' => 'চালক পাওয়া যায়নি'], 404);
    }
    $dstmt->close();
}

$ustmt = $conn->prepare("UPDATE rentals SET vehicle_id = ?, driver_id = ? WHERE id = ?");
$ustmt->bind_param('iii', $vehicle_id, $driver_id, $rental_id);
if (!$ustmt->execute()) {
    json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $ustmt->error], 500);
}
$ustmt->close();

json_response(['success' => true, 'message' => 'গাড়ি ও ড্রাইভার সফলভাবে আপডেট হয়েছে',
    'data' => ['vehicle_id' => $vehicle_id, 'driver_id' => $driver_id]]);
