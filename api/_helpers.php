<?php
function json_response(array $data, int $status = 200): void {
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit();
}

function require_auth(): void {
    if (!isset($_SESSION['user_id']) && !isset($_SESSION['driver_id'])) {
        json_response(['success' => false, 'message' => 'অনুমোদন নেই — আগে লগইন করুন'], 401);
    }
}

function require_role(string $role): void {
    require_auth();
    if ($_SESSION['role'] !== $role) {
        json_response(['success' => false, 'message' => 'এই কাজের অনুমতি নেই'], 403);
    }
}

function input(): array {
    $raw = file_get_contents('php://input');
    return json_decode($raw, true) ?? [];
}

function only_method(string ...$methods): void {
    if (!in_array($_SERVER['REQUEST_METHOD'], $methods, true)) {
        json_response(['success' => false, 'message' => 'Method not allowed'], 405);
    }
}
