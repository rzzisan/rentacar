<?php
require_once '../config/config.php';
require_once '../config/Database.php';
require_once '../includes/User.php';
require_once '../includes/Vehicle.php';

$database = new Database();
$conn = $database->connect();
$user = new User($conn);

// Check if user is logged in and is admin
if (!$user->isLoggedIn() || !$user->hasRole('admin')) {
    header('Location: ../index.php');
    exit();
}

// Get vehicle ID from query string
$id = intval($_GET['id'] ?? 0);

if ($id <= 0) {
    $_SESSION['error'] = 'অবৈধ গাড়ি আইডি।';
} else {
    $vehicle = new Vehicle($conn);
    
    // Delete the vehicle
    $result = $vehicle->deleteVehicle($id);
    
    if ($result['success']) {
        $_SESSION['success'] = 'গাড়ি সফলভাবে মুছে ফেলা হয়েছে।';
    } else {
        $_SESSION['error'] = $result['message'] ?? 'গাড়ি মুছে ফেলতে ব্যর্থ হয়েছে।';
    }
}

// Redirect back to vehicles list
header('Location: vehicles.php');
exit();
?>
