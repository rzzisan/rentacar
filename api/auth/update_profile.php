<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('PUT');
require_auth();

// super_admins টেবিল আলাদা — এই endpoint শুধু `users` টেবিলের রো (admin/employee/customer) আপডেট করে।
// superadmin session-এর user_id আসলে super_admins.id, users.id নয় — এখানে চললে ভুল ইউজারের ডেটা বদলে যেতে পারে।
if (($_SESSION['role'] ?? '') === 'superadmin') {
    json_response(['success' => false, 'message' => 'এই কাজের অনুমতি নেই'], 403);
}

$body         = input();
$new_email    = trim($body['email'] ?? '');
$current_pass = $body['current_password'] ?? '';
$new_pass     = $body['new_password'] ?? '';

if (!$current_pass) {
    json_response(['success' => false, 'message' => 'বর্তমান পাসওয়ার্ড দিন'], 400);
}

if (!$new_email && !$new_pass) {
    json_response(['success' => false, 'message' => 'ইমেইল বা নতুন পাসওয়ার্ড দিন'], 400);
}

if ($new_email && !filter_var($new_email, FILTER_VALIDATE_EMAIL)) {
    json_response(['success' => false, 'message' => 'সঠিক ইমেইল ঠিকানা দিন'], 400);
}

if ($new_pass && strlen($new_pass) < 6) {
    json_response(['success' => false, 'message' => 'পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে'], 400);
}

$conn    = (new Database())->connect();
$user_id = (int) $_SESSION['user_id'];

// Verify current password
$stmt = $conn->prepare("SELECT password FROM users WHERE id = ?");
$stmt->bind_param('i', $user_id);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$row || !password_verify($current_pass, $row['password'])) {
    json_response(['success' => false, 'message' => 'বর্তমান পাসওয়ার্ড সঠিক নয়'], 401);
}

// Check email uniqueness
if ($new_email && $new_email !== $_SESSION['email']) {
    $stmt = $conn->prepare("SELECT id FROM users WHERE email = ? AND id != ?");
    $stmt->bind_param('si', $new_email, $user_id);
    $stmt->execute();
    if ($stmt->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
    }
    $stmt->close();
}

// Build update fields
$fields = [];
$types  = '';
$values = [];

if ($new_email && $new_email !== $_SESSION['email']) {
    $fields[] = 'email = ?';
    $types   .= 's';
    $values[] = $new_email;
}

if ($new_pass) {
    $fields[] = 'password = ?';
    $types   .= 's';
    $values[] = password_hash($new_pass, PASSWORD_BCRYPT);
}

if (empty($fields)) {
    json_response(['success' => false, 'message' => 'কোনো পরিবর্তন করা হয়নি'], 400);
}

$fields[] = 'updated_at = NOW()';
$types   .= 'i';
$values[] = $user_id;

$stmt = $conn->prepare("UPDATE users SET " . implode(', ', $fields) . " WHERE id = ?");
$stmt->bind_param($types, ...$values);

if (!$stmt->execute()) {
    json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ হয়েছে'], 500);
}
$stmt->close();

if ($new_email && $new_email !== $_SESSION['email']) {
    $_SESSION['email'] = $new_email;
}

json_response([
    'success' => true,
    'message' => 'তথ্য সফলভাবে আপডেট হয়েছে',
    'data'    => [
        'id'       => $user_id,
        'username' => $_SESSION['username'],
        'email'    => $_SESSION['email'],
        'role'     => $_SESSION['role'],
    ],
]);
