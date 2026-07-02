<?php
// PHP PUT-এ multipart/form-data parse করে না, তাই update-এ POST ব্যবহার হয় (admin/drivers/update.php-এর সাথে সামঞ্জস্যপূর্ণ)
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
$tid        = get_tenant_id();
only_method('POST');

$id = (int) ($_GET['id'] ?? 0);
if (!$id) json_response(['success' => false, 'message' => 'ড্রাইভার ID দিন'], 400);

$conn = (new Database())->connect();
$vids = get_manager_vehicle_ids($conn, $manager_id);

// এই ড্রাইভার manager-এর assigned গাড়ির সাথে যুক্ত কিনা যাচাই (তেনান্ট + ownership)
$in = manager_vehicle_in_clause($vids);
$t  = str_repeat('i', count($vids));

$ownStmt = $conn->prepare(
    "SELECT DISTINCT d.* FROM drivers d
     JOIN driver_vehicles dv ON dv.driver_id = d.id
     WHERE d.id = ? AND d.tenant_id = ? AND dv.vehicle_id IN $in"
);
$ownStmt->bind_param('ii' . $t, ...array_merge([$id, $tid], $vids));
$ownStmt->execute();
$driver = $ownStmt->get_result()->fetch_assoc();
$ownStmt->close();

if (!$driver) json_response(['success' => false, 'message' => 'ড্রাইভার পাওয়া যায়নি'], 404);

$name            = trim($_POST['name']            ?? $driver['name']);
$mobile          = trim($_POST['mobile']          ?? $driver['mobile']);
$email           = trim($_POST['email']           ?? $driver['email']);
$commission_rate = (float) ($_POST['commission_rate'] ?? $driver['commission_rate']);
$status          = in_array($_POST['status'] ?? '', ['active', 'inactive']) ? $_POST['status'] : $driver['status'];
$new_password    = $_POST['password'] ?? '';
$vehicle_ids     = array_key_exists('vehicle_ids', $_POST)
                     ? (json_decode($_POST['vehicle_ids'], true) ?: [])
                     : null; // null = ভেহিকেল অ্যাসাইনমেন্ট পরিবর্তন হবে না

if (!$name || !$mobile || !$email) {
    json_response(['success' => false, 'message' => 'নাম, মোবাইল, ইমেইল আবশ্যক'], 400);
}

if ($email !== $driver['email']) {
    $echk = $conn->prepare("SELECT id FROM drivers WHERE email = ? AND id != ?");
    $echk->bind_param('si', $email, $id);
    $echk->execute();
    if ($echk->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
    }
    $echk->close();
}

// Profile picture upload
$picture = $driver['profile_picture'];
if (!empty($_FILES['profile_picture']['name'])) {
    $file = $_FILES['profile_picture'];
    $ext  = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
    if (!in_array($ext, ALLOWED_IMAGE_TYPES)) {
        json_response(['success' => false, 'message' => 'শুধুমাত্র JPG, PNG, GIF ছবি অনুমোদিত'], 400);
    }
    if ($file['size'] > MAX_UPLOAD_SIZE) {
        json_response(['success' => false, 'message' => 'ছবির সাইজ সর্বোচ্চ ৫ MB'], 400);
    }
    $dir = UPLOAD_PATH . 'drivers/';
    if (!is_dir($dir)) mkdir($dir, 0755, true);
    $filename = 'driver_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
    move_uploaded_file($file['tmp_name'], $dir . $filename);
    if ($driver['profile_picture']) {
        $old = UPLOAD_PATH . 'drivers/' . basename($driver['profile_picture']);
        if (file_exists($old)) @unlink($old);
    }
    $picture = 'uploads/drivers/' . $filename;
}

if ($new_password) {
    if (strlen($new_password) < 6) {
        json_response(['success' => false, 'message' => 'পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে'], 400);
    }
    $hashed = password_hash($new_password, PASSWORD_BCRYPT);
    $stmt   = $conn->prepare(
        "UPDATE drivers SET name=?, mobile=?, profile_picture=?, email=?, password=?,
         commission_rate=?, status=?, updated_at=NOW() WHERE id=? AND tenant_id=?"
    );
    $stmt->bind_param('sssssdsii', $name, $mobile, $picture, $email, $hashed, $commission_rate, $status, $id, $tid);
} else {
    $stmt = $conn->prepare(
        "UPDATE drivers SET name=?, mobile=?, profile_picture=?, email=?,
         commission_rate=?, status=?, updated_at=NOW() WHERE id=? AND tenant_id=?"
    );
    $stmt->bind_param('ssssdsii', $name, $mobile, $picture, $email, $commission_rate, $status, $id, $tid);
}

if (!$stmt->execute()) {
    json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $stmt->error], 500);
}
$stmt->close();

// ভেহিকেল অ্যাসাইনমেন্ট আপডেট — শুধু manager-এর নিজের assigned গাড়িতেই (IDOR প্রতিরোধ)
if ($vehicle_ids !== null) {
    // manager শুধু তার নিজের vehicle-এর সাথে এই ড্রাইভারের সংযোগ মুছবে, tenant-এর অন্য manager-এর
    // vehicle-driver সংযোগ অক্ষত থাকবে
    if ($vids) {
        $del = $conn->prepare("DELETE FROM driver_vehicles WHERE driver_id = ? AND vehicle_id IN $in");
        $del->bind_param('i' . $t, ...array_merge([$id], $vids));
        $del->execute();
        $del->close();
    }

    $allowedVehicleIds = array_intersect(array_map('intval', $vehicle_ids), $vids);
    if ($allowedVehicleIds) {
        $vstmt = $conn->prepare("INSERT IGNORE INTO driver_vehicles (driver_id, vehicle_id) VALUES (?, ?)");
        foreach ($allowedVehicleIds as $vid) {
            $vstmt->bind_param('ii', $id, $vid);
            $vstmt->execute();
        }
        $vstmt->close();
    }
}

json_response(['success' => true, 'message' => 'ড্রাইভার তথ্য আপডেট হয়েছে']);
