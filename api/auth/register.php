<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('POST');

$conn = (new Database())->connect();
$b    = input();

$name           = trim($b['name'] ?? '');
$email          = trim($b['email'] ?? '');
$phone          = trim($b['phone'] ?? '') ?: null;
$address        = trim($b['address'] ?? '') ?: null;
$admin_name     = trim($b['admin_name'] ?? '');
$admin_email    = trim($b['admin_email'] ?? '');
$admin_phone    = trim($b['admin_phone'] ?? '');
$admin_password = $b['admin_password'] ?? '';

if (!$name || !$email || !$admin_name || !$admin_email || !$admin_phone || !$admin_password) {
    json_response(['success' => false, 'message' => 'ব্যবসার নাম/ইমেইল ও আপনার নাম/ইমেইল/মোবাইল/পাসওয়ার্ড আবশ্যক'], 400);
}
if (!filter_var($email, FILTER_VALIDATE_EMAIL) || !filter_var($admin_email, FILTER_VALIDATE_EMAIL)) {
    json_response(['success' => false, 'message' => 'সঠিক ইমেইল ঠিকানা দিন'], 400);
}
$admin_phone_clean = preg_replace('/[^\d]/', '', $admin_phone);
if (!preg_match('/^\d{10,15}$/', $admin_phone_clean)) {
    json_response(['success' => false, 'message' => 'সঠিক মোবাইল নম্বর দিন'], 400);
}
if (strlen($admin_password) < 6) {
    json_response(['success' => false, 'message' => 'পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে'], 400);
}

// Tenant email uniqueness
$chk = $conn->prepare('SELECT id FROM tenants WHERE email = ?');
$chk->bind_param('s', $email);
$chk->execute();
if ($chk->get_result()->num_rows > 0) {
    json_response(['success' => false, 'message' => 'এই ব্যবসার ইমেইল ইতিমধ্যে নিবন্ধিত'], 409);
}
$chk->close();

// Admin email uniqueness (users টেবিল global-unique)
$chk2 = $conn->prepare('SELECT id FROM users WHERE email = ?');
$chk2->bind_param('s', $admin_email);
$chk2->execute();
if ($chk2->get_result()->num_rows > 0) {
    json_response(['success' => false, 'message' => 'এই ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
}
$chk2->close();

// Admin phone uniqueness
$chk3 = $conn->prepare('SELECT id FROM users WHERE phone = ?');
$chk3->bind_param('s', $admin_phone_clean);
$chk3->execute();
if ($chk3->get_result()->num_rows > 0) {
    json_response(['success' => false, 'message' => 'এই মোবাইল নম্বর ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
}
$chk3->close();

// username জেনারেট করা হয় ইমেইলের local-part থেকে, collision হলে সংখ্যা যোগ হয়
$base_username = preg_replace('/[^a-zA-Z0-9_]/', '', strtolower(explode('@', $admin_email)[0])) ?: 'admin';
$username = $base_username;
$suffix = 1;
while (true) {
    $ustmt = $conn->prepare('SELECT id FROM users WHERE username = ?');
    $ustmt->bind_param('s', $username);
    $ustmt->execute();
    $taken = $ustmt->get_result()->num_rows > 0;
    $ustmt->close();
    if (!$taken) break;
    $username = $base_username . $suffix;
    $suffix++;
}

// সাস্ক্রিপশন সেটিংস থেকে trial period
$tstmt2 = $conn->prepare("SELECT value FROM saas_settings WHERE key_name = 'trial_days'");
$tstmt2->execute();
$trial_row = $tstmt2->get_result()->fetch_assoc();
$tstmt2->close();
$trial_days    = $trial_row ? (int) $trial_row['value'] : 30;
$trial_ends_at = date('Y-m-d', strtotime("+{$trial_days} days"));

$conn->begin_transaction();
try {
    $tstmt = $conn->prepare(
        "INSERT INTO tenants (name, email, phone, address, status, trial_ends_at) VALUES (?, ?, ?, ?, 'trial', ?)"
    );
    $tstmt->bind_param('sssss', $name, $email, $phone, $address, $trial_ends_at);
    $tstmt->execute();
    $tenant_id = $conn->insert_id;
    $tstmt->close();

    $sstmt = $conn->prepare(
        "INSERT INTO subscriptions (tenant_id, started_at, status, vehicle_count) VALUES (?, CURDATE(), 'trialing', 0)"
    );
    $sstmt->bind_param('i', $tenant_id);
    $sstmt->execute();
    $sstmt->close();

    $hashed = password_hash($admin_password, PASSWORD_BCRYPT);
    $arole  = 'admin';
    $astmt  = $conn->prepare(
        'INSERT INTO users (tenant_id, username, email, phone, password, role, status) VALUES (?, ?, ?, ?, ?, ?, "active")'
    );
    $astmt->bind_param('isssss', $tenant_id, $username, $admin_email, $admin_phone_clean, $hashed, $arole);
    $astmt->execute();
    $admin_user_id = $conn->insert_id;
    $astmt->close();

    $conn->commit();
} catch (Throwable $e) {
    $conn->rollback();
    json_response(['success' => false, 'message' => 'নিবন্ধন ব্যর্থ: ' . $e->getMessage()], 500);
}

// অটো-লগইন — নতুন করে আবার লগইন করতে হবে না
session_regenerate_id(true);
$_SESSION['user_id']   = $admin_user_id;
$_SESSION['tenant_id'] = $tenant_id;
$_SESSION['username']  = $admin_name;
$_SESSION['email']     = $admin_email;
$_SESSION['role']      = 'admin';
$_SESSION['login_time']= time();

json_response([
    'success' => true,
    'message' => 'নিবন্ধন সফল হয়েছে! আপনার ' . $trial_days . ' দিনের ট্রায়াল শুরু হলো।',
    'data'    => [
        'id'        => $admin_user_id,
        'tenant_id' => $tenant_id,
        'username'  => $admin_name,
        'email'     => $admin_email,
        'role'      => 'admin',
    ],
], 201);
