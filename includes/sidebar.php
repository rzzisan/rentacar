<?php
/**
 * Sidebar Component - Navigation sidebar with role-based menus
 * Include this file after header.php
 */

// Get current page for active state
$current_page = basename($_SERVER['PHP_SELF']);
$current_path = $_SERVER['REQUEST_URI'];

function isActive($page, $current) {
    return basename($current) === $page ? 'active' : '';
}

function isActivePath($path, $current) {
    return strpos($current, $path) !== false ? 'active' : '';
}
?>

<aside class="sidebar" id="sidebar">
    <ul class="sidebar-menu">
        <?php if ($current_user['role'] === 'admin'): ?>
            <!-- Admin Menu -->
            
            <!-- Dashboard -->
            <li class="menu-item">
                <a href="dashboard.php" class="menu-link <?php echo isActive('dashboard.php', $current_page); ?>">
                    <span class="menu-icon">📊</span>
                    <span class="menu-label">ড্যাশবোর্ড</span>
                </a>
            </li>

            <!-- Vehicle Management Section -->
            <div class="menu-section-title">গাড়ি ব্যবস্থাপনা</div>
            
            <li class="menu-item has-submenu">
                <a href="#" class="menu-link <?php echo isActivePath('vehicle', $current_path) ? 'active' : ''; ?>">
                    <span class="menu-icon">🚗</span>
                    <span class="menu-label">গাড়ি</span>
                    <span class="menu-toggle">▶</span>
                </a>
                <ul class="submenu <?php echo isActivePath('vehicle', $current_path) ? 'open' : ''; ?>">
                    <li class="submenu-item">
                        <a href="vehicles.php" class="submenu-link <?php echo isActive('vehicles.php', $current_page); ?>">সকল গাড়ি দেখুন</a>
                    </li>
                    <li class="submenu-item">
                        <a href="add-vehicle.php" class="submenu-link <?php echo isActive('add-vehicle.php', $current_page); ?>">নতুন গাড়ি যোগ করুন</a>
                    </li>
                </ul>
            </li>

            <!-- Rental Management Section -->
            <div class="menu-section-title">ভাড়া ব্যবস্থাপনা</div>
            
            <li class="menu-item">
                <a href="rentals.php" class="menu-link <?php echo isActive('rentals.php', $current_page); ?>">
                    <span class="menu-icon">📋</span>
                    <span class="menu-label">ভাড়ার অর্ডার</span>
                </a>
            </li>

            <!-- Customer Management Section -->
            <div class="menu-section-title">গ্রাহক ব্যবস্থাপনা</div>
            
            <li class="menu-item">
                <a href="customers.php" class="menu-link <?php echo isActive('customers.php', $current_page); ?>">
                    <span class="menu-icon">👥</span>
                    <span class="menu-label">সকল গ্রাহক</span>
                </a>
            </li>

            <!-- Financial Section -->
            <div class="menu-section-title">আর্থিক</div>
            
            <li class="menu-item">
                <a href="payments.php" class="menu-link <?php echo isActive('payments.php', $current_page); ?>">
                    <span class="menu-icon">💳</span>
                    <span class="menu-label">পেমেন্ট</span>
                </a>
            </li>

            <li class="menu-item">
                <a href="reports.php" class="menu-link <?php echo isActive('reports.php', $current_page); ?>">
                    <span class="menu-icon">📈</span>
                    <span class="menu-label">রিপোর্ট</span>
                </a>
            </li>

            <!-- Maintenance Section -->
            <div class="menu-section-title">রক্ষণাবেক্ষণ</div>
            
            <li class="menu-item">
                <a href="maintenance.php" class="menu-link <?php echo isActive('maintenance.php', $current_page); ?>">
                    <span class="menu-icon">🔧</span>
                    <span class="menu-label">রক্ষণাবেক্ষণ</span>
                </a>
            </li>

            <li class="menu-item">
                <a href="damage-reports.php" class="menu-link <?php echo isActive('damage-reports.php', $current_page); ?>">
                    <span class="menu-icon">⚠️</span>
                    <span class="menu-label">ক্ষতি রিপোর্ট</span>
                </a>
            </li>

            <!-- Settings Section -->
            <div class="menu-section-title">সেটিংস</div>
            
            <li class="menu-item">
                <a href="employees.php" class="menu-link <?php echo isActive('employees.php', $current_page); ?>">
                    <span class="menu-icon">👔</span>
                    <span class="menu-label">কর্মচারী</span>
                </a>
            </li>

            <li class="menu-item">
                <a href="settings.php" class="menu-link <?php echo isActive('settings.php', $current_page); ?>">
                    <span class="menu-icon">⚙️</span>
                    <span class="menu-label">সেটিংস</span>
                </a>
            </li>

        <?php elseif ($current_user['role'] === 'employee'): ?>
            <!-- Employee Menu -->
            
            <li class="menu-item">
                <a href="dashboard.php" class="menu-link <?php echo isActive('dashboard.php', $current_page); ?>">
                    <span class="menu-icon">📊</span>
                    <span class="menu-label">ড্যাশবোর্ড</span>
                </a>
            </li>

            <div class="menu-section-title">মূল কার্যক্রম</div>

            <li class="menu-item">
                <a href="rentals.php" class="menu-link <?php echo isActive('rentals.php', $current_page); ?>">
                    <span class="menu-icon">📋</span>
                    <span class="menu-label">পেন্ডিং ভাড়া</span>
                </a>
            </li>

            <li class="menu-item">
                <a href="vehicles.php" class="menu-link <?php echo isActive('vehicles.php', $current_page); ?>">
                    <span class="menu-icon">🚗</span>
                    <span class="menu-label">গাড়ি দেখুন</span>
                </a>
            </li>

            <li class="menu-item">
                <a href="customers.php" class="menu-link <?php echo isActive('customers.php', $current_page); ?>">
                    <span class="menu-icon">👥</span>
                    <span class="menu-label">গ্রাহক</span>
                </a>
            </li>

            <div class="menu-section-title">ক্ষতি ও রক্ষণাবেক্ষণ</div>

            <li class="menu-item">
                <a href="damage-reports.php" class="menu-link <?php echo isActive('damage-reports.php', $current_page); ?>">
                    <span class="menu-icon">⚠️</span>
                    <span class="menu-label">ক্ষতি রিপোর্ট</span>
                </a>
            </li>

            <li class="menu-item">
                <a href="maintenance.php" class="menu-link <?php echo isActive('maintenance.php', $current_page); ?>">
                    <span class="menu-icon">🔧</span>
                    <span class="menu-label">রক্ষণাবেক্ষণ</span>
                </a>
            </li>

        <?php elseif ($current_user['role'] === 'customer'): ?>
            <!-- Customer Menu -->
            
            <li class="menu-item">
                <a href="dashboard.php" class="menu-link <?php echo isActive('dashboard.php', $current_page); ?>">
                    <span class="menu-icon">🏠</span>
                    <span class="menu-label">ড্যাশবোর্ড</span>
                </a>
            </li>

            <div class="menu-section-title">ভাড়া পরিষেবা</div>

            <li class="menu-item">
                <a href="vehicles.php" class="menu-link <?php echo isActive('vehicles.php', $current_page); ?>">
                    <span class="menu-icon">🚗</span>
                    <span class="menu-label">গাড়ি ব্রাউজ করুন</span>
                </a>
            </li>

            <li class="menu-item">
                <a href="bookings.php" class="menu-link <?php echo isActive('bookings.php', $current_page); ?>">
                    <span class="menu-icon">📅</span>
                    <span class="menu-label">আমার বুকিং</span>
                </a>
            </li>

            <div class="menu-section-title">অ্যাকাউন্ট</div>

            <li class="menu-item">
                <a href="invoices.php" class="menu-link <?php echo isActive('invoices.php', $current_page); ?>">
                    <span class="menu-icon">🧾</span>
                    <span class="menu-label">আমার ইনভয়েস</span>
                </a>
            </li>

            <li class="menu-item">
                <a href="profile.php" class="menu-link <?php echo isActive('profile.php', $current_page); ?>">
                    <span class="menu-icon">👤</span>
                    <span class="menu-label">আমার প্রোফাইল</span>
                </a>
            </li>

            <li class="menu-item">
                <a href="reviews.php" class="menu-link <?php echo isActive('reviews.php', $current_page); ?>">
                    <span class="menu-icon">⭐</span>
                    <span class="menu-label">আমার রিভিউ</span>
                </a>
            </li>

        <?php endif; ?>
    </ul>
</aside>

<script>
    // Set menu as open if current page is in submenu
    const currentPage = '<?php echo $current_page; ?>';
    const submenus = document.querySelectorAll('.submenu.open');
    submenus.forEach(submenu => {
        const toggle = submenu.previousElementSibling.querySelector('.menu-toggle');
        if (toggle) {
            toggle.classList.add('rotated');
        }
    });
</script>
