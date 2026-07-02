<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('GET');
$tid = get_tenant_id();

$id = (int)($_GET['id'] ?? 0);
if (!$id) json_response(['success' => false, 'message' => 'id আবশ্যক'], 422);

$conn = (new Database())->connect();

// গ্রাহক তথ্য
$stmt = $conn->prepare(
    "SELECT c.*,
            COUNT(r.id)                                                    AS total_trips,
            SUM(CASE WHEN r.rental_status = 'completed' THEN 1 ELSE 0 END) AS completed_trips,
            SUM(CASE WHEN r.rental_status = 'completed'
                     THEN r.agreed_amount ELSE 0 END)                      AS total_spent,
            SUM(CASE WHEN r.payment_status IN ('pending','partial') THEN 1 ELSE 0 END) AS pending_payment_trips
     FROM customers c
     LEFT JOIN rentals r ON r.customer_id = c.id
     WHERE c.id = ? AND c.tenant_id = ?
     GROUP BY c.id"
);
$stmt->bind_param('ii', $id, $tid);
$stmt->execute();
$c = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$c) json_response(['success' => false, 'message' => 'গ্রাহক পাওয়া যায়নি'], 404);

// ট্রিপ ইতিহাস (সর্বশেষ ২০টি)
$rstmt = $conn->prepare(
    "SELECT r.id, r.start_date, r.end_date, r.actual_start_time, r.actual_end_time,
            r.pickup_location, r.dropoff_location, r.trip_type,
            r.agreed_amount, r.rental_status, r.payment_status,
            v.brand as vehicle_brand, v.model as vehicle_model,
            v.registration_number as vehicle_reg,
            d.name as driver_name
     FROM rentals r
     LEFT JOIN vehicles v ON r.vehicle_id = v.id
     LEFT JOIN drivers d ON r.driver_id = d.id
     WHERE r.customer_id = ?
     ORDER BY r.start_date DESC
     LIMIT 20"
);
$rstmt->bind_param('i', $id);
$rstmt->execute();
$rentals_raw = $rstmt->get_result()->fetch_all(MYSQLI_ASSOC);
$rstmt->close();

$rentals = array_map(function ($r) {
    return [
        'id'              => (int)$r['id'],
        'start_date'      => $r['start_date'],
        'end_date'        => $r['end_date'],
        'actual_start_time' => $r['actual_start_time'],
        'actual_end_time'   => $r['actual_end_time'],
        'pickup_location' => $r['pickup_location'],
        'dropoff_location'=> $r['dropoff_location'],
        'trip_type'       => $r['trip_type'],
        'agreed_amount'   => (float)$r['agreed_amount'],
        'rental_status'   => $r['rental_status'],
        'payment_status'  => $r['payment_status'],
        'vehicle_brand'   => $r['vehicle_brand'],
        'vehicle_model'   => $r['vehicle_model'],
        'vehicle_reg'     => $r['vehicle_reg'],
        'driver_name'     => $r['driver_name'],
    ];
}, $rentals_raw);

$data = [
    'customer' => [
        'id'             => (int)$c['id'],
        'first_name'     => $c['first_name'],
        'last_name'      => $c['last_name'],
        'phone'          => $c['phone'],
        'email'          => $c['email'],
        'nid'            => $c['nid'],
        'address'        => $c['address'],
        'city'           => $c['city'],
        'license_number' => $c['license_number'],
        'license_expiry' => $c['license_expiry'],
        'status'         => $c['status'],
        'created_at'     => $c['created_at'],
    ],
    'stats' => [
        'total_trips'          => (int)$c['total_trips'],
        'completed_trips'      => (int)$c['completed_trips'],
        'total_spent'          => (float)$c['total_spent'],
        'pending_payment_trips'=> (int)$c['pending_payment_trips'],
    ],
    'rentals' => $rentals,
];

json_response(['success' => true, 'data' => $data]);
