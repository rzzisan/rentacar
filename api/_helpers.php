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

function require_driver(): int {
    require_auth();
    if (($_SESSION['role'] ?? '') !== 'driver' || !isset($_SESSION['driver_id'])) {
        json_response(['success' => false, 'message' => 'এই কাজের অনুমতি নেই'], 403);
    }
    return (int)$_SESSION['driver_id'];
}

function require_manager(): int {
    require_auth();
    if (($_SESSION['role'] ?? '') !== 'manager' || !isset($_SESSION['manager_id'])) {
        json_response(['success' => false, 'message' => 'এই কাজের অনুমতি নেই'], 403);
    }
    return (int)$_SESSION['manager_id'];
}

function require_admin_or_manager(): array {
    require_auth();
    $role = $_SESSION['role'] ?? '';
    if ($role === 'admin') {
        return ['role' => 'admin', 'id' => null];
    }
    if ($role === 'manager' && isset($_SESSION['manager_id'])) {
        return ['role' => 'manager', 'id' => (int)$_SESSION['manager_id']];
    }
    json_response(['success' => false, 'message' => 'এই কাজের অনুমতি নেই'], 403);
}

function get_manager_vehicle_ids(mysqli $conn, int $manager_id): array {
    $stmt = $conn->prepare("SELECT vehicle_id FROM manager_vehicles WHERE manager_id = ?");
    $stmt->bind_param('i', $manager_id);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();
    return array_map('intval', array_column($rows, 'vehicle_id'));
}

function manager_vehicle_in_clause(array $vids): string {
    if (!$vids) return '(0)';
    return '(' . implode(',', array_fill(0, count($vids), '?')) . ')';
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

// সম্পন্ন রেন্টালের জন্য সেটেলমেন্ট তৈরি করে (idempotent — আগে থাকলে নতুন তৈরি হয় না)।
// রিটার্ন: ['id' => settlement_id, 'created' => bool] অথবা null (রেন্টাল completed নয়/পাওয়া যায়নি/insert ব্যর্থ)
function create_settlement_for_rental(mysqli $conn, int $rental_id): ?array {
    $rstmt = $conn->prepare(
        "SELECT r.driver_id, r.agreed_amount, d.commission_rate
         FROM rentals r
         LEFT JOIN drivers d ON r.driver_id = d.id
         WHERE r.id = ? AND r.rental_status = 'completed'"
    );
    $rstmt->bind_param('i', $rental_id);
    $rstmt->execute();
    $rental_result = $rstmt->get_result();

    if ($rental_result->num_rows === 0) {
        $rstmt->close();
        return null;
    }
    $rental = $rental_result->fetch_assoc();
    $rstmt->close();

    // আগে থেকে সেটেলমেন্ট থাকলে সেটাই ফেরত দাও
    $cstmt = $conn->prepare("SELECT id FROM settlements WHERE rental_id = ?");
    $cstmt->bind_param('i', $rental_id);
    $cstmt->execute();
    $existing = $cstmt->get_result()->fetch_assoc();
    $cstmt->close();
    if ($existing) {
        return ['id' => (int)$existing['id'], 'created' => false];
    }

    // মোট খরচ
    $estmt = $conn->prepare("SELECT SUM(amount) as total FROM trip_expenses WHERE rental_id = ?");
    $estmt->bind_param('i', $rental_id);
    $estmt->execute();
    $expense_row = $estmt->get_result()->fetch_assoc();
    $total_expenses = $expense_row['total'] ? (float)$expense_row['total'] : 0;
    $estmt->close();

    // হিসাব: net = চুক্তি − খরচ, কমিশন net-এর %, সংগ্রহযোগ্য = net − কমিশন
    $agreed_amount      = (float)$rental['agreed_amount'];
    $net_amount         = $agreed_amount - $total_expenses;
    $commission_percent = $rental['commission_rate'] ? (float)$rental['commission_rate'] : 0;
    $driver_commission  = ($net_amount > 0) ? ($net_amount * $commission_percent / 100) : 0;
    $amount_to_collect  = $net_amount - $driver_commission;

    // remaining_amount শুরুতে পুরো সংগ্রহযোগ্য পরিমাণ
    $istmt = $conn->prepare(
        "INSERT INTO settlements
         (rental_id, driver_id, agreed_amount, total_expenses, net_amount,
          commission_percent, driver_commission, amount_to_collect,
          paid_amount, remaining_amount, payment_status)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, 'pending')"
    );
    $istmt->bind_param(
        'iiddddddd',
        $rental_id,
        $rental['driver_id'],
        $agreed_amount,
        $total_expenses,
        $net_amount,
        $commission_percent,
        $driver_commission,
        $amount_to_collect,
        $amount_to_collect
    );

    if (!$istmt->execute()) {
        $istmt->close();
        return null;
    }
    $settlement_id = $conn->insert_id;
    $istmt->close();

    return ['id' => $settlement_id, 'created' => true];
}
