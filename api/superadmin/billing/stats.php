<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

only_method('GET');
require_superadmin();

$conn = (new Database())->connect();
$q = fn(string $sql) => $conn->query($sql)->fetch_assoc();

$stats = [
    'total_tenants'     => (int) $q("SELECT COUNT(*) c FROM tenants")['c'],
    'trial_tenants'     => (int) $q("SELECT COUNT(*) c FROM tenants WHERE status='trial'")['c'],
    'active_tenants'    => (int) $q("SELECT COUNT(*) c FROM tenants WHERE status='active'")['c'],
    'suspended_tenants' => (int) $q("SELECT COUNT(*) c FROM tenants WHERE status='suspended'")['c'],
    'cancelled_tenants' => (int) $q("SELECT COUNT(*) c FROM tenants WHERE status='cancelled'")['c'],

    'this_month_total'  => (float) $q(
        "SELECT COALESCE(SUM(total_amount),0) v FROM subscription_invoices
         WHERE DATE_FORMAT(invoice_date, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')"
    )['v'],
    'this_month_paid'   => (float) $q(
        "SELECT COALESCE(SUM(total_amount),0) v FROM subscription_invoices
         WHERE DATE_FORMAT(invoice_date, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m') AND status='paid'"
    )['v'],
    'pending_amount'    => (float) $q(
        "SELECT COALESCE(SUM(total_amount),0) v FROM subscription_invoices WHERE status IN ('pending','overdue')"
    )['v'],
    'overdue_count'     => (int) $q("SELECT COUNT(*) c FROM subscription_invoices WHERE status='overdue'")['c'],
    'overdue_amount'    => (float) $q(
        "SELECT COALESCE(SUM(total_amount),0) v FROM subscription_invoices WHERE status='overdue'"
    )['v'],
];

// গত ৬ মাসের মাসিক আয় ট্রেন্ড (paid invoice-ভিত্তিক)
$trend_rows = $conn->query(
    "SELECT DATE_FORMAT(invoice_date, '%Y-%m') AS ym, COALESCE(SUM(total_amount),0) AS total
     FROM subscription_invoices
     WHERE invoice_date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
     GROUP BY ym ORDER BY ym ASC"
)->fetch_all(MYSQLI_ASSOC);

$stats['monthly_trend'] = array_map(fn($r) => [
    'month' => $r['ym'],
    'total' => (float) $r['total'],
], $trend_rows);

// সাম্প্রতিক ৫টি invoice
$recent = $conn->query(
    "SELECT si.id, si.invoice_number, si.total_amount, si.status, si.invoice_date, t.name AS tenant_name
     FROM subscription_invoices si JOIN tenants t ON t.id = si.tenant_id
     ORDER BY si.created_at DESC LIMIT 5"
)->fetch_all(MYSQLI_ASSOC);

$stats['recent_invoices'] = array_map(fn($r) => [
    'id'             => (int) $r['id'],
    'invoice_number' => $r['invoice_number'],
    'total_amount'   => (float) $r['total_amount'],
    'status'         => $r['status'],
    'invoice_date'   => $r['invoice_date'],
    'tenant_name'    => $r['tenant_name'],
], $recent);

json_response(['success' => true, 'data' => $stats]);
