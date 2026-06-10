<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');
only_method('POST');

$conn = (new Database())->connect();
$rental_id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$data = input();

if (!$rental_id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

// Get current rental
$stmt = $conn->prepare("SELECT id, rental_status, vehicle_id, driver_id FROM rentals WHERE id = ?");
$stmt->bind_param('i', $rental_id);
$stmt->execute();
$rental = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$rental) {
    json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি'], 404);
}

// গাড়ি/ড্রাইভার শুধু ট্রিপ শুরুর আগে (pending অবস্থায়) পরিবর্তনযোগ্য
if ($rental['rental_status'] !== 'pending') {
    json_response(['success' => false, 'message' => 'ট্রিপ শুরু হয়ে গেলে গাড়ি বা ড্রাইভার পরিবর্তন করা যায় না'], 400);
}

$vehicle_id = isset($data['vehicle_id']) && $data['vehicle_id'] ? (int)$data['vehicle_id'] : (int)$rental['vehicle_id'];
$driver_given = array_key_exists('driver_id', $data);
$driver_id = $driver_given && $data['driver_id'] ? (int)$data['driver_id'] : null;

// Validate vehicle
$vstmt = $conn->prepare("SELECT id FROM vehicles WHERE id = ?");
$vstmt->bind_param('i', $vehicle_id);
$vstmt->execute();
if ($vstmt->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'গাড়ি পাওয়া যায়নি'], 404);
}
$vstmt->close();

// গাড়ি বদলে গেছে কিন্তু ড্রাইভার পাঠানো হয়নি — নতুন গাড়ির অ্যাসাইনকৃত ড্রাইভার নেওয়া হয়
if (!$driver_given && $vehicle_id !== (int)$rental['vehicle_id']) {
    $astmt = $conn->prepare(
        "SELECT dv.driver_id FROM driver_vehicles dv
         JOIN drivers d ON d.id = dv.driver_id AND d.status = 'active'
         WHERE dv.vehicle_id = ?
         ORDER BY dv.assigned_at DESC LIMIT 1"
    );
    $astmt->bind_param('i', $vehicle_id);
    $astmt->execute();
    $arow = $astmt->get_result()->fetch_assoc();
    $astmt->close();
    $driver_id = $arow ? (int)$arow['driver_id'] : null;
} elseif (!$driver_given) {
    $driver_id = $rental['driver_id'] ? (int)$rental['driver_id'] : null;
}

// Validate driver if set
if ($driver_id) {
    $dstmt = $conn->prepare("SELECT id FROM drivers WHERE id = ?");
    $dstmt->bind_param('i', $driver_id);
    $dstmt->execute();
    if ($dstmt->get_result()->num_rows === 0) {
        json_response(['success' => false, 'message' => 'চালক পাওয়া যায়নি'], 404);
    }
    $dstmt->close();
}

$ustmt = $conn->prepare("UPDATE rentals SET vehicle_id = ?, driver_id = ? WHERE id = ?");
$ustmt->bind_param('iii', $vehicle_id, $driver_id, $rental_id);
if (!$ustmt->execute()) {
    json_response(['success' => false, 'message' => 'আপডেট ব্যর্থ: ' . $ustmt->error], 500);
}
$ustmt->close();

json_response([
    'success' => true,
    'message' => 'গাড়ি ও ড্রাইভার সফলভাবে আপডেট হয়েছে',
    'data' => ['vehicle_id' => $vehicle_id, 'driver_id' => $driver_id]
]);
?>
