<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

$driver_id = require_driver();
$tid       = get_tenant_id();
$conn      = (new Database())->connect();
$method    = $_SERVER['REQUEST_METHOD'];

// ── GET: নিজের প্রোফাইল তথ্য ─────────────────────────────────────
if ($method === 'GET') {
    $stmt = $conn->prepare(
        "SELECT id, name, mobile, email, commission_rate, profile_picture, status, created_at
         FROM drivers WHERE id = ? AND tenant_id = ?"
    );
    $stmt->bind_param('ii', $driver_id, $tid);
    $stmt->execute();
    $driver = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if (!$driver) {
        json_response(['success' => false, 'message' => 'ড্রাইভার পাওয়া যায়নি'], 404);
    }

    unset($driver['password']);
    $driver['id']              = (int)$driver['id'];
    $driver['commission_rate'] = (float)$driver['commission_rate'];

    // assigned vehicles
    $vstmt = $conn->prepare(
        "SELECT v.id, v.brand, v.model, v.registration_number, v.status as vehicle_status, v.vehicle_type
         FROM driver_vehicles dv
         JOIN vehicles v ON v.id = dv.vehicle_id
         WHERE dv.driver_id = ? AND v.tenant_id = ?"
    );
    $vstmt->bind_param('ii', $driver_id, $tid);
    $vstmt->execute();
    $vehicles = $vstmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $vstmt->close();

    foreach ($vehicles as &$v) {
        $v['id'] = (int)$v['id'];
    }

    // summary stats
    $sstmt = $conn->prepare(
        "SELECT
            COUNT(*) as total_trips,
            COUNT(CASE WHEN rental_status = 'completed' THEN 1 END) as completed_trips,
            COUNT(CASE WHEN rental_status = 'active' THEN 1 END) as active_trips,
            COUNT(CASE WHEN rental_status = 'pending' THEN 1 END) as pending_trips
         FROM rentals WHERE driver_id = ? AND tenant_id = ?"
    );
    $sstmt->bind_param('ii', $driver_id, $tid);
    $sstmt->execute();
    $stats = $sstmt->get_result()->fetch_assoc();
    $sstmt->close();

    $stats['total_trips']     = (int)$stats['total_trips'];
    $stats['completed_trips'] = (int)$stats['completed_trips'];
    $stats['active_trips']    = (int)$stats['active_trips'];
    $stats['pending_trips']   = (int)$stats['pending_trips'];

    json_response([
        'success' => true,
        'data' => [
            'driver'   => $driver,
            'vehicles' => $vehicles,
            'stats'    => $stats,
        ],
    ]);
}

// ── POST: প্রোফাইল আপডেট ─────────────────────────────────────────
if ($method === 'POST') {
    $name             = trim($_POST['name'] ?? '');
    $mobile           = trim($_POST['mobile'] ?? '');
    $current_password = $_POST['current_password'] ?? '';
    $new_password     = $_POST['new_password'] ?? '';

    if (!$name || !$mobile) {
        json_response(['success' => false, 'message' => 'নাম ও মোবাইল নম্বর আবশ্যক'], 400);
    }

    // যদি পাসওয়ার্ড পরিবর্তন করতে চায়
    $new_password_hash = null;
    if ($new_password) {
        if (!$current_password) {
            json_response(['success' => false, 'message' => 'পাসওয়ার্ড পরিবর্তনে বর্তমান পাসওয়ার্ড আবশ্যক'], 400);
        }
        if (strlen($new_password) < 6) {
            json_response(['success' => false, 'message' => 'নতুন পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে'], 400);
        }

        $pstmt = $conn->prepare("SELECT password FROM drivers WHERE id = ? AND tenant_id = ?");
        $pstmt->bind_param('ii', $driver_id, $tid);
        $pstmt->execute();
        $row = $pstmt->get_result()->fetch_assoc();
        $pstmt->close();

        if (!$row || !password_verify($current_password, $row['password'])) {
            json_response(['success' => false, 'message' => 'বর্তমান পাসওয়ার্ড সঠিক নয়'], 401);
        }
        $new_password_hash = password_hash($new_password, PASSWORD_BCRYPT);
    }

    // Profile picture upload
    $picture = null;
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
        if (move_uploaded_file($file['tmp_name'], $dir . $filename)) {
            $picture = 'uploads/drivers/' . $filename;
        }
    }

    if ($new_password_hash && $picture) {
        $ustmt = $conn->prepare("UPDATE drivers SET name=?, mobile=?, profile_picture=?, password=? WHERE id=? AND tenant_id=?");
        $ustmt->bind_param('ssssii', $name, $mobile, $picture, $new_password_hash, $driver_id, $tid);
    } elseif ($new_password_hash) {
        $ustmt = $conn->prepare("UPDATE drivers SET name=?, mobile=?, password=? WHERE id=? AND tenant_id=?");
        $ustmt->bind_param('sssii', $name, $mobile, $new_password_hash, $driver_id, $tid);
    } elseif ($picture) {
        $ustmt = $conn->prepare("UPDATE drivers SET name=?, mobile=?, profile_picture=? WHERE id=? AND tenant_id=?");
        $ustmt->bind_param('sssii', $name, $mobile, $picture, $driver_id, $tid);
    } else {
        $ustmt = $conn->prepare("UPDATE drivers SET name=?, mobile=? WHERE id=? AND tenant_id=?");
        $ustmt->bind_param('ssii', $name, $mobile, $driver_id, $tid);
    }

    if (!$ustmt->execute()) {
        json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ হয়েছে'], 500);
    }
    $ustmt->close();

    // session username আপডেট
    $_SESSION['username'] = $name;

    json_response([
        'success' => true,
        'message' => 'প্রোফাইল সফলভাবে আপডেট হয়েছে',
        'data'    => ['name' => $name, 'mobile' => $mobile],
    ]);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
