<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');
$manager_id = require_manager();
$tid = get_tenant_id();
$conn = (new Database())->connect();

$vids = get_manager_vehicle_ids($conn, $manager_id);
if (!$vids) {
    json_response(['success' => true, 'data' => [
        'total_vehicles' => 0, 'available_vehicles' => 0,
        'active_rentals' => 0, 'pending_rentals' => 0,
        'monthly_revenue' => 0.0, 'total_dues' => 0.0, 'today_trips' => 0,
        'active_trips' => [], 'upcoming_trips' => [],
    ]]);
}

$in   = manager_vehicle_in_clause($vids);
$t    = str_repeat('i', count($vids));

$q = function (string $sql, array $params = [], string $types = '') use ($conn): array {
    $stmt = $conn->prepare($sql);
    if ($params) $stmt->bind_param($types, ...$params);
    $stmt->execute();
    return $stmt->get_result()->fetch_assoc() ?? [];
};

$vt = $t . 'i';
$vp = array_merge($vids, [$tid]);

$stats = [
    'total_vehicles'     => (int)($q("SELECT COUNT(*) c FROM vehicles WHERE id IN $in AND tenant_id = ?", $vp, $vt)['c'] ?? 0),
    'available_vehicles' => (int)($q("SELECT COUNT(*) c FROM vehicles WHERE id IN $in AND tenant_id = ? AND status='available'", $vp, $vt)['c'] ?? 0),
    'active_rentals'     => (int)($q("SELECT COUNT(*) c FROM rentals WHERE vehicle_id IN $in AND tenant_id = ? AND rental_status='active'", $vp, $vt)['c'] ?? 0),
    'pending_rentals'    => (int)($q("SELECT COUNT(*) c FROM rentals WHERE vehicle_id IN $in AND tenant_id = ? AND rental_status='pending'", $vp, $vt)['c'] ?? 0),
    'monthly_revenue'    => (float)($q(
        "SELECT COALESCE(SUM(CASE WHEN agreed_amount > 0 THEN agreed_amount ELSE COALESCE(total_amount,0) END),0) r
         FROM rentals WHERE vehicle_id IN $in AND tenant_id = ? AND rental_status != 'cancelled'
           AND MONTH(created_at)=MONTH(CURDATE()) AND YEAR(created_at)=YEAR(CURDATE())",
        $vp, $vt
    )['r'] ?? 0),
    'total_dues'         => (float)($q(
        "SELECT COALESCE(SUM(s.remaining_amount),0) d FROM settlements s
         JOIN rentals r ON s.rental_id = r.id
         WHERE r.vehicle_id IN $in AND r.tenant_id = ? AND s.payment_status != 'paid'",
        $vp, $vt
    )['d'] ?? 0),
    'today_trips'        => (int)($q(
        "SELECT COUNT(*) c FROM rentals WHERE vehicle_id IN $in AND tenant_id = ?
         AND DATE(start_date)=CURDATE() AND rental_status != 'cancelled'",
        $vp, $vt
    )['c'] ?? 0),
];

$trip_fields = "SELECT r.id, r.customer_id, r.vehicle_id, r.driver_id,
        r.start_date, r.actual_start_time, r.pickup_location, r.dropoff_location,
        r.trip_type, r.agreed_amount, r.rental_status,
        c.first_name as customer_first_name, c.last_name as customer_last_name, c.phone as customer_phone,
        v.brand as vehicle_brand, v.model as vehicle_model, v.registration_number as vehicle_registration_number,
        d.name as driver_name
        FROM rentals r
        LEFT JOIN customers c ON r.customer_id = c.id
        LEFT JOIN vehicles v ON r.vehicle_id = v.id
        LEFT JOIN drivers d ON r.driver_id = d.id
        WHERE r.vehicle_id IN $in AND r.tenant_id = ?";

$cast_trips = function (array $rows): array {
    foreach ($rows as &$row) {
        $row['id']            = (int)$row['id'];
        $row['customer_id']   = (int)$row['customer_id'];
        $row['vehicle_id']    = (int)$row['vehicle_id'];
        $row['driver_id']     = $row['driver_id'] ? (int)$row['driver_id'] : null;
        $row['agreed_amount'] = (float)$row['agreed_amount'];
    }
    return $rows;
};

$astmt = $conn->prepare("$trip_fields AND r.rental_status = 'active' ORDER BY COALESCE(r.actual_start_time, r.start_date) ASC");
$astmt->bind_param($vt, ...$vp);
$astmt->execute();
$active_trips = $astmt->get_result()->fetch_all(MYSQLI_ASSOC);
$astmt->close();

$pstmt = $conn->prepare("$trip_fields AND r.rental_status = 'pending' ORDER BY r.start_date ASC");
$pstmt->bind_param($vt, ...$vp);
$pstmt->execute();
$upcoming_trips = $pstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$pstmt->close();

$stats['active_trips']   = $cast_trips($active_trips);
$stats['upcoming_trips'] = $cast_trips($upcoming_trips);

json_response(['success' => true, 'data' => $stats]);
