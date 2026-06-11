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
            r.start_date as rental_start_date,
            r.pickup_location,
            r.dropoff_location,
            r.trip_type
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
        $row['paid_amount']             = (float)$row['paid_amount'];
        $row['remaining_amount']        = (float)$row['remaining_amount'];
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

    // হিসাব ও insert — shared helper (ট্রিপ completed হলে এখন স্বয়ংক্রিয়ভাবেই তৈরি হয়;
    // এটি পুরনো সম্পন্ন ট্রিপের জন্য fallback)
    $settlement = create_settlement_for_rental($conn, $rental_id);

    if ($settlement === null) {
        json_response(['success' => false, 'message' => 'সম্পন্ন রেন্টাল পাওয়া যায়নি'], 404);
    }

    if (!$settlement['created']) {
        json_response(['success' => false, 'message' => 'এই রেন্টালের জন্য সেটেলমেন্ট ইতিমধ্যে তৈরি হয়েছে'], 400);
    }

    json_response([
        'success' => true,
        'message' => 'সেটেলমেন্ট সফলভাবে তৈরি হয়েছে',
        'data' => ['settlement_id' => $settlement['id']]
    ], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
?>
