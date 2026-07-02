<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

require_auth();
$tid = get_tenant_id();

$conn = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

// GET  /api/vehicles/index.php  — list (admin/employee)
if ($method === 'GET') {
    $where  = ['tenant_id = ?'];
    $params = [$tid];
    $types  = 'i';

    if (!empty($_GET['status'])) {
        $where[] = 'status = ?';
        $params[] = $_GET['status'];
        $types .= 's';
    }
    if (!empty($_GET['vehicle_type'])) {
        $where[] = 'vehicle_type = ?';
        $params[] = $_GET['vehicle_type'];
        $types .= 's';
    }
    if (!empty($_GET['search'])) {
        $where[] = '(brand LIKE ? OR model LIKE ? OR registration_number LIKE ?)';
        $s = '%' . $_GET['search'] . '%';
        $params = array_merge($params, [$s, $s, $s]);
        $types .= 'sss';
    }

    $sql  = 'SELECT * FROM vehicles WHERE ' . implode(' AND ', $where) . ' ORDER BY brand, model';
    $stmt = $conn->prepare($sql);

    if ($params) {
        $stmt->bind_param($types, ...$params);
    }
    $stmt->execute();
    $result = $stmt->get_result();

    $vehicles = [];
    while ($row = $result->fetch_assoc()) {
        $vehicles[] = $row;
    }

    json_response(['success' => true, 'data' => $vehicles]);
}

// POST /api/vehicles/index.php  — create (admin only)
if ($method === 'POST') {
    require_role('admin');

    $b = input();
    $required = ['registration_number', 'brand', 'model', 'year', 'vehicle_type', 'daily_rent_price'];
    foreach ($required as $field) {
        if (empty($b[$field])) {
            json_response(['success' => false, 'message' => "$field প্রয়োজন"]);
        }
    }

    $stmt = $conn->prepare(
        'INSERT INTO vehicles
         (tenant_id, registration_number, brand, model, year, vehicle_type, color, fuel_type,
          seating_capacity, daily_rent_price, status)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
    );

    $reg   = trim($b['registration_number']);
    $brand = trim($b['brand']);
    $model = trim($b['model']);
    $year  = (int) $b['year'];
    $type  = trim($b['vehicle_type']);
    $color = trim($b['color'] ?? '');
    $fuel  = trim($b['fuel_type'] ?? 'petrol');
    $seats = (int) ($b['seating_capacity'] ?? 5);
    $price = (float) $b['daily_rent_price'];
    $status= trim($b['status'] ?? 'available');

    $stmt->bind_param('isssississs',
        $tid, $reg, $brand, $model, $year, $type, $color, $fuel, $seats, $price, $status
    );

    if ($stmt->execute()) {
        json_response([
            'success'    => true,
            'message'    => 'গাড়ি সফলভাবে যোগ করা হয়েছে',
            'vehicle_id' => $conn->insert_id,
        ], 201);
    }

    json_response(['success' => false, 'message' => 'গাড়ি যোগ করতে ব্যর্থ: ' . $stmt->error]);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
