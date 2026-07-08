<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

only_method('DELETE');
$manager_id = require_manager();
$tid = get_tenant_id();

$conn = (new Database())->connect();
$id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if (!$id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

// manager শুধু তার assigned vehicle-এর pending ট্রিপ ডিলিট করতে পারবে — IDOR প্রতিরোধ
// (ownership check কখনো if ($vids)-এর ভেতরে রাখা যাবে না, manager_vehicle_in_clause([]) নিরাপদে '(0)' রিটার্ন করে)
$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

$chk = $conn->prepare("SELECT rental_status FROM rentals WHERE id = ? AND vehicle_id IN $in AND tenant_id = ?");
$params = array_merge([$id], $vids, [$tid]);
$types  = 'i' . str_repeat('i', count($vids)) . 'i';
$chk->bind_param($types, ...$params);
$chk->execute();
$result = $chk->get_result();

if ($result->num_rows === 0) {
    json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি'], 404);
}
$rental = $result->fetch_assoc();
$chk->close();

if ($rental['rental_status'] !== 'pending') {
    json_response(['success' => false, 'message' => 'শুধু অপেক্ষমান (pending) ট্রিপ মুছা যাবে'], 400);
}

// রসিদ ছবিগুলোর পথ আগে সংগ্রহ করে রাখা — DB cascade delete-এর পর ফাইল মুছতে লাগবে
$estmt = $conn->prepare('SELECT receipt_image FROM trip_expenses WHERE rental_id = ? AND receipt_image IS NOT NULL');
$estmt->bind_param('i', $id);
$estmt->execute();
$receiptImages = array_column($estmt->get_result()->fetch_all(MYSQLI_ASSOC), 'receipt_image');
$estmt->close();

// rentals ডিলিট করলেই trip_expenses/settlements/settlement_payments/trip_locations/
// damage_reports/payments/reviews সব cascade delete হয় (FK ON DELETE CASCADE)
$stmt = $conn->prepare('DELETE FROM rentals WHERE id = ? AND tenant_id = ?');
$stmt->bind_param('ii', $id, $tid);
$stmt->execute();
$affected = $stmt->affected_rows;
$stmt->close();

if ($affected === 0) {
    json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি বা মুছতে ব্যর্থ'], 404);
}

foreach ($receiptImages as $img) {
    $path = UPLOAD_PATH . $img;
    if (file_exists($path)) {
        @unlink($path);
    }
}

json_response(['success' => true, 'message' => 'ট্রিপ ও সম্পর্কিত সকল ডাটা মুছে ফেলা হয়েছে']);
