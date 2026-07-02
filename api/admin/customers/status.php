<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('PUT');
$tid = get_tenant_id();

$id = (int)($_GET['id'] ?? 0);
if (!$id) json_response(['success' => false, 'message' => 'id আবশ্যক'], 422);

$d      = input();
$status = $d['status'] ?? '';

if (!in_array($status, ['active', 'inactive'], true)) {
    json_response(['success' => false, 'message' => 'status অবশ্যই active বা inactive হতে হবে'], 422);
}

$conn = (new Database())->connect();

$stmt = $conn->prepare("UPDATE customers SET status = ? WHERE id = ? AND tenant_id = ?");
$stmt->bind_param('sii', $status, $id, $tid);

if (!$stmt->execute() || $stmt->affected_rows === 0) {
    $stmt->close();
    json_response(['success' => false, 'message' => 'গ্রাহক পাওয়া যায়নি বা পরিবর্তন হয়নি'], 404);
}
$stmt->close();

$label = $status === 'active' ? 'সক্রিয়' : 'নিষ্ক্রিয়';
json_response(['success' => true, 'message' => "গ্রাহক $label করা হয়েছে"]);
