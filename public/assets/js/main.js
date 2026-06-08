/* ============================================
   App JavaScript
   ============================================ */

document.addEventListener('DOMContentLoaded', function () {

    // ---- Sidebar Toggle ----
    const sidebar        = document.getElementById('sidebar');
    const sidebarToggle  = document.getElementById('sidebarToggle');
    const sidebarOverlay = document.getElementById('sidebarOverlay');
    const isMobile       = () => window.innerWidth <= 768;

    function openSidebar() {
        if (isMobile()) {
            sidebar.classList.add('mobile-open');
            sidebarOverlay.classList.add('visible');
        } else {
            sidebar.classList.remove('collapsed');
            localStorage.setItem('sidebarCollapsed', 'false');
        }
    }

    function closeSidebar() {
        if (isMobile()) {
            sidebar.classList.remove('mobile-open');
            sidebarOverlay.classList.remove('visible');
        } else {
            sidebar.classList.add('collapsed');
            localStorage.setItem('sidebarCollapsed', 'true');
        }
    }

    function toggleSidebar() {
        if (isMobile()) {
            sidebar.classList.contains('mobile-open') ? closeSidebar() : openSidebar();
        } else {
            sidebar.classList.contains('collapsed') ? openSidebar() : closeSidebar();
        }
    }

    // Restore desktop collapse state
    if (!isMobile() && localStorage.getItem('sidebarCollapsed') === 'true') {
        sidebar.classList.add('collapsed');
    }

    if (sidebarToggle) sidebarToggle.addEventListener('click', toggleSidebar);
    if (sidebarOverlay) sidebarOverlay.addEventListener('click', closeSidebar);

    // Close mobile sidebar on resize to desktop
    window.addEventListener('resize', function () {
        if (!isMobile()) {
            sidebar.classList.remove('mobile-open');
            sidebarOverlay.classList.remove('visible');
        }
    });

    // ---- Submenu Toggle ----
    document.querySelectorAll('.menu-item.has-submenu > .menu-link').forEach(function (link) {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            const submenu = this.nextElementSibling;
            const toggle  = this.querySelector('.menu-toggle');
            if (submenu && submenu.classList.contains('submenu')) {
                submenu.classList.toggle('open');
                if (toggle) toggle.classList.toggle('rotated');
            }
        });
    });

    // ---- User Dropdown ----
    const wrapper  = document.getElementById('userDropdownWrapper');
    const trigger  = document.getElementById('userDropdownTrigger');

    if (trigger && wrapper) {
        trigger.addEventListener('click', function (e) {
            e.stopPropagation();
            wrapper.classList.toggle('open');
        });

        document.addEventListener('click', function (e) {
            if (!wrapper.contains(e.target)) {
                wrapper.classList.remove('open');
            }
        });
    }

    // ---- Set header page title from <h1> ----
    const h1 = document.querySelector('.main-content h1');
    const titleEl = document.getElementById('headerPageTitle');
    if (h1 && titleEl) {
        titleEl.textContent = h1.textContent;
    }

    // ---- Auto-dismiss alerts ----
    document.querySelectorAll('.alert').forEach(function (alert) {
        setTimeout(function () {
            alert.style.opacity = '0';
            alert.style.transition = 'opacity 0.5s ease';
            setTimeout(function () { alert.remove(); }, 500);
        }, 5000);
    });

});

// ---- Utility Functions ----

function confirmDelete(message) {
    return confirm(message || 'আপনি কি নিশ্চিত? এই অ্যাকশনটি পূর্বাবস্থায় ফেরানো যাবে না।');
}

function formatCurrency(amount) {
    return '৳ ' + new Intl.NumberFormat('bn-BD').format(amount);
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('bn-BD', {
        year: 'numeric', month: 'long', day: 'numeric'
    });
}

function submitForm(formId) {
    const form = document.getElementById(formId);
    if (form) form.submit();
}

function redirectTo(url) {
    window.location.href = url;
}
