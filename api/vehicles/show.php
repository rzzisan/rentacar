<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');
require_auth();

$id = (int) ($_GET['id'] ?? 0);
if (!$id) {
    json_response(['success' => false, 'message' => 'ID দিন'], 400);
}

$conn = (new Database())->connect();
$stmt = $conn->prepare('SELECT * FROM vehicles WHERE id = ?');
$stmt->bind_param('i', $id);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();

if (!$row) {
    json_response(['success' => false, 'message' => 'গাড়ি পাওয়া যায়নি'], 404);
}

json_response(['success' => true, 'data' => $row]);
