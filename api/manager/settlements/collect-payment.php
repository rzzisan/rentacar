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

$payment_amount = isset($data['amount']) ? round((float)$data['amount'], 2) : 0;
$payment_method = $data['payment_method'] ?? null;
$payment_notes  = $data['payment_notes'] ?? null;

if ($payment_amount <= 0) {
    json_response(['success' => false, 'message' => 'পেমেন্ট পরিমান ০ থেকে বেশি হতে হবে'], 400);
}

$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

$stmt = $conn->prepare(
    "SELECT s.id, s.driver_id, s.amount_to_collect, s.paid_amount, s.remaining_amount, s.payment_status
     FROM settlements s
     JOIN rentals r ON s.rental_id = r.id
     WHERE s.id = ? AND r.vehicle_id IN $in"
);
$params = array_merge([$settlement_id], $vids);
$types  = 'i' . str_repeat('i', count($vids));
$stmt->bind_param($types, ...$params);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট পাওয়া যায়নি'], 404);
}

$settlement = $result->fetch_assoc();
$stmt->close();

$remaining = round((float)$settlement['remaining_amount'], 2);
if ($payment_amount > $remaining + 0.009) {
    json_response(['success' => false, 'message' => 'পেমেন্ট পরিমান বকেয়া টাকার চেয়ে বেশি হতে পারে না (বকেয়া: ৳' . number_format($remaining, 2) . ')'], 400);
}

$new_paid_amount      = round((float)$settlement['paid_amount'] + $payment_amount, 2);
$new_remaining_amount = round((float)$settlement['amount_to_collect'] - $new_paid_amount, 2);
if ($new_remaining_amount < 0) $new_remaining_amount = 0.0;
$new_payment_status   = $new_remaining_amount <= 0.009 ? 'paid' : 'partial';

$conn->begin_transaction();

try {
    $update_stmt = $conn->prepare(
        "UPDATE settlements SET paid_amount = ?, remaining_amount = ?, payment_status = ?,
         paid_date = IF(? = 'paid', NOW(), paid_date), updated_at = NOW() WHERE id = ?"
    );
    $update_stmt->bind_param('ddssi', $new_paid_amount, $new_remaining_amount, $new_payment_status, $new_payment_status, $settlement_id);
    if (!$update_stmt->execute()) {
        throw new Exception('সেটেলমেন্ট আপডেট ব্যর্থ: ' . $update_stmt->error);
    }
    $update_stmt->close();

    $user_id   = null;
    $driver_id = $settlement['driver_id'] ? (int)$settlement['driver_id'] : null;

    $log_stmt = $conn->prepare(
        "INSERT INTO settlement_payments (settlement_id, amount, payment_method, notes, recorded_by, paid_by_driver_id)
         VALUES (?, ?, ?, ?, ?, ?)"
    );
    $log_stmt->bind_param('idssii', $settlement_id, $payment_amount, $payment_method, $payment_notes, $user_id, $driver_id);
    if (!$log_stmt->execute()) {
        throw new Exception('পেমেন্ট লগ করতে ব্যর্থ: ' . $log_stmt->error);
    }
    $payment_log_id = $conn->insert_id;
    $log_stmt->close();

    $conn->commit();

    json_response([
        'success' => true,
        'message' => 'পেমেন্ট সফলভাবে সংগ্রহ করা হয়েছে',
        'data'    => [
            'settlement_id'    => $settlement_id,
            'payment_id'       => $payment_log_id,
            'paid_amount'      => $new_paid_amount,
            'remaining_amount' => $new_remaining_amount,
            'payment_status'   => $new_payment_status,
            'auto_paid'        => $new_remaining_amount <= 0,
        ]
    ], 201);

} catch (Exception $e) {
    $conn->rollback();
    json_response(['success' => false, 'message' => $e->getMessage()], 500);
}
