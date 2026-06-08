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

$vehicle = new Vehicle($conn);
$current_user = $user->getCurrentUser();
$error = '';
$success = '';
$vehicle_data = null;

// Get vehicle ID
$id = intval($_GET['id'] ?? 0);
if ($id <= 0) {
    $error = 'অবৈধ গাড়ি আইডি।';
} else {
    $result = $vehicle->getVehicleById($id);
    if ($result['success']) {
        $vehicle_data = $result['data'];
    } else {
        $error = 'গাড়ি খুঁজে পাওয়া যায়নি।';
    }
}

// Handle form submission
if ($_SERVER['REQUEST_METHOD'] == 'POST' && $vehicle_data) {
    $data = [
        'model' => trim($_POST['model'] ?? ''),
        'status' => trim($_POST['status'] ?? 'available'),
        'daily_rent_price' => floatval($_POST['daily_rent_price'] ?? 0),
    ];

    // Validate required fields
    if (empty($data['model']) || $data['daily_rent_price'] <= 0) {
        $error = 'সমস্ত প্রয়োজনীয় ক্ষেত্র পূরণ করুন।';
    } else {
        // Update vehicle
        $result = $vehicle->updateVehicle($id, $data);
        if ($result['success']) {
            $success = 'গাড়ি সফলভাবে আপডেট করা হয়েছে।';
            // Refresh vehicle data
            $result = $vehicle->getVehicleById($id);
            $vehicle_data = $result['data'];
        } else {
            $error = $result['message'] ?? 'গাড়ি আপডেট করতে ব্যর্থ হয়েছে।';
        }
    }
}
?>
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>গাড়ি সম্পাদনা করুন - <?php echo APP_NAME; ?></title>
    <link rel="stylesheet" href="../public/assets/css/layout.css">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f7fa;
            color: #333;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px 0;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }
        .header-content {
            max-width: 1200px;
            margin: 0 auto;
            padding: 0 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .container {
            max-width: 800px;
            margin: 40px auto;
            padding: 0 20px;
        }
        .page-header {
            background: white;
            padding: 20px;
            border-radius: 10px;
            margin-bottom: 30px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
        }
        .page-header h1 {
            font-size: 28px;
            margin: 0;
        }
        .back-link {
            color: #667eea;
            text-decoration: none;
            font-size: 14px;
            display: inline-block;
            margin-top: 10px;
        }
        .back-link:hover {
            text-decoration: underline;
        }
        .form-card {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-group label {
            display: block;
            margin-bottom: 8px;
            font-weight: 500;
            color: #333;
        }
        .form-group input,
        .form-group select {
            width: 100%;
            padding: 10px 12px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
            font-family: inherit;
        }
        .form-group input:focus,
        .form-group select:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }
        .form-group .info {
            display: block;
            font-size: 13px;
            color: #666;
            margin-top: 5px;
            padding: 10px;
            background: #f8f9fa;
            border-radius: 3px;
            border-left: 3px solid #667eea;
        }
        .form-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
        }
        .btn {
            display: inline-block;
            background: #667eea;
            color: white;
            padding: 12px 24px;
            border-radius: 5px;
            text-decoration: none;
            transition: background 0.3s;
            border: none;
            cursor: pointer;
            font-size: 14px;
            font-weight: 500;
        }
        .btn:hover {
            background: #5568d3;
        }
        .btn-cancel {
            background: #6c757d;
            margin-left: 10px;
        }
        .btn-cancel:hover {
            background: #5a6268;
        }
        .alert {
            padding: 15px 20px;
            border-radius: 5px;
            margin-bottom: 20px;
            font-size: 14px;
        }
        .alert-success {
            background: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .alert-error {
            background: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .vehicle-info {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
        }
        .info-item {
            font-size: 13px;
        }
        .info-item .label {
            font-weight: 500;
            color: #666;
        }
        .info-item .value {
            color: #333;
            margin-top: 3px;
        }
        .navbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            width: 100%;
        }
        .user-info {
            color: white;
            font-size: 14px;
        }
        .logout-btn {
            color: white;
            text-decoration: none;
            background: rgba(255, 255, 255, 0.2);
            padding: 8px 16px;
            border-radius: 5px;
            transition: background 0.3s;
            border: none;
            cursor: pointer;
            font-size: 14px;
        }
        .logout-btn:hover {
            background: rgba(255, 255, 255, 0.3);
        }
    </style>
</head>
<body>
    <div class="app-layout">
        <?php include '../includes/header.php'; ?>

        <div class="app-content">
            <?php include '../includes/sidebar.php'; ?>

            <main class="main-content">
                <!-- Page Header -->
                <div class="page-header">
                    <div>
                        <h1>গাড়ি সম্পাদনা করুন</h1>
                        <div class="breadcrumb">এডমিন প্যানেল / গাড়ি / সম্পাদনা</div>
                    </div>
                </div>

        <?php if (!empty($success)): ?>
            <div class="alert alert-success">✓ <?php echo $success; ?></div>
        <?php endif; ?>

        <?php if (!empty($error)): ?>
            <div class="alert alert-error">✗ <?php echo $error; ?></div>
        <?php endif; ?>

        <?php if ($vehicle_data): ?>
            <div class="content-card" style="max-width: 700px;">
                <!-- Vehicle Information -->
                <div class="vehicle-info">
                    <div class="info-item">
                        <div class="label">রেজিস্ট্রেশন নম্বর</div>
                        <div class="value"><?php echo htmlspecialchars($vehicle_data['registration_number']); ?></div>
                    </div>
                    <div class="info-item">
                        <div class="label">ব্র্যান্ড</div>
                        <div class="value"><?php echo htmlspecialchars($vehicle_data['brand']); ?></div>
                    </div>
                    <div class="info-item">
                        <div class="label">ধরন</div>
                        <div class="value"><?php echo htmlspecialchars($vehicle_data['vehicle_type']); ?></div>
                    </div>
                    <div class="info-item">
                        <div class="label">বছর</div>
                        <div class="value"><?php echo htmlspecialchars($vehicle_data['year']); ?></div>
                    </div>
                </div>

                <!-- Edit Form -->
                <form method="POST" action="">
                    <div class="form-row">
                        <div class="form-group">
                            <label for="model">মডেল *</label>
                            <input type="text" id="model" name="model" 
                                   value="<?php echo htmlspecialchars($vehicle_data['model']); ?>" required>
                        </div>
                        <div class="form-group">
                            <label for="status">স্ট্যাটাস *</label>
                            <select id="status" name="status" required>
                                <option value="available" <?php if ($vehicle_data['status'] === 'available') echo 'selected'; ?>>উপলব্ধ</option>
                                <option value="rented" <?php if ($vehicle_data['status'] === 'rented') echo 'selected'; ?>>ভাড়া নেওয়া</option>
                                <option value="maintenance" <?php if ($vehicle_data['status'] === 'maintenance') echo 'selected'; ?>>রক্ষণাবেক্ষণ</option>
                                <option value="inactive" <?php if ($vehicle_data['status'] === 'inactive') echo 'selected'; ?>>নিষ্ক্রিয়</option>
                            </select>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="daily_rent_price">দৈনিক ভাড়া মূল্য (টাকা) *</label>
                        <input type="number" id="daily_rent_price" name="daily_rent_price" min="0" step="0.01"
                               value="<?php echo htmlspecialchars($vehicle_data['daily_rent_price']); ?>" required>
                    </div>

                    <div style="display: flex; gap: 10px; margin-top: 30px;">
                        <button type="submit" class="btn">পরিবর্তন সংরক্ষণ করুন</button>
                        <a href="vehicles.php" class="btn btn-cancel">বাতিল করুন</a>
                    </div>
                </form>
            </div>
        <?php else: ?>
            <div class="content-card" style="text-align: center; color: #999;">
                <p>গাড়ির তথ্য পাওয়া যায়নি।</p>
            </div>
        <?php endif; ?>
            </main>
        </div>
    </div>
</body>
</html>
