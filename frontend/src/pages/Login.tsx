import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '@/api/client';
import type { User } from '@/types';

interface Props {
  onLogin: (user: User) => void;
}

export default function Login({ onLogin }: Props) {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await api.post<User>('/auth/login.php', { email, password, remember_me: rememberMe });
      if (res.success && res.data) {
        onLogin(res.data);
        const role = res.data.role;
        const dest = role === 'admin' ? '/admin'
                   : role === 'manager' ? '/manager'
                   : role === 'driver' ? '/driver'
                   : role === 'employee' ? '/employee'
                   : '/customer';
        navigate(dest, { replace: true });
      } else {
        setError(res.message ?? 'লগইন ব্যর্থ হয়েছে');
      }
    } catch {
      setError('সার্ভারের সাথে সংযোগ ব্যর্থ হয়েছে');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-600 via-purple-600 to-indigo-800 flex items-center justify-center p-4">
      <div className="w-full max-w-4xl grid md:grid-cols-2 gap-8 items-center">

        {/* Left — branding */}
        <div className="hidden md:block text-white">
          <div className="flex items-center gap-3 mb-6">
            <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center text-2xl">🚗</div>
            <div>
              <h1 className="text-2xl font-bold">কার রেন্টাল</h1>
              <p className="text-indigo-200 text-sm">ম্যানেজমেন্ট সিস্টেম</p>
            </div>
          </div>
          <p className="text-lg text-indigo-100 mb-6 leading-relaxed">
            আপনার গাড়ি ভাড়া ব্যবসা পরিচালনা করুন সহজভাবে এবং দক্ষতার সাথে।
          </p>
          <ul className="space-y-3">
            {[
              'সম্পূর্ণ গাড়ি ইনভেন্টরি ম্যানেজমেন্ট',
              'বুকিং এবং রিজার্ভেশন সিস্টেম',
              'পেমেন্ট ট্র্যাকিং এবং রিপোর্টিং',
              'রিয়েল-টাইম ড্যাশবোর্ড',
            ].map(f => (
              <li key={f} className="flex items-center gap-2 text-indigo-100">
                <span className="w-5 h-5 rounded-full bg-green-400 flex items-center justify-center text-xs text-white font-bold flex-shrink-0">✓</span>
                {f}
              </li>
            ))}
          </ul>
        </div>

        {/* Right — login form */}
        <div className="bg-white rounded-2xl shadow-2xl p-8">
          <div className="flex items-center gap-2 mb-6 md:hidden">
            <span className="text-2xl">🚗</span>
            <h1 className="text-xl font-bold text-slate-800">কার রেন্টাল</h1>
          </div>
          <h2 className="text-2xl font-bold text-slate-800 mb-1">লগইন</h2>
          <p className="text-slate-500 text-sm mb-6">আপনার অ্যাকাউন্টে প্রবেশ করুন</p>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">ইমেইল</label>
              <input
                type="email"
                required
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
                placeholder="আপনার ইমেইল"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">পাসওয়ার্ড</label>
              <input
                type="password"
                required
                value={password}
                onChange={e => setPassword(e.target.value)}
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
                placeholder="পাসওয়ার্ড"
              />
            </div>
            <label className="flex items-center gap-2.5 cursor-pointer select-none">
              <div className="relative">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={e => setRememberMe(e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-9 h-5 bg-slate-200 peer-checked:bg-indigo-600 rounded-full transition-colors" />
                <div className="absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform peer-checked:translate-x-4" />
              </div>
              <span className="text-sm text-slate-600">আমাকে মনে রাখুন <span className="text-slate-400 text-xs">(৩ মাস)</span></span>
            </label>
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white font-semibold rounded-lg text-sm transition"
            >
              {loading ? 'লগইন হচ্ছে…' : 'লগইন করুন'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
