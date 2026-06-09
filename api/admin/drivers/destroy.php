<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('DELETE');

$id = (int)($_GET['id'] ?? 0);
if (!$id) json_response(['success' => false, 'message' => 'ড্রাইভার ID দিন'], 400);

$conn = (new Database())->connect();

$stmt = $conn->prepare("SELECT profile_picture FROM drivers WHERE id = ?");
$stmt->bind_param('i', $id);
$stmt->execute();
$driver = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$driver) json_response(['success' => false, 'message' => 'ড্রাইভার পাওয়া যায়নি'], 404);

$stmt = $conn->prepare("DELETE FROM drivers WHERE id = ?");
$stmt->bind_param('i', $id);
if (!$stmt->execute()) {
    json_response(['success' => false, 'message' => 'মুছতে ব্যর্থ: ' . $stmt->error], 500);
}
$stmt->close();

// Delete profile picture file
if ($driver['profile_picture']) {
    $path = UPLOAD_PATH . 'drivers/' . basename($driver['profile_picture']);
    if (file_exists($path)) @unlink($path);
}

json_response(['success' => true, 'message' => 'ড্রাইভার মুছে ফেলা হয়েছে']);
