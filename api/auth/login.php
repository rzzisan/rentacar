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

        json_response([
            'success' => true,
            'message' => 'লগইন সফল হয়েছে',
            'data'    => [
                'id'       => (int)$driver['id'],
                'username' => $driver['name'],
                'email'    => $driver['email'],
                'role'     => 'driver',
            ],
        ]);
    } else {
        json_response(['success' => false, 'message' => 'অবৈধ পাসওয়ার্ড বা নিষ্ক্রিয় অ্যাকাউন্ট'], 401);
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

        json_response([
            'success' => true,
            'message' => 'লগইন সফল হয়েছে',
            'data'    => [
                'id'       => (int)$manager['id'],
                'username' => $manager['name'],
                'email'    => $manager['email'],
                'role'     => 'manager',
            ],
        ]);
    } else {
        json_response(['success' => false, 'message' => 'অবৈধ পাসওয়ার্ড বা নিষ্ক্রিয় অ্যাকাউন্ট'], 401);
    }
}

$mstmt->close();
json_response(['success' => false, 'message' => $result['message']], 401);
