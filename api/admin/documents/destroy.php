<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

only_method('DELETE');
require_role('admin');
$tid = get_tenant_id();
$conn = (new Database())->connect();

$id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
if (!$id) json_response(['success' => false, 'message' => 'ID আবশ্যক'], 422);

// vehicle_documents-এ tenant_id নেই — vehicles জয়েন করে যাচাই (IDOR প্রতিরোধ)
$chk = $conn->prepare(
    "SELECT vd.id FROM vehicle_documents vd JOIN vehicles v ON v.id = vd.vehicle_id
     WHERE vd.id = ? AND v.tenant_id = ?"
);
$chk->bind_param('ii', $id, $tid);
$chk->execute();
if ($chk->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'ডকুমেন্ট পাওয়া যায়নি'], 404);
}
$chk->close();

$stmt = $conn->prepare("DELETE FROM vehicle_documents WHERE id=?");
$stmt->bind_param('i', $id);
$stmt->execute();
$stmt->close();

json_response(['success' => true, 'message' => 'মুছে ফেলা হয়েছে']);
