<?php
require_once '../../config/config.php';
require_once '../../config/Database.php';
require_once '../_helpers.php';

only_method('PUT');
require_role('admin');

$id = (int) ($_GET['id'] ?? 0);
if (!$id) {
    json_response(['success' => false, 'message' => 'ID দিন'], 400);
}

$b = input();
$allowed = ['brand', 'model', 'year', 'vehicle_type', 'color', 'fuel_type',
            'seating_capacity', 'mileage', 'daily_rent_price', 'status'];

$sets   = [];
$params = [];
$types  = '';

foreach ($allowed as $field) {
    if (!array_key_exists($field, $b)) continue;
    $sets[]   = "$field = ?";
    $params[] = $b[$field];
    $types   .= in_array($field, ['year', 'seating_capacity', 'mileage']) ? 'i'
              : (in_array($field, ['daily_rent_price']) ? 'd' : 's');
}

if (!$sets) {
    json_response(['success' => false, 'message' => 'আপডেট করার কিছু নেই']);
}

$params[] = $id;
$types   .= 'i';

$conn = (new Database())->connect();
$stmt = $conn->prepare('UPDATE vehicles SET ' . implode(', ', $sets) . ' WHERE id = ?');
$stmt->bind_param($types, ...$params);

if ($stmt->execute()) {
    json_response(['success' => true, 'message' => 'গাড়ির তথ্য আপডেট হয়েছে']);
}

json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $stmt->error]);
