<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id    = require_manager();
only_method('POST');
$conn          = (new Database())->connect();
$settlement_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$data          = input();

if (!$settlement_id) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট আইডি প্রয়োজন'], 400);
}

$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

$stmt = $conn->prepare(
    "SELECT s.id, s.rental_id FROM settlements s
     JOIN rentals r ON s.rental_id = r.id
     WHERE s.id = ? AND r.vehicle_id IN $in"
);
$params = array_merge([$settlement_id], $vids);
$types  = 'i' . str_repeat('i', count($vids));
$stmt->bind_param($types, ...$params);
$stmt->execute();
$settlement = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$settlement) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট পাওয়া যায়নি'], 404);
}

$rental_id = $settlement['rental_id'];

if (isset($data['agreed_amount'])) {
    $agreed_amount = (float)$data['agreed_amount'];

    $estmt = $conn->prepare("SELECT SUM(amount) as total FROM trip_expenses WHERE rental_id = ?");
    $estmt->bind_param('i', $rental_id);
    $estmt->execute();
    $total_expenses = (float)($estmt->get_result()->fetch_assoc()['total'] ?? 0);
    $estmt->close();

    $sstmt = $conn->prepare("SELECT commission_percent FROM settlements WHERE id = ?");
    $sstmt->bind_param('i', $settlement_id);
    $sstmt->execute();
    $commission_percent = (float)($sstmt->get_result()->fetch_assoc()['commission_percent'] ?? 0);
    $sstmt->close();

    $net_amount        = $agreed_amount - $total_expenses;
    $driver_commission = ($net_amount > 0) ? ($net_amount * $commission_percent / 100) : 0;
    $amount_to_collect = round($net_amount - $driver_commission, 2);

    $ustmt = $conn->prepare(
        "UPDATE settlements
         SET agreed_amount = ?, net_amount = ?, driver_commission = ?, amount_to_collect = ?,
             remaining_amount = GREATEST(? - paid_amount, 0),
             payment_status = CASE
                 WHEN payment_status = 'refunded' THEN payment_status
                 WHEN ? - paid_amount <= 0.009 THEN 'paid'
                 WHEN paid_amount > 0 THEN 'partial'
                 ELSE 'pending'
             END
         WHERE id = ?"
    );
    $ustmt->bind_param('ddddddi', $agreed_amount, $net_amount, $driver_commission, $amount_to_collect, $amount_to_collect, $amount_to_collect, $settlement_id);
    if (!$ustmt->execute()) {
        json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $ustmt->error], 500);
    }
    $ustmt->close();

    $rstmt = $conn->prepare("UPDATE rentals SET agreed_amount = ? WHERE id = ?");
    $rstmt->bind_param('di', $agreed_amount, $rental_id);
    $rstmt->execute();
    $rstmt->close();
}

if (isset($data['payment_status'])) {
    $valid_statuses = ['pending', 'paid', 'partial', 'refunded'];
    if (!in_array($data['payment_status'], $valid_statuses)) {
        json_response(['success' => false, 'message' => 'অবৈধ পেমেন্ট স্ট্যাটাস'], 400);
    }
    $payment_status = $data['payment_status'];
    $payment_method = $data['payment_method'] ?? null;
    $payment_notes  = $data['payment_notes'] ?? null;
    $paid_date      = in_array($payment_status, ['paid', 'partial']) ? date('Y-m-d H:i:s') : null;

    $pstmt = $conn->prepare(
        "UPDATE settlements
         SET payment_status = ?, payment_method = ?, payment_notes = ?, paid_date = ?,
             paid_amount = IF(? = 'paid', amount_to_collect, paid_amount),
             remaining_amount = IF(? = 'paid', 0, GREATEST(amount_to_collect - paid_amount, 0))
         WHERE id = ?"
    );
    $pstmt->bind_param('ssssssi', $payment_status, $payment_method, $payment_notes, $paid_date, $payment_status, $payment_status, $settlement_id);
    if (!$pstmt->execute()) {
        json_response(['success' => false, 'message' => 'পেমেন্ট আপডেট ব্যর্থ: ' . $pstmt->error], 500);
    }
    $pstmt->close();
}

json_response(['success' => true, 'message' => 'সেটেলমেন্ট সফলভাবে আপডেট হয়েছে']);
