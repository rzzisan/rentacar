<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

only_method('PUT');
require_superadmin();

$id = (int) ($_GET['id'] ?? 0);
if (!$id) {
    json_response(['success' => false, 'message' => 'ID দিন'], 400);
}

$b = input();
$allowed = ['name', 'email', 'phone', 'address'];

$sets   = [];
$params = [];
$types  = '';

foreach ($allowed as $field) {
    if (!array_key_exists($field, $b)) continue;
    $sets[]   = "$field = ?";
    $params[] = trim((string) $b[$field]) ?: null;
    $types   .= 's';
}

if (!$sets) {
    json_response(['success' => false, 'message' => 'আপডেট করার কিছু নেই']);
}

$conn = (new Database())->connect();

$chk = $conn->prepare('SELECT id FROM tenants WHERE id = ?');
$chk->bind_param('i', $id);
$chk->execute();
if ($chk->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'Tenant পাওয়া যায়নি'], 404);
}
$chk->close();

if (isset($b['email'])) {
    $echk = $conn->prepare('SELECT id FROM tenants WHERE email = ? AND id != ?');
    $email = trim($b['email']);
    $echk->bind_param('si', $email, $id);
    $echk->execute();
    if ($echk->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই ইমেইল ইতিমধ্যে অন্য tenant ব্যবহার করছে'], 409);
    }
    $echk->close();
}

$params[] = $id;
$types   .= 'i';

$stmt = $conn->prepare('UPDATE tenants SET ' . implode(', ', $sets) . ', updated_at = NOW() WHERE id = ?');
$stmt->bind_param($types, ...$params);

if ($stmt->execute()) {
    json_response(['success' => true, 'message' => 'Tenant তথ্য আপডেট হয়েছে']);
}

json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $stmt->error]);
