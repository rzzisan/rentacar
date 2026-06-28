<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
only_method('GET');

$conn = (new Database())->connect();
$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

// Manager শুধু তার assigned গাড়িতে ট্রিপ করা গ্রাহকদের দেখতে পারবে
$where  = ["c.id IN (SELECT DISTINCT customer_id FROM rentals WHERE vehicle_id IN $in)"];
$params = $vids;
$types  = str_repeat('i', count($vids));

if (!empty($_GET['search'])) {
    $s       = '%' . $_GET['search'] . '%';
    $where[] = '(c.first_name LIKE ? OR c.last_name LIKE ? OR c.phone LIKE ?)';
    $params  = array_merge($params, [$s, $s, $s]);
    $types  .= 'sss';
}

if (!empty($_GET['status'])) {
    $where[] = 'c.status = ?';
    $params[] = $_GET['status'];
    $types   .= 's';
}

$sql = "SELECT c.id, c.first_name, c.last_name, c.phone, c.email,
               c.nid, c.address, c.city, c.license_number, c.license_expiry,
               c.status, c.created_at,
               COUNT(r.id)                                                    AS total_trips,
               SUM(CASE WHEN r.rental_status = 'completed' THEN 1 ELSE 0 END) AS completed_trips,
               SUM(CASE WHEN r.rental_status = 'completed' AND r.vehicle_id IN $in
                        THEN r.agreed_amount ELSE 0 END)                      AS total_spent,
               MAX(r.start_date)                                               AS last_trip_date
        FROM customers c
        LEFT JOIN rentals r ON r.customer_id = c.id
        WHERE " . implode(' AND ', $where) . "
        GROUP BY c.id
        ORDER BY last_trip_date DESC";

$stmt = $conn->prepare($sql);
if ($params) {
    $stmt->bind_param($types, ...$params);
}
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

$customers = array_map(function ($r) {
    return [
        'id'              => (int)$r['id'],
        'first_name'      => $r['first_name'],
        'last_name'       => $r['last_name'],
        'phone'           => $r['phone'],
        'email'           => $r['email'],
        'nid'             => $r['nid'],
        'address'         => $r['address'],
        'city'            => $r['city'],
        'license_number'  => $r['license_number'],
        'license_expiry'  => $r['license_expiry'],
        'status'          => $r['status'],
        'created_at'      => $r['created_at'],
        'total_trips'     => (int)$r['total_trips'],
        'completed_trips' => (int)$r['completed_trips'],
        'total_spent'     => (float)$r['total_spent'],
        'last_trip_date'  => $r['last_trip_date'],
    ];
}, $rows);

json_response(['success' => true, 'data' => $customers]);
