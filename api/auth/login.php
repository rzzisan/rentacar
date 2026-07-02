<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../../includes/User.php';
require_once '../_helpers.php';

only_method('POST');

// API token তৈরি করে DB-তে সংরক্ষণ করে; $days মেয়াদ (default 30)
// $tenant_id null হলে superadmin token (tenant-agnostic)
function _generate_api_token(mysqli $conn, string $role, ?int $tenant_id, int $user_id, int $driver_id, int $manager_id, string $username, string $email, int $days = 30): string {
    $token   = bin2hex(random_bytes(32)); // 64-char hex token
    $expires = date('Y-m-d H:i:s', time() + $days * 24 * 3600);
    $stmt    = $conn->prepare(
        "INSERT INTO api_tokens (token, role, tenant_id, user_id, driver_id, manager_id, username, email, expires_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
    );
    $stmt->bind_param('ssiiiisss', $token, $role, $tenant_id, $user_id, $driver_id, $manager_id, $username, $email, $expires);
    $stmt->execute();
    $stmt->close();
    return $token;
}

// Remember Me হলে session cookie-র মেয়াদ 90 দিনে বাড়িয়ে দেয়
function _apply_remember_me(): void {
    $lifetime = REMEMBER_ME_DURATION; // 90 days
    $params   = session_get_cookie_params();
    setcookie(
        session_name(),
        session_id(),
        [
            'expires'  => time() + $lifetime,
            'path'     => $params['path'],
            'domain'   => $params['domain'],
            'secure'   => $params['secure'],
            'httponly' => $params['httponly'],
            'samesite' => 'Strict',
        ]
    );
    ini_set('session.gc_maxlifetime', $lifetime);
}

$body        = input();
$identifier  = trim($body['identifier'] ?? $body['email'] ?? ''); // ইমেইল অথবা মোবাইল নম্বর
$pass        = $body['password'] ?? '';
$is_mobile   = ($body['source'] ?? '') === 'mobile';
$remember_me = !empty($body['remember_me']);
$token_days  = $remember_me ? 90 : 30;

if (!$identifier || !$pass) {
    json_response(['success' => false, 'message' => 'ইমেইল/মোবাইল এবং পাসওয়ার্ড দিন']);
}

$conn = (new Database())->connect();

// ── ধাপ ১: super_admins টেবিল চেক (tenant-agnostic, সবার আগে; শুধু ইমেইল — মোবাইল কলাম নেই) ─
$sstmt = $conn->prepare("SELECT id, name, email, password FROM super_admins WHERE email = ?");
$sstmt->bind_param('s', $identifier);
$sstmt->execute();
$sa_result = $sstmt->get_result();

if ($sa_result->num_rows === 1) {
    $sa = $sa_result->fetch_assoc();
    $sstmt->close();

    if (password_verify($pass, $sa['password'])) {
        session_regenerate_id(true);
        $_SESSION['user_id'] = (int)$sa['id'];
        $_SESSION['role']    = 'superadmin';
        $_SESSION['username']= $sa['name'] ?: $sa['email'];
        $_SESSION['email']   = $sa['email'];
        unset($_SESSION['tenant_id']);

        if ($remember_me) _apply_remember_me();
        $resp = [
            'id'       => (int)$sa['id'],
            'username' => $_SESSION['username'],
            'email'    => $sa['email'],
            'role'     => 'superadmin',
        ];
        if ($is_mobile) {
            $resp['token'] = _generate_api_token(
                $conn, 'superadmin', null, (int)$sa['id'], 0, 0,
                $_SESSION['username'], $sa['email'], $token_days
            );
        }
        json_response(['success' => true, 'message' => 'লগইন সফল হয়েছে', 'data' => $resp]);
    }
    json_response(['success' => false, 'message' => 'অবৈধ পাসওয়ার্ড']);
}
$sstmt->close();

// ── ধাপ ২: users টেবিল (admin/employee/customer) ─────────────────
$user = new User($conn);
$result = $user->login($identifier, $pass);

if ($result['success']) {
    // suspended/cancelled tenant হলে ব্লক
    require_active_tenant();

    if ($remember_me) _apply_remember_me();
    $resp = [
        'id'        => (int) $_SESSION['user_id'],
        'tenant_id' => (int) $_SESSION['tenant_id'],
        'username'  => $_SESSION['username'],
        'email'     => $_SESSION['email'],
        'role'      => $_SESSION['role'],
    ];
    if ($is_mobile) {
        $resp['token'] = _generate_api_token(
            $conn, $_SESSION['role'], (int)$_SESSION['tenant_id'], (int)$_SESSION['user_id'], 0, 0,
            $_SESSION['username'], $_SESSION['email'], $token_days
        );
    }
    json_response(['success' => true, 'message' => 'লগইন সফল হয়েছে', 'data' => $resp]);
}

// ── ধাপ ৩: drivers টেবিল (ইমেইল অথবা মোবাইল) ──────────────────────
$dstmt = $conn->prepare("SELECT id, tenant_id, name, email, password, status FROM drivers WHERE email = ? OR mobile = ?");
$dstmt->bind_param('ss', $identifier, $identifier);
$dstmt->execute();
$driver_result = $dstmt->get_result();

if ($driver_result->num_rows === 1) {
    $driver = $driver_result->fetch_assoc();

    if ($driver['status'] === 'active' && password_verify($pass, $driver['password'])) {
        session_regenerate_id(true);
        $_SESSION['driver_id'] = (int)$driver['id'];
        $_SESSION['tenant_id'] = (int)$driver['tenant_id'];
        $_SESSION['role']      = 'driver';
        $_SESSION['username']  = $driver['name'];
        $_SESSION['email']     = $driver['email'];

        require_active_tenant();

        if ($remember_me) _apply_remember_me();
        $resp = [
            'id'        => (int)$driver['id'],
            'tenant_id' => (int)$driver['tenant_id'],
            'username'  => $driver['name'],
            'email'     => $driver['email'],
            'role'      => 'driver',
        ];
        if ($is_mobile) {
            $resp['token'] = _generate_api_token(
                $conn, 'driver', (int)$driver['tenant_id'], 0, (int)$driver['id'], 0,
                $driver['name'], $driver['email'], $token_days
            );
        }
        json_response(['success' => true, 'message' => 'লগইন সফল হয়েছে', 'data' => $resp]);
    } else {
        json_response(['success' => false, 'message' => 'অবৈধ পাসওয়ার্ড বা নিষ্ক্রিয় অ্যাকাউন্ট']);
    }
}

$dstmt->close();

// ── ধাপ ৪: managers টেবিল (ইমেইল অথবা মোবাইল) ─────────────────────
$mstmt = $conn->prepare("SELECT id, tenant_id, name, email, password, status FROM managers WHERE email = ? OR mobile = ?");
$mstmt->bind_param('ss', $identifier, $identifier);
$mstmt->execute();
$manager_result = $mstmt->get_result();

if ($manager_result->num_rows === 1) {
    $manager = $manager_result->fetch_assoc();

    if ($manager['status'] === 'active' && password_verify($pass, $manager['password'])) {
        session_regenerate_id(true);
        $_SESSION['manager_id'] = (int)$manager['id'];
        $_SESSION['tenant_id']  = (int)$manager['tenant_id'];
        $_SESSION['role']       = 'manager';
        $_SESSION['username']   = $manager['name'];
        $_SESSION['email']      = $manager['email'];

        require_active_tenant();

        if ($remember_me) _apply_remember_me();
        $resp = [
            'id'        => (int)$manager['id'],
            'tenant_id' => (int)$manager['tenant_id'],
            'username'  => $manager['name'],
            'email'     => $manager['email'],
            'role'      => 'manager',
        ];
        if ($is_mobile) {
            $resp['token'] = _generate_api_token(
                $conn, 'manager', (int)$manager['tenant_id'], 0, 0, (int)$manager['id'],
                $manager['name'], $manager['email'], $token_days
            );
        }
        json_response(['success' => true, 'message' => 'লগইন সফল হয়েছে', 'data' => $resp]);
    } else {
        json_response(['success' => false, 'message' => 'অবৈধ পাসওয়ার্ড বা নিষ্ক্রিয় অ্যাকাউন্ট']);
    }
}

$mstmt->close();
json_response(['success' => false, 'message' => $result['message']]);
