<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
$tid = get_tenant_id();
only_method('GET');

$conn = (new Database())->connect();
$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);
$t    = str_repeat('i', count($vids));

// Manager শুধু তার assigned গাড়িতে ট্রিপ করা গ্রাহকদের দেখতে পারবে
$where  = ["c.tenant_id = ?", "c.id IN (SELECT DISTINCT customer_id FROM rentals WHERE vehicle_id IN $in)"];
$extra_where  = [];
$extra_params = [];
$extra_types  = '';

if (!empty($_GET['search'])) {
    $s               = '%' . $_GET['search'] . '%';
    $extra_where[]   = '(c.first_name LIKE ? OR c.last_name LIKE ? OR c.phone LIKE ?)';
    $extra_params    = array_merge($extra_params, [$s, $s, $s]);
    $extra_types    .= 'sss';
}

if (!empty($_GET['status'])) {
    $extra_where[]   = 'c.status = ?';
    $extra_params[]  = $_GET['status'];
    $extra_types    .= 's';
}

$where = array_merge($where, $extra_where);

// $in একই কোয়েরিতে ২ বার ব্যবহৃত হয় (SELECT-এর SUM CASE + WHERE-এর সাবকোয়েরি) —
// bind_param-এ params SQL টেক্সটে placeholder-এর ক্রম অনুযায়ী দিতে হবে:
// প্রথমে SELECT-list-এর $in, তারপর tenant_id, তারপর WHERE-এর $in, তারপর extra filters
$params = array_merge($vids, [$tid], $vids, $extra_params);
$types  = $t . 'i' . $t . $extra_types;

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
