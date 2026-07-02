<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
$tid = get_tenant_id();
$conn = (new Database())->connect();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $vehicle_id = isset($_GET['vehicle_id']) ? (int)$_GET['vehicle_id'] : 0;
    $status     = $_GET['status'] ?? '';

    $where  = ['m.tenant_id = ?'];
    $params = [$tid];
    $types  = 'i';

    if ($vehicle_id) {
        $where[] = 'm.vehicle_id = ?';
        $params[] = $vehicle_id;
        $types   .= 'i';
    }
    if ($status) {
        $where[] = 'm.status = ?';
        $params[] = $status;
        $types   .= 's';
    }

    $sql = "SELECT m.*, v.brand, v.model, v.registration_number
            FROM maintenance m
            JOIN vehicles v ON v.id = m.vehicle_id"
         . ($where ? ' WHERE ' . implode(' AND ', $where) : '')
         . " ORDER BY m.start_date DESC LIMIT 200";

    $stmt = $conn->prepare($sql);
    if ($types) $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    $data = array_map(fn($r) => [
        'id'                  => (int)$r['id'],
        'vehicle_id'          => (int)$r['vehicle_id'],
        'vehicle'             => $r['brand'] . ' ' . $r['model'],
        'registration_number' => $r['registration_number'],
        'maintenance_type'    => $r['maintenance_type'],
        'description'         => $r['description'],
        'vendor'              => $r['vendor'],
        'start_date'          => $r['start_date'],
        'end_date'            => $r['end_date'],
        'km_reading'          => $r['km_reading'] ? (int)$r['km_reading'] : null,
        'next_due_date'       => $r['next_due_date'],
        'next_due_km'         => $r['next_due_km'] ? (int)$r['next_due_km'] : null,
        'cost'                => $r['cost'] ? (float)$r['cost'] : null,
        'status'              => $r['status'],
        'created_at'          => $r['created_at'],
    ], $rows);

    json_response(['success' => true, 'data' => $data]);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = input();

    $vehicle_id   = (int)($data['vehicle_id'] ?? 0);
    $type         = trim($data['maintenance_type'] ?? '');
    $desc         = trim($data['description'] ?? '');
    $vendor       = trim($data['vendor'] ?? '');
    $start_date   = trim($data['start_date'] ?? '');
    $end_date     = $data['end_date'] ? trim($data['end_date']) : null;
    $km           = $data['km_reading'] ? (int)$data['km_reading'] : null;
    $next_date    = $data['next_due_date'] ? trim($data['next_due_date']) : null;
    $next_km      = $data['next_due_km'] ? (int)$data['next_due_km'] : null;
    $cost         = $data['cost'] ? (float)$data['cost'] : null;
    $status       = trim($data['status'] ?? 'pending');

    if (!$vehicle_id || !$type || !$start_date)
        json_response(['success' => false, 'message' => 'গাড়ি, ধরন ও তারিখ আবশ্যক'], 422);

    // গাড়ি এই tenant-এর কিনা যাচাই — IDOR প্রতিরোধ
    $vchk = $conn->prepare("SELECT id FROM vehicles WHERE id = ? AND tenant_id = ?");
    $vchk->bind_param('ii', $vehicle_id, $tid);
    $vchk->execute();
    if ($vchk->get_result()->num_rows === 0) {
        json_response(['success' => false, 'message' => 'গাড়ি পাওয়া যায়নি'], 404);
    }
    $vchk->close();

    $stmt = $conn->prepare(
        "INSERT INTO maintenance
            (tenant_id, vehicle_id, maintenance_type, description, vendor, start_date, end_date,
             km_reading, next_due_date, next_due_km, cost, status)
         VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
    );
    $stmt->bind_param('iisssssisiss', $tid, $vehicle_id, $type, $desc, $vendor, $start_date,
        $end_date, $km, $next_date, $next_km, $cost, $status);
    $stmt->execute();
    $new_id = $stmt->insert_id;
    $stmt->close();

    json_response(['success' => true, 'data' => ['id' => $new_id], 'message' => 'রক্ষণাবেক্ষণ রেকর্ড যোগ হয়েছে']);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
