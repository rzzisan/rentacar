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

if ($method === 'GET') {
    if (!$vids) {
        json_response(['success' => true, 'data' => []]);
    }

    $where  = ["r.vehicle_id IN $in", "r.tenant_id = ?"];
    $params = array_merge($vids, [$tid]);
    $types  = $t . 'i';

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
            c.first_name as customer_first_name, c.last_name as customer_last_name, c.phone as customer_phone,
            v.brand as vehicle_brand, v.model as vehicle_model, v.registration_number as vehicle_registration_number,
            d.name as driver_name, d.commission_rate as driver_commission_rate,
            r.start_date as rental_start_date, r.pickup_location, r.dropoff_location, r.trip_type
            FROM settlements s
            LEFT JOIN rentals r ON s.rental_id = r.id
            LEFT JOIN customers c ON r.customer_id = c.id
            LEFT JOIN vehicles v ON r.vehicle_id = v.id
            LEFT JOIN drivers d ON s.driver_id = d.id
            WHERE " . implode(' AND ', $where) . "
            ORDER BY s.created_at DESC";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    foreach ($rows as &$row) {
        $row['id']                 = (int)$row['id'];
        $row['rental_id']          = (int)$row['rental_id'];
        $row['driver_id']          = $row['driver_id'] ? (int)$row['driver_id'] : null;
        $row['agreed_amount']      = (float)$row['agreed_amount'];
        $row['total_expenses']     = (float)$row['total_expenses'];
        $row['net_amount']         = (float)$row['net_amount'];
        $row['commission_percent'] = (float)$row['commission_percent'];
        $row['driver_commission']  = (float)$row['driver_commission'];
        $row['amount_to_collect']  = (float)$row['amount_to_collect'];
        $row['paid_amount']        = (float)$row['paid_amount'];
        $row['remaining_amount']   = (float)$row['remaining_amount'];
    }

    json_response(['success' => true, 'data' => $rows]);
}

if ($method === 'POST') {
    $data      = input();
    $rental_id = isset($data['rental_id']) ? (int)$data['rental_id'] : 0;

    if (!$rental_id) {
        json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
    }

    // Verify rental belongs to manager's vehicles
    if ($vids) {
        $vchk = $conn->prepare("SELECT id FROM rentals WHERE id = ? AND vehicle_id IN $in AND tenant_id = ?");
        $p = array_merge([$rental_id], $vids, [$tid]);
        $tp = 'i' . $t . 'i';
        $vchk->bind_param($tp, ...$p);
        $vchk->execute();
        if ($vchk->get_result()->num_rows === 0) {
            json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি'], 404);
        }
        $vchk->close();
    }

    $settlement = create_settlement_for_rental($conn, $rental_id);
    if ($settlement === null) {
        json_response(['success' => false, 'message' => 'সম্পন্ন রেন্টাল পাওয়া যায়নি'], 404);
    }
    if (!$settlement['created']) {
        json_response(['success' => false, 'message' => 'এই রেন্টালের জন্য সেটেলমেন্ট ইতিমধ্যে তৈরি হয়েছে'], 400);
    }

    json_response(['success' => true, 'message' => 'সেটেলমেন্ট সফলভাবে তৈরি হয়েছে', 'data' => ['settlement_id' => $settlement['id']]], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
