import { useState, useEffect } from 'react';
import { api } from '@/api/client';

interface Stats {
  total_vehicles: number;
  available_vehicles: number;
  active_rentals: number;
  pending_rentals: number;
  total_customers: number;
  monthly_revenue: number;
}

const statCards = (s: Stats) => [
  {
    label: 'মোট গাড়ি',
    value: s.total_vehicles,
    sub: `${s.available_vehicles} উপলব্ধ`,
    icon: '🚗',
    color: 'from-indigo-500 to-violet-600',
    bg: 'bg-indigo-50',
  },
  {
    label: 'সক্রিয় রেন্টাল',
    value: s.active_rentals,
    sub: `${s.pending_rentals} অপেক্ষমাণ`,
    icon: '📋',
    color: 'from-orange-400 to-orange-500',
    bg: 'bg-orange-50',
  },
  {
    label: 'মোট গ্রাহক',
    value: s.total_customers,
    sub: 'নিবন্ধিত',
    icon: '👥',
    color: 'from-sky-500 to-blue-600',
    bg: 'bg-sky-50',
  },
  {
    label: 'মাসিক আয়',
    value: '৳' + s.monthly_revenue.toLocaleString('bn-BD'),
    sub: 'এই মাসে',
    icon: '💰',
    color: 'from-green-500 to-emerald-600',
    bg: 'bg-green-50',
  },
];

export default function AdminDashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get<Stats>('/admin/stats.php')
      .then(res => {
        if (res.success && res.data) setStats(res.data);
        else setError(res.message ?? 'ডেটা লোড ব্যর্থ');
      })
      .catch(() => setError('সার্ভার সংযোগ ব্যর্থ'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl md:text-2xl font-bold text-slate-800">ড্যাশবোর্ড</h1>
        <p className="text-sm text-slate-500 mt-0.5">এডমিন প্যানেল</p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm mb-4">
          {error}
        </div>
      )}

      {/* Stats grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {loading
          ? Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm animate-pulse">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-slate-200 flex-shrink-0" />
                  <div className="space-y-2 flex-1">
                    <div className="h-6 bg-slate-200 rounded w-12" />
                    <div className="h-3 bg-slate-200 rounded w-16" />
                  </div>
                </div>
              </div>
            ))
          : stats && statCards(stats).map(card => (
              <div key={card.label} className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm hover:shadow-md transition-shadow">
                <div className="flex items-center gap-3">
                  <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${card.color} flex items-center justify-center text-xl flex-shrink-0`}>
                    {card.icon}
                  </div>
                  <div className="min-w-0">
                    <div className="text-2xl font-bold text-slate-800 truncate">{card.value}</div>
                    <div className="text-xs text-slate-500 truncate">{card.label}</div>
                    <div className="text-[11px] text-slate-400 truncate">{card.sub}</div>
                  </div>
                </div>
              </div>
            ))
        }
      </div>

      {/* Quick links */}
      {stats && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[
            { title: 'গাড়ি ব্যবস্থাপনা', desc: 'গাড়ি যোগ, সম্পাদনা এবং স্ট্যাটাস দেখুন', href: '/admin/vehicles', icon: '🚗', color: 'border-indigo-200 hover:border-indigo-400' },
            { title: 'রেন্টাল', desc: 'সক্রিয় ও অপেক্ষমাণ রেন্টাল দেখুন', href: '/admin/rentals', icon: '📋', color: 'border-orange-200 hover:border-orange-400' },
            { title: 'গ্রাহক', desc: 'নিবন্ধিত গ্রাহকদের তালিকা', href: '/admin/customers', icon: '👥', color: 'border-sky-200 hover:border-sky-400' },
          ].map(item => (
            <a
              key={item.title}
              href={item.href}
              className={`bg-white rounded-xl border p-5 shadow-sm ${item.color} transition-colors no-underline group`}
            >
              <div className="text-2xl mb-2">{item.icon}</div>
              <div className="font-semibold text-slate-800 text-sm mb-1 group-hover:text-indigo-600 transition-colors">{item.title}</div>
              <div className="text-xs text-slate-400">{item.desc}</div>
            </a>
          ))}
        </div>
      )}
    </div>
  );
}
