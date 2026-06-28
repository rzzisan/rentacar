import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import AppLayout from '@/layouts/AppLayout';
import AdminDashboard from '@/pages/admin/Dashboard';
import AdminVehicles from '@/pages/admin/Vehicles';
import AdminDrivers from '@/pages/admin/Drivers';
import AdminManagers from '@/pages/admin/Managers';
import AdminRentals from '@/pages/admin/Rentals';
import AdminSettlements from '@/pages/admin/Settlements';
import DriverCollections from '@/pages/admin/DriverCollections';
import DriverDashboard from '@/pages/driver/Dashboard';
import DriverRentals from '@/pages/driver/Rentals';
import DriverProfile from '@/pages/driver/Profile';
import ManagerDashboard from '@/pages/manager/Dashboard';
import ManagerVehicles from '@/pages/manager/Vehicles';
import ManagerRentals from '@/pages/manager/Rentals';
import ManagerSettlements from '@/pages/manager/Settlements';
import ManagerDrivers from '@/pages/manager/Drivers';
import ManagerDriverCollections from '@/pages/manager/DriverCollections';
import ManagerReports from '@/pages/manager/Reports';
import AdminCustomers from '@/pages/admin/Customers';
import AdminReports from '@/pages/admin/Reports';
import AdminMaintenance from '@/pages/admin/Maintenance';
import Login from '@/pages/Login';
import { api } from '@/api/client';
import type { User } from '@/types';

function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl md:text-2xl font-bold text-slate-800">{title}</h1>
      </div>
      <div className="bg-white rounded-xl border border-slate-200 p-12 shadow-sm text-center">
        <div className="text-4xl mb-3">🚧</div>
        <p className="text-slate-500 text-sm">এই পেজটি তৈরি হচ্ছে</p>
      </div>
    </div>
  );
}

function ProtectedRoute({
  user,
  role,
  children,
}: {
  user: User | null;
  role?: string;
  children: React.ReactNode;
}) {
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) {
    const dest = user.role === 'admin' ? '/admin'
               : user.role === 'manager' ? '/manager'
               : user.role === 'employee' ? '/employee'
               : user.role === 'driver' ? '/driver'
               : '/customer';
    return <Navigate to={dest} replace />;
  }
  return <>{children}</>;
}

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<User>('/auth/me.php')
      .then(res => {
        setUser(res.success && res.data ? res.data : null);
      })
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  function handleLogin(u: User) {
    setUser(u);
  }

  async function handleLogout() {
    await api.post('/auth/logout.php', {}).catch(() => {});
    setUser(null);
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-slate-500 text-sm">লোড হচ্ছে…</p>
        </div>
      </div>
    );
  }

  return (
    <BrowserRouter>
      <Routes>
        {/* Login */}
        <Route
          path="/login"
          element={
            user
              ? <Navigate to={user.role === 'admin' ? '/admin' : user.role === 'manager' ? '/manager' : user.role === 'employee' ? '/employee' : user.role === 'driver' ? '/driver' : '/customer'} replace />
              : <Login onLogin={handleLogin} />
          }
        />

        {/* Admin */}
        <Route
          path="/admin"
          element={
            <ProtectedRoute user={user} role="admin">
              <AppLayout user={user!} onLogout={handleLogout} onUserUpdate={setUser} />
            </ProtectedRoute>
          }
        >
          <Route index element={<AdminDashboard />} />
          <Route path="vehicles"    element={<AdminVehicles />} />
          <Route path="rentals"     element={<AdminRentals />} />
          <Route path="settlements" element={<AdminSettlements />} />
          <Route path="driver-collections" element={<DriverCollections />} />
          <Route path="customers"   element={<AdminCustomers />} />
          <Route path="drivers"     element={<AdminDrivers />} />
          <Route path="managers"    element={<AdminManagers />} />
          <Route path="employees"   element={<PlaceholderPage title="কর্মচারী" />} />
          <Route path="maintenance" element={<AdminMaintenance />} />
          <Route path="reports"     element={<AdminReports />} />
          <Route path="settings"    element={<PlaceholderPage title="সেটিংস" />} />
        </Route>

        {/* Manager */}
        <Route
          path="/manager"
          element={
            <ProtectedRoute user={user} role="manager">
              <AppLayout user={user!} onLogout={handleLogout} onUserUpdate={setUser} />
            </ProtectedRoute>
          }
        >
          <Route index element={<ManagerDashboard />} />
          <Route path="vehicles"          element={<ManagerVehicles />} />
          <Route path="rentals"           element={<ManagerRentals />} />
          <Route path="settlements"       element={<ManagerSettlements />} />
          <Route path="driver-collections" element={<ManagerDriverCollections />} />
          <Route path="drivers"           element={<ManagerDrivers />} />
          <Route path="reports"           element={<ManagerReports />} />
        </Route>

        {/* Employee */}
        <Route
          path="/employee"
          element={
            <ProtectedRoute user={user} role="employee">
              <AppLayout user={user!} onLogout={handleLogout} onUserUpdate={setUser} />
            </ProtectedRoute>
          }
        >
          <Route index element={<PlaceholderPage title="কর্মচারী ড্যাশবোর্ড" />} />
          <Route path="vehicles"  element={<PlaceholderPage title="গাড়ি" />} />
          <Route path="rentals"   element={<PlaceholderPage title="রেন্টাল" />} />
          <Route path="customers" element={<PlaceholderPage title="গ্রাহক" />} />
        </Route>

        {/* Customer */}
        <Route
          path="/customer"
          element={
            <ProtectedRoute user={user} role="customer">
              <AppLayout user={user!} onLogout={handleLogout} onUserUpdate={setUser} />
            </ProtectedRoute>
          }
        >
          <Route index element={<PlaceholderPage title="গ্রাহক ড্যাশবোর্ড" />} />
          <Route path="vehicles" element={<PlaceholderPage title="গাড়ি খুঁজুন" />} />
          <Route path="bookings" element={<PlaceholderPage title="আমার বুকিং" />} />
          <Route path="invoices" element={<PlaceholderPage title="পেমেন্ট" />} />
          <Route path="profile"  element={<PlaceholderPage title="প্রোফাইল" />} />
        </Route>

        {/* Driver */}
        <Route
          path="/driver"
          element={
            <ProtectedRoute user={user} role="driver">
              <AppLayout user={user!} onLogout={handleLogout} onUserUpdate={setUser} />
            </ProtectedRoute>
          }
        >
          <Route index element={<DriverDashboard />} />
          <Route path="rentals" element={<DriverRentals />} />
          <Route path="profile" element={<DriverProfile />} />
        </Route>

        {/* Root redirect */}
        <Route
          path="/"
          element={
            user
              ? <Navigate to={user.role === 'admin' ? '/admin' : user.role === 'manager' ? '/manager' : user.role === 'employee' ? '/employee' : user.role === 'driver' ? '/driver' : '/customer'} replace />
              : <Navigate to="/login" replace />
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
