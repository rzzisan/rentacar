<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

only_method('PUT');
require_role('admin');
$conn = (new Database())->connect();

$id   = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$data = input();

if (!$id) json_response(['success' => false, 'message' => 'ID আবশ্যক'], 422);

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

if (!$type || !$start) json_response(['success' => false, 'message' => 'ধরন ও তারিখ আবশ্যক'], 422);

$stmt = $conn->prepare(
    "UPDATE maintenance SET
        maintenance_type=?, description=?, vendor=?, start_date=?, end_date=?,
        km_reading=?, next_due_date=?, next_due_km=?, cost=?, status=?
     WHERE id=?"
);
$stmt->bind_param('sssssisissi', $type, $desc, $vendor, $start, $end,
    $km, $next_date, $next_km, $cost, $status, $id);
$stmt->execute();
$stmt->close();

json_response(['success' => true, 'message' => 'আপডেট হয়েছে']);
