<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');

$conn   = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

// ── GET: list managers ────────────────────────────────────────────
if ($method === 'GET') {
    $where  = ['1=1'];
    $params = [];
    $types  = '';

    if (!empty($_GET['search'])) {
        $s       = '%' . $_GET['search'] . '%';
        $where[] = '(m.name LIKE ? OR m.mobile LIKE ? OR m.email LIKE ?)';
        $params  = array_merge($params, [$s, $s, $s]);
        $types  .= 'sss';
    }
    if (!empty($_GET['status'])) {
        $where[] = 'm.status = ?';
        $params[] = $_GET['status'];
        $types   .= 's';
    }

    $sql  = 'SELECT m.* FROM managers m WHERE ' . implode(' AND ', $where) . ' ORDER BY m.name';
    $stmt = $conn->prepare($sql);
    if ($params) $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    // Attach assigned vehicle (each manager has at most one vehicle per UNIQUE constraint,
    // but we keep as array for future flexibility)
    $managerIds = array_column($rows, 'id');
    $vehicleMap = [];
    if ($managerIds) {
        $in   = implode(',', array_fill(0, count($managerIds), '?'));
        $vstmt = $conn->prepare(
            "SELECT mv.manager_id, v.id, v.brand, v.model, v.registration_number, v.status as vehicle_status
             FROM manager_vehicles mv
             JOIN vehicles v ON v.id = mv.vehicle_id
             WHERE mv.manager_id IN ($in)"
        );
        $t = str_repeat('i', count($managerIds));
        $vstmt->bind_param($t, ...$managerIds);
        $vstmt->execute();
        foreach ($vstmt->get_result()->fetch_all(MYSQLI_ASSOC) as $vr) {
            $vehicleMap[(int)$vr['manager_id']][] = [
                'id'                  => (int)$vr['id'],
                'brand'               => $vr['brand'],
                'model'               => $vr['model'],
                'registration_number' => $vr['registration_number'],
                'vehicle_status'      => $vr['vehicle_status'],
            ];
        }
        $vstmt->close();
    }

    $managers = array_map(function ($m) use ($vehicleMap) {
        unset($m['password']);
        $m['id']       = (int)$m['id'];
        $m['vehicles'] = $vehicleMap[$m['id']] ?? [];
        return $m;
    }, $rows);

    json_response(['success' => true, 'data' => $managers]);
}

// ── POST: create manager ──────────────────────────────────────────
if ($method === 'POST') {
    $name       = trim($_POST['name'] ?? '');
    $mobile     = trim($_POST['mobile'] ?? '');
    $email      = trim($_POST['email'] ?? '');
    $password   = $_POST['password'] ?? '';
    $status     = in_array($_POST['status'] ?? '', ['active', 'inactive']) ? $_POST['status'] : 'active';
    $vehicle_id = isset($_POST['vehicle_id']) && (int)$_POST['vehicle_id'] > 0
                  ? (int)$_POST['vehicle_id'] : null;

    if (!$name || !$mobile || !$email || !$password) {
        json_response(['success' => false, 'message' => 'নাম, মোবাইল, ইমেইল ও পাসওয়ার্ড আবশ্যক'], 400);
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        json_response(['success' => false, 'message' => 'সঠিক ইমেইল ঠিকানা দিন'], 400);
    }
    if (strlen($password) < 6) {
        json_response(['success' => false, 'message' => 'পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে'], 400);
    }

    $chk = $conn->prepare("SELECT id FROM managers WHERE email = ?");
    $chk->bind_param('s', $email);
    $chk->execute();
    if ($chk->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
    }
    $chk->close();

    // Vehicle uniqueness check
    if ($vehicle_id) {
        $vchk = $conn->prepare("SELECT id FROM manager_vehicles WHERE vehicle_id = ?");
        $vchk->bind_param('i', $vehicle_id);
        $vchk->execute();
        if ($vchk->get_result()->num_rows > 0) {
            json_response(['success' => false, 'message' => 'এই গাড়ি ইতিমধ্যে অন্য ম্যানেজারকে অ্যাসাইন করা আছে'], 409);
        }
        $vchk->close();
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
        $dir = UPLOAD_PATH . 'managers/';
        if (!is_dir($dir)) mkdir($dir, 0755, true);
        $filename = 'manager_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
        move_uploaded_file($file['tmp_name'], $dir . $filename);
        $picture = 'uploads/managers/' . $filename;
    }

    $hashed = password_hash($password, PASSWORD_BCRYPT);
    $stmt   = $conn->prepare(
        "INSERT INTO managers (name, mobile, profile_picture, email, password, status)
         VALUES (?, ?, ?, ?, ?, ?)"
    );
    $stmt->bind_param('ssssss', $name, $mobile, $picture, $email, $hashed, $status);

    if (!$stmt->execute()) {
        json_response(['success' => false, 'message' => 'ম্যানেজার যোগ করতে ব্যর্থ: ' . $stmt->error], 500);
    }
    $manager_id = $conn->insert_id;
    $stmt->close();

    if ($vehicle_id) {
        $avs = $conn->prepare("INSERT IGNORE INTO manager_vehicles (manager_id, vehicle_id) VALUES (?, ?)");
        $avs->bind_param('ii', $manager_id, $vehicle_id);
        $avs->execute();
        $avs->close();
    }

    json_response([
        'success' => true,
        'message' => 'ম্যানেজার সফলভাবে যোগ করা হয়েছে',
        'data'    => ['manager_id' => $manager_id],
    ], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
