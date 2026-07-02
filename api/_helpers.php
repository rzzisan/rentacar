<?php
function json_response(array $data, int $status = 200): void {
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit();
}

// _helpers.php-এর নিজস্ব lazy DB connection (token validation-এর জন্য)
function _helpers_db(): mysqli {
    static $conn = null;
    if ($conn === null) {
        require_once __DIR__ . '/../config/Database.php';
        $conn = (new Database())->connect();
    }
    return $conn;
}

// Authorization: Bearer <token> header থেকে session populate করে।
// Token অবৈধ বা মেয়াদোত্তীর্ণ হলে 401 দেয়।
// Token না থাকলে নিরবে ফেরত আসে (session-based auth চলবে)।
function _validate_bearer_token(): void {
    $headers = function_exists('getallheaders') ? (getallheaders() ?: []) : [];
    $auth    = $headers['Authorization'] ?? $headers['authorization'] ?? '';
    if (!preg_match('/^Bearer\s+(\S+)$/i', $auth, $m)) return;

    $token = $m[1];
    $conn  = _helpers_db();
    $stmt  = $conn->prepare(
        "SELECT role, tenant_id, user_id, driver_id, manager_id, username, email
         FROM api_tokens
         WHERE token = ? AND (expires_at IS NULL OR expires_at > NOW())"
    );
    $stmt->bind_param('s', $token);
    $stmt->execute();
    $row = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if (!$row) {
        json_response(['success' => false, 'message' => 'অবৈধ বা মেয়াদোত্তীর্ণ টোকেন'], 401);
    }

    // Session variable পুরণ করো — বাকি সব function তখন স্বাভাবিকভাবে কাজ করবে
    $_SESSION['role']     = $row['role'];
    $_SESSION['username'] = $row['username'];
    $_SESSION['email']    = $row['email'];
    if ($row['tenant_id'] !== null)  $_SESSION['tenant_id']  = (int)$row['tenant_id'];
    if ((int)$row['driver_id']  > 0) $_SESSION['driver_id']  = (int)$row['driver_id'];
    if ((int)$row['manager_id'] > 0) $_SESSION['manager_id'] = (int)$row['manager_id'];
    if ((int)$row['user_id']    > 0) $_SESSION['user_id']    = (int)$row['user_id'];
}

function require_auth(): void {
    _validate_bearer_token(); // token থাকলে session populate করে
    if (!isset($_SESSION['user_id']) && !isset($_SESSION['driver_id']) && !isset($_SESSION['manager_id'])) {
        json_response(['success' => false, 'message' => 'অনুমোদন নেই — আগে লগইন করুন'], 401);
    }
    // superadmin-এর tenant_id নেই; বাকি সব role tenant-scoped — প্রতি রিকোয়েস্টে suspended/cancelled চেক হয়
    // (শুধু login-এ চেক করলে আগে থেকে লগইন করা সেশন suspend হওয়ার পরেও চলতে থাকত)
    if (($_SESSION['role'] ?? '') !== 'superadmin') {
        require_active_tenant();
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

// বর্তমান সেশনের tenant_id — superadmin-এর tenant_id নেই, তাই 403 দেয়
function get_tenant_id(): int {
    if (!isset($_SESSION['tenant_id'])) {
        json_response(['success' => false, 'message' => 'Tenant পাওয়া যায়নি'], 403);
    }
    return (int)$_SESSION['tenant_id'];
}

function require_superadmin(): void {
    require_auth();
    if (($_SESSION['role'] ?? '') !== 'superadmin') {
        json_response(['success' => false, 'message' => 'এই কাজের অনুমতি নেই'], 403);
    }
}

// tenant suspended/cancelled হলে block করে — login-এর সময় ব্যবহার হয়
function require_active_tenant(): void {
    $tid  = get_tenant_id();
    $conn = _helpers_db();
    $stmt = $conn->prepare("SELECT status FROM tenants WHERE id = ?");
    $stmt->bind_param('i', $tid);
    $stmt->execute();
    $row = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if (!$row || in_array($row['status'], ['suspended', 'cancelled'], true)) {
        json_response([
            'success' => false,
            'message' => 'বকেয়া বিলের কারণে আপনার অ্যাকাউন্ট সাময়িকভাবে স্থগিত করা হয়েছে। পেমেন্ট সম্পন্ন করতে বা বিস্তারিত জানতে আমাদের সাথে যোগাযোগ করুন।',
        ], 403);
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
