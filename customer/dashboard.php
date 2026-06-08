<?php
require_once '../config/config.php';
require_once '../config/Database.php';
require_once '../includes/User.php';
require_once '../includes/Rental.php';

$database = new Database();
$conn = $database->connect();
$user = new User($conn);

// Check if user is logged in and is customer
if (!$user->isLoggedIn() || !$user->hasRole('customer')) {
    header('Location: ../index.php');
    exit();
}

$current_user = $user->getCurrentUser();

// Get customer rentals
$rental = new Rental($conn);
$rentals_result = $rental->getAllRentals();

$my_rentals = array_filter($rentals_result['data'] ?? [], function($r) use ($current_user) {
    return $r['customer_id'] === $current_user['id'];
});

$active_bookings = count(array_filter($my_rentals, function($r) {
    return $r['rental_status'] === 'active';
}));

$completed_bookings = count(array_filter($my_rentals, function($r) {
    return $r['rental_status'] === 'completed';
}));
?>
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>গ্রাহক ড্যাশবোর্ড - <?php echo APP_NAME; ?></title>
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
                        <h1>গাড়ি ভাড়া নিন সহজেই</h1>
                        <div class="breadcrumb">গ্রাহক প্যানেল / ড্যাশবোর্ড</div>
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
                        <div class="stat-icon">📅</div>
                        <div class="stat-number"><?php echo $active_bookings; ?></div>
                        <div class="stat-label">সক্রিয় বুকিং</div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-icon">✅</div>
                        <div class="stat-number"><?php echo $completed_bookings; ?></div>
                        <div class="stat-label">সম্পূর্ণ বুকিং</div>
                    </div>

                    <div class="stat-card">
                        <div class="stat-icon">⭐</div>
                        <div class="stat-number">--</div>
                        <div class="stat-label">আমার রেটিং</div>
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
                            <div class="quick-link-text">গাড়ি ব্রাউজ করুন</div>
                        </a>
                        <a href="bookings.php" class="quick-link-card">
                            <div class="quick-link-icon">📅</div>
                            <div class="quick-link-text">আমার বুকিং</div>
                        </a>
                        <a href="invoices.php" class="quick-link-card">
                            <div class="quick-link-icon">🧾</div>
                            <div class="quick-link-text">আমার ইনভয়েস</div>
                        </a>
                        <a href="profile.php" class="quick-link-card">
                            <div class="quick-link-icon">👤</div>
                            <div class="quick-link-text">আমার প্রোফাইল</div>
                        </a>
                    </div>
                </div>

                <!-- Features -->
                <div class="dashboard-section">
                    <div class="section-header">
                        <h2>আমাদের সেবা</h2>
                    </div>
                    <ul style="line-height: 2.5; margin-left: 20px; font-size: 15px;">
                        <li>✓ সাশ্রয়ী মূল্যে গাড়ি ভাড়া নিন</li>
                        <li>✓ নির্ভরযোগ্য এবং নিরাপদ গাড়ি</li>
                        <li>✓ ২৪/৭ কাস্টমার সাপোর্ট</li>
                        <li>✓ সহজ অনলাইন বুকিং সিস্টেম</li>
                        <li>✓ নমনীয় পেমেন্ট অপশন</li>
                    </ul>
                </div>
            </main>
        </div>
    </div>

    <script src="../public/assets/js/main.js"></script>
</body>
</html>
