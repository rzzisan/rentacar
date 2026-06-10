import { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import type { Role } from '@/types';

interface NavItem {
  label: string;
  icon: string;
  href: string;
  children?: { label: string; href: string }[];
}

const adminNav: NavItem[] = [
  { label: 'ড্যাশবোর্ড', icon: '🏠', href: '/admin' },
  { label: 'গাড়ি ব্যবস্থাপনা', icon: '🚗', href: '/admin/vehicles' },
  { label: 'রেন্টাল ব্যবস্থাপনা', icon: '📋', href: '/admin/rentals' },
  { label: 'পেমেন্ট সেটেলমেন্ট', icon: '💳', href: '/admin/settlements' },
  { label: 'ড্রাইভার বকেয়া জমা', icon: '💰', href: '/admin/driver-collections' },
  { label: 'গ্রাহক ব্যবস্থাপনা', icon: '👥', href: '/admin/customers' },
  { label: 'ড্রাইভার', icon: '🧑‍✈️', href: '/admin/drivers' },
  { label: 'কর্মচারী', icon: '👤', href: '/admin/employees' },
  { label: 'রক্ষণাবেক্ষণ', icon: '🔧', href: '/admin/maintenance' },
  { label: 'রিপোর্ট', icon: '📊', href: '/admin/reports' },
  { label: 'সেটিংস', icon: '⚙️', href: '/admin/settings' },
];

const employeeNav: NavItem[] = [
  { label: 'ড্যাশবোর্ড', icon: '🏠', href: '/employee' },
  { label: 'গাড়ি', icon: '🚗', href: '/employee/vehicles' },
  { label: 'রেন্টাল', icon: '📋', href: '/employee/rentals' },
  { label: 'গ্রাহক', icon: '👥', href: '/employee/customers' },
];

const customerNav: NavItem[] = [
  { label: 'ড্যাশবোর্ড', icon: '🏠', href: '/customer' },
  { label: 'গাড়ি খুঁজুন', icon: '🔍', href: '/customer/vehicles' },
  { label: 'আমার বুকিং', icon: '📋', href: '/customer/bookings' },
  { label: 'পেমেন্ট', icon: '💳', href: '/customer/invoices' },
  { label: 'প্রোফাইল', icon: '👤', href: '/customer/profile' },
];

const driverNav: NavItem[] = [
  { label: 'আমার লেজার', icon: '📊', href: '/driver' },
];

const navByRole: Record<Role, NavItem[]> = {
  admin: adminNav,
  employee: employeeNav,
  customer: customerNav,
  driver: driverNav,
};

interface Props {
  role: Role;
  username: string;
  isOpen: boolean;
  onClose: () => void;
}

export default function Sidebar({ role, username, isOpen, onClose }: Props) {
  const [openSubs, setOpenSubs] = useState<Record<string, boolean>>({});
  const location = useLocation();
  const navItems = navByRole[role];

  const toggleSub = (href: string) =>
    setOpenSubs(prev => ({ ...prev, [href]: !prev[href] }));

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black/50 backdrop-blur-sm z-[90] lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={[
          'h-full z-[150] flex flex-col overflow-hidden min-w-0',
          'bg-[#0f172a] transition-all duration-300 ease-in-out',
          'fixed top-0 left-0 lg:relative flex-shrink-0',
          isOpen
            ? 'w-[260px] translate-x-0'
            : 'w-[260px] -translate-x-full lg:translate-x-0 lg:w-16',
        ].join(' ')}
      >
        {/* Brand */}
        <div className={`flex items-center h-16 border-b border-slate-700 flex-shrink-0 transition-all duration-300 ${isOpen ? 'gap-3 px-5' : 'gap-3 px-5 lg:justify-center lg:px-0'}`}>
          <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-lg flex-shrink-0">
            🚗
          </div>
          <div className={`transition-all duration-200 ${isOpen ? '' : 'lg:hidden'}`}>
            <div className="text-sm font-bold text-white leading-tight">CarRental</div>
            <div className="text-[10px] text-slate-500 uppercase tracking-wider">ম্যানেজমেন্ট</div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto py-4 scrollbar-thin scrollbar-thumb-slate-700">
          <ul className="list-none p-0 m-0">
            {navItems.map(item => {
              const isActive = location.pathname === item.href ||
                (item.href !== '/admin' && item.href !== '/employee' && item.href !== '/customer' &&
                  location.pathname.startsWith(item.href));

              if (item.children) {
                return (
                  <li key={item.href} className="mx-2.5 mb-0.5">
                    <button
                      onClick={() => toggleSub(item.href)}
                      title={!isOpen ? item.label : undefined}
                      className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-slate-300 hover:bg-white/5 hover:text-white transition-colors text-left ${!isOpen ? 'lg:justify-center lg:px-2.5' : ''}`}
                    >
                      <span className="w-5 text-base text-center flex-shrink-0">{item.icon}</span>
                      <span className={`flex-1 text-sm font-medium ${!isOpen ? 'lg:hidden' : ''}`}>{item.label}</span>
                      <span className={`text-[10px] text-slate-500 transition-transform ${openSubs[item.href] ? 'rotate-90' : ''} ${!isOpen ? 'lg:hidden' : ''}`}>▶</span>
                    </button>
                    {isOpen && openSubs[item.href] && (
                      <ul className="ml-4 mt-0.5 list-none p-0">
                        {item.children.map(child => (
                          <li key={child.href} className="mx-0 mb-0.5">
                            <NavLink
                              to={child.href}
                              className={({ isActive }) =>
                                `flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px] transition-colors no-underline ${
                                  isActive
                                    ? 'bg-indigo-500/20 text-indigo-400'
                                    : 'text-slate-500 hover:text-white hover:bg-white/5'
                                }`
                              }
                            >
                              <span className="w-1.5 h-1.5 rounded-full bg-slate-600 flex-shrink-0" />
                              {child.label}
                            </NavLink>
                          </li>
                        ))}
                      </ul>
                    )}
                  </li>
                );
              }

              return (
                <li key={item.href} className="mx-2.5 mb-0.5">
                  <NavLink
                    to={item.href}
                    end={item.href === '/admin' || item.href === '/employee' || item.href === '/customer'}
                    title={!isOpen ? item.label : undefined}
                    className={`flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors no-underline ${
                      isActive
                        ? 'bg-indigo-500/20 text-indigo-400 border border-indigo-500/20'
                        : 'text-slate-300 hover:bg-white/5 hover:text-white'
                    } ${!isOpen ? 'lg:justify-center lg:px-2.5' : ''}`}
                  >
                    <span className="w-5 text-base text-center flex-shrink-0">{item.icon}</span>
                    <span className={`text-[13.5px] font-medium ${!isOpen ? 'lg:hidden' : ''}`}>{item.label}</span>
                  </NavLink>
                </li>
              );
            })}
          </ul>
        </nav>

        {/* Footer */}
        <div className="px-2.5 py-3 border-t border-slate-700 flex-shrink-0">
          <div className={`flex items-center gap-2.5 px-3 py-2 rounded-lg ${!isOpen ? 'lg:justify-center lg:px-0' : ''}`}>
            <div
              className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
              title={!isOpen ? username : undefined}
            >
              {username.charAt(0).toUpperCase()}
            </div>
            <div className={`flex-1 min-w-0 ${!isOpen ? 'lg:hidden' : ''}`}>
              <div className="text-xs font-semibold text-slate-300 truncate">{username}</div>
              <div className="text-[10px] text-slate-500">
                {role === 'admin' ? 'এডমিন' : role === 'employee' ? 'কর্মচারী' : 'গ্রাহক'}
              </div>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
}
