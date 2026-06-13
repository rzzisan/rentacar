<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('DELETE');

$conn       = (new Database())->connect();
$manager_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$manager_id) {
    json_response(['success' => false, 'message' => 'ম্যানেজার আইডি প্রয়োজন'], 400);
}

$chk = $conn->prepare("SELECT id FROM managers WHERE id = ?");
$chk->bind_param('i', $manager_id);
$chk->execute();
if ($chk->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'ম্যানেজার পাওয়া যায়নি'], 404);
}
$chk->close();

$stmt = $conn->prepare("DELETE FROM managers WHERE id = ?");
$stmt->bind_param('i', $manager_id);
if (!$stmt->execute()) {
    json_response(['success' => false, 'message' => 'মুছতে ব্যর্থ: ' . $stmt->error], 500);
}
$stmt->close();

json_response(['success' => true, 'message' => 'ম্যানেজার মুছে ফেলা হয়েছে']);
