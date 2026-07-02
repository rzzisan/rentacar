<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
$tid        = get_tenant_id();
$conn       = (new Database())->connect();
$method     = $_SERVER['REQUEST_METHOD'];

$vids = get_manager_vehicle_ids($conn, $manager_id);

if ($method === 'GET') {
    if (!$vids) {
        json_response(['success' => true, 'data' => []]);
    }

    $in = manager_vehicle_in_clause($vids);
    $t  = str_repeat('i', count($vids));

    // Drivers assigned to manager's vehicles (via driver_vehicles)
    $where  = ["dv.vehicle_id IN $in", "d.tenant_id = ?"];
    // $in একই কোয়েরিতে ৪ বার ব্যবহৃত হয় (৩টি সাবকোয়েরি + মূল WHERE) —
    // bind_param-এ params SQL টেক্সটে placeholder-এর ক্রম অনুযায়ী দিতে হবে
    $params = array_merge($vids, $vids, $vids, $vids, [$tid]);
    $types  = $t . $t . $t . $t . 'i';

    $extra_where  = [];
    $extra_params = [];
    $extra_types  = '';
    if (!empty($_GET['search'])) {
        $s              = '%' . $_GET['search'] . '%';
        $extra_where[]  = '(d.name LIKE ? OR d.mobile LIKE ? OR d.email LIKE ?)';
        $extra_params   = array_merge($extra_params, [$s, $s, $s]);
        $extra_types   .= 'sss';
    }
    if (!empty($_GET['status'])) {
        $extra_where[]  = 'd.status = ?';
        $extra_params[] = $_GET['status'];
        $extra_types   .= 's';
    }
    $where  = array_merge($where, $extra_where);
    $params = array_merge($params, $extra_params);
    $types .= $extra_types;

    $sql = "SELECT DISTINCT d.*,
            (SELECT COUNT(*) FROM rentals r WHERE r.driver_id = d.id AND r.vehicle_id IN $in AND r.rental_status = 'completed') as total_trips,
            (SELECT COUNT(*) FROM rentals r WHERE r.driver_id = d.id AND r.vehicle_id IN $in AND r.rental_status = 'completed'
               AND MONTH(r.start_date) = MONTH(CURDATE()) AND YEAR(r.start_date) = YEAR(CURDATE())) as this_month_trips,
            (SELECT COALESCE(SUM(s.remaining_amount), 0) FROM settlements s JOIN rentals r ON s.rental_id = r.id WHERE s.driver_id = d.id AND r.vehicle_id IN $in AND s.payment_status != 'paid') as total_due
            FROM drivers d
            JOIN driver_vehicles dv ON d.id = dv.driver_id
            WHERE " . implode(' AND ', $where) . " ORDER BY d.name";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    // Attach vehicles for each driver
    $driverIds  = array_column($rows, 'id');
    $vehicleMap = [];
    if ($driverIds) {
        $din   = '(' . implode(',', array_fill(0, count($driverIds), '?')) . ')';
        $vstmt = $conn->prepare(
            "SELECT dv.driver_id, v.id, v.brand, v.model, v.registration_number
             FROM driver_vehicles dv
             JOIN vehicles v ON v.id = dv.vehicle_id
             WHERE dv.driver_id IN $din"
        );
        $dt = str_repeat('i', count($driverIds));
        $vstmt->bind_param($dt, ...$driverIds);
        $vstmt->execute();
        foreach ($vstmt->get_result()->fetch_all(MYSQLI_ASSOC) as $vr) {
            $vehicleMap[(int)$vr['driver_id']][] = [
                'id' => (int)$vr['id'], 'brand' => $vr['brand'],
                'model' => $vr['model'], 'registration_number' => $vr['registration_number'],
            ];
        }
        $vstmt->close();
    }

    $drivers = array_map(function ($d) use ($vehicleMap) {
        unset($d['password']);
        $d['id']               = (int)$d['id'];
        $d['commission_rate']  = (float)$d['commission_rate'];
        $d['total_trips']      = (int)($d['total_trips'] ?? 0);
        $d['this_month_trips'] = (int)($d['this_month_trips'] ?? 0);
        $d['total_due']        = (float)($d['total_due'] ?? 0);
        $d['vehicles']         = $vehicleMap[$d['id']] ?? [];
        return $d;
    }, $rows);

    json_response(['success' => true, 'data' => $drivers]);
}

// ── POST: নতুন ড্রাইভার তৈরি (শুধু manager-এর নিজের assigned গাড়িতে অ্যাসাইন করা যাবে) ─
if ($method === 'POST') {
    $name            = trim($_POST['name'] ?? '');
    $mobile          = trim($_POST['mobile'] ?? '');
    $email           = trim($_POST['email'] ?? '');
    $password        = $_POST['password'] ?? '';
    $commission_rate = (float) ($_POST['commission_rate'] ?? 0);
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

    $echk = $conn->prepare('SELECT id FROM drivers WHERE email = ?');
    $echk->bind_param('s', $email);
    $echk->execute();
    if ($echk->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
    }
    $echk->close();

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

    // শুধু manager-এর নিজের assigned গাড়িতেই অ্যাসাইন করা যাবে (IDOR প্রতিরোধ — tenant-এর অন্য গাড়ি না)
    if ($vehicle_ids) {
        $allowedVehicleIds = array_intersect(array_map('intval', $vehicle_ids), $vids);
        if ($allowedVehicleIds) {
            $vstmt = $conn->prepare("INSERT IGNORE INTO driver_vehicles (driver_id, vehicle_id) VALUES (?, ?)");
            foreach ($allowedVehicleIds as $vid) {
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
