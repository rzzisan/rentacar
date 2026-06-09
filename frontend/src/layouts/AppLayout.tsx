import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from '@/components/layout/Sidebar';
import Header from '@/components/layout/Header';
import type { User } from '@/types';

interface Props {
  user: User;
  onLogout: () => void;
  onUserUpdate: (user: User) => void;
}

const pageTitles: Record<string, string> = {
  '/admin': 'ড্যাশবোর্ড',
  '/admin/vehicles': 'গাড়ি ব্যবস্থাপনা',
  '/admin/vehicles/add': 'নতুন গাড়ি যোগ',
  '/admin/rentals': 'রেন্টাল ব্যবস্থাপনা',
  '/admin/settlements': 'পেমেন্ট সেটেলমেন্ট',
  '/admin/customers': 'গ্রাহক ব্যবস্থাপনা',
  '/admin/drivers': 'ড্রাইভার ব্যবস্থাপনা',
  '/admin/employees': 'কর্মচারী',
  '/admin/maintenance': 'রক্ষণাবেক্ষণ',
  '/admin/reports': 'রিপোর্ট',
  '/admin/settings': 'সেটিংস',
  '/employee': 'ড্যাশবোর্ড',
  '/employee/vehicles': 'গাড়ি',
  '/employee/rentals': 'রেন্টাল',
  '/employee/customers': 'গ্রাহক',
  '/customer': 'ড্যাশবোর্ড',
  '/customer/vehicles': 'গাড়ি খুঁজুন',
  '/customer/bookings': 'আমার বুকিং',
  '/customer/invoices': 'পেমেন্ট',
  '/customer/profile': 'প্রোফাইল',
};

export default function AppLayout({ user, onLogout, onUserUpdate }: Props) {
  const [sidebarOpen, setSidebarOpen] = useState(() => window.innerWidth >= 1024);
  const location = useLocation();
  const pageTitle = pageTitles[location.pathname];

  return (
    <div className="flex flex-col h-full">
      <Header
        user={user}
        pageTitle={pageTitle}
        onToggleSidebar={() => setSidebarOpen(p => !p)}
        onLogout={onLogout}
        onUserUpdate={onUserUpdate}
      />

      <div className="flex flex-1 min-h-0">
        <Sidebar
          role={user.role}
          username={user.username}
          isOpen={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
        />

        <main className="flex-1 overflow-y-auto p-4 md:p-6 min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
