<?php
// PHP does not parse multipart/form-data on PUT, so update uses POST
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('POST');
$tid = get_tenant_id();

$id = (int)($_GET['id'] ?? 0);
if (!$id) json_response(['success' => false, 'message' => 'ড্রাইভার ID দিন'], 400);

$conn = (new Database())->connect();

$stmt = $conn->prepare("SELECT * FROM drivers WHERE id = ? AND tenant_id = ?");
$stmt->bind_param('ii', $id, $tid);
$stmt->execute();
$driver = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$driver) json_response(['success' => false, 'message' => 'ড্রাইভার পাওয়া যায়নি'], 404);

$name            = trim($_POST['name']            ?? $driver['name']);
$mobile          = trim($_POST['mobile']          ?? $driver['mobile']);
$email           = trim($_POST['email']           ?? $driver['email']);
$commission_rate = (float)($_POST['commission_rate'] ?? $driver['commission_rate']);
$status          = in_array($_POST['status'] ?? '', ['active', 'inactive']) ? $_POST['status'] : $driver['status'];
$new_password    = $_POST['password'] ?? '';
$vehicle_ids     = array_key_exists('vehicle_ids', $_POST)
                     ? (json_decode($_POST['vehicle_ids'], true) ?: [])
                     : null; // null = don't update vehicles

if (!$name || !$mobile || !$email) {
    json_response(['success' => false, 'message' => 'নাম, মোবাইল, ইমেইল আবশ্যক'], 400);
}

// Email uniqueness check
if ($email !== $driver['email']) {
    $stmt = $conn->prepare("SELECT id FROM drivers WHERE email = ? AND id != ?");
    $stmt->bind_param('si', $email, $id);
    $stmt->execute();
    if ($stmt->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
    }
    $stmt->close();
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
    // Delete old picture
    if ($driver['profile_picture']) {
        $old = UPLOAD_PATH . 'drivers/' . basename($driver['profile_picture']);
        if (file_exists($old)) @unlink($old);
    }
    $picture = 'uploads/drivers/' . $filename;
}

// Build update query
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

// Update vehicle assignments if provided
if ($vehicle_ids !== null) {
    $del = $conn->prepare("DELETE FROM driver_vehicles WHERE driver_id = ?");
    $del->bind_param('i', $id);
    $del->execute();
    $del->close();

    if (count($vehicle_ids) > 0) {
        // শুধু এই tenant-এর গাড়ি অ্যাসাইন করা যাবে — IDOR প্রতিরোধ
        $vin  = implode(',', array_fill(0, count($vehicle_ids), '?'));
        $vchk = $conn->prepare("SELECT id FROM vehicles WHERE tenant_id = ? AND id IN ($vin)");
        $vtypes  = 'i' . str_repeat('i', count($vehicle_ids));
        $vparams = array_merge([$tid], array_map('intval', $vehicle_ids));
        $vchk->bind_param($vtypes, ...$vparams);
        $vchk->execute();
        $validVehicleIds = array_column($vchk->get_result()->fetch_all(MYSQLI_ASSOC), 'id');
        $vchk->close();

        if ($validVehicleIds) {
            $vstmt = $conn->prepare("INSERT IGNORE INTO driver_vehicles (driver_id, vehicle_id) VALUES (?, ?)");
            foreach ($validVehicleIds as $vid) {
                $vid = (int)$vid;
                $vstmt->bind_param('ii', $id, $vid);
                $vstmt->execute();
            }
            $vstmt->close();
        }
    }
}

json_response(['success' => true, 'message' => 'ড্রাইভার তথ্য আপডেট হয়েছে']);
