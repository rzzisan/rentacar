<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');

$conn = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

// ── GET: list settlements ────────────────────────────────────────
if ($method === 'GET') {
    $where  = ['1=1'];
    $params = [];
    $types  = '';

    if (!empty($_GET['status'])) {
        $where[] = 's.payment_status = ?';
        $params[] = $_GET['status'];
        $types   .= 's';
    }

    if (!empty($_GET['search'])) {
        $s = '%' . $_GET['search'] . '%';
        $where[] = '(c.first_name LIKE ? OR c.last_name LIKE ? OR c.phone LIKE ?)';
        $params  = array_merge($params, [$s, $s, $s]);
        $types  .= 'sss';
    }

    $sql = "SELECT s.*,
            c.first_name as customer_first_name,
            c.last_name as customer_last_name,
            c.phone as customer_phone,
            v.brand as vehicle_brand,
            v.model as vehicle_model,
            v.registration_number as vehicle_registration_number,
            d.name as driver_name,
            d.commission_rate as driver_commission_rate,
            r.start_date as rental_start_date
            FROM settlements s
            LEFT JOIN rentals r ON s.rental_id = r.id
            LEFT JOIN customers c ON r.customer_id = c.id
            LEFT JOIN vehicles v ON r.vehicle_id = v.id
            LEFT JOIN drivers d ON s.driver_id = d.id
            WHERE " . implode(' AND ', $where) . "
            ORDER BY s.created_at DESC";

    $stmt = $conn->prepare($sql);
    if ($params) {
        $stmt->bind_param($types, ...$params);
    }
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    // Convert numeric fields
    foreach ($rows as &$row) {
        $row['id']                      = (int)$row['id'];
        $row['rental_id']               = (int)$row['rental_id'];
        $row['driver_id']               = $row['driver_id'] ? (int)$row['driver_id'] : null;
        $row['agreed_amount']           = (float)$row['agreed_amount'];
        $row['total_expenses']          = (float)$row['total_expenses'];
        $row['net_amount']              = (float)$row['net_amount'];
        $row['commission_percent']      = (float)$row['commission_percent'];
        $row['driver_commission']       = (float)$row['driver_commission'];
        $row['amount_to_collect']       = (float)$row['amount_to_collect'];
    }

    json_response(['success' => true, 'data' => $rows]);
}

// ── POST: create settlement from completed rental ───────────────────
if ($method === 'POST') {
    $data = input();
    $rental_id = isset($data['rental_id']) ? (int)$data['rental_id'] : 0;

    if (!$rental_id) {
        json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
    }

    // Verify rental exists and is completed
    $rstmt = $conn->prepare(
        "SELECT r.*, d.commission_rate FROM rentals r
         LEFT JOIN drivers d ON r.driver_id = d.id
         WHERE r.id = ? AND r.rental_status = 'completed'"
    );
    $rstmt->bind_param('i', $rental_id);
    $rstmt->execute();
    $rental_result = $rstmt->get_result();

    if ($rental_result->num_rows === 0) {
        json_response(['success' => false, 'message' => 'সম্পন্ন রেন্টাল পাওয়া যায়নি'], 404);
    }

    $rental = $rental_result->fetch_assoc();
    $rstmt->close();

    // Check if settlement already exists
    $cstmt = $conn->prepare("SELECT id FROM settlements WHERE rental_id = ?");
    $cstmt->bind_param('i', $rental_id);
    $cstmt->execute();
    if ($cstmt->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই রেন্টালের জন্য সেটেলমেন্ট ইতিমধ্যে তৈরি হয়েছে'], 400);
    }
    $cstmt->close();

    // Get total expenses
    $estmt = $conn->prepare("SELECT SUM(amount) as total FROM trip_expenses WHERE rental_id = ?");
    $estmt->bind_param('i', $rental_id);
    $estmt->execute();
    $expense_row = $estmt->get_result()->fetch_assoc();
    $total_expenses = $expense_row['total'] ? (float)$expense_row['total'] : 0;
    $estmt->close();

    // Calculate amounts
    $agreed_amount = (float)$rental['agreed_amount'];
    $net_amount = $agreed_amount - $total_expenses;
    $commission_percent = $rental['commission_rate'] ? (float)$rental['commission_rate'] : 0;
    $driver_commission = ($net_amount > 0) ? ($net_amount * $commission_percent / 100) : 0;
    $amount_to_collect = $net_amount - $driver_commission;

    // Insert settlement
    $istmt = $conn->prepare(
        "INSERT INTO settlements
         (rental_id, driver_id, agreed_amount, total_expenses, net_amount,
          commission_percent, driver_commission, amount_to_collect, payment_status)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending')"
    );

    $istmt->bind_param(
        'iiddddds',
        $rental_id,
        $rental['driver_id'],
        $agreed_amount,
        $total_expenses,
        $net_amount,
        $commission_percent,
        $driver_commission,
        $amount_to_collect
    );

    if (!$istmt->execute()) {
        json_response(['success' => false, 'message' => 'সেটেলমেন্ট তৈরি করতে ব্যর্থ: ' . $istmt->error], 500);
    }

    $settlement_id = $conn->insert_id;
    $istmt->close();

    json_response([
        'success' => true,
        'message' => 'সেটেলমেন্ট সফলভাবে তৈরি হয়েছে',
        'data' => ['settlement_id' => $settlement_id]
    ], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
?>
