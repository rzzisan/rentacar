<?php
/**
 * Header Component - Consistent header for all pages
 * Include this file at the top of every page after session/auth checks
 */

// Get current user role from session or User class
$current_user = $user->getCurrentUser() ?? ['username' => 'User', 'role' => 'guest'];
$user_role_label = ucfirst($current_user['role']);

// Role labels in Bengali
$role_labels = [
    'admin' => 'এডমিন',
    'employee' => 'কর্মচারী',
    'customer' => 'গ্রাহক'
];

$role_display = $role_labels[$current_user['role']] ?? $user_role_label;
?>

<header class="header">
    <div class="header-left">
        <button class="sidebar-toggle" id="sidebarToggle" title="সাইডবার টগল করুন">☰</button>
        <a href="<?php echo $current_user['role'] === 'admin' ? '/admin/dashboard.php' : ($current_user['role'] === 'employee' ? '/employee/dashboard.php' : '/customer/dashboard.php'); ?>" class="header-brand">
            🚗 <?php echo APP_NAME; ?>
        </a>
    </div>

    <div class="header-right">
        <div class="user-info">
            <div class="user-avatar">👤</div>
            <div>
                <div class="user-name"><?php echo htmlspecialchars($current_user['username']); ?></div>
                <div class="user-role"><?php echo $role_display; ?></div>
            </div>
        </div>
        <a href="../api/logout.php" class="logout-btn">লগআউট</a>
    </div>
</header>

<script>
    // Sidebar toggle functionality
    document.getElementById('sidebarToggle').addEventListener('click', function() {
        const sidebar = document.querySelector('.sidebar');
        if (sidebar) {
            sidebar.classList.toggle('collapsed');
            // Save preference
            localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed'));
        }
    });

    // Restore sidebar state from localStorage
    window.addEventListener('load', function() {
        const sidebar = document.querySelector('.sidebar');
        if (sidebar && localStorage.getItem('sidebarCollapsed') === 'true') {
            sidebar.classList.add('collapsed');
        }
    });

    // Submenu toggle
    const menuItems = document.querySelectorAll('.menu-item.has-submenu > .menu-link');
    menuItems.forEach(link => {
        link.addEventListener('click', function(e) {
            // Only toggle if not a link click
            if (e.target.tagName !== 'A' || e.currentTarget.classList.contains('menu-link')) {
                e.preventDefault();
                
                const submenu = this.nextElementSibling;
                const toggle = this.querySelector('.menu-toggle');
                
                if (submenu && submenu.classList.contains('submenu')) {
                    submenu.classList.toggle('open');
                    toggle.classList.toggle('rotated');
                }
            }
        });
    });
</script>
