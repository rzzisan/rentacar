<?php
require_once '../../config/config.php';
require_once '../_helpers.php';

only_method('POST');

if (session_status() === PHP_SESSION_ACTIVE) {
    $params = session_get_cookie_params();
    setcookie(session_name(), '', time() - 42000,
        $params['path'], $params['domain'],
        $params['secure'], $params['httponly']
    );
    session_destroy();
}

json_response(['success' => true, 'message' => 'লগআউট সফল হয়েছে']);
