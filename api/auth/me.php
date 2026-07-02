<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');

// Bearer token থাকলে session populate করবে; না থাকলে session check করবে
require_auth();

// Handle user session
if (isset($_SESSION['user_id'])) {
    json_response([
        'success' => true,
        'data' => [
            'id'        => (int) $_SESSION['user_id'],
            'tenant_id' => isset($_SESSION['tenant_id']) ? (int) $_SESSION['tenant_id'] : null,
            'username'  => $_SESSION['username'],
            'email'     => $_SESSION['email'],
            'role'      => $_SESSION['role'],
        ],
    ]);
}

// Handle driver session
if (isset($_SESSION['driver_id'])) {
    $conn = (new Database())->connect();
    $stmt = $conn->prepare("SELECT id, tenant_id, name, email, status FROM drivers WHERE id = ?");
    $stmt->bind_param('i', $_SESSION['driver_id']);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows === 1) {
        $driver = $result->fetch_assoc();
        $stmt->close();

        json_response([
            'success' => true,
            'data' => [
                'id'        => (int) $driver['id'],
                'tenant_id' => (int) $driver['tenant_id'],
                'username'  => $driver['name'],
                'email'     => $driver['email'],
                'role'      => 'driver',
            ],
        ]);
    }
    $stmt->close();
}

// Handle manager session
if (isset($_SESSION['manager_id'])) {
    $conn = (new Database())->connect();
    $stmt = $conn->prepare("SELECT id, tenant_id, name, email, status FROM managers WHERE id = ?");
    $stmt->bind_param('i', $_SESSION['manager_id']);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows === 1) {
        $manager = $result->fetch_assoc();
        $stmt->close();

        json_response([
            'success' => true,
            'data' => [
                'id'        => (int) $manager['id'],
                'tenant_id' => (int) $manager['tenant_id'],
                'username'  => $manager['name'],
                'email'     => $manager['email'],
                'role'      => 'manager',
            ],
        ]);
    }
    $stmt->close();
}

json_response(['success' => false, 'message' => 'লগইন করুন'], 401);
