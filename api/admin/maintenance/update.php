<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

only_method('PUT');
require_role('admin');
$tid = get_tenant_id();
$conn = (new Database())->connect();

$id   = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$data = input();

if (!$id) json_response(['success' => false, 'message' => 'ID আবশ্যক'], 422);

$vehicle_id = (int)($data['vehicle_id'] ?? 0);
$type      = trim($data['maintenance_type'] ?? '');
$desc      = trim($data['description'] ?? '');
$vendor    = trim($data['vendor'] ?? '');
$start     = trim($data['start_date'] ?? '');
$end       = $data['end_date'] ? trim($data['end_date']) : null;
$km        = $data['km_reading'] ? (int)$data['km_reading'] : null;
$next_date = $data['next_due_date'] ? trim($data['next_due_date']) : null;
$next_km   = $data['next_due_km'] ? (int)$data['next_due_km'] : null;
$cost      = $data['cost'] !== null ? (float)$data['cost'] : null;
$status    = trim($data['status'] ?? 'pending');

if (!$vehicle_id || !$type || !$start) json_response(['success' => false, 'message' => 'গাড়ি, ধরন ও তারিখ আবশ্যক'], 422);

// গাড়ি এই tenant-এর কিনা যাচাই — IDOR প্রতিরোধ (create endpoint-এর মতোই)
$vchk = $conn->prepare("SELECT id FROM vehicles WHERE id = ? AND tenant_id = ?");
$vchk->bind_param('ii', $vehicle_id, $tid);
$vchk->execute();
if ($vchk->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'গাড়ি পাওয়া যায়নি'], 404);
}
$vchk->close();

$stmt = $conn->prepare(
    "UPDATE maintenance SET
        vehicle_id=?, maintenance_type=?, description=?, vendor=?, start_date=?, end_date=?,
        km_reading=?, next_due_date=?, next_due_km=?, cost=?, status=?
     WHERE id=? AND tenant_id=?"
);
$stmt->bind_param('isssssisidsii', $vehicle_id, $type, $desc, $vendor, $start, $end,
    $km, $next_date, $next_km, $cost, $status, $id, $tid);
$stmt->execute();
$affected = $stmt->affected_rows;
$stmt->close();

if ($affected === 0) json_response(['success' => false, 'message' => 'রেকর্ড পাওয়া যায়নি'], 404);

json_response(['success' => true, 'message' => 'আপডেট হয়েছে']);
