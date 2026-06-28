<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

only_method('DELETE');
require_role('admin');
$conn = (new Database())->connect();

$id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
if (!$id) json_response(['success' => false, 'message' => 'ID আবশ্যক'], 422);

$stmt = $conn->prepare("DELETE FROM maintenance WHERE id=?");
$stmt->bind_param('i', $id);
$stmt->execute();
$stmt->close();

json_response(['success' => true, 'message' => 'মুছে ফেলা হয়েছে']);
