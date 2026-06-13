<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

$manager_id = require_manager();
$conn       = (new Database())->connect();
$method     = $_SERVER['REQUEST_METHOD'];
$rental_id  = isset($_GET['rental_id']) ? (int)$_GET['rental_id'] : 0;

if (!$rental_id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

$vids = get_manager_vehicle_ids($conn, $manager_id);
$in   = manager_vehicle_in_clause($vids);

// Verify rental belongs to this manager's vehicles
$chkStmt = $conn->prepare("SELECT id FROM rentals WHERE id = ? AND vehicle_id IN $in");
$params  = array_merge([$rental_id], $vids);
$types   = 'i' . str_repeat('i', count($vids));
$chkStmt->bind_param($types, ...$params);
$chkStmt->execute();
if ($chkStmt->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি'], 404);
}
$chkStmt->close();

if ($method === 'GET') {
    $estmt = $conn->prepare(
        "SELECT id, rental_id, expense_type, description, amount, receipt_image,
                location_name, latitude, longitude, created_at
         FROM trip_expenses WHERE rental_id = ? ORDER BY created_at DESC"
    );
    $estmt->bind_param('i', $rental_id);
    $estmt->execute();
    $expenses = $estmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $estmt->close();

    foreach ($expenses as &$exp) {
        $exp['id']        = (int)$exp['id'];
        $exp['rental_id'] = (int)$exp['rental_id'];
        $exp['amount']    = (float)$exp['amount'];
        $exp['latitude']  = $exp['latitude']  !== null ? (float)$exp['latitude']  : null;
        $exp['longitude'] = $exp['longitude'] !== null ? (float)$exp['longitude'] : null;
    }

    json_response(['success' => true, 'data' => $expenses]);
}

if ($method === 'POST') {
    $expense_type  = $_POST['expense_type'] ?? '';
    $amount        = isset($_POST['amount']) ? (float)$_POST['amount'] : 0;
    $description   = trim($_POST['description'] ?? '');
    $location_name = trim($_POST['location_name'] ?? '') ?: null;
    $latitude      = isset($_POST['latitude'])  && $_POST['latitude']  !== '' ? (float)$_POST['latitude']  : null;
    $longitude     = isset($_POST['longitude']) && $_POST['longitude'] !== '' ? (float)$_POST['longitude'] : null;

    $valid_types = ['toll', 'fuel', 'parking', 'repair', 'driver_allowance', 'other'];
    if (!in_array($expense_type, $valid_types)) {
        json_response(['success' => false, 'message' => 'অবৈধ খরচের ধরন'], 400);
    }
    if ($amount <= 0) {
        json_response(['success' => false, 'message' => 'পরিমান ০ এর বেশি হতে হবে'], 400);
    }

    $receipt_image = null;
    if (!empty($_FILES['receipt_image']['name'])) {
        $file = $_FILES['receipt_image'];
        $ext  = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
        if (!in_array($ext, ALLOWED_IMAGE_TYPES)) {
            json_response(['success' => false, 'message' => 'শুধুমাত্র JPG, PNG, GIF ছবি অনুমোদিত'], 400);
        }
        if ($file['size'] > MAX_UPLOAD_SIZE) {
            json_response(['success' => false, 'message' => 'ছবির সাইজ সর্বোচ্চ ৫ MB'], 400);
        }
        $dir = UPLOAD_PATH . 'expenses/';
        if (!is_dir($dir)) mkdir($dir, 0755, true);
        $filename = 'expense_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
        if (!move_uploaded_file($file['tmp_name'], $dir . $filename)) {
            json_response(['success' => false, 'message' => 'ছবি আপলোড ব্যর্থ'], 500);
        }
        $receipt_image = 'uploads/expenses/' . $filename;
    }

    $istmt = $conn->prepare(
        "INSERT INTO trip_expenses
         (rental_id, expense_type, description, amount, receipt_image, location_name, latitude, longitude)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    );
    $istmt->bind_param('issdssdd', $rental_id, $expense_type, $description, $amount, $receipt_image, $location_name, $latitude, $longitude);
    if (!$istmt->execute()) {
        json_response(['success' => false, 'message' => 'খরচ যোগ করতে ব্যর্থ: ' . $istmt->error], 500);
    }
    $expense_id = $conn->insert_id;
    $istmt->close();

    json_response(['success' => true, 'message' => 'খরচ সফলভাবে যোগ করা হয়েছে', 'data' => ['expense_id' => $expense_id]], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
