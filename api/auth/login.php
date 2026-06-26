<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../../includes/User.php';
require_once '../_helpers.php';

only_method('POST');

// API token তৈরি করে DB-তে সংরক্ষণ করে; $days মেয়াদ (default 30)
function _generate_api_token(mysqli $conn, string $role, int $user_id, int $driver_id, int $manager_id, string $username, string $email, int $days = 30): string {
    $token   = bin2hex(random_bytes(32)); // 64-char hex token
    $expires = date('Y-m-d H:i:s', time() + $days * 24 * 3600);
    $stmt    = $conn->prepare(
        "INSERT INTO api_tokens (token, role, user_id, driver_id, manager_id, username, email, expires_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    );
    $stmt->bind_param('ssiiisss', $token, $role, $user_id, $driver_id, $manager_id, $username, $email, $expires);
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
$email       = trim($body['email'] ?? '');
$pass        = $body['password'] ?? '';
$is_mobile   = ($body['source'] ?? '') === 'mobile';
$remember_me = !empty($body['remember_me']);
$token_days  = $remember_me ? 90 : 30;

if (!$email || !$pass) {
    json_response(['success' => false, 'message' => 'ইমেইল এবং পাসওয়ার্ড দিন']);
}

$conn = (new Database())->connect();
$user = new User($conn);
$result = $user->login($email, $pass);

if ($result['success']) {
    if ($remember_me) _apply_remember_me();
    $resp = [
        'id'       => (int) $_SESSION['user_id'],
        'username' => $_SESSION['username'],
        'email'    => $_SESSION['email'],
        'role'     => $_SESSION['role'],
    ];
    if ($is_mobile) {
        $resp['token'] = _generate_api_token(
            $conn, $_SESSION['role'], (int)$_SESSION['user_id'], 0, 0,
            $_SESSION['username'], $_SESSION['email'], $token_days
        );
    }
    json_response(['success' => true, 'message' => 'লগইন সফল হয়েছে', 'data' => $resp]);
}

// Fallback: try drivers table
$dstmt = $conn->prepare("SELECT id, name, email, password, status FROM drivers WHERE email = ?");
$dstmt->bind_param('s', $email);
$dstmt->execute();
$driver_result = $dstmt->get_result();

if ($driver_result->num_rows === 1) {
    $driver = $driver_result->fetch_assoc();

    if ($driver['status'] === 'active' && password_verify($pass, $driver['password'])) {
        session_regenerate_id(true);
        $_SESSION['driver_id'] = (int)$driver['id'];
        $_SESSION['role']      = 'driver';
        $_SESSION['username']  = $driver['name'];
        $_SESSION['email']     = $driver['email'];

        if ($remember_me) _apply_remember_me();
        $resp = [
            'id'       => (int)$driver['id'],
            'username' => $driver['name'],
            'email'    => $driver['email'],
            'role'     => 'driver',
        ];
        if ($is_mobile) {
            $resp['token'] = _generate_api_token(
                $conn, 'driver', 0, (int)$driver['id'], 0,
                $driver['name'], $driver['email'], $token_days
            );
        }
        json_response(['success' => true, 'message' => 'লগইন সফল হয়েছে', 'data' => $resp]);
    } else {
        json_response(['success' => false, 'message' => 'অবৈধ পাসওয়ার্ড বা নিষ্ক্রিয় অ্যাকাউন্ট']);
    }
}

$dstmt->close();

// Fallback: try managers table
$mstmt = $conn->prepare("SELECT id, name, email, password, status FROM managers WHERE email = ?");
$mstmt->bind_param('s', $email);
$mstmt->execute();
$manager_result = $mstmt->get_result();

if ($manager_result->num_rows === 1) {
    $manager = $manager_result->fetch_assoc();

    if ($manager['status'] === 'active' && password_verify($pass, $manager['password'])) {
        session_regenerate_id(true);
        $_SESSION['manager_id'] = (int)$manager['id'];
        $_SESSION['role']       = 'manager';
        $_SESSION['username']   = $manager['name'];
        $_SESSION['email']      = $manager['email'];

        if ($remember_me) _apply_remember_me();
        $resp = [
            'id'       => (int)$manager['id'],
            'username' => $manager['name'],
            'email'    => $manager['email'],
            'role'     => 'manager',
        ];
        if ($is_mobile) {
            $resp['token'] = _generate_api_token(
                $conn, 'manager', 0, 0, (int)$manager['id'],
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
