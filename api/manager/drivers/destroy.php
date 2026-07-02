<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
$tid        = get_tenant_id();
only_method('DELETE');

$id = (int) ($_GET['id'] ?? 0);
if (!$id) json_response(['success' => false, 'message' => 'ড্রাইভার ID দিন'], 400);

$conn = (new Database())->connect();
$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);
$t    = str_repeat('i', count($vids));

// এই ড্রাইভার manager-এর assigned গাড়ির সাথে যুক্ত কিনা যাচাই (তেনান্ট + ownership)
$ownStmt = $conn->prepare(
    "SELECT DISTINCT d.id, d.profile_picture FROM drivers d
     JOIN driver_vehicles dv ON dv.driver_id = d.id
     WHERE d.id = ? AND d.tenant_id = ? AND dv.vehicle_id IN $in"
);
$ownStmt->bind_param('ii' . $t, ...array_merge([$id, $tid], $vids));
$ownStmt->execute();
$driver = $ownStmt->get_result()->fetch_assoc();
$ownStmt->close();

if (!$driver) json_response(['success' => false, 'message' => 'ড্রাইভার পাওয়া যায়নি'], 404);

$stmt = $conn->prepare("DELETE FROM drivers WHERE id = ? AND tenant_id = ?");
$stmt->bind_param('ii', $id, $tid);
if (!$stmt->execute()) {
    json_response(['success' => false, 'message' => 'মুছতে ব্যর্থ: ' . $stmt->error], 500);
}
$stmt->close();

if ($driver['profile_picture']) {
    $path = UPLOAD_PATH . 'drivers/' . basename($driver['profile_picture']);
    if (file_exists($path)) @unlink($path);
}

json_response(['success' => true, 'message' => 'ড্রাইভার মুছে ফেলা হয়েছে']);
