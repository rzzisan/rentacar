<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('POST');

$conn = (new Database())->connect();
$data = input();
$driver_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$driver_id) {
    json_response(['success' => false, 'message' => 'ড্রাইভার আইডি প্রয়োজন'], 400);
}

$amount = isset($data['amount']) ? round((float)$data['amount'], 2) : 0;
$payment_method = !empty($data['payment_method']) ? $data['payment_method'] : null;
$notes = !empty($data['notes']) ? $data['notes'] : null;
// Optional: target a single settlement instead of FIFO allocation
$target_settlement_id = isset($data['settlement_id']) ? (int)$data['settlement_id'] : 0;

if ($amount <= 0) {
    json_response(['success' => false, 'message' => 'জমার পরিমান ০ থেকে বেশি হতে হবে'], 400);
}

// Verify driver exists
$dstmt = $conn->prepare("SELECT id, name FROM drivers WHERE id = ?");
$dstmt->bind_param('i', $driver_id);
$dstmt->execute();
$driver = $dstmt->get_result()->fetch_assoc();
$dstmt->close();

if (!$driver) {
    json_response(['success' => false, 'message' => 'ড্রাইভার পাওয়া যায়নি'], 404);
}

$user_id = $_SESSION['user_id'] ?? null;

$conn->begin_transaction();

try {
    // Lock this driver's outstanding settlements (oldest trip first)
    if ($target_settlement_id) {
        $sstmt = $conn->prepare(
            "SELECT s.id, s.amount_to_collect, s.paid_amount, s.remaining_amount
             FROM settlements s
             WHERE s.id = ? AND s.driver_id = ? AND s.remaining_amount > 0
               AND s.payment_status IN ('pending', 'partial')
             FOR UPDATE"
        );
        $sstmt->bind_param('ii', $target_settlement_id, $driver_id);
    } else {
        $sstmt = $conn->prepare(
            "SELECT s.id, s.amount_to_collect, s.paid_amount, s.remaining_amount
             FROM settlements s
             LEFT JOIN rentals r ON s.rental_id = r.id
             WHERE s.driver_id = ? AND s.remaining_amount > 0
               AND s.payment_status IN ('pending', 'partial')
             ORDER BY COALESCE(r.start_date, s.created_at) ASC, s.id ASC
             FOR UPDATE"
        );
        $sstmt->bind_param('i', $driver_id);
    }
    $sstmt->execute();
    $settlements = $sstmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $sstmt->close();

    if (count($settlements) === 0) {
        $conn->rollback();
        json_response(['success' => false, 'message' => 'এই ড্রাইভারের কোনো বকেয়া সেটেলমেন্ট নেই'], 400);
    }

    $total_due = 0;
    foreach ($settlements as $s) {
        $total_due += (float)$s['remaining_amount'];
    }
    $total_due = round($total_due, 2);

    if ($amount > $total_due + 0.009) {
        $conn->rollback();
        json_response([
            'success' => false,
            'message' => 'জমার পরিমান মোট বকেয়ার চেয়ে বেশি হতে পারে না (বকেয়া: ৳' . number_format($total_due, 2) . ')',
        ], 400);
    }

    $update_stmt = $conn->prepare(
        "UPDATE settlements
         SET paid_amount = ?,
             remaining_amount = ?,
             payment_status = ?,
             paid_date = IF(? = 'paid', NOW(), paid_date),
             updated_at = NOW()
         WHERE id = ?"
    );
    $log_stmt = $conn->prepare(
        "INSERT INTO settlement_payments
         (settlement_id, amount, payment_method, notes, recorded_by, paid_by_driver_id)
         VALUES (?, ?, ?, ?, ?, ?)"
    );

    $remaining_to_allocate = $amount;
    $allocations = [];

    foreach ($settlements as $s) {
        if ($remaining_to_allocate <= 0.009) break;

        $sid = (int)$s['id'];
        $s_remaining = round((float)$s['remaining_amount'], 2);
        $pay = min($s_remaining, $remaining_to_allocate);
        $pay = round($pay, 2);

        $new_paid = round((float)$s['paid_amount'] + $pay, 2);
        $new_remaining = round((float)$s['amount_to_collect'] - $new_paid, 2);
        if ($new_remaining < 0) $new_remaining = 0.0;
        $new_status = $new_remaining <= 0.009 ? 'paid' : 'partial';

        $update_stmt->bind_param('ddssi', $new_paid, $new_remaining, $new_status, $new_status, $sid);
        if (!$update_stmt->execute()) {
            throw new Exception('সেটেলমেন্ট আপডেট ব্যর্থ: ' . $update_stmt->error);
        }

        $log_stmt->bind_param('idssii', $sid, $pay, $payment_method, $notes, $user_id, $driver_id);
        if (!$log_stmt->execute()) {
            throw new Exception('পেমেন্ট লগ করতে ব্যর্থ: ' . $log_stmt->error);
        }

        $allocations[] = [
            'settlement_id' => $sid,
            'payment_id' => $conn->insert_id,
            'allocated' => $pay,
            'new_paid_amount' => $new_paid,
            'new_remaining_amount' => $new_remaining,
            'payment_status' => $new_status,
        ];

        $remaining_to_allocate = round($remaining_to_allocate - $pay, 2);
    }

    $update_stmt->close();
    $log_stmt->close();

    $conn->commit();

    json_response([
        'success' => true,
        'message' => '৳' . number_format($amount, 2) . ' সফলভাবে জমা হয়েছে (' . count($allocations) . 'টি সেটেলমেন্টে)',
        'data' => [
            'driver_id' => $driver_id,
            'driver_name' => $driver['name'],
            'collected_amount' => $amount,
            'remaining_due' => round($total_due - $amount, 2),
            'allocations' => $allocations,
        ],
    ], 201);

} catch (Exception $e) {
    $conn->rollback();
    json_response(['success' => false, 'message' => $e->getMessage()], 500);
}
?>
