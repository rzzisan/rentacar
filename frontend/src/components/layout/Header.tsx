import { useState, useRef, useEffect } from 'react';
import type { User } from '@/types';
import ProfileModal from '@/components/ProfileModal';

interface Props {
  user: User;
  pageTitle?: string;
  onToggleSidebar: () => void;
  onLogout: () => void;
  onUserUpdate: (user: User) => void;
}

const roleLabel: Record<string, string> = {
  superadmin: 'সুপার অ্যাডমিন',
  admin: 'এডমিন',
  manager: 'ম্যানেজার',
  employee: 'কর্মচারী',
  customer: 'গ্রাহক',
  driver: 'চালক',
};

export default function Header({ user, pageTitle, onToggleSidebar, onLogout, onUserUpdate }: Props) {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const dashboardUrl = `/${user.role}`;

  return (
    <>
      <header className="sticky top-0 z-[200] flex items-center justify-between px-4 md:px-6 bg-white border-b border-slate-200 h-16 shadow-sm flex-shrink-0">
        {/* Left */}
        <div className="flex items-center gap-3 md:gap-4">
          <button
            onClick={onToggleSidebar}
            className="w-9 h-9 flex items-center justify-center rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-100 hover:text-indigo-600 hover:border-indigo-300 transition-colors"
            title="সাইডবার টগল"
          >
            ☰
          </button>
          {pageTitle && (
            <span className="hidden sm:block text-sm font-semibold text-slate-800">
              {pageTitle}
            </span>
          )}
        </div>

        {/* Right */}
        <div className="flex items-center gap-2">
          {/* Notification */}
          <button className="relative w-9 h-9 flex items-center justify-center rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-100 hover:text-indigo-600 hover:border-indigo-300 transition-colors">
            🔔
            <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white" />
          </button>

          {/* User dropdown */}
          <div className="relative" ref={wrapperRef}>
            <button
              onClick={() => setDropdownOpen(p => !p)}
              className="flex items-center gap-2 px-2 py-1.5 rounded-lg border border-transparent hover:bg-slate-100 hover:border-slate-200 transition-colors"
            >
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-sm font-bold text-white flex-shrink-0">
                {user.username.charAt(0).toUpperCase()}
              </div>
              <div className="hidden sm:flex flex-col text-left">
                <span className="text-[13px] font-semibold text-slate-800 leading-tight">{user.username}</span>
                <span className="text-[11px] text-slate-400 leading-tight">{roleLabel[user.role]}</span>
              </div>
              <span className={`hidden sm:block text-[10px] text-slate-400 ml-1 transition-transform ${dropdownOpen ? 'rotate-180' : ''}`}>▼</span>
            </button>

            {dropdownOpen && (
              <div className="absolute right-0 top-[calc(100%+8px)] w-52 bg-white border border-slate-200 rounded-xl shadow-lg z-[300] p-2 animate-in fade-in slide-in-from-top-1 duration-150">
                <div className="px-3 py-2 mb-2 border-b border-slate-100">
                  <div className="text-[13px] font-semibold text-slate-800">{user.username}</div>
                  <div className="text-[11px] text-slate-400">{user.email}</div>
                </div>
                <a
                  href={dashboardUrl}
                  className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px] text-slate-600 hover:bg-slate-100 hover:text-indigo-600 no-underline transition-colors"
                >
                  <span>🏠</span> ড্যাশবোর্ড
                </a>
                {user.role !== 'superadmin' && (
                  <button
                    onClick={() => { setDropdownOpen(false); setProfileOpen(true); }}
                    className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px] text-slate-600 hover:bg-slate-100 hover:text-indigo-600 transition-colors w-full text-left"
                  >
                    <span>⚙️</span> অ্যাকাউন্ট সেটিংস
                  </button>
                )}
                <div className="my-2 h-px bg-slate-100" />
                <button
                  onClick={() => { if (confirm('লগআউট করতে চান?')) onLogout(); }}
                  className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px] text-red-600 hover:bg-red-50 transition-colors w-full text-left"
                >
                  <span>➤</span> লগআউট
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <ProfileModal
        isOpen={profileOpen}
        user={user}
        onClose={() => setProfileOpen(false)}
        onUpdate={(updatedUser) => { onUserUpdate(updatedUser); }}
      />
    </>
  );
}
