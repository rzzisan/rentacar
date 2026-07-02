<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');
require_role('admin');
$tid = get_tenant_id();

$conn = (new Database())->connect();

$q = fn(string $sql) => $conn->query($sql)->fetch_assoc();

$stats = [
    'total_vehicles'    => (int) $q("SELECT COUNT(*) c FROM vehicles WHERE tenant_id = $tid")['c'],
    'available_vehicles'=> (int) $q("SELECT COUNT(*) c FROM vehicles WHERE tenant_id = $tid AND status='available'")['c'],
    'active_rentals'    => (int) $q("SELECT COUNT(*) c FROM rentals WHERE tenant_id = $tid AND rental_status='active'")['c'],
    'pending_rentals'   => (int) $q("SELECT COUNT(*) c FROM rentals WHERE tenant_id = $tid AND rental_status='pending'")['c'],
    'total_customers'   => (int) $q("SELECT COUNT(*) c FROM customers WHERE tenant_id = $tid")['c'],
    // ট্রিপ-ভিত্তিক রেন্টালে agreed_amount-ই মূল অঙ্ক; পুরনো রেন্টালে total_amount
    'monthly_revenue'   => (float) $q(
        "SELECT COALESCE(SUM(CASE WHEN agreed_amount > 0 THEN agreed_amount
                                  ELSE COALESCE(total_amount, 0) END), 0) r
         FROM rentals
         WHERE tenant_id = $tid AND rental_status != 'cancelled'
           AND MONTH(created_at)=MONTH(CURDATE())
           AND YEAR(created_at)=YEAR(CURDATE())"
    )['r'],
    // অসংগৃহীত সেটেলমেন্ট বকেয়া (settlements-এ tenant_id নেই, rentals জয়েন করে ফিল্টার)
    'total_dues'        => (float) $q(
        "SELECT COALESCE(SUM(s.remaining_amount), 0) d FROM settlements s
         JOIN rentals r ON r.id = s.rental_id
         WHERE r.tenant_id = $tid AND s.payment_status != 'paid'"
    )['d'],
    // আজ শুরু হওয়ার কথা এমন ট্রিপ
    'today_trips'       => (int) $q(
        "SELECT COUNT(*) c FROM rentals
         WHERE tenant_id = $tid AND DATE(start_date) = CURDATE() AND rental_status != 'cancelled'"
    )['c'],
];

// চলমান ও আপকামিং ট্রিপের তালিকা (ড্যাশবোর্ড হাইলাইট কার্ডের জন্য)
$trip_fields = "SELECT r.id, r.customer_id, r.vehicle_id, r.driver_id,
        r.start_date, r.actual_start_time, r.pickup_location, r.dropoff_location,
        r.trip_type, r.agreed_amount, r.rental_status,
        c.first_name as customer_first_name,
        c.last_name as customer_last_name,
        c.phone as customer_phone,
        v.brand as vehicle_brand,
        v.model as vehicle_model,
        v.registration_number as vehicle_registration_number,
        d.name as driver_name
        FROM rentals r
        LEFT JOIN customers c ON r.customer_id = c.id
        LEFT JOIN vehicles v ON r.vehicle_id = v.id
        LEFT JOIN drivers d ON r.driver_id = d.id";

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

$active_trips = $conn->query(
    "$trip_fields WHERE r.tenant_id = $tid AND r.rental_status = 'active'
     ORDER BY COALESCE(r.actual_start_time, r.start_date) ASC"
)->fetch_all(MYSQLI_ASSOC);

$upcoming_trips = $conn->query(
    "$trip_fields WHERE r.tenant_id = $tid AND r.rental_status = 'pending'
     ORDER BY r.start_date ASC"
)->fetch_all(MYSQLI_ASSOC);

$stats['active_trips']   = $cast_trips($active_trips);
$stats['upcoming_trips'] = $cast_trips($upcoming_trips);

json_response(['success' => true, 'data' => $stats]);
