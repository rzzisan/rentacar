<?php
// মাসিক invoice generator — cron দিয়ে চালানোর জন্য (CLI-only, HTTP-এর মাধ্যমে অ্যাক্সেসযোগ্য নয়)
// crontab: 0 6 1 * * php /var/www/html/car.zisan.me/api/cron/generate-invoices.php
if (php_sapi_name() !== 'cli') {
    http_response_code(403);
    exit('Forbidden — এই স্ক্রিপ্ট শুধু CLI থেকে চালানো যাবে');
}

require_once __DIR__ . '/../../config/config.php';
require_once __DIR__ . '/../../config/Database.php';

$conn = (new Database())->connect();

function get_saas_setting(mysqli $conn, string $key, string $default): string {
    $stmt = $conn->prepare('SELECT value FROM saas_settings WHERE key_name = ?');
    $stmt->bind_param('s', $key);
    $stmt->execute();
    $row = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    return $row ? $row['value'] : $default;
}

$price_per_vehicle = (float) get_saas_setting($conn, 'price_per_vehicle', '500');
$due_days          = (int) get_saas_setting($conn, 'invoice_due_days', '7');

$today    = date('Y-m-d');
$due_date = date('Y-m-d', strtotime("+{$due_days} days"));

// trial ও active — দুই ধরনের tenant-এরই মাসিক invoice তৈরি হয় (সিদ্ধান্ত: 2026-07-02)
$tstmt = $conn->prepare(
    "SELECT t.id AS tenant_id, s.id AS subscription_id
     FROM tenants t
     JOIN subscriptions s ON s.tenant_id = t.id
     WHERE t.status IN ('trial', 'active')"
);
$tstmt->execute();
$tenants = $tstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$tstmt->close();

$created = 0;
$skipped = 0;

foreach ($tenants as $t) {
    $tenant_id = (int) $t['tenant_id'];
    $sub_id    = (int) $t['subscription_id'];

    // এই মাসে ইতিমধ্যে invoice তৈরি হয়ে থাকলে স্কিপ করো — cron দুবার চললেও ডুপ্লিকেট হবে না
    $invoice_number = sprintf('INV-%s-T%d', date('Ym'), $tenant_id);
    $chk = $conn->prepare('SELECT id FROM subscription_invoices WHERE invoice_number = ?');
    $chk->bind_param('s', $invoice_number);
    $chk->execute();
    $exists = $chk->get_result()->num_rows > 0;
    $chk->close();
    if ($exists) {
        echo "SKIP: tenant {$tenant_id} — {$invoice_number} ইতিমধ্যে আছে\n";
        $skipped++;
        continue;
    }

    // সক্রিয় গাড়ির সংখ্যা (inactive বাদে)
    $vstmt = $conn->prepare("SELECT COUNT(*) c FROM vehicles WHERE tenant_id = ? AND status != 'inactive'");
    $vstmt->bind_param('i', $tenant_id);
    $vstmt->execute();
    $vehicle_count = (int) $vstmt->get_result()->fetch_assoc()['c'];
    $vstmt->close();

    if ($vehicle_count === 0) {
        echo "SKIP: tenant {$tenant_id} — কোনো সক্রিয় গাড়ি নেই, invoice দরকার নেই\n";
        $skipped++;
        continue;
    }

    $total_amount = $vehicle_count * $price_per_vehicle;

    $istmt = $conn->prepare(
        "INSERT INTO subscription_invoices
         (tenant_id, subscription_id, invoice_number, invoice_date, due_date,
          vehicle_count, price_per_vehicle, total_amount, status)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending')"
    );
    $istmt->bind_param(
        'iisssidd',
        $tenant_id, $sub_id, $invoice_number, $today, $due_date,
        $vehicle_count, $price_per_vehicle, $total_amount
    );
    $istmt->execute();
    $istmt->close();

    // subscriptions.vehicle_count স্ন্যাপশট আপডেট
    $ustmt = $conn->prepare('UPDATE subscriptions SET vehicle_count = ? WHERE id = ?');
    $ustmt->bind_param('ii', $vehicle_count, $sub_id);
    $ustmt->execute();
    $ustmt->close();

    echo "CREATED: {$invoice_number} — tenant {$tenant_id}, {$vehicle_count} গাড়ি × {$price_per_vehicle} = {$total_amount} টাকা, due {$due_date}\n";
    $created++;
}

echo "সম্পন্ন: {$created} টি নতুন invoice তৈরি হয়েছে, {$skipped} টি স্কিপ হয়েছে।\n";
