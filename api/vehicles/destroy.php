<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('DELETE');
require_role('admin');

$id = (int) ($_GET['id'] ?? 0);
if (!$id) {
    json_response(['success' => false, 'message' => 'ID দিন'], 400);
}

$conn = (new Database())->connect();

// Prevent deletion if vehicle has active/pending rentals
$stmt = $conn->prepare(
    "SELECT COUNT(*) c FROM rentals
     WHERE vehicle_id = ? AND rental_status IN ('active','pending')"
);
$stmt->bind_param('i', $id);
$stmt->execute();
$count = (int) $stmt->get_result()->fetch_assoc()['c'];

if ($count > 0) {
    json_response(['success' => false, 'message' => 'এই গাড়িতে সক্রিয় রেন্টাল আছে, মুছতে পারবেন না']);
}

$stmt = $conn->prepare('DELETE FROM vehicles WHERE id = ?');
$stmt->bind_param('i', $id);

if ($stmt->execute() && $stmt->affected_rows > 0) {
    json_response(['success' => true, 'message' => 'গাড়ি মুছে ফেলা হয়েছে']);
}

json_response(['success' => false, 'message' => 'গাড়ি পাওয়া যায়নি বা মুছতে ব্যর্থ'], 404);
