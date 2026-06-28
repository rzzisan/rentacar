<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('PUT');

$id = (int)($_GET['id'] ?? 0);
if (!$id) json_response(['success' => false, 'message' => 'id আবশ্যক'], 422);

$conn = (new Database())->connect();

// গ্রাহক আছে কিনা
$chk = $conn->prepare("SELECT id FROM customers WHERE id = ?");
$chk->bind_param('i', $id);
$chk->execute();
if ($chk->get_result()->num_rows === 0) {
    $chk->close();
    json_response(['success' => false, 'message' => 'গ্রাহক পাওয়া যায়নি'], 404);
}
$chk->close();

$d = input();

$first   = trim($d['first_name']     ?? '');
$last    = trim($d['last_name']      ?? '');
$phone   = preg_replace('/\D/', '', $d['phone'] ?? '');
$email   = trim($d['email']          ?? '') ?: null;
$nid     = trim($d['nid']            ?? '') ?: null;
$address = trim($d['address']        ?? '') ?: null;
$city    = trim($d['city']           ?? '') ?: null;
$license = trim($d['license_number'] ?? '') ?: null;
$expiry  = trim($d['license_expiry'] ?? '') ?: null;

if (!$first || !$phone) {
    json_response(['success' => false, 'message' => 'নাম ও মোবাইল নম্বর আবশ্যক'], 422);
}

// অন্য গ্রাহকের ফোন নম্বরের সাথে conflict
$conflict = $conn->prepare("SELECT id FROM customers WHERE phone = ? AND id != ?");
$conflict->bind_param('si', $phone, $id);
$conflict->execute();
if ($conflict->get_result()->num_rows > 0) {
    $conflict->close();
    json_response(['success' => false, 'message' => 'এই মোবাইল নম্বর অন্য গ্রাহকের'], 409);
}
$conflict->close();

$stmt = $conn->prepare(
    "UPDATE customers
     SET first_name = ?, last_name = ?, phone = ?, email = ?,
         nid = ?, address = ?, city = ?, license_number = ?, license_expiry = ?
     WHERE id = ?"
);
$stmt->bind_param('sssssssssi',
    $first, $last, $phone, $email, $nid, $address, $city, $license, $expiry, $id
);

if (!$stmt->execute()) {
    $stmt->close();
    json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ'], 500);
}
$stmt->close();

json_response(['success' => true, 'message' => 'গ্রাহক তথ্য আপডেট হয়েছে']);
