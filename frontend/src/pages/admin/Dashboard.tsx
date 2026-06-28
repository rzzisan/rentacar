import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '@/api/client';
import type { Rental } from '@/types';

interface ExpiringDoc {
  id: number;
  vehicle: string;
  registration_number: string;
  doc_type: string;
  expiry_date: string;
  days_remaining: number;
}

const DOC_LABELS: Record<string, string> = {
  fitness: 'ফিটনেস', insurance: 'বীমা', tax_token: 'ট্যাক্স টোকেন',
  route_permit: 'রুট পারমিট', registration: 'রেজিস্ট্রেশন', other: 'ডকুমেন্ট',
};

interface Stats {
  total_vehicles: number;
  available_vehicles: number;
  active_rentals: number;
  pending_rentals: number;
  total_customers: number;
  monthly_revenue: number;
  total_dues: number;
  today_trips: number;
  active_trips: Rental[];
  upcoming_trips: Rental[];
}

const formatDuration = (ms: number): string => {
  const total = Math.max(0, Math.floor(ms / 1000));
  const d = Math.floor(total / 86400);
  const h = Math.floor((total % 86400) / 3600);
  const m = Math.floor((total % 3600) / 60);
  const parts: string[] = [];
  if (d) parts.push(`${d} দিন`);
  if (h) parts.push(`${h} ঘণ্টা`);
  parts.push(`${m} মিনিট`);
  return parts.join(' ');
};

// এক-দিকে: শুরু→শেষ, রাউন্ড ট্রিপ: শুরু→শেষ→শুরু
const tripRoute = (r: { trip_type?: string; pickup_location?: string; dropoff_location?: string }) => {
  if (!r.pickup_location && !r.dropoff_location) return '—';
  const pickup = r.pickup_location || '?';
  const dropoff = r.dropoff_location || '?';
  return r.trip_type === 'round_trip'
    ? `${pickup} → ${dropoff} → ${pickup}`
    : `${pickup} → ${dropoff}`;
};

const statCards = (s: Stats) => [
  {
    label: 'মোট গাড়ি',
    value: s.total_vehicles,
    sub: `${s.available_vehicles} উপলব্ধ`,
    icon: '🚗',
    color: 'from-indigo-500 to-violet-600',
    href: '/admin/vehicles',
  },
  {
    label: 'সক্রিয় ট্রিপ',
    value: s.active_rentals,
    sub: `${s.pending_rentals} অপেক্ষমাণ`,
    icon: '📋',
    color: 'from-orange-400 to-orange-500',
    href: '/admin/rentals',
  },
  {
    label: 'আজকের ট্রিপ',
    value: s.today_trips,
    sub: 'আজ শুরু',
    icon: '📅',
    color: 'from-sky-500 to-blue-600',
    href: '/admin/rentals',
  },
  {
    label: 'মাসিক আয়',
    value: '৳' + s.monthly_revenue.toLocaleString('bn-BD'),
    sub: 'এই মাসে',
    icon: '💰',
    color: 'from-green-500 to-emerald-600',
    href: '/admin/settlements',
  },
  {
    label: 'মোট বকেয়া',
    value: '৳' + s.total_dues.toLocaleString('bn-BD'),
    sub: 'অসংগৃহীত',
    icon: '⏳',
    color: 'from-rose-500 to-red-600',
    href: '/admin/driver-collections',
  },
  {
    label: 'মোট গ্রাহক',
    value: s.total_customers,
    sub: 'নিবন্ধিত',
    icon: '👥',
    color: 'from-slate-500 to-slate-600',
    href: '/admin/customers',
  },
];

export default function AdminDashboard() {
  const [stats, setStats]         = useState<Stats | null>(null);
  const [expiringDocs, setExpiring] = useState<ExpiringDoc[]>([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [now, setNow]             = useState(Date.now());

  useEffect(() => {
    Promise.all([
      api.get<Stats>('/admin/stats.php'),
      api.get<ExpiringDoc[]>('/admin/documents/index.php?expiring_days=60'),
    ]).then(([sres, dres]) => {
      if (sres.success && sres.data) setStats(sres.data);
      else setError(sres.message ?? 'ডেটা লোড ব্যর্থ');
      if (dres.success && dres.data) setExpiring(dres.data);
    }).catch(() => setError('সার্ভার সংযোগ ব্যর্থ'))
      .finally(() => setLoading(false));
  }, []);

  // চলমান/আপকামিং ট্রিপের লাইভ টাইমার ও কাউন্টডাউনের জন্য প্রতি মিনিটে টিক
  const hasTrips = !!stats && (stats.active_trips.length > 0 || stats.upcoming_trips.length > 0);
  useEffect(() => {
    if (!hasTrips) return;
    const timer = setInterval(() => setNow(Date.now()), 60000);
    return () => clearInterval(timer);
  }, [hasTrips]);

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

      {/* ডকুমেন্ট মেয়াদ সতর্কতা */}
      {expiringDocs.length > 0 && (
        <div className="mb-5 bg-amber-50 border border-amber-300 rounded-xl p-4">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-amber-600 font-bold text-sm">⚠️ মেয়াদ শেষ হচ্ছে ({expiringDocs.length}টি ডকুমেন্ট)</span>
            <Link to="/admin/maintenance" className="ml-auto text-xs text-indigo-600 hover:underline">সব দেখুন →</Link>
          </div>
          <div className="flex flex-wrap gap-2">
            {expiringDocs.slice(0, 6).map(d => (
              <div key={d.id} className={`text-xs px-2.5 py-1.5 rounded-lg border font-medium ${
                d.days_remaining < 0 ? 'bg-red-100 border-red-300 text-red-700'
                : d.days_remaining <= 15 ? 'bg-red-50 border-red-200 text-red-600'
                : d.days_remaining <= 30 ? 'bg-orange-50 border-orange-200 text-orange-600'
                : 'bg-yellow-50 border-yellow-200 text-yellow-700'
              }`}>
                <span className="font-semibold">{d.vehicle}</span> · {DOC_LABELS[d.doc_type] ?? d.doc_type}
                <span className="ml-1 opacity-80">
                  {d.days_remaining < 0 ? `(${Math.abs(d.days_remaining)}দি আগে শেষ)` : `(${d.days_remaining}দি বাকি)`}
                </span>
              </div>
            ))}
            {expiringDocs.length > 6 && (
              <div className="text-xs px-2.5 py-1.5 text-gray-500">+{expiringDocs.length - 6}টি আরও</div>
            )}
          </div>
        </div>
      )}

      {/* চলমান ট্রিপ — সবার উপরে হাইলাইট */}
      {stats && stats.active_trips.length > 0 && (
        <div className="mb-6 space-y-3">
          {stats.active_trips.map((trip) => (
            <div
              key={trip.id}
              className="bg-green-50 border-2 border-green-500 rounded-lg p-4 shadow-md ring-2 ring-green-200"
            >
              <div className="flex items-center gap-2 mb-3">
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-green-600"></span>
                </span>
                <span className="font-bold text-green-800">চলমান ট্রিপ</span>
                <span className="ml-auto text-xs font-medium text-green-700">
                  চলছে {formatDuration(now - new Date(trip.actual_start_time || trip.start_date).getTime())} ধরে
                </span>
              </div>
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <div className="text-sm">
                  <div className="font-medium text-gray-900">
                    {trip.customer_first_name} {trip.customer_last_name}
                    {trip.customer_phone && <span className="text-gray-600 font-normal"> • {trip.customer_phone}</span>}
                  </div>
                  <div className="text-gray-700 mt-1">{tripRoute(trip)}</div>
                  <div className="text-gray-600 mt-1">
                    {trip.vehicle_brand} {trip.vehicle_model}
                    {trip.driver_name && <> • চালক: {trip.driver_name}</>}
                    {' '}• চুক্তি ৳{trip.agreed_amount.toFixed(0)}
                  </div>
                </div>
                <Link
                  to={`/admin/rentals?open=${trip.id}`}
                  className="shrink-0 text-center bg-green-600 text-white px-5 py-2.5 rounded-lg font-medium hover:bg-green-700 transition"
                >
                  বিস্তারিত দেখুন
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* আপকামিং ট্রিপ — চলমান ট্রিপের নিচে */}
      {stats && stats.upcoming_trips.length > 0 && (
        <div className="mb-6 space-y-3">
          {stats.upcoming_trips.map((trip) => {
            const diff = new Date(trip.start_date).getTime() - now;
            return (
              <div
                key={trip.id}
                className="bg-amber-50 border-2 border-amber-400 rounded-lg p-4 shadow-sm"
              >
                <div className="flex items-center gap-2 mb-3">
                  <span className="inline-flex rounded-full h-3 w-3 bg-amber-500"></span>
                  <span className="font-bold text-amber-800">আপকামিং ট্রিপ</span>
                  <span className={`ml-auto text-xs font-medium ${diff > 0 ? 'text-amber-700' : 'text-red-600'}`}>
                    {diff > 0
                      ? `শুরু হতে বাকি ${formatDuration(diff)}`
                      : 'নির্ধারিত সময় পেরিয়ে গেছে'}
                  </span>
                </div>
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                  <div className="text-sm">
                    <div className="font-medium text-gray-900">
                      {trip.customer_first_name} {trip.customer_last_name}
                      {trip.customer_phone && <span className="text-gray-600 font-normal"> • {trip.customer_phone}</span>}
                    </div>
                    <div className="text-gray-700 mt-1">{tripRoute(trip)}</div>
                    <div className="text-gray-600 mt-1">
                      {trip.vehicle_brand} {trip.vehicle_model}
                      {trip.driver_name && <> • চালক: {trip.driver_name}</>}
                      {' '}• চুক্তি ৳{trip.agreed_amount.toFixed(0)} •{' '}
                      {new Date(trip.start_date).toLocaleString('bn-BD')}
                    </div>
                  </div>
                  <Link
                    to={`/admin/rentals?open=${trip.id}`}
                    className="shrink-0 text-center bg-amber-500 text-white px-5 py-2.5 rounded-lg font-medium hover:bg-amber-600 transition"
                  >
                    বিস্তারিত দেখুন
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Stats grid */}
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
        {loading
          ? Array.from({ length: 6 }).map((_, i) => (
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
              <Link
                key={card.label}
                to={card.href}
                className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm hover:shadow-md transition-shadow no-underline"
              >
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
              </Link>
            ))
        }
      </div>

      {/* Quick links */}
      {stats && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[
            { title: 'ট্রিপ ব্যবস্থাপনা', desc: 'নতুন ট্রিপ তৈরি, স্ট্যাটাস ও খরচ দেখুন', href: '/admin/rentals', icon: '📋', color: 'border-orange-200 hover:border-orange-400' },
            { title: 'সেটেলমেন্ট', desc: 'কমিশন হিসাব ও পেমেন্ট সংগ্রহ', href: '/admin/settlements', icon: '🧾', color: 'border-green-200 hover:border-green-400' },
            { title: 'বকেয়া আদায়', desc: 'ড্রাইভার অনুযায়ী বকেয়া জমা নিন', href: '/admin/driver-collections', icon: '💵', color: 'border-rose-200 hover:border-rose-400' },
            { title: 'ড্রাইভার', desc: 'ড্রাইভার ও গাড়ি অ্যাসাইনমেন্ট', href: '/admin/drivers', icon: '🧑‍✈️', color: 'border-violet-200 hover:border-violet-400' },
            { title: 'গাড়ি ব্যবস্থাপনা', desc: 'গাড়ি যোগ, সম্পাদনা এবং স্ট্যাটাস দেখুন', href: '/admin/vehicles', icon: '🚗', color: 'border-indigo-200 hover:border-indigo-400' },
            { title: 'গ্রাহক', desc: 'নিবন্ধিত গ্রাহকদের তালিকা', href: '/admin/customers', icon: '👥', color: 'border-sky-200 hover:border-sky-400' },
          ].map(item => (
            <Link
              key={item.title}
              to={item.href}
              className={`bg-white rounded-xl border p-5 shadow-sm ${item.color} transition-colors no-underline group`}
            >
              <div className="text-2xl mb-2">{item.icon}</div>
              <div className="font-semibold text-slate-800 text-sm mb-1 group-hover:text-indigo-600 transition-colors">{item.title}</div>
              <div className="text-xs text-slate-400">{item.desc}</div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
