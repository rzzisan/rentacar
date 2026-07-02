<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('DELETE');
require_role('admin');
$tid = get_tenant_id();

$id = (int) ($_GET['id'] ?? 0);
if (!$id) {
    json_response(['success' => false, 'message' => 'ID দিন'], 400);
}

$conn = (new Database())->connect();

// vehicle এই tenant-এর কিনা যাচাই
$chk = $conn->prepare('SELECT id FROM vehicles WHERE id = ? AND tenant_id = ?');
$chk->bind_param('ii', $id, $tid);
$chk->execute();
if ($chk->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'গাড়ি পাওয়া যায়নি'], 404);
}
$chk->close();

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

$stmt = $conn->prepare('DELETE FROM vehicles WHERE id = ? AND tenant_id = ?');
$stmt->bind_param('ii', $id, $tid);

if ($stmt->execute() && $stmt->affected_rows > 0) {
    json_response(['success' => true, 'message' => 'গাড়ি মুছে ফেলা হয়েছে']);
}

json_response(['success' => false, 'message' => 'গাড়ি পাওয়া যায়নি বা মুছতে ব্যর্থ'], 404);
