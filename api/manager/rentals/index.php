<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
$tid        = get_tenant_id();
$conn       = (new Database())->connect();
$method     = $_SERVER['REQUEST_METHOD'];

$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);
$t    = str_repeat('i', count($vids));

// ── GET: list rentals ────────────────────────────────────────────
if ($method === 'GET') {
    if (!$vids) {
        json_response(['success' => true, 'data' => []]);
    }

    $where  = ["r.vehicle_id IN $in", "r.tenant_id = ?"];
    $params = array_merge($vids, [$tid]);
    $types  = $t . 'i';

    if (!empty($_GET['status'])) {
        $where[] = 'r.rental_status = ?';
        $params[] = $_GET['status'];
        $types   .= 's';
    }

    if (!empty($_GET['search'])) {
        $s = '%' . $_GET['search'] . '%';
        $where[] = '(c.first_name LIKE ? OR c.last_name LIKE ? OR c.phone LIKE ?)';
        $params  = array_merge($params, [$s, $s, $s]);
        $types  .= 'sss';
    }

    $sql = "SELECT r.*,
            c.first_name as customer_first_name, c.last_name as customer_last_name, c.phone as customer_phone,
            v.brand as vehicle_brand, v.model as vehicle_model, v.registration_number as vehicle_registration_number,
            d.name as driver_name
            FROM rentals r
            LEFT JOIN customers c ON r.customer_id = c.id
            LEFT JOIN vehicles v ON r.vehicle_id = v.id
            LEFT JOIN drivers d ON r.driver_id = d.id
            WHERE " . implode(' AND ', $where) . "
            ORDER BY r.start_date DESC";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    foreach ($rows as &$row) {
        $row['id']            = (int)$row['id'];
        $row['customer_id']   = (int)$row['customer_id'];
        $row['vehicle_id']    = (int)$row['vehicle_id'];
        $row['employee_id']   = $row['employee_id'] ? (int)$row['employee_id'] : null;
        $row['driver_id']     = $row['driver_id'] ? (int)$row['driver_id'] : null;
        $row['agreed_amount'] = (float)$row['agreed_amount'];
        $row['total_days']    = $row['total_days'] ? (int)$row['total_days'] : null;
        $row['daily_rate']    = $row['daily_rate'] ? (float)$row['daily_rate'] : null;
        $row['subtotal']      = $row['subtotal'] ? (float)$row['subtotal'] : null;
        $row['discount']      = $row['discount'] ? (float)$row['discount'] : null;
        $row['tax']           = $row['tax'] ? (float)$row['tax'] : null;
        $row['total_amount']  = $row['total_amount'] ? (float)$row['total_amount'] : null;
    }

    json_response(['success' => true, 'data' => $rows]);
}

// ── POST: create rental ──────────────────────────────────────────
if ($method === 'POST') {
    $data = input();

    $passenger_name   = trim($data['passenger_name'] ?? '');
    $passenger_mobile = trim($data['passenger_mobile'] ?? '');
    $vehicle_id       = isset($data['vehicle_id']) ? (int)$data['vehicle_id'] : 0;
    $driver_id        = isset($data['driver_id']) ? (int)$data['driver_id'] : null;
    $pickup_location  = trim($data['pickup_location'] ?? '');
    $dropoff_location = trim($data['dropoff_location'] ?? '');
    $trip_type        = in_array($data['trip_type'] ?? '', ['one_way', 'round_trip']) ? $data['trip_type'] : 'one_way';
    $start_datetime   = $data['start_datetime'] ?? '';
    $agreed_amount    = isset($data['agreed_amount']) ? (float)$data['agreed_amount'] : 0;
    $notes            = trim($data['notes'] ?? '');

    if (!$passenger_name || !$passenger_mobile || !$vehicle_id || !$start_datetime) {
        json_response(['success' => false, 'message' => 'যাত্রির নাম, মোবাইল, গাড়ি এবং শুরুর তারিখ আবশ্যক'], 400);
    }

    // Manager can only use their assigned vehicles
    if (!$vids || !in_array($vehicle_id, $vids)) {
        json_response(['success' => false, 'message' => 'এই গাড়ি আপনার অধীনে নেই'], 403);
    }

    if (!preg_match('/^\d{10,15}$/', preg_replace('/[^\d]/', '', $passenger_mobile))) {
        json_response(['success' => false, 'message' => 'সঠিক মোবাইল নম্বর দিন'], 400);
    }

    // Auto-assign driver from vehicle if not given
    if (!$driver_id) {
        $astmt = $conn->prepare(
            "SELECT dv.driver_id FROM driver_vehicles dv
             JOIN drivers d ON d.id = dv.driver_id AND d.status = 'active'
             WHERE dv.vehicle_id = ? ORDER BY dv.assigned_at DESC LIMIT 1"
        );
        $astmt->bind_param('i', $vehicle_id);
        $astmt->execute();
        $arow = $astmt->get_result()->fetch_assoc();
        $astmt->close();
        if ($arow) $driver_id = (int)$arow['driver_id'];
    }

    // Find or create customer
    $phone_clean = preg_replace('/[^\d]/', '', $passenger_mobile);
    $cstmt = $conn->prepare("SELECT id FROM customers WHERE phone = ? AND tenant_id = ?");
    $cstmt->bind_param('si', $phone_clean, $tid);
    $cstmt->execute();
    $cresult = $cstmt->get_result();

    if ($cresult->num_rows > 0) {
        $customer_id = (int)$cresult->fetch_assoc()['id'];
    } else {
        $names       = explode(' ', $passenger_name, 2);
        $first_name  = $names[0];
        $last_name   = $names[1] ?? '';
        $istmt = $conn->prepare("INSERT INTO customers (tenant_id, first_name, last_name, phone, email) VALUES (?, ?, ?, ?, NULL)");
        $istmt->bind_param('isss', $tid, $first_name, $last_name, $phone_clean);
        if (!$istmt->execute()) {
            json_response(['success' => false, 'message' => 'যাত্রি তৈরি করতে ব্যর্থ'], 500);
        }
        $customer_id = $conn->insert_id;
        $istmt->close();
    }
    $cstmt->close();

    $rental_status  = 'pending';
    $payment_status = 'pending';
    $employee_id    = null;

    $rstmt = $conn->prepare(
        "INSERT INTO rentals
         (tenant_id, customer_id, vehicle_id, employee_id, driver_id, start_date,
          pickup_location, dropoff_location, trip_type, agreed_amount, rental_status, payment_status, notes)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    );
    $rstmt->bind_param(
        'iiiiissssdsss',
        $tid, $customer_id, $vehicle_id, $employee_id, $driver_id, $start_datetime,
        $pickup_location, $dropoff_location, $trip_type, $agreed_amount,
        $rental_status, $payment_status, $notes
    );

    if (!$rstmt->execute()) {
        json_response(['success' => false, 'message' => 'রেন্টাল তৈরি করতে ব্যর্থ: ' . $rstmt->error], 500);
    }

    $rental_id = $conn->insert_id;
    $rstmt->close();

    json_response(['success' => true, 'message' => 'রেন্টাল সফলভাবে তৈরি হয়েছে', 'data' => ['rental_id' => $rental_id]], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
