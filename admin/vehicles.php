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
$filters = [];

if ($_GET['status'] ?? false) {
    $filters['status'] = $_GET['status'];
}

$result = $vehicle->getAllVehicles($filters);
$vehicles = $result['data'] ?? [];
$current_user = $user->getCurrentUser();

// Get session messages
$success_message = $_SESSION['success'] ?? '';
$error_message = $_SESSION['error'] ?? '';
unset($_SESSION['success']);
unset($_SESSION['error']);
?>
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>গাড়ি ম্যানেজমেন্ট - <?php echo APP_NAME; ?></title>
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
            max-width: 1200px;
            margin: 40px auto;
            padding: 0 20px;
        }
        .page-header {
            background: white;
            padding: 20px;
            border-radius: 10px;
            margin-bottom: 30px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .page-header h1 {
            font-size: 28px;
            margin: 0;
        }
        .btn {
            display: inline-block;
            background: #667eea;
            color: white;
            padding: 10px 20px;
            border-radius: 5px;
            text-decoration: none;
            transition: background 0.3s;
            border: none;
            cursor: pointer;
            font-size: 14px;
        }
        .btn:hover {
            background: #5568d3;
        }
        .btn-secondary {
            background: #6c757d;
        }
        .btn-secondary:hover {
            background: #5a6268;
        }
        .btn-danger {
            background: #dc3545;
        }
        .btn-danger:hover {
            background: #c82333;
        }
        .filter-section {
            background: white;
            padding: 20px;
            border-radius: 10px;
            margin-bottom: 30px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
        }
        .vehicles-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
            gap: 20px;
        }
        .vehicle-card {
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            overflow: hidden;
            transition: transform 0.3s;
        }
        .vehicle-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
        }
        .vehicle-image {
            height: 200px;
            background: #f0f0f0;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #999;
            font-size: 14px;
        }
        .vehicle-info {
            padding: 15px;
        }
        .vehicle-name {
            font-size: 18px;
            font-weight: bold;
            margin-bottom: 8px;
            color: #333;
        }
        .vehicle-details {
            font-size: 13px;
            color: #666;
            margin-bottom: 12px;
        }
        .vehicle-price {
            font-size: 16px;
            font-weight: bold;
            color: #667eea;
            margin-bottom: 12px;
        }
        .vehicle-status {
            display: inline-block;
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            margin-bottom: 12px;
        }
        .status-available {
            background: #efe;
            color: #3c3;
        }
        .status-rented {
            background: #fee;
            color: #c33;
        }
        .status-maintenance {
            background: #fef5e7;
            color: #8b7a00;
        }
        .vehicle-actions {
            display: flex;
            gap: 8px;
        }
        .vehicle-actions a,
        .vehicle-actions button {
            flex: 1;
            padding: 8px;
            text-align: center;
            border-radius: 5px;
            text-decoration: none;
            font-size: 12px;
            border: none;
            cursor: pointer;
        }
        .vehicle-actions .edit-btn {
            background: #667eea;
            color: white;
        }
        .vehicle-actions .delete-btn {
            background: #dc3545;
            color: white;
        }
        .logout-btn {
            background: rgba(255, 255, 255, 0.2);
            color: white;
            border: 1px solid white;
            padding: 8px 16px;
            border-radius: 5px;
            cursor: pointer;
            text-decoration: none;
        }
        .back-link {
            display: inline-block;
            margin-bottom: 20px;
            color: #667eea;
            text-decoration: none;
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
    </style>
</head>
<body>
    <div class="app-layout">
        <?php include '../includes/header.php'; ?>

        <div class="app-content">
            <?php include '../includes/sidebar.php'; ?>

            <main class="main-content">

        <?php if (!empty($success_message)): ?>
            <div class="alert alert-success">✓ <?php echo htmlspecialchars($success_message); ?></div>
        <?php endif; ?>

        <?php if (!empty($error_message)): ?>
            <div class="alert alert-error">✗ <?php echo htmlspecialchars($error_message); ?></div>
        <?php endif; ?>
                
                <!-- Page Header -->
                <div class="page-header">
                    <div>
                        <h1>গাড়ি ম্যানেজমেন্ট</h1>
                        <div class="breadcrumb">এডমিন প্যানেল / গাড়ি</div>
                    </div>
                    <a href="add-vehicle.php" class="btn">নতুন গাড়ি যোগ করুন</a>
                </div>

                <!-- Filter Section -->
                <div class="content-card">
                    <h3>ফিল্টার করুন</h3>
                    <form method="GET" style="display: flex; gap: 10px; align-items: flex-end;">
                        <div class="form-group" style="flex: 0 0 200px; margin-bottom: 0;">
                            <label for="status" style="margin-bottom: 5px;">অবস্থা:</label>
                            <select name="status" id="status" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
                                <option value="">সব অবস্থা</option>
                                <option value="available" <?php echo ($_GET['status'] ?? '') === 'available' ? 'selected' : ''; ?>>উপলব্ধ</option>
                                <option value="rented" <?php echo ($_GET['status'] ?? '') === 'rented' ? 'selected' : ''; ?>>ভাড়া নেওয়া</option>
                                <option value="maintenance" <?php echo ($_GET['status'] ?? '') === 'maintenance' ? 'selected' : ''; ?>>রক্ষণাবেক্ষণ</option>
                            </select>
                        </div>
                        <button type="submit" class="btn">ফিল্টার করুন</button>
                    </form>
                </div>

        <?php if (empty($vehicles)): ?>
            <div class="content-card" style="text-align: center; padding: 40px;">
                <p style="color: #999; font-size: 16px;">কোনো গাড়ি পাওয়া যাচ্ছে না।</p>
            </div>
        <?php else: ?>
            <div class="content-card">
                <div class="vehicles-grid">
                <?php foreach ($vehicles as $v): ?>
                    <div class="vehicle-card">
                        <div class="vehicle-image">
                            <?php if ($v['image_path']): ?>
                                <img src="<?php echo htmlspecialchars($v['image_path']); ?>" alt="<?php echo htmlspecialchars($v['model']); ?>" style="width: 100%; height: 100%; object-fit: cover;">
                            <?php else: ?>
                                গাড়ির ছবি নেই
                            <?php endif; ?>
                        </div>
                        <div class="vehicle-info">
                            <div class="vehicle-name"><?php echo htmlspecialchars($v['brand'] . ' ' . $v['model']); ?></div>
                            <div class="vehicle-details">
                                <div>রেজিস্ট্রেশন: <?php echo htmlspecialchars($v['registration_number']); ?></div>
                                <div>বছর: <?php echo htmlspecialchars($v['year']); ?></div>
                                <div>জ্বালানী: <?php echo htmlspecialchars($v['fuel_type']); ?></div>
                                <div>সিট: <?php echo htmlspecialchars($v['seating_capacity']); ?></div>
                            </div>
                            <div class="vehicle-price">৳<?php echo number_format($v['daily_rent_price'], 2); ?>/দিন</div>
                            <div class="vehicle-status status-<?php echo $v['status']; ?>">
                                <?php 
                                    $statusMap = [
                                        'available' => 'উপলব্ধ',
                                        'rented' => 'ভাড়া নেওয়া',
                                        'maintenance' => 'রক্ষণাবেক্ষণ',
                                        'inactive' => 'নিষ্ক্রিয়'
                                    ];
                                    echo $statusMap[$v['status']] ?? $v['status'];
                                ?>
                            </div>
                            <div class="vehicle-actions">
                                <a href="edit-vehicle.php?id=<?php echo $v['id']; ?>" class="edit-btn">সম্পাদনা</a>
                                <button class="delete-btn" onclick="if(confirm('আপনি নিশ্চিত?')) window.location='delete-vehicle.php?id=<?php echo $v['id']; ?>'">মুছে ফেলুন</button>
                            </div>
                        </div>
                    </div>
                <?php endforeach; ?>
                </div>
            </div>
        <?php endif; ?>
            </main>
        </div>
    </div>
</body>
</html>
