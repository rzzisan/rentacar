<?php
require_once '../config/config.php';
require_once '../config/Database.php';
require_once '../includes/User.php';
require_once '../includes/Vehicle.php';
require_once '../includes/Rental.php';

$database = new Database();
$conn = $database->connect();
$user = new User($conn);

// Check if user is logged in and is admin
if (!$user->isLoggedIn() || !$user->hasRole('admin')) {
    header('Location: ../index.php');
    exit();
}

$current_user = $user->getCurrentUser();

// Get statistics
$vehicle = new Vehicle($conn);
$rental = new Rental($conn);

$vehicles_result = $vehicle->getAllVehicles();
$total_vehicles = count($vehicles_result['data'] ?? []);

$rentals_result = $rental->getAllRentals();
$total_rentals = count($rentals_result['data'] ?? []);

// Get available vehicles count
$available_vehicles = count(array_filter($vehicles_result['data'] ?? [], function($v) {
    return $v['status'] === 'available';
}));

// Get pending rentals count
$pending_rentals = count(array_filter($rentals_result['data'] ?? [], function($r) {
    return $r['rental_status'] === 'pending';
}));
?>
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin ড্যাশবোর্ড - <?php echo APP_NAME; ?></title>
    <link rel="stylesheet" href="../public/assets/css/layout.css">
    <style>
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            text-align: center;
            transition: all 0.3s ease;
        }

        .stat-card:hover {
            box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
            transform: translateY(-5px);
        }

        .stat-icon {
            font-size: 40px;
            margin-bottom: 10px;
        }

        .stat-number {
            font-size: 32px;
            font-weight: bold;
            color: #667eea;
            margin: 10px 0;
        }

        .stat-label {
            font-size: 14px;
            color: #666;
        }

        .dashboard-section {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            margin-bottom: 30px;
        }

        .section-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            padding-bottom: 15px;
            border-bottom: 2px solid #f5f7fa;
        }

        .section-header h2 {
            font-size: 20px;
            color: #333;
            margin: 0;
        }

        .quick-links {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-top: 20px;
        }

        .quick-link-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 10px;
            text-decoration: none;
            text-align: center;
            transition: transform 0.3s ease;
        }

        .quick-link-card:hover {
            transform: translateY(-5px);
        }

        .quick-link-icon {
            font-size: 30px;
            margin-bottom: 10px;
        }

        .quick-link-text {
            font-weight: 500;
            font-size: 14px;
        }

        @media (max-width: 768px) {
            .stats-grid {
                grid-template-columns: 1fr;
            }

            .quick-links {
                grid-template-columns: 1fr;
            }
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
                        <h1>ড্যাশবোর্ড</h1>
                        <div class="breadcrumb">এডমিন প্যানেল / ড্যাশবোর্ড</div>
                    </div>
                    <div>
                        <span style="color: #666; font-size: 14px;">
                            আজকের তারিখ: <?php echo date('d/m/Y'); ?>
                        </span>
                    </div>
                </div>

                <!-- Statistics Cards -->
                <div class="stats-grid">
                    <div class="stat-card">
                        <div class="stat-icon">🚗</div>
                        <div class="stat-number"><?php echo $total_vehicles; ?></div>
                        <div class="stat-label">মোট গাড়ি</div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-icon">✅</div>
                        <div class="stat-number"><?php echo $available_vehicles; ?></div>
                        <div class="stat-label">উপলব্ধ গাড়ি</div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-icon">📋</div>
                        <div class="stat-number"><?php echo $total_rentals; ?></div>
                        <div class="stat-label">মোট ভাড়া</div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-icon">⏳</div>
                        <div class="stat-number"><?php echo $pending_rentals; ?></div>
                        <div class="stat-label">পেন্ডিং অর্ডার</div>
                    </div>
                </div>

                <!-- Quick Actions -->
                <div class="dashboard-section">
                    <div class="section-header">
                        <h2>দ্রুত কার্যক্রম</h2>
                    </div>
                    <div class="quick-links">
                        <a href="vehicles.php" class="quick-link-card">
                            <div class="quick-link-icon">🚗</div>
                            <div class="quick-link-text">গাড়ি পরিচালনা করুন</div>
                        </a>
                        <a href="add-vehicle.php" class="quick-link-card">
                            <div class="quick-link-icon">➕</div>
                            <div class="quick-link-text">নতুন গাড়ি যোগ করুন</div>
                        </a>
                        <a href="rentals.php" class="quick-link-card">
                            <div class="quick-link-icon">📋</div>
                            <div class="quick-link-text">ভাড়া অর্ডার দেখুন</div>
                        </a>
                        <a href="customers.php" class="quick-link-card">
                            <div class="quick-link-icon">👥</div>
                            <div class="quick-link-text">গ্রাহক ব্যবস্থাপনা</div>
                        </a>
                        <a href="payments.php" class="quick-link-card">
                            <div class="quick-link-icon">💳</div>
                            <div class="quick-link-text">পেমেন্ট দেখুন</div>
                        </a>
                        <a href="reports.php" class="quick-link-card">
                            <div class="quick-link-icon">📈</div>
                            <div class="quick-link-text">রিপোর্ট তৈরি করুন</div>
                        </a>
                    </div>
                </div>

                <!-- Recent Activity -->
                <div class="dashboard-section">
                    <div class="section-header">
                        <h2>সিস্টেম তথ্য</h2>
                    </div>
                    <table class="table">
                        <tr>
                            <td style="font-weight: 500;">অ্যাপ্লিকেশন নাম:</td>
                            <td><?php echo APP_NAME; ?></td>
                        </tr>
                        <tr>
                            <td style="font-weight: 500;">সংস্করণ:</td>
                            <td><?php echo APP_VERSION; ?></td>
                        </tr>
                        <tr>
                            <td style="font-weight: 500;">আপনার ভূমিকা:</td>
                            <td><span class="badge badge-info"><?php echo $current_user['role']; ?></span></td>
                        </tr>
                        <tr>
                            <td style="font-weight: 500;">লগইন সময়:</td>
                            <td><?php echo date('d/m/Y H:i'); ?></td>
                        </tr>
                    </table>
                </div>
            </main>
        </div>
    </div>

    <script src="../public/assets/js/main.js"></script>
</body>
</html>
