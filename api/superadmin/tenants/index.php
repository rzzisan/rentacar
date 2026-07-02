<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_superadmin();

$conn   = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];

// সহায়ক: saas_settings-এর একটি key পড়ে
function _saas_setting(mysqli $conn, string $key, string $default): string {
    $stmt = $conn->prepare('SELECT value FROM saas_settings WHERE key_name = ?');
    $stmt->bind_param('s', $key);
    $stmt->execute();
    $row = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    return $row ? $row['value'] : $default;
}

// ── GET: সব tenant তালিকা ──────────────────────────────────────────
if ($method === 'GET') {
    $sql = "SELECT t.*,
                   s.status AS subscription_status,
                   (SELECT COUNT(*) FROM vehicles v WHERE v.tenant_id = t.id AND v.status != 'inactive') AS vehicle_count,
                   (SELECT COUNT(*) FROM users u WHERE u.tenant_id = t.id AND u.role = 'admin') AS admin_count,
                   li.status       AS latest_invoice_status,
                   li.total_amount AS latest_invoice_amount,
                   li.due_date     AS latest_invoice_due_date
            FROM tenants t
            LEFT JOIN subscriptions s ON s.tenant_id = t.id
            LEFT JOIN subscription_invoices li ON li.id = (
                SELECT id FROM subscription_invoices
                WHERE tenant_id = t.id ORDER BY invoice_date DESC LIMIT 1
            )
            ORDER BY t.created_at DESC";
    $rows = $conn->query($sql)->fetch_all(MYSQLI_ASSOC);

    $tenants = array_map(function ($r) {
        return [
            'id'                     => (int) $r['id'],
            'name'                   => $r['name'],
            'email'                  => $r['email'],
            'phone'                  => $r['phone'],
            'address'                => $r['address'],
            'status'                 => $r['status'],
            'trial_ends_at'          => $r['trial_ends_at'],
            'subscription_status'    => $r['subscription_status'],
            'vehicle_count'          => (int) $r['vehicle_count'],
            'admin_count'            => (int) $r['admin_count'],
            'latest_invoice_status'  => $r['latest_invoice_status'],
            'latest_invoice_amount'  => $r['latest_invoice_amount'] !== null ? (float) $r['latest_invoice_amount'] : null,
            'latest_invoice_due_date'=> $r['latest_invoice_due_date'],
            'created_at'             => $r['created_at'],
        ];
    }, $rows);

    json_response(['success' => true, 'data' => $tenants]);
}

// ── POST: নতুন tenant + প্রথম admin তৈরি ────────────────────────────
if ($method === 'POST') {
    $b = input();

    $name          = trim($b['name'] ?? '');
    $email         = trim($b['email'] ?? '');
    $phone         = trim($b['phone'] ?? '') ?: null;
    $address       = trim($b['address'] ?? '') ?: null;
    $admin_name    = trim($b['admin_name'] ?? '');
    $admin_email   = trim($b['admin_email'] ?? '');
    $admin_phone   = trim($b['admin_phone'] ?? '') ?: null;
    $admin_password= $b['admin_password'] ?? '';

    if (!$name || !$email || !$admin_name || !$admin_email || !$admin_password) {
        json_response(['success' => false, 'message' => 'Tenant নাম/ইমেইল ও Admin নাম/ইমেইল/পাসওয়ার্ড আবশ্যক'], 400);
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL) || !filter_var($admin_email, FILTER_VALIDATE_EMAIL)) {
        json_response(['success' => false, 'message' => 'সঠিক ইমেইল ঠিকানা দিন'], 400);
    }
    if ($admin_phone && !preg_match('/^\d{10,15}$/', preg_replace('/[^\d]/', '', $admin_phone))) {
        json_response(['success' => false, 'message' => 'সঠিক মোবাইল নম্বর দিন'], 400);
    }
    if (strlen($admin_password) < 6) {
        json_response(['success' => false, 'message' => 'পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে'], 400);
    }

    // Tenant email uniqueness
    $chk = $conn->prepare('SELECT id FROM tenants WHERE email = ?');
    $chk->bind_param('s', $email);
    $chk->execute();
    if ($chk->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই tenant ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
    }
    $chk->close();

    // Admin user email uniqueness (users টেবিল global-unique)
    $chk2 = $conn->prepare('SELECT id FROM users WHERE email = ?');
    $chk2->bind_param('s', $admin_email);
    $chk2->execute();
    if ($chk2->get_result()->num_rows > 0) {
        json_response(['success' => false, 'message' => 'এই admin ইমেইল ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
    }
    $chk2->close();

    // Admin phone uniqueness (দেওয়া থাকলে)
    if ($admin_phone) {
        $chk3 = $conn->prepare('SELECT id FROM users WHERE phone = ?');
        $chk3->bind_param('s', $admin_phone);
        $chk3->execute();
        if ($chk3->get_result()->num_rows > 0) {
            json_response(['success' => false, 'message' => 'এই মোবাইল নম্বর ইতিমধ্যে ব্যবহৃত হচ্ছে'], 409);
        }
        $chk3->close();
    }

    // username জেনারেট করা হয় ইমেইলের local-part থেকে, collision হলে সংখ্যা যোগ হয়
    $base_username = preg_replace('/[^a-zA-Z0-9_]/', '', strtolower(explode('@', $admin_email)[0])) ?: 'admin';
    $username = $base_username;
    $suffix = 1;
    while (true) {
        $ustmt = $conn->prepare('SELECT id FROM users WHERE username = ?');
        $ustmt->bind_param('s', $username);
        $ustmt->execute();
        $taken = $ustmt->get_result()->num_rows > 0;
        $ustmt->close();
        if (!$taken) break;
        $username = $base_username . $suffix;
        $suffix++;
    }

    $trial_days   = (int) _saas_setting($conn, 'trial_days', '30');
    $trial_ends_at = date('Y-m-d', strtotime("+{$trial_days} days"));

    $conn->begin_transaction();
    try {
        $tstmt = $conn->prepare(
            'INSERT INTO tenants (name, email, phone, address, status, trial_ends_at) VALUES (?, ?, ?, ?, ?, ?)'
        );
        $status = 'trial';
        $tstmt->bind_param('ssssss', $name, $email, $phone, $address, $status, $trial_ends_at);
        $tstmt->execute();
        $tenant_id = $conn->insert_id;
        $tstmt->close();

        $sstmt = $conn->prepare(
            "INSERT INTO subscriptions (tenant_id, started_at, status, vehicle_count) VALUES (?, CURDATE(), 'trialing', 0)"
        );
        $sstmt->bind_param('i', $tenant_id);
        $sstmt->execute();
        $sstmt->close();

        $hashed = password_hash($admin_password, PASSWORD_BCRYPT);
        $arole  = 'admin';
        $astmt  = $conn->prepare(
            'INSERT INTO users (tenant_id, username, email, phone, password, role, status) VALUES (?, ?, ?, ?, ?, ?, "active")'
        );
        $astmt->bind_param('isssss', $tenant_id, $username, $admin_email, $admin_phone, $hashed, $arole);
        $astmt->execute();
        $astmt->close();

        $conn->commit();
    } catch (Throwable $e) {
        $conn->rollback();
        json_response(['success' => false, 'message' => 'Tenant তৈরি ব্যর্থ: ' . $e->getMessage()], 500);
    }

    json_response([
        'success' => true,
        'message' => 'নতুন Tenant ও Admin অ্যাকাউন্ট তৈরি হয়েছে',
        'data'    => ['tenant_id' => $tenant_id, 'admin_username' => $username],
    ], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
