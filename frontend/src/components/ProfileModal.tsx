import { useState, useEffect } from 'react';
import type { User } from '@/types';
import { api } from '@/api/client';

interface Props {
  isOpen: boolean;
  user: User;
  onClose: () => void;
  onUpdate: (user: User) => void;
}

type Tab = 'email' | 'password';

export default function ProfileModal({ isOpen, user, onClose, onUpdate }: Props) {
  const [activeTab, setActiveTab] = useState<Tab>('email');

  const [email, setEmail] = useState(user.email);
  const [emailCurrentPw, setEmailCurrentPw] = useState('');

  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [confirmPw, setConfirmPw] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [showEmailPw, setShowEmailPw] = useState(false);
  const [showCurrentPw, setShowCurrentPw] = useState(false);
  const [showNewPw, setShowNewPw] = useState(false);
  const [showConfirmPw, setShowConfirmPw] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setActiveTab('email');
      setEmail(user.email);
      setEmailCurrentPw('');
      setCurrentPw('');
      setNewPw('');
      setConfirmPw('');
      setError('');
      setSuccess('');
      setShowEmailPw(false);
      setShowCurrentPw(false);
      setShowNewPw(false);
      setShowConfirmPw(false);
    }
  }, [isOpen, user.email]);

  useEffect(() => {
    setError('');
    setSuccess('');
  }, [activeTab]);

  if (!isOpen) return null;

  async function handleEmailSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (email === user.email) {
      setError('নতুন ইমেইল ঠিকানা বর্তমানটির থেকে আলাদা হতে হবে');
      return;
    }

    setLoading(true);
    const res = await api.put<User>('/auth/update_profile.php', {
      email,
      current_password: emailCurrentPw,
    });
    setLoading(false);

    if (res.success && res.data) {
      setSuccess('ইমেইল সফলভাবে পরিবর্তন হয়েছে');
      setEmailCurrentPw('');
      onUpdate(res.data);
    } else {
      setError(res.message || 'আপডেট ব্যর্থ হয়েছে');
    }
  }

  async function handlePasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (newPw.length < 6) {
      setError('পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে');
      return;
    }
    if (newPw !== confirmPw) {
      setError('পাসওয়ার্ড দুটি মিলছে না');
      return;
    }

    setLoading(true);
    const res = await api.put<User>('/auth/update_profile.php', {
      current_password: currentPw,
      new_password: newPw,
    });
    setLoading(false);

    if (res.success && res.data) {
      setSuccess('পাসওয়ার্ড সফলভাবে পরিবর্তন হয়েছে');
      setCurrentPw('');
      setNewPw('');
      setConfirmPw('');
      onUpdate(res.data);
    } else {
      setError(res.message || 'আপডেট ব্যর্থ হয়েছে');
    }
  }

  const inputClass =
    'w-full px-3 py-2.5 rounded-lg border border-slate-300 text-sm text-slate-800 ' +
    'focus:outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 transition-all';

  function EyeBtn({ show, onToggle }: { show: boolean; onToggle: () => void }) {
    return (
      <button
        type="button"
        onClick={onToggle}
        className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 text-sm leading-none"
        tabIndex={-1}
      >
        {show ? '🙈' : '👁️'}
      </button>
    );
  }

  return (
    <div className="fixed inset-0 z-[400] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />

      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        {/* Modal header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-sm font-bold text-white">
              {user.username.charAt(0).toUpperCase()}
            </div>
            <div>
              <p className="text-sm font-bold text-slate-800 leading-tight">{user.username}</p>
              <p className="text-[11px] text-slate-400 leading-tight">{user.email}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors text-lg leading-none"
          >
            ✕
          </button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-slate-200">
          {(['email', 'password'] as Tab[]).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex-1 py-3 text-sm font-medium transition-colors border-b-2 ${
                activeTab === tab
                  ? 'text-indigo-600 border-indigo-600'
                  : 'text-slate-500 hover:text-slate-700 border-transparent'
              }`}
            >
              {tab === 'email' ? '✉️ ইমেইল পরিবর্তন' : '🔒 পাসওয়ার্ড পরিবর্তন'}
            </button>
          ))}
        </div>

        <div className="px-6 py-5">
          {error && (
            <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-start gap-2">
              <span className="flex-shrink-0 mt-0.5">⚠️</span>
              <span>{error}</span>
            </div>
          )}
          {success && (
            <div className="mb-4 px-4 py-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700 flex items-center gap-2">
              <span>✅</span>
              <span>{success}</span>
            </div>
          )}

          {/* Email Tab */}
          {activeTab === 'email' && (
            <form onSubmit={handleEmailSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                  নতুন ইমেইল ঠিকানা
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  className={inputClass}
                  placeholder="নতুন ইমেইল লিখুন"
                  required
                  autoComplete="email"
                />
                <p className="mt-1 text-[11px] text-slate-400">বর্তমান: {user.email}</p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                  বর্তমান পাসওয়ার্ড <span className="text-red-400">(নিশ্চিত করতে)</span>
                </label>
                <div className="relative">
                  <input
                    type={showEmailPw ? 'text' : 'password'}
                    value={emailCurrentPw}
                    onChange={e => setEmailCurrentPw(e.target.value)}
                    className={inputClass + ' pr-10'}
                    placeholder="বর্তমান পাসওয়ার্ড"
                    required
                    autoComplete="current-password"
                  />
                  <EyeBtn show={showEmailPw} onToggle={() => setShowEmailPw(p => !p)} />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-semibold rounded-lg transition-colors flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    সংরক্ষণ হচ্ছে...
                  </>
                ) : 'ইমেইল আপডেট করুন'}
              </button>
            </form>
          )}

          {/* Password Tab */}
          {activeTab === 'password' && (
            <form onSubmit={handlePasswordSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                  বর্তমান পাসওয়ার্ড
                </label>
                <div className="relative">
                  <input
                    type={showCurrentPw ? 'text' : 'password'}
                    value={currentPw}
                    onChange={e => setCurrentPw(e.target.value)}
                    className={inputClass + ' pr-10'}
                    placeholder="বর্তমান পাসওয়ার্ড"
                    required
                    autoComplete="current-password"
                  />
                  <EyeBtn show={showCurrentPw} onToggle={() => setShowCurrentPw(p => !p)} />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                  নতুন পাসওয়ার্ড
                </label>
                <div className="relative">
                  <input
                    type={showNewPw ? 'text' : 'password'}
                    value={newPw}
                    onChange={e => setNewPw(e.target.value)}
                    className={inputClass + ' pr-10'}
                    placeholder="কমপক্ষে ৬ অক্ষর"
                    required
                    minLength={6}
                    autoComplete="new-password"
                  />
                  <EyeBtn show={showNewPw} onToggle={() => setShowNewPw(p => !p)} />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                  নতুন পাসওয়ার্ড নিশ্চিত করুন
                </label>
                <div className="relative">
                  <input
                    type={showConfirmPw ? 'text' : 'password'}
                    value={confirmPw}
                    onChange={e => setConfirmPw(e.target.value)}
                    className={`${inputClass} pr-10 ${
                      confirmPw && confirmPw !== newPw
                        ? 'border-red-400 focus:border-red-500 focus:ring-red-500/20'
                        : confirmPw && confirmPw === newPw
                        ? 'border-green-400 focus:border-green-500 focus:ring-green-500/20'
                        : ''
                    }`}
                    placeholder="পাসওয়ার্ড পুনরায় লিখুন"
                    required
                    autoComplete="new-password"
                  />
                  <EyeBtn show={showConfirmPw} onToggle={() => setShowConfirmPw(p => !p)} />
                </div>
                {confirmPw && confirmPw !== newPw && (
                  <p className="mt-1 text-[11px] text-red-500">পাসওয়ার্ড দুটি মিলছে না</p>
                )}
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-semibold rounded-lg transition-colors flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    সংরক্ষণ হচ্ছে...
                  </>
                ) : 'পাসওয়ার্ড আপডেট করুন'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
