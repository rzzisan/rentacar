<?php
require_once 'config/config.php';
require_once 'config/Database.php';
require_once 'includes/User.php';

$database = new Database();
$conn = $database->connect();

// Check if user is already logged in
$user = new User($conn);

if ($user->isLoggedIn()) {
    // Redirect to appropriate dashboard based on role
    $role = $_SESSION['role'];
    if ($role === 'admin') {
        header('Location: admin/dashboard.php');
    } elseif ($role === 'employee') {
        header('Location: employee/dashboard.php');
    } else {
        header('Location: customer/dashboard.php');
    }
    exit();
}

// Check for login form submission
$error = '';
if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST' && isset($_POST['action'])) {
    if ($_POST['action'] === 'login') {
        $email = $_POST['email'] ?? '';
        $password = $_POST['password'] ?? '';
        
        $result = $user->login($email, $password);
        
        if ($result['success']) {
            // Redirect to appropriate dashboard
            if ($_SESSION['role'] === 'admin') {
                header('Location: admin/dashboard.php');
            } elseif ($_SESSION['role'] === 'employee') {
                header('Location: employee/dashboard.php');
            } else {
                header('Location: customer/dashboard.php');
            }
            exit();
        } else {
            $error = $result['message'];
        }
    }
}
?>
<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo APP_NAME; ?></title>
    <link rel="stylesheet" href="public/assets/css/style.css">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .container {
            width: 100%;
            max-width: 1200px;
            padding: 20px;
        }
        .login-wrapper {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 40px;
            align-items: center;
        }
        .login-content {
            color: white;
        }
        .login-content h1 {
            font-size: 48px;
            margin-bottom: 20px;
            font-weight: 700;
        }
        .login-content p {
            font-size: 18px;
            line-height: 1.6;
            opacity: 0.9;
            margin-bottom: 20px;
        }
        .features {
            list-style: none;
        }
        .features li {
            font-size: 16px;
            margin-bottom: 12px;
            padding-left: 30px;
            position: relative;
        }
        .features li:before {
            content: "✓";
            position: absolute;
            left: 0;
            color: #4ade80;
            font-weight: bold;
            font-size: 20px;
        }
        .login-form {
            background: white;
            padding: 40px;
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
        }
        .login-form h2 {
            font-size: 28px;
            margin-bottom: 30px;
            color: #333;
            text-align: center;
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-group label {
            display: block;
            margin-bottom: 8px;
            color: #555;
            font-weight: 500;
        }
        .form-group input {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 16px;
            transition: border-color 0.3s;
        }
        .form-group input:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }
        .btn {
            width: 100%;
            padding: 12px;
            background: #667eea;
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.3s;
        }
        .btn:hover {
            background: #5568d3;
        }
        .alert {
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
            font-size: 14px;
        }
        .alert-danger {
            background: #fee;
            color: #c33;
            border: 1px solid #fcc;
        }
        .signup-link {
            text-align: center;
            margin-top: 20px;
            color: #666;
        }
        .signup-link a {
            color: #667eea;
            text-decoration: none;
            font-weight: 600;
        }
        @media (max-width: 768px) {
            .login-wrapper {
                grid-template-columns: 1fr;
            }
            .login-content {
                text-align: center;
            }
            .login-content h1 {
                font-size: 36px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="login-wrapper">
            <div class="login-content">
                <h1><?php echo APP_NAME; ?></h1>
                <p>আপনার গাড়ি ভাড়া ব্যবসা পরিচালনা করুন সহজভাবে এবং দক্ষতার সাথে।</p>
                <ul class="features">
                    <li>সম্পূর্ণ গাড়ি ইনভেন্টরি ম্যানেজমেন্ট</li>
                    <li>বুকিং এবং রিজার্ভেশন সিস্টেম</li>
                    <li>পেমেন্ট ট্র্যাকিং এবং রিপোর্টিং</li>
                    <li>গ্রাহক এবং কর্মচারী পরিচালনা</li>
                    <li>রিয়েল-টাইম ড্যাশবোর্ড এবং বিশ্লেষণ</li>
                </ul>
            </div>
            
            <div class="login-form">
                <h2>লগইন</h2>
                
                <?php if ($error): ?>
                    <div class="alert alert-danger">
                        <?php echo htmlspecialchars($error); ?>
                    </div>
                <?php endif; ?>
                
                <form method="POST">
                    <input type="hidden" name="action" value="login">
                    
                    <div class="form-group">
                        <label for="email">ইমেইল</label>
                        <input type="email" id="email" name="email" required>
                    </div>
                    
                    <div class="form-group">
                        <label for="password">পাসওয়ার্ড</label>
                        <input type="password" id="password" name="password" required>
                    </div>
                    
                    <button type="submit" class="btn">লগইন করুন</button>
                    
                    <div class="signup-link">
                        নতুন ব্যবহারকারী? <a href="register.php">নিবন্ধন করুন</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</body>
</html>
