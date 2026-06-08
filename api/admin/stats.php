<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');
require_role('admin');

$conn = (new Database())->connect();

$q = fn(string $sql) => $conn->query($sql)->fetch_assoc();

$stats = [
    'total_vehicles'    => (int) $q("SELECT COUNT(*) c FROM vehicles")['c'],
    'available_vehicles'=> (int) $q("SELECT COUNT(*) c FROM vehicles WHERE status='available'")['c'],
    'active_rentals'    => (int) $q("SELECT COUNT(*) c FROM rentals WHERE rental_status='active'")['c'],
    'pending_rentals'   => (int) $q("SELECT COUNT(*) c FROM rentals WHERE rental_status='pending'")['c'],
    'total_customers'   => (int) $q("SELECT COUNT(*) c FROM customers")['c'],
    'monthly_revenue'   => (float) $q(
        "SELECT COALESCE(SUM(total_amount),0) r FROM rentals
         WHERE rental_status != 'cancelled'
           AND MONTH(created_at)=MONTH(CURDATE())
           AND YEAR(created_at)=YEAR(CURDATE())"
    )['r'],
];

json_response(['success' => true, 'data' => $stats]);
