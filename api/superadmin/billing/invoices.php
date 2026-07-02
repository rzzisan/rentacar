<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_superadmin();

$conn   = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

// ── GET: সব invoice তালিকা (filter: tenant_id, status, month=YYYY-MM) ─
if ($method === 'GET') {
    $where  = ['1=1'];
    $params = [];
    $types  = '';

    if (!empty($_GET['tenant_id'])) {
        $where[]  = 'si.tenant_id = ?';
        $params[] = (int) $_GET['tenant_id'];
        $types   .= 'i';
    }
    if (!empty($_GET['status'])) {
        $where[]  = 'si.status = ?';
        $params[] = $_GET['status'];
        $types   .= 's';
    }
    if (!empty($_GET['month'])) {
        $where[]  = "DATE_FORMAT(si.invoice_date, '%Y-%m') = ?";
        $params[] = $_GET['month'];
        $types   .= 's';
    }

    $sql = 'SELECT si.*, t.name AS tenant_name, t.email AS tenant_email
            FROM subscription_invoices si
            JOIN tenants t ON t.id = si.tenant_id
            WHERE ' . implode(' AND ', $where) . '
            ORDER BY si.invoice_date DESC, si.id DESC';

    $stmt = $conn->prepare($sql);
    if ($params) $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    $invoices = array_map(function ($r) {
        return [
            'id'                => (int) $r['id'],
            'tenant_id'         => (int) $r['tenant_id'],
            'tenant_name'       => $r['tenant_name'],
            'tenant_email'      => $r['tenant_email'],
            'subscription_id'   => (int) $r['subscription_id'],
            'invoice_number'    => $r['invoice_number'],
            'invoice_date'      => $r['invoice_date'],
            'due_date'          => $r['due_date'],
            'vehicle_count'     => (int) $r['vehicle_count'],
            'price_per_vehicle' => (float) $r['price_per_vehicle'],
            'total_amount'      => (float) $r['total_amount'],
            'status'            => $r['status'],
            'paid_at'           => $r['paid_at'],
            'payment_method'    => $r['payment_method'],
            'payment_ref'       => $r['payment_ref'],
            'notes'             => $r['notes'],
            'created_at'        => $r['created_at'],
        ];
    }, $rows);

    json_response(['success' => true, 'data' => $invoices]);
}

// ── POST: manual mark-paid ─────────────────────────────────────────
if ($method === 'POST') {
    $b = input();

    $invoice_id     = (int) ($b['invoice_id'] ?? 0);
    $payment_method = trim($b['payment_method'] ?? '');
    $payment_ref    = trim($b['payment_ref'] ?? '') ?: null;
    $notes          = trim($b['notes'] ?? '') ?: null;

    if (!$invoice_id || !$payment_method) {
        json_response(['success' => false, 'message' => 'invoice ও payment method আবশ্যক'], 400);
    }

    $istmt = $conn->prepare('SELECT id, tenant_id, subscription_id, status FROM subscription_invoices WHERE id = ?');
    $istmt->bind_param('i', $invoice_id);
    $istmt->execute();
    $invoice = $istmt->get_result()->fetch_assoc();
    $istmt->close();

    if (!$invoice) {
        json_response(['success' => false, 'message' => 'Invoice পাওয়া যায়নি'], 404);
    }
    if ($invoice['status'] === 'paid') {
        json_response(['success' => false, 'message' => 'এই invoice ইতিমধ্যে পরিশোধিত']);
    }

    $conn->begin_transaction();
    try {
        $ustmt = $conn->prepare(
            "UPDATE subscription_invoices
             SET status = 'paid', paid_at = NOW(), payment_method = ?, payment_ref = ?, notes = ?
             WHERE id = ?"
        );
        $ustmt->bind_param('sssi', $payment_method, $payment_ref, $notes, $invoice_id);
        $ustmt->execute();
        $ustmt->close();

        // এই invoice-এর কারণে suspend হয়ে থাকলে payment confirm হওয়ার সাথে সাথে access ফিরিয়ে দেওয়া হয়
        $tstmt = $conn->prepare(
            "UPDATE tenants SET status = 'active', updated_at = NOW() WHERE id = ? AND status = 'suspended'"
        );
        $tstmt->bind_param('i', $invoice['tenant_id']);
        $tstmt->execute();
        $reactivated = $tstmt->affected_rows > 0;
        $tstmt->close();

        if ($reactivated) {
            $sstmt = $conn->prepare("UPDATE subscriptions SET status = 'active' WHERE id = ?");
            $sstmt->bind_param('i', $invoice['subscription_id']);
            $sstmt->execute();
            $sstmt->close();
        }

        $conn->commit();
    } catch (Throwable $e) {
        $conn->rollback();
        json_response(['success' => false, 'message' => 'Mark-paid ব্যর্থ: ' . $e->getMessage()], 500);
    }

    json_response([
        'success' => true,
        'message' => $reactivated ? 'Invoice পরিশোধিত মার্ক হয়েছে এবং Tenant পুনরায় সক্রিয় হয়েছে' : 'Invoice পরিশোধিত মার্ক হয়েছে',
        'data'    => ['id' => $invoice_id, 'tenant_reactivated' => $reactivated],
    ]);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
