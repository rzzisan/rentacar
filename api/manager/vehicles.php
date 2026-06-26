<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET', 'PUT');
$manager_id = require_manager();
$conn = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

// ── GET: assigned vehicles তালিকা ────────────────────────────────
if ($method === 'GET') {
    $vids = get_manager_vehicle_ids($conn, $manager_id);
    if (!$vids) {
        json_response(['success' => true, 'data' => []]);
    }

    $in = manager_vehicle_in_clause($vids);
    $t  = str_repeat('i', count($vids));

    $stmt = $conn->prepare("SELECT * FROM vehicles WHERE id IN $in ORDER BY brand, model");
    $stmt->bind_param($t, ...$vids);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    foreach ($rows as &$row) {
        $row['id']               = (int)$row['id'];
        $row['seating_capacity'] = (int)$row['seating_capacity'];
        $row['daily_rent_price'] = (float)$row['daily_rent_price'];
    }

    json_response(['success' => true, 'data' => $rows]);
}

// ── PUT: গাড়ির স্ট্যাটাস পরিবর্তন ───────────────────────────────
if ($method === 'PUT') {
    $vehicle_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
    if (!$vehicle_id) {
        json_response(['success' => false, 'message' => 'গাড়ির আইডি প্রয়োজন'], 400);
    }

    // Manager-এর assigned গাড়ি কিনা যাচাই
    $vids = get_manager_vehicle_ids($conn, $manager_id);
    if (!in_array($vehicle_id, $vids)) {
        json_response(['success' => false, 'message' => 'এই গাড়ি আপনার অধীনে নেই'], 403);
    }

    $body  = input();
    $status = $body['status'] ?? '';
    $allowed = ['available', 'maintenance', 'inactive'];

    if (!in_array($status, $allowed)) {
        json_response(['success' => false, 'message' => 'অবৈধ স্ট্যাটাস — শুধুমাত্র available/maintenance/inactive অনুমোদিত'], 400);
    }

    // rented গাড়ি maintenance/inactive করা যাবে না
    $cstmt = $conn->prepare("SELECT status FROM vehicles WHERE id = ?");
    $cstmt->bind_param('i', $vehicle_id);
    $cstmt->execute();
    $current = $cstmt->get_result()->fetch_assoc();
    $cstmt->close();

    if (!$current) {
        json_response(['success' => false, 'message' => 'গাড়ি পাওয়া যায়নি'], 404);
    }

    if ($current['status'] === 'rented' && $status !== 'rented') {
        json_response(['success' => false, 'message' => 'চলমান ভাড়া থাকাকালীন স্ট্যাটাস পরিবর্তন করা যাবে না'], 400);
    }

    $ustmt = $conn->prepare("UPDATE vehicles SET status = ?, updated_at = NOW() WHERE id = ?");
    $ustmt->bind_param('si', $status, $vehicle_id);
    if (!$ustmt->execute()) {
        json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ হয়েছে'], 500);
    }
    $ustmt->close();

    $status_labels = ['available' => 'উপলব্ধ', 'maintenance' => 'রক্ষণাবেক্ষণ', 'inactive' => 'নিষ্ক্রিয়'];

    json_response([
        'success' => true,
        'message' => 'গাড়ির স্ট্যাটাস ' . ($status_labels[$status] ?? $status) . ' করা হয়েছে',
        'data'    => ['id' => $vehicle_id, 'status' => $status],
    ]);
}
