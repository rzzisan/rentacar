<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('GET');
$manager_id = require_manager();
$conn = (new Database())->connect();

$vids = get_manager_vehicle_ids($conn, $manager_id);
if (!$vids) {
    json_response(['success' => true, 'data' => []]);
}

$in = manager_vehicle_in_clause($vids);
$t  = str_repeat('i', count($vids));

$stmt = $conn->prepare("SELECT * FROM vehicles WHERE id IN $in ORDER BY brand, model");
$stmt->bind_param($t, ...$vids);
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

foreach ($rows as &$row) {
    $row['id']               = (int)$row['id'];
    $row['seating_capacity'] = (int)$row['seating_capacity'];
    $row['daily_rent_price'] = (float)$row['daily_rent_price'];
}

json_response(['success' => true, 'data' => $rows]);
