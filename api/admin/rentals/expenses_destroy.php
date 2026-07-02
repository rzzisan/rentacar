<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('DELETE');
$tid = get_tenant_id();

$conn = (new Database())->connect();
$expense_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$expense_id) {
    json_response(['success' => false, 'message' => 'খরচ আইডি প্রয়োজন'], 400);
}

// Get expense details (trip_expenses-এ tenant_id নেই — rentals জয়েন করে যাচাই)
$stmt = $conn->prepare(
    "SELECT te.receipt_image
     FROM trip_expenses te
     JOIN rentals r ON r.id = te.rental_id
     WHERE te.id = ? AND r.tenant_id = ?"
);
$stmt->bind_param('ii', $expense_id, $tid);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    json_response(['success' => false, 'message' => 'খরচ পাওয়া যায়নি'], 404);
}

$expense = $result->fetch_assoc();
$receipt_image = $expense['receipt_image'];
$stmt->close();

// Delete expense record
$dstmt = $conn->prepare("DELETE FROM trip_expenses WHERE id = ?");
$dstmt->bind_param('i', $expense_id);

if (!$dstmt->execute()) {
    json_response(['success' => false, 'message' => 'ডিলিট ব্যর্থ: ' . $dstmt->error], 500);
}
$dstmt->close();

// Delete receipt image file if exists
if ($receipt_image) {
    $file_path = UPLOAD_PATH . $receipt_image;
    if (file_exists($file_path)) {
        @unlink($file_path);
    }
}

json_response(['success' => true, 'message' => 'খরচ সফলভাবে ডিলিট করা হয়েছে']);
?>
