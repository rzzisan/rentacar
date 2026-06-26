<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('POST');

// Bearer token থাকলে DB থেকে revoke করো
$headers = function_exists('getallheaders') ? (getallheaders() ?: []) : [];
$auth    = $headers['Authorization'] ?? $headers['authorization'] ?? '';
if (preg_match('/^Bearer\s+(\S+)$/i', $auth, $m)) {
    $conn  = (new Database())->connect();
    $stmt  = $conn->prepare("DELETE FROM api_tokens WHERE token = ?");
    $stmt->bind_param('s', $m[1]);
    $stmt->execute();
    $stmt->close();
}

// Session-based logout
if (session_status() === PHP_SESSION_ACTIVE) {
    $params = session_get_cookie_params();
    setcookie(session_name(), '', time() - 42000,
        $params['path'], $params['domain'],
        $params['secure'], $params['httponly']
    );
    session_destroy();
}

json_response(['success' => true, 'message' => 'লগআউট সফল হয়েছে']);
