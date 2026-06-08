<?php
require_once '../../config/config.php';
require_once '../_helpers.php';

only_method('GET');

if (!isset($_SESSION['user_id'])) {
    json_response(['success' => false, 'message' => 'লগইন করুন'], 401);
}

json_response([
    'success' => true,
    'data' => [
        'id'       => (int) $_SESSION['user_id'],
        'username' => $_SESSION['username'],
        'email'    => $_SESSION['email'],
        'role'     => $_SESSION['role'],
    ],
]);
