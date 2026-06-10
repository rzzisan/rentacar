<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('GET');

$conn = (new Database())->connect();
$driver_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

// ── GET ?id=N: single driver detail (outstanding settlements + collection history) ──
if ($driver_id) {
    $dstmt = $conn->prepare("SELECT id, name, mobile, status, commission_rate FROM drivers WHERE id = ?");
    $dstmt->bind_param('i', $driver_id);
    $dstmt->execute();
    $driver = $dstmt->get_result()->fetch_assoc();
    $dstmt->close();

    if (!$driver) {
        json_response(['success' => false, 'message' => 'ড্রাইভার পাওয়া যায়নি'], 404);
    }

    $driver['id'] = (int)$driver['id'];
    $driver['commission_rate'] = (float)$driver['commission_rate'];

    // Outstanding settlements (oldest trip first — collection order)
    $sstmt = $conn->prepare(
        "SELECT s.id, s.rental_id, s.agreed_amount, s.total_expenses, s.net_amount,
                s.driver_commission, s.amount_to_collect, s.paid_amount, s.remaining_amount,
                s.payment_status, s.created_at,
                r.start_date as rental_start_date,
                r.pickup_location, r.dropoff_location,
                c.first_name as customer_first_name, c.last_name as customer_last_name,
                v.brand as vehicle_brand, v.model as vehicle_model,
                v.registration_number as vehicle_registration_number
         FROM settlements s
         LEFT JOIN rentals r ON s.rental_id = r.id
         LEFT JOIN customers c ON r.customer_id = c.id
         LEFT JOIN vehicles v ON r.vehicle_id = v.id
         WHERE s.driver_id = ? AND s.remaining_amount > 0
           AND s.payment_status IN ('pending', 'partial')
         ORDER BY COALESCE(r.start_date, s.created_at) ASC, s.id ASC"
    );
    $sstmt->bind_param('i', $driver_id);
    $sstmt->execute();
    $due_settlements = $sstmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $sstmt->close();

    $total_due = 0;
    foreach ($due_settlements as &$s) {
        $s['id'] = (int)$s['id'];
        $s['rental_id'] = (int)$s['rental_id'];
        $s['agreed_amount'] = (float)$s['agreed_amount'];
        $s['total_expenses'] = (float)$s['total_expenses'];
        $s['net_amount'] = (float)$s['net_amount'];
        $s['driver_commission'] = (float)$s['driver_commission'];
        $s['amount_to_collect'] = (float)$s['amount_to_collect'];
        $s['paid_amount'] = (float)$s['paid_amount'];
        $s['remaining_amount'] = (float)$s['remaining_amount'];
        $total_due += $s['remaining_amount'];
    }
    unset($s);

    // Collection history of this driver (across all settlements)
    $hstmt = $conn->prepare(
        "SELECT sp.id, sp.settlement_id, sp.amount, sp.payment_method,
                sp.notes as payment_notes, sp.payment_date,
                u.username as recorded_by_name,
                r.start_date as rental_start_date,
                c.first_name as customer_first_name, c.last_name as customer_last_name,
                v.brand as vehicle_brand, v.model as vehicle_model
         FROM settlement_payments sp
         LEFT JOIN users u ON sp.recorded_by = u.id
         LEFT JOIN settlements s ON sp.settlement_id = s.id
         LEFT JOIN rentals r ON s.rental_id = r.id
         LEFT JOIN customers c ON r.customer_id = c.id
         LEFT JOIN vehicles v ON r.vehicle_id = v.id
         WHERE sp.paid_by_driver_id = ?
         ORDER BY sp.payment_date DESC, sp.id DESC
         LIMIT 100"
    );
    $hstmt->bind_param('i', $driver_id);
    $hstmt->execute();
    $collections = $hstmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $hstmt->close();

    $total_collected = 0;
    foreach ($collections as &$p) {
        $p['id'] = (int)$p['id'];
        $p['settlement_id'] = (int)$p['settlement_id'];
        $p['amount'] = (float)$p['amount'];
        $total_collected += $p['amount'];
    }
    unset($p);

    json_response([
        'success' => true,
        'data' => [
            'driver' => $driver,
            'total_due' => round($total_due, 2),
            'due_settlements' => $due_settlements,
            'total_collected' => round($total_collected, 2),
            'collections' => $collections,
        ],
    ]);
}

// ── GET (no id): all drivers with aggregated dues ──────────────────
$sql = "SELECT d.id, d.name, d.mobile, d.status, d.commission_rate,
        COUNT(s.id) AS settlement_count,
        COALESCE(SUM(s.amount_to_collect), 0) AS total_to_collect,
        COALESCE(SUM(s.paid_amount), 0) AS total_paid,
        COALESCE(SUM(s.remaining_amount), 0) AS total_due,
        SUM(CASE WHEN s.remaining_amount > 0 AND s.payment_status IN ('pending','partial') THEN 1 ELSE 0 END) AS due_settlement_count,
        (SELECT MAX(sp.payment_date) FROM settlement_payments sp WHERE sp.paid_by_driver_id = d.id) AS last_payment_date
        FROM drivers d
        LEFT JOIN settlements s ON s.driver_id = d.id
        GROUP BY d.id, d.name, d.mobile, d.status, d.commission_rate
        ORDER BY total_due DESC, d.name ASC";

$result = $conn->query($sql);
$rows = $result->fetch_all(MYSQLI_ASSOC);

$grand_total_due = 0;
$grand_total_paid = 0;
foreach ($rows as &$row) {
    $row['id'] = (int)$row['id'];
    $row['commission_rate'] = (float)$row['commission_rate'];
    $row['settlement_count'] = (int)$row['settlement_count'];
    $row['due_settlement_count'] = (int)$row['due_settlement_count'];
    $row['total_to_collect'] = (float)$row['total_to_collect'];
    $row['total_paid'] = (float)$row['total_paid'];
    $row['total_due'] = (float)$row['total_due'];
    $grand_total_due += $row['total_due'];
    $grand_total_paid += $row['total_paid'];
}
unset($row);

json_response([
    'success' => true,
    'data' => [
        'drivers' => $rows,
        'summary' => [
            'grand_total_due' => round($grand_total_due, 2),
            'grand_total_paid' => round($grand_total_paid, 2),
            'drivers_with_due' => count(array_filter($rows, fn($r) => $r['total_due'] > 0)),
        ],
    ],
]);
?>
