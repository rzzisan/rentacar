import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '@/api/client';
import { roleHome } from '@/lib/roleHome';
import type { User } from '@/types';

interface Props {
  onLogin: (user: User) => void;
}

interface FormState {
  name: string;
  email: string;
  phone: string;
  address: string;
  admin_name: string;
  admin_email: string;
  admin_phone: string;
  admin_password: string;
}

const emptyForm = (): FormState => ({
  name: '', email: '', phone: '', address: '',
  admin_name: '', admin_email: '', admin_phone: '', admin_password: '',
});

export default function Register({ onLogin }: Props) {
  const navigate = useNavigate();
  const [form, setForm] = useState<FormState>(emptyForm());
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function set<K extends keyof FormState>(key: K, value: string) {
    setForm(f => ({ ...f, [key]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await api.post<User>('/auth/register.php', form);
      if (res.success && res.data) {
        onLogin(res.data);
        navigate(roleHome(res.data.role), { replace: true });
      } else {
        setError(res.message ?? 'নিবন্ধন ব্যর্থ হয়েছে');
      }
    } catch {
      setError('সার্ভারের সাথে সংযোগ ব্যর্থ হয়েছে');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-600 via-purple-600 to-indigo-800 flex items-center justify-center p-4 py-8">
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
            আপনার রেন্ট-এ-কার ব্যবসা আজই শুরু করুন — সম্পূর্ণ ফ্রি ট্রায়াল দিয়ে।
          </p>
          <ul className="space-y-3">
            {[
              'কোনো ক্রেডিট কার্ড লাগবে না',
              'গাড়ি/ড্রাইভার/ম্যানেজার সংযোজন',
              'ট্রিপ ও পেমেন্ট ট্র্যাকিং',
              'রিয়েল-টাইম ড্যাশবোর্ড ও রিপোর্ট',
            ].map(f => (
              <li key={f} className="flex items-center gap-2 text-indigo-100">
                <span className="w-5 h-5 rounded-full bg-green-400 flex items-center justify-center text-xs text-white font-bold flex-shrink-0">✓</span>
                {f}
              </li>
            ))}
          </ul>
        </div>

        {/* Right — register form */}
        <div className="bg-white rounded-2xl shadow-2xl p-8">
          <div className="flex items-center gap-2 mb-6 md:hidden">
            <span className="text-2xl">🚗</span>
            <h1 className="text-xl font-bold text-slate-800">কার রেন্টাল</h1>
          </div>
          <h2 className="text-2xl font-bold text-slate-800 mb-1">নতুন ব্যবসা নিবন্ধন</h2>
          <p className="text-slate-500 text-sm mb-6">ফ্রি ট্রায়াল দিয়ে শুরু করুন</p>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wide">ব্যবসার তথ্য</div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">ব্যবসার নাম *</label>
              <input required value={form.name} onChange={e => set('name', e.target.value)}
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
                placeholder="যেমন: রহিম কার রেন্টাল" />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">ব্যবসার ইমেইল *</label>
                <input type="email" required value={form.email} onChange={e => set('email', e.target.value)}
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">ব্যবসার মোবাইল</label>
                <input value={form.phone} onChange={e => set('phone', e.target.value)}
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition" />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">ঠিকানা</label>
              <input value={form.address} onChange={e => set('address', e.target.value)}
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition" />
            </div>

            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wide pt-2">আপনার (Admin) তথ্য</div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">আপনার নাম *</label>
              <input required value={form.admin_name} onChange={e => set('admin_name', e.target.value)}
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition" />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">আপনার ইমেইল *</label>
                <input type="email" required value={form.admin_email} onChange={e => set('admin_email', e.target.value)}
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">আপনার মোবাইল *</label>
                <input required value={form.admin_phone} onChange={e => set('admin_phone', e.target.value)}
                  className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
                  placeholder="01XXXXXXXXX" />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">পাসওয়ার্ড *</label>
              <input type="password" required minLength={6} value={form.admin_password} onChange={e => set('admin_password', e.target.value)}
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
                placeholder="কমপক্ষে ৬ অক্ষর" />
              <p className="text-xs text-slate-400 mt-1">এই ইমেইল/মোবাইল ও পাসওয়ার্ড দিয়েই পরে লগইন করবেন</p>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white font-semibold rounded-lg text-sm transition"
            >
              {loading ? 'নিবন্ধন হচ্ছে…' : 'ফ্রি ট্রায়াল শুরু করুন'}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500 mt-6">
            আগে থেকে অ্যাকাউন্ট আছে? <Link to="/login" className="text-indigo-600 font-medium hover:underline">লগইন করুন</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
