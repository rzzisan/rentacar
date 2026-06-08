<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../../includes/User.php';
require_once '../_helpers.php';

only_method('POST');

$body  = input();
$email = trim($body['email'] ?? '');
$pass  = $body['password'] ?? '';

if (!$email || !$pass) {
    json_response(['success' => false, 'message' => 'ইমেইল এবং পাসওয়ার্ড দিন']);
}

$conn = (new Database())->connect();
$user = new User($conn);
$result = $user->login($email, $pass);

if ($result['success']) {
    json_response([
        'success' => true,
        'message' => 'লগইন সফল হয়েছে',
        'data'    => [
            'id'       => (int) $_SESSION['user_id'],
            'username' => $_SESSION['username'],
            'email'    => $_SESSION['email'],
            'role'     => $_SESSION['role'],
        ],
    ]);
}

json_response(['success' => false, 'message' => $result['message']], 401);
