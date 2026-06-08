<?php
/**
 * Application Configuration
 */

// Database Configuration
define('DB_HOST', 'localhost');
define('DB_NAME', 'car_rental_db');
define('DB_USER', 'carapp');
define('DB_PASSWORD', 'CarApp@2026Secure123!');

// Application Configuration
define('APP_NAME', 'Car Rental Management System');
define('APP_URL', 'http://car.zisan.me');
define('APP_VERSION', '1.0.0');

// Session Configuration
define('SESSION_TIMEOUT', 3600); // 1 hour
define('REMEMBER_ME_DURATION', 2592000); // 30 days

// Set session configuration
ini_set('session.gc_maxlifetime', SESSION_TIMEOUT);
session_set_cookie_params([
    'lifetime' => SESSION_TIMEOUT,
    'path' => '/',
    'domain' => '',
    'secure' => true,      // HTTPS only
    'httponly' => true,    // No JavaScript access
    'samesite' => 'Strict'
]);

// Tax Configuration
define('TAX_RATE', 15); // 15% tax

// Upload Configuration
define('UPLOAD_PATH', __DIR__ . '/../public/uploads/');
define('MAX_UPLOAD_SIZE', 5242880); // 5MB
define('ALLOWED_IMAGE_TYPES', ['jpg', 'jpeg', 'png', 'gif']);

// Email Configuration (Optional)
define('SMTP_HOST', 'smtp.gmail.com');
define('SMTP_PORT', 587);
define('SMTP_USER', 'your-email@gmail.com');
define('SMTP_PASSWORD', 'your-password');

// Pagination
define('ITEMS_PER_PAGE', 10);

// Error Display (set to false in production)
define('DEBUG_MODE', true);

// Set timezone
date_default_timezone_set('Asia/Dhaka');

// Start session if not already started
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}
?>
