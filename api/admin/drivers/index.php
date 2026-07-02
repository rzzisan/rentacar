<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
$tid = get_tenant_id();

$conn   = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

// ── GET: list drivers ────────────────────────────────────────────
if ($method === 'GET') {
    $where  = ['d.tenant_id = ?'];
    $params = [$tid];
    $types  = 'i';

    if (!empty($_GET['search'])) {
        $s       = '%' . $_GET['search'] . '%';
        $where[] = '(d.name LIKE ? OR d.mobile LIKE ? OR d.email LIKE ?)';
        $params  = array_merge($params, [$s, $s, $s]);
        $types  .= 'sss';
    }
    if (!empty($_GET['status'])) {
        $where[] = 'd.status = ?';
        $params[] = $_GET['status'];
        $types   .= 's';
    }

    $sql  = 'SELECT d.* FROM drivers d WHERE ' . implode(' AND ', $where) . ' ORDER BY d.name';
    $stmt = $conn->prepare($sql);
    if ($params) $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    // Attach assigned vehicles
    $driverIds  = array_column($rows, 'id');
    $vehicleMap = [];
    if ($driverIds) {
        $in   = implode(',', array_fill(0, count($driverIds), '?'));
        $stmt = $conn->prepare(
            "SELECT dv.driver_id, v.id, v.brand, v.model, v.registration_number
             FROM driver_vehicles dv
             JOIN vehicles v ON v.id = dv.vehicle_id
             WHERE dv.driver_id IN ($in)"
        );
        $t = str_repeat('i', count($driverIds));
        $stmt->bind_param($t, ...$driverIds);
        $stmt->execute();
        foreach ($stmt->get_result()->fetch_all(MYSQLI_ASSOC) as $vr) {
            $vehicleMap[(int)$vr['driver_id']][] = [
                'id'                  => (int)$vr['id'],
                'brand'               => $vr['brand'],
                'model'               => $vr['model'],
                'registration_number' => $vr['registration_number'],
            ];
        }
        $stmt->close();
    }

    $drivers = array_map(function ($d) use ($vehicleMap) {
        unset($d['password']);
        $d['id']              = (int)$d['id'];
        $d['commission_rate'] = (float)$d['commission_rate'];
        $d['vehicles']        = $vehicleMap[$d['id']] ?? [];
        return $d;
    }, $rows);

    json_response(['success' => true, 'data' => $drivers]);
}

// ── POST: create driver ──────────────────────────────────────────
if ($method === 'POST') {
    $name            = trim($_POST['name'] ?? '');
    $mobile          = trim($_POST['mobile'] ?? '');
    $email           = trim($_POST['email'] ?? '');
    $password        = $_POST['password'] ?? '';
    $commission_rate = (float)($_POST['commission_rate'] ?? 0);
    $status          = in_array($_POST['status'] ?? '', ['active', 'inactive']) ? $_POST['status'] : 'active';
    $vehicle_ids     = json_decode($_POST['vehicle_ids'] ?? '[]', true) ?: [];

    if (!$name || !$mobile || !$email || !$password) {
        json_response(['success' => false, 'message' => 'নাম, মোবাইল, ইমেইল ও পাসওয়ার্ড আবশ্যক'], 400);
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        json_response(['success' => false, 'message' => 'সঠিক ইমেইল ঠিকানা দিন'], 400);
    }
    if (strlen($password) < 6) {
        json_response(['success' => false, 'message' => 'পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে'], 400);
    }

    $stmt = $conn->prepare("SELECT id FROM drivers WHERE email = ?");
    $stmt->bind_param('s', $email);
    $stmt->execute();
    if ($stmt->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
    }
    $stmt->close();

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
        move_uploaded_file($file['tmp_name'], $dir . $filename);
        $picture = 'uploads/drivers/' . $filename;
    }

    $hashed = password_hash($password, PASSWORD_BCRYPT);
    $stmt   = $conn->prepare(
        "INSERT INTO drivers (tenant_id, name, mobile, profile_picture, email, password, commission_rate, status)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    );
    $stmt->bind_param('isssssds', $tid, $name, $mobile, $picture, $email, $hashed, $commission_rate, $status);

    if (!$stmt->execute()) {
        json_response(['success' => false, 'message' => 'ড্রাইভার যোগ করতে ব্যর্থ: ' . $stmt->error], 500);
    }
    $driver_id = $conn->insert_id;
    $stmt->close();

    // Assign vehicles (শুধু এই tenant-এর গাড়ি অ্যাসাইন করা যাবে — IDOR প্রতিরোধ)
    if ($vehicle_ids) {
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
                $vstmt->bind_param('ii', $driver_id, $vid);
                $vstmt->execute();
            }
            $vstmt->close();
        }
    }

    json_response([
        'success' => true,
        'message' => 'ড্রাইভার সফলভাবে যোগ করা হয়েছে',
        'data'    => ['driver_id' => $driver_id],
    ], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
