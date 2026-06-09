<?php
require_once '../../../config/config.php';
require_once '../../../config/Database.php';
require_once '../../_helpers.php';

require_role('admin');

$conn = (new Database())->connect();
$method = $_SERVER['REQUEST_METHOD'];
$rental_id = isset($_GET['rental_id']) ? (int)$_GET['rental_id'] : 0;

if (!$rental_id) {
    json_response(['success' => false, 'message' => 'রেন্টাল আইডি প্রয়োজন'], 400);
}

// Verify rental exists
$stmt = $conn->prepare("SELECT id FROM rentals WHERE id = ?");
$stmt->bind_param('i', $rental_id);
$stmt->execute();
if ($stmt->get_result()->num_rows === 0) {
    json_response(['success' => false, 'message' => 'রেন্টাল পাওয়া যায়নি'], 404);
}
$stmt->close();

// ── GET: list expenses ───────────────────────────────────────────
if ($method === 'GET') {
    $estmt = $conn->prepare(
        "SELECT id, rental_id, expense_type, description, amount, receipt_image, created_at
         FROM trip_expenses
         WHERE rental_id = ?
         ORDER BY created_at DESC"
    );
    $estmt->bind_param('i', $rental_id);
    $estmt->execute();
    $expenses = $estmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $estmt->close();

    foreach ($expenses as &$exp) {
        $exp['id']     = (int)$exp['id'];
        $exp['rental_id'] = (int)$exp['rental_id'];
        $exp['amount'] = (float)$exp['amount'];
    }

    json_response(['success' => true, 'data' => $expenses]);
}

// ── POST: add expense ────────────────────────────────────────────
if ($method === 'POST') {
    $expense_type = $_POST['expense_type'] ?? '';
    $amount       = isset($_POST['amount']) ? (float)$_POST['amount'] : 0;
    $description  = trim($_POST['description'] ?? '');

    $valid_types = ['toll', 'fuel', 'parking', 'repair', 'driver_allowance', 'other'];
    if (!in_array($expense_type, $valid_types)) {
        json_response(['success' => false, 'message' => 'অবৈধ খরচের ধরন'], 400);
    }

    if ($amount <= 0) {
        json_response(['success' => false, 'message' => 'পরিমান ০ এর বেশি হতে হবে'], 400);
    }

    // File upload (optional)
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
        if (!is_dir($dir)) {
            mkdir($dir, 0755, true);
        }

        $filename = 'expense_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
        if (!move_uploaded_file($file['tmp_name'], $dir . $filename)) {
            json_response(['success' => false, 'message' => 'ছবি আপলোড ব্যর্থ'], 500);
        }

        $receipt_image = 'uploads/expenses/' . $filename;
    }

    // Insert expense
    $istmt = $conn->prepare(
        "INSERT INTO trip_expenses
         (rental_id, expense_type, description, amount, receipt_image)
         VALUES (?, ?, ?, ?, ?)"
    );

    $istmt->bind_param('issds', $rental_id, $expense_type, $description, $amount, $receipt_image);

    if (!$istmt->execute()) {
        json_response(['success' => false, 'message' => 'খরচ যোগ করতে ব্যর্থ: ' . $istmt->error], 500);
    }

    $expense_id = $conn->insert_id;
    $istmt->close();

    json_response([
        'success' => true,
        'message' => 'খরচ সফলভাবে যোগ করা হয়েছে',
        'data' => ['expense_id' => $expense_id]
    ], 201);
}

json_response(['success' => false, 'message' => 'Method not allowed'], 405);
?>
