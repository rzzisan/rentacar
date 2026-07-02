<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

only_method('PUT');
require_superadmin();

$id = (int) ($_GET['id'] ?? 0);
if (!$id) {
    json_response(['success' => false, 'message' => 'ID দিন'], 400);
}

$b      = input();
$status = $b['status'] ?? '';
$allowed = ['trial', 'active', 'suspended', 'cancelled'];

if (!in_array($status, $allowed, true)) {
    json_response(['success' => false, 'message' => 'অবৈধ status — trial/active/suspended/cancelled অনুমোদিত'], 400);
}

$conn = (new Database())->connect();

$chk = $conn->prepare('SELECT id FROM tenants WHERE id = ?');
$chk->bind_param('i', $id);
$chk->execute();
if ($chk->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'Tenant পাওয়া যায়নি'], 404);
}
$chk->close();

// tenants.status -> subscriptions.status ম্যাপিং (Phase 2 migration-এর সাথে সামঞ্জস্যপূর্ণ)
$sub_status_map = [
    'trial'     => 'trialing',
    'active'    => 'active',
    'suspended' => 'past_due',
    'cancelled' => 'cancelled',
];
$sub_status = $sub_status_map[$status];

$conn->begin_transaction();
try {
    $tstmt = $conn->prepare('UPDATE tenants SET status = ?, updated_at = NOW() WHERE id = ?');
    $tstmt->bind_param('si', $status, $id);
    $tstmt->execute();
    $tstmt->close();

    $sstmt = $conn->prepare('UPDATE subscriptions SET status = ? WHERE tenant_id = ?');
    $sstmt->bind_param('si', $sub_status, $id);
    $sstmt->execute();
    $sstmt->close();

    $conn->commit();
} catch (Throwable $e) {
    $conn->rollback();
    json_response(['success' => false, 'message' => 'Status পরিবর্তন ব্যর্থ: ' . $e->getMessage()], 500);
}

$status_labels = ['trial' => 'ট্রায়াল', 'active' => 'সক্রিয়', 'suspended' => 'স্থগিত', 'cancelled' => 'বাতিল'];
json_response([
    'success' => true,
    'message' => 'Tenant status ' . ($status_labels[$status] ?? $status) . ' করা হয়েছে',
    'data'    => ['id' => $id, 'status' => $status],
]);
