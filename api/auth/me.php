<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');

// Bearer token থাকলে session populate করবে; না থাকলে session check করবে
require_auth();

// tenant_id দিয়ে tenants.status ও trial_ends_at পড়ে — superadmin (tenant_id null) হলে দুটোই null
function _tenant_info(mysqli $conn, ?int $tenant_id): array {
    if (!$tenant_id) return ['tenant_status' => null, 'trial_ends_at' => null];
    $stmt = $conn->prepare('SELECT status, trial_ends_at FROM tenants WHERE id = ?');
    $stmt->bind_param('i', $tenant_id);
    $stmt->execute();
    $row = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    return [
        'tenant_status' => $row['status'] ?? null,
        'trial_ends_at' => $row['trial_ends_at'] ?? null,
    ];
}

// Handle user session
if (isset($_SESSION['user_id'])) {
    $conn = (new Database())->connect();
    $tenant_id = isset($_SESSION['tenant_id']) ? (int) $_SESSION['tenant_id'] : null;

    json_response([
        'success' => true,
        'data' => array_merge([
            'id'        => (int) $_SESSION['user_id'],
            'tenant_id' => $tenant_id,
            'username'  => $_SESSION['username'],
            'email'     => $_SESSION['email'],
            'role'      => $_SESSION['role'],
        ], _tenant_info($conn, $tenant_id)),
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
            'data' => array_merge([
                'id'        => (int) $driver['id'],
                'tenant_id' => (int) $driver['tenant_id'],
                'username'  => $driver['name'],
                'email'     => $driver['email'],
                'role'      => 'driver',
            ], _tenant_info($conn, (int) $driver['tenant_id'])),
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
            'data' => array_merge([
                'id'        => (int) $manager['id'],
                'tenant_id' => (int) $manager['tenant_id'],
                'username'  => $manager['name'],
                'email'     => $manager['email'],
                'role'      => 'manager',
            ], _tenant_info($conn, (int) $manager['tenant_id'])),
        ]);
    }
    $stmt->close();
}

json_response(['success' => false, 'message' => 'লগইন করুন'], 401);
