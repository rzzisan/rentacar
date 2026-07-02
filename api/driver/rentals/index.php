<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$driver_id = require_driver();
$tid = get_tenant_id();

$conn   = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

// ── GET: নিজের ট্রিপের তালিকা ─────────────────────────────────────
if ($method === 'GET') {
    $where  = ['r.driver_id = ?', 'r.tenant_id = ?'];
    $params = [$driver_id, $tid];
    $types  = 'ii';

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

    if (!empty($_GET['date_from'])) {
        $where[] = 'DATE(r.start_date) >= ?';
        $params[] = $_GET['date_from'];
        $types   .= 's';
    }

    if (!empty($_GET['date_to'])) {
        $where[] = 'DATE(r.start_date) <= ?';
        $params[] = $_GET['date_to'];
        $types   .= 's';
    }

    $sql = "SELECT r.*,
            c.first_name as customer_first_name,
            c.last_name as customer_last_name,
            c.phone as customer_phone,
            v.brand as vehicle_brand,
            v.model as vehicle_model,
            v.registration_number as vehicle_registration_number,
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

    // Convert numeric fields
    foreach ($rows as &$row) {
        $row['id']              = (int)$row['id'];
        $row['customer_id']     = (int)$row['customer_id'];
        $row['vehicle_id']      = (int)$row['vehicle_id'];
        $row['employee_id']     = $row['employee_id'] ? (int)$row['employee_id'] : null;
        $row['driver_id']       = $row['driver_id'] ? (int)$row['driver_id'] : null;
        $row['agreed_amount']   = (float)$row['agreed_amount'];
        $row['total_days']      = $row['total_days'] ? (int)$row['total_days'] : null;
        $row['daily_rate']      = $row['daily_rate'] ? (float)$row['daily_rate'] : null;
        $row['subtotal']        = $row['subtotal'] ? (float)$row['subtotal'] : null;
        $row['discount']        = $row['discount'] ? (float)$row['discount'] : null;
        $row['tax']             = $row['tax'] ? (float)$row['tax'] : null;
        $row['total_amount']    = $row['total_amount'] ? (float)$row['total_amount'] : null;
    }

    json_response(['success' => true, 'data' => $rows]);
}

// ── POST: নিজের অ্যাসাইন করা গাড়ির জন্য ট্রিপ তৈরি ─────────────────
if ($method === 'POST') {
    $data = input();

    $passenger_name   = trim($data['passenger_name'] ?? '');
    $passenger_mobile = trim($data['passenger_mobile'] ?? '');
    $vehicle_id       = isset($data['vehicle_id']) ? (int)$data['vehicle_id'] : 0;
    $pickup_location  = trim($data['pickup_location'] ?? '');
    $dropoff_location = trim($data['dropoff_location'] ?? '');
    $trip_type        = in_array($data['trip_type'] ?? '', ['one_way', 'round_trip'])
                        ? $data['trip_type']
                        : 'one_way';
    $start_datetime   = $data['start_datetime'] ?? '';
    $agreed_amount    = isset($data['agreed_amount']) ? (float)$data['agreed_amount'] : 0;
    $notes            = trim($data['notes'] ?? '');

    if (!$passenger_name || !$passenger_mobile || !$vehicle_id || !$start_datetime) {
        json_response([
            'success' => false,
            'message' => 'যাত্রির নাম, মোবাইল, গাড়ি এবং শুরুর তারিখ আবশ্যক'
        ], 400);
    }

    // Validate phone format (basic)
    if (!preg_match('/^\d{10,15}$/', preg_replace('/[^\d]/', '', $passenger_mobile))) {
        json_response(['success' => false, 'message' => 'সঠিক মোবাইল নম্বর দিন'], 400);
    }

    // গাড়িটি অবশ্যই এই ড্রাইভারকে অ্যাসাইন করা থাকতে হবে
    $vstmt = $conn->prepare(
        "SELECT v.id FROM driver_vehicles dv
         JOIN vehicles v ON v.id = dv.vehicle_id
         WHERE dv.driver_id = ? AND dv.vehicle_id = ? AND v.tenant_id = ?"
    );
    $vstmt->bind_param('iii', $driver_id, $vehicle_id, $tid);
    $vstmt->execute();
    if ($vstmt->get_result()->num_rows === 0) {
        json_response(['success' => false, 'message' => 'এই গাড়িটি আপনাকে অ্যাসাইন করা নয়'], 403);
    }
    $vstmt->close();

    // Find or create customer
    $phone_clean = preg_replace('/[^\d]/', '', $passenger_mobile);
    $cstmt = $conn->prepare("SELECT id FROM customers WHERE phone = ? AND tenant_id = ?");
    $cstmt->bind_param('si', $phone_clean, $tid);
    $cstmt->execute();
    $cresult = $cstmt->get_result();

    if ($cresult->num_rows > 0) {
        $customer_id = (int)$cresult->fetch_assoc()['id'];
    } else {
        $names = explode(' ', $passenger_name, 2);
        $first_name = $names[0];
        $last_name = $names[1] ?? '';

        $istmt = $conn->prepare(
            "INSERT INTO customers (tenant_id, first_name, last_name, phone, email)
             VALUES (?, ?, ?, ?, NULL)"
        );
        $istmt->bind_param('isss', $tid, $first_name, $last_name, $phone_clean);

        if (!$istmt->execute()) {
            json_response(['success' => false, 'message' => 'যাত্রি তৈরি করতে ব্যর্থ: ' . $istmt->error], 500);
        }
        $customer_id = $conn->insert_id;
        $istmt->close();
    }
    $cstmt->close();

    // Insert rental — ড্রাইভার নিজেই trip-এর চালক
    $rental_status  = 'pending';
    $payment_status = 'pending';

    $rstmt = $conn->prepare(
        "INSERT INTO rentals
         (tenant_id, customer_id, vehicle_id, driver_id, start_date,
          pickup_location, dropoff_location, trip_type, agreed_amount, rental_status,
          payment_status, notes)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    );

    $rstmt->bind_param(
        'iiiissssdsss',
        $tid,
        $customer_id,
        $vehicle_id,
        $driver_id,
        $start_datetime,
        $pickup_location,
        $dropoff_location,
        $trip_type,
        $agreed_amount,
        $rental_status,
        $payment_status,
        $notes
    );

    if (!$rstmt->execute()) {
        json_response(['success' => false, 'message' => 'ট্রিপ তৈরি করতে ব্যর্থ: ' . $rstmt->error], 500);
    }

    $rental_id = $conn->insert_id;
    $rstmt->close();

    json_response([
        'success' => true,
        'message' => 'ট্রিপ সফলভাবে তৈরি হয়েছে',
        'data' => ['rental_id' => $rental_id]
    ], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
?>
