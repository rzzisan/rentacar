<?php
// Overdue invoice check — cron দিয়ে প্রতিদিন চালানোর জন্য (CLI-only)
// crontab: 0 8 * * * php /var/www/html/car.zisan.me/api/cron/check-overdue.php
if (php_sapi_name() !== 'cli') {
    http_response_code(403);
    exit('Forbidden — এই স্ক্রিপ্ট শুধু CLI থেকে চালানো যাবে');
}

require_once __DIR__ . '/../../config/config.php';
require_once __DIR__ . '/../../config/Database.php';

$conn = (new Database())->connect();

// due_date পার হয়ে যাওয়া কিন্তু এখনো pending — এমন সব invoice খুঁজে বের করো
$stmt = $conn->prepare(
    "SELECT id, tenant_id, subscription_id, invoice_number
     FROM subscription_invoices
     WHERE status = 'pending' AND due_date < CURDATE()"
);
$stmt->execute();
$overdue_invoices = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

$suspended_tenants = 0;

foreach ($overdue_invoices as $inv) {
    $invoice_id      = (int) $inv['id'];
    $tenant_id       = (int) $inv['tenant_id'];
    $subscription_id = (int) $inv['subscription_id'];

    $ustmt = $conn->prepare("UPDATE subscription_invoices SET status = 'overdue' WHERE id = ?");
    $ustmt->bind_param('i', $invoice_id);
    $ustmt->execute();
    $ustmt->close();

    // ইতিমধ্যে suspended/cancelled tenant-কে আবার ছুঁয়ো না
    $tstmt = $conn->prepare(
        "UPDATE tenants SET status = 'suspended' WHERE id = ? AND status NOT IN ('suspended', 'cancelled')"
    );
    $tstmt->bind_param('i', $tenant_id);
    $tstmt->execute();
    $newly_suspended = $tstmt->affected_rows > 0;
    $tstmt->close();

    $sstmt = $conn->prepare("UPDATE subscriptions SET status = 'past_due' WHERE id = ?");
    $sstmt->bind_param('i', $subscription_id);
    $sstmt->execute();
    $sstmt->close();

    if ($newly_suspended) {
        $suspended_tenants++;
        echo "SUSPENDED: tenant {$tenant_id} — {$inv['invoice_number']} overdue-এর কারণে\n";
    } else {
        echo "OVERDUE marked: tenant {$tenant_id} — {$inv['invoice_number']} (tenant ইতিমধ্যে suspended/cancelled ছিল)\n";
    }
}

echo "সম্পন্ন: " . count($overdue_invoices) . " টি invoice overdue mark হয়েছে, {$suspended_tenants} টি tenant নতুন করে suspend হয়েছে।\n";
