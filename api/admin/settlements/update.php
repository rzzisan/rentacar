<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('POST');

$conn = (new Database())->connect();
$data = input();
$settlement_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$settlement_id) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট আইডি প্রয়োজন'], 400);
}

// Verify settlement exists
$stmt = $conn->prepare("SELECT id, rental_id FROM settlements WHERE id = ?");
$stmt->bind_param('i', $settlement_id);
$stmt->execute();
if ($stmt->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'সেটেলমেন্ট পাওয়া যায়নি'], 404);
}
$settlement = $stmt->get_result()->fetch_assoc();
$stmt->close();

$rental_id = $settlement['rental_id'];

// Handle agreed_amount update
if (isset($data['agreed_amount'])) {
    $agreed_amount = (float)$data['agreed_amount'];

    // Get total expenses
    $estmt = $conn->prepare("SELECT SUM(amount) as total FROM trip_expenses WHERE rental_id = ?");
    $estmt->bind_param('i', $rental_id);
    $estmt->execute();
    $expense_row = $estmt->get_result()->fetch_assoc();
    $total_expenses = $expense_row['total'] ? (float)$expense_row['total'] : 0;
    $estmt->close();

    // Get commission percent
    $sstmt = $conn->prepare(
        "SELECT s.commission_percent FROM settlements s WHERE s.id = ?"
    );
    $sstmt->bind_param('i', $settlement_id);
    $sstmt->execute();
    $s_row = $sstmt->get_result()->fetch_assoc();
    $commission_percent = (float)$s_row['commission_percent'];
    $sstmt->close();

    // Recalculate
    $net_amount = $agreed_amount - $total_expenses;
    $driver_commission = ($net_amount > 0) ? ($net_amount * $commission_percent / 100) : 0;
    $amount_to_collect = $net_amount - $driver_commission;

    // Update settlement
    $ustmt = $conn->prepare(
        "UPDATE settlements
         SET agreed_amount = ?, net_amount = ?, driver_commission = ?, amount_to_collect = ?
         WHERE id = ?"
    );
    $ustmt->bind_param('ddddi', $agreed_amount, $net_amount, $driver_commission, $amount_to_collect, $settlement_id);
    if (!$ustmt->execute()) {
        json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $ustmt->error], 500);
    }
    $ustmt->close();
}

// Handle payment update
if (isset($data['payment_status'])) {
    $valid_statuses = ['pending', 'paid', 'partial', 'refunded'];
    if (!in_array($data['payment_status'], $valid_statuses)) {
        json_response(['success' => false, 'message' => 'অবৈধ পেমেন্ট স্ট্যাটাস'], 400);
    }

    $payment_status = $data['payment_status'];
    $payment_method = $data['payment_method'] ?? null;
    $payment_notes = $data['payment_notes'] ?? null;
    $paid_date = ($payment_status === 'paid' || $payment_status === 'partial') ? date('Y-m-d H:i:s') : null;

    $pstmt = $conn->prepare(
        "UPDATE settlements
         SET payment_status = ?, payment_method = ?, payment_notes = ?, paid_date = ?
         WHERE id = ?"
    );
    $pstmt->bind_param('ssssi', $payment_status, $payment_method, $payment_notes, $paid_date, $settlement_id);
    if (!$pstmt->execute()) {
        json_response(['success' => false, 'message' => 'পেমেন্ট আপডেট ব্যর্থ: ' . $pstmt->error], 500);
    }
    $pstmt->close();
}

json_response([
    'success' => true,
    'message' => 'সেটেলমেন্ট সফলভাবে আপডেট হয়েছে'
], 200);
?>
