<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');

// Handle user session
if (isset($_SESSION['user_id'])) {
    json_response([
        'success' => true,
        'data' => [
            'id'       => (int) $_SESSION['user_id'],
            'username' => $_SESSION['username'],
            'email'    => $_SESSION['email'],
            'role'     => $_SESSION['role'],
        ],
    ]);
}

// Handle driver session
if (isset($_SESSION['driver_id'])) {
    $conn = (new Database())->connect();
    $stmt = $conn->prepare("SELECT id, name, email, status FROM drivers WHERE id = ?");
    $stmt->bind_param('i', $_SESSION['driver_id']);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows === 1) {
        $driver = $result->fetch_assoc();
        $stmt->close();

        json_response([
            'success' => true,
            'data' => [
                'id'       => (int) $driver['id'],
                'username' => $driver['name'],
                'email'    => $driver['email'],
                'role'     => 'driver',
            ],
        ]);
    }
    $stmt->close();
}

json_response(['success' => false, 'message' => 'লগইন করুন'], 401);
