<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('POST');

$conn       = (new Database())->connect();
$manager_id = isset($_POST['id']) ? (int)$_POST['id'] : 0;

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

$name     = trim($_POST['name'] ?? '');
$mobile   = trim($_POST['mobile'] ?? '');
$email    = trim($_POST['email'] ?? '');
$status   = in_array($_POST['status'] ?? '', ['active', 'inactive']) ? $_POST['status'] : null;
$password = $_POST['password'] ?? '';
$vehicle_id = array_key_exists('vehicle_id', $_POST)
              ? ((int)$_POST['vehicle_id'] > 0 ? (int)$_POST['vehicle_id'] : null)
              : false; // false = not provided

if (!$name || !$mobile || !$email) {
    json_response(['success' => false, 'message' => 'নাম, মোবাইল ও ইমেইল আবশ্যক'], 400);
}
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    json_response(['success' => false, 'message' => 'সঠিক ইমেইল ঠিকানা দিন'], 400);
}

// Email uniqueness (excluding self)
$echk = $conn->prepare("SELECT id FROM managers WHERE email = ? AND id != ?");
$echk->bind_param('si', $email, $manager_id);
$echk->execute();
if ($echk->get_result()->num_rows > 0) {
    json_response(['success' => false, 'message' => 'এই ইমেইল অন্য ম্যানেজার ব্যবহার করছেন'], 409);
}
$echk->close();

// Build update query
$sets   = ['name = ?', 'mobile = ?', 'email = ?'];
$params = [$name, $mobile, $email];
$types  = 'sss';

if ($status) {
    $sets[]   = 'status = ?';
    $params[] = $status;
    $types   .= 's';
}

if ($password !== '') {
    if (strlen($password) < 6) {
        json_response(['success' => false, 'message' => 'পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে'], 400);
    }
    $sets[]   = 'password = ?';
    $params[] = password_hash($password, PASSWORD_BCRYPT);
    $types   .= 's';
}

// Profile picture
if (!empty($_FILES['profile_picture']['name'])) {
    $file = $_FILES['profile_picture'];
    $ext  = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
    if (!in_array($ext, ALLOWED_IMAGE_TYPES)) {
        json_response(['success' => false, 'message' => 'শুধুমাত্র JPG, PNG, GIF ছবি অনুমোদিত'], 400);
    }
    if ($file['size'] > MAX_UPLOAD_SIZE) {
        json_response(['success' => false, 'message' => 'ছবির সাইজ সর্বোচ্চ ৫ MB'], 400);
    }
    $dir = UPLOAD_PATH . 'managers/';
    if (!is_dir($dir)) mkdir($dir, 0755, true);
    $filename = 'manager_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
    move_uploaded_file($file['tmp_name'], $dir . $filename);
    $sets[]   = 'profile_picture = ?';
    $params[] = 'uploads/managers/' . $filename;
    $types   .= 's';
}

$params[] = $manager_id;
$types   .= 'i';
$sql = 'UPDATE managers SET ' . implode(', ', $sets) . ' WHERE id = ?';
$stmt = $conn->prepare($sql);
$stmt->bind_param($types, ...$params);
if (!$stmt->execute()) {
    json_response(['success' => false, 'message' => 'আপডেট করতে ব্যর্থ: ' . $stmt->error], 500);
}
$stmt->close();

// Handle vehicle assignment (vehicle_id = false means not sent, null means clear)
if ($vehicle_id !== false) {
    // Remove existing assignment for this manager
    $del = $conn->prepare("DELETE FROM manager_vehicles WHERE manager_id = ?");
    $del->bind_param('i', $manager_id);
    $del->execute();
    $del->close();

    if ($vehicle_id) {
        // Check uniqueness
        $vchk = $conn->prepare("SELECT id FROM manager_vehicles WHERE vehicle_id = ?");
        $vchk->bind_param('i', $vehicle_id);
        $vchk->execute();
        if ($vchk->get_result()->num_rows > 0) {
            json_response(['success' => false, 'message' => 'এই গাড়ি ইতিমধ্যে অন্য ম্যানেজারকে অ্যাসাইন করা আছে'], 409);
        }
        $vchk->close();

        $ins = $conn->prepare("INSERT INTO manager_vehicles (manager_id, vehicle_id) VALUES (?, ?)");
        $ins->bind_param('ii', $manager_id, $vehicle_id);
        $ins->execute();
        $ins->close();
    }
}

json_response(['success' => true, 'message' => 'ম্যানেজার সফলভাবে আপডেট হয়েছে']);
