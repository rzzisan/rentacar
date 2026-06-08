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

// Handle form submission
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $data = [
        'registration_number' => trim($_POST['registration_number'] ?? ''),
        'model' => trim($_POST['model'] ?? ''),
        'brand' => trim($_POST['brand'] ?? ''),
        'year' => intval($_POST['year'] ?? 0),
        'vehicle_type' => trim($_POST['vehicle_type'] ?? ''),
        'color' => trim($_POST['color'] ?? ''),
        'fuel_type' => trim($_POST['fuel_type'] ?? ''),
        'seating_capacity' => intval($_POST['seating_capacity'] ?? 0),
        'daily_rent_price' => floatval($_POST['daily_rent_price'] ?? 0),
        'status' => 'available',
        'image_path' => ''
    ];

    // Validate required fields
    if (empty($data['registration_number']) || empty($data['brand']) || empty($data['model']) || 
        $data['year'] <= 0 || empty($data['vehicle_type']) || $data['daily_rent_price'] <= 0 || 
        $data['seating_capacity'] <= 0) {
        $error = 'সমস্ত প্রয়োজনীয় ক্ষেত্র পূরণ করুন।';
    } else {
        // Add vehicle
        $result = $vehicle->addVehicle($data);
        if ($result['success']) {
            $success = 'গাড়ি সফলভাবে যোগ করা হয়েছে।';
            // Reset form
            $_POST = [];
        } else {
            $error = $result['message'] ?? 'গাড়ি যোগ করতে ব্যর্থ হয়েছে।';
        }
    }
}
?>
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>নতুন গাড়ি যোগ করুন - <?php echo APP_NAME; ?></title>
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
                        <h1>নতুন গাড়ি যোগ করুন</h1>
                        <div class="breadcrumb">এডমিন প্যানেল / গাড়ি / নতুন যোগ করুন</div>
                    </div>
                </div>

        <?php if (!empty($success)): ?>
            <div class="alert alert-success">✓ <?php echo $success; ?></div>
        <?php endif; ?>

        <?php if (!empty($error)): ?>
            <div class="alert alert-error">✗ <?php echo $error; ?></div>
        <?php endif; ?>

                <div class="content-card" style="max-width: 700px;">
            <form method="POST" action="">
                <div class="form-row">
                    <div class="form-group">
                        <label for="registration_number">রেজিস্ট্রেশন নম্বর *</label>
                        <input type="text" id="registration_number" name="registration_number" 
                               value="<?php echo htmlspecialchars($_POST['registration_number'] ?? ''); ?>" required>
                    </div>
                    <div class="form-group">
                        <label for="brand">ব্র্যান্ড *</label>
                        <input type="text" id="brand" name="brand" 
                               value="<?php echo htmlspecialchars($_POST['brand'] ?? ''); ?>" required>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="model">মডেল *</label>
                        <input type="text" id="model" name="model" 
                               value="<?php echo htmlspecialchars($_POST['model'] ?? ''); ?>" required>
                    </div>
                    <div class="form-group">
                        <label for="year">বছর *</label>
                        <input type="number" id="year" name="year" min="1990" max="<?php echo date('Y'); ?>"
                               value="<?php echo htmlspecialchars($_POST['year'] ?? date('Y')); ?>" required>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="vehicle_type">গাড়ির ধরন *</label>
                        <select id="vehicle_type" name="vehicle_type" required>
                            <option value="">নির্বাচন করুন</option>
                            <option value="sedan" <?php if (($_POST['vehicle_type'] ?? '') === 'sedan') echo 'selected'; ?>>সেডান</option>
                            <option value="suv" <?php if (($_POST['vehicle_type'] ?? '') === 'suv') echo 'selected'; ?>>এসইউভি</option>
                            <option value="van" <?php if (($_POST['vehicle_type'] ?? '') === 'van') echo 'selected'; ?>>ভ্যান</option>
                            <option value="pickup" <?php if (($_POST['vehicle_type'] ?? '') === 'pickup') echo 'selected'; ?>>পিকআপ</option>
                            <option value="truck" <?php if (($_POST['vehicle_type'] ?? '') === 'truck') echo 'selected'; ?>>ট্রাক</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="color">রং</label>
                        <input type="text" id="color" name="color" 
                               value="<?php echo htmlspecialchars($_POST['color'] ?? ''); ?>">
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="fuel_type">জ্বালানির ধরন *</label>
                        <select id="fuel_type" name="fuel_type" required>
                            <option value="">নির্বাচন করুন</option>
                            <option value="petrol" <?php if (($_POST['fuel_type'] ?? '') === 'petrol') echo 'selected'; ?>>পেট্রোল</option>
                            <option value="diesel" <?php if (($_POST['fuel_type'] ?? '') === 'diesel') echo 'selected'; ?>>ডিজেল</option>
                            <option value="hybrid" <?php if (($_POST['fuel_type'] ?? '') === 'hybrid') echo 'selected'; ?>>হাইব্রিড</option>
                            <option value="electric" <?php if (($_POST['fuel_type'] ?? '') === 'electric') echo 'selected'; ?>>ইলেকট্রিক</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="seating_capacity">আসনের ক্ষমতা *</label>
                        <input type="number" id="seating_capacity" name="seating_capacity" min="1" max="20"
                               value="<?php echo htmlspecialchars($_POST['seating_capacity'] ?? '5'); ?>" required>
                    </div>
                </div>

                <div class="form-group">
                    <label for="daily_rent_price">দৈনিক ভাড়া মূল্য (টাকা) *</label>
                    <input type="number" id="daily_rent_price" name="daily_rent_price" min="0" step="0.01"
                           value="<?php echo htmlspecialchars($_POST['daily_rent_price'] ?? ''); ?>" required>
                </div>

                <div style="display: flex; gap: 10px; margin-top: 30px;">
                    <button type="submit" class="btn">গাড়ি যোগ করুন</button>
                    <a href="vehicles.php" class="btn btn-cancel">বাতিল করুন</a>
                </div>
            </form>
            </div>
            </main>
        </div>
    </div>
</body>
</html>
