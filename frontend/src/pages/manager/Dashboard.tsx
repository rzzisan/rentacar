import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '@/api/client';
import type { Rental } from '@/types';

interface Stats {
  total_vehicles: number;
  available_vehicles: number;
  active_rentals: number;
  pending_rentals: number;
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

const tripRoute = (r: { trip_type?: string; pickup_location?: string; dropoff_location?: string }) => {
  if (!r.pickup_location && !r.dropoff_location) return '—';
  const p = r.pickup_location || '?', dr = r.dropoff_location || '?';
  return r.trip_type === 'round_trip' ? `${p} → ${dr} → ${p}` : `${p} → ${dr}`;
};

export default function ManagerDashboard() {
  const [stats, setStats]   = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]   = useState('');
  const [now, setNow]       = useState(Date.now());

  useEffect(() => {
    api.get<Stats>('/manager/stats.php')
      .then(res => {
        if (res.success && res.data) setStats(res.data);
        else setError(res.message ?? 'লোড ব্যর্থ');
      })
      .catch(() => setError('নেটওয়ার্ক সমস্যা'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(id);
  }, []);

  if (loading) return (
    <div className="space-y-4">
      <div className="h-8 w-48 bg-slate-200 rounded animate-pulse" />
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[1,2,3,4].map(i => <div key={i} className="h-24 bg-slate-200 rounded-xl animate-pulse" />)}
      </div>
    </div>
  );
  if (error) return <div className="text-red-500 text-sm">{error}</div>;
  if (!stats) return null;

  const statCards = [
    { label: 'আমার গাড়ি', value: stats.total_vehicles, sub: `${stats.available_vehicles} উপলব্ধ`, icon: '🚗', color: 'from-indigo-500 to-violet-600', href: '/manager/vehicles' },
    { label: 'সক্রিয় ট্রিপ', value: stats.active_rentals, sub: `${stats.pending_rentals} অপেক্ষমাণ`, icon: '📋', color: 'from-orange-400 to-orange-500', href: '/manager/rentals' },
    { label: 'আজকের ট্রিপ', value: stats.today_trips, sub: 'আজ শুরু', icon: '📅', color: 'from-sky-500 to-blue-600', href: '/manager/rentals' },
    { label: 'মাসিক আয়', value: '৳' + stats.monthly_revenue.toLocaleString('bn-BD'), sub: 'এই মাসে', icon: '💰', color: 'from-green-500 to-emerald-600', href: '/manager/settlements' },
    { label: 'মোট বকেয়া', value: '৳' + stats.total_dues.toLocaleString('bn-BD'), sub: 'অসংগৃহীত', icon: '⏳', color: 'from-rose-500 to-red-600', href: '/manager/driver-collections' },
    { label: 'অপেক্ষমাণ ট্রিপ', value: stats.pending_rentals, sub: 'শুরু হয়নি', icon: '🕐', color: 'from-slate-500 to-slate-600', href: '/manager/rentals?status=pending' },
  ];

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl md:text-2xl font-bold text-slate-800">ম্যানেজার ড্যাশবোর্ড</h1>
        <p className="text-sm text-slate-500 mt-0.5">আপনার অধীনস্থ গাড়ির সারসংক্ষেপ</p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
        {statCards.map(c => (
          <Link key={c.label} to={c.href} className="bg-white rounded-xl border border-slate-200 shadow-sm p-4 hover:shadow-md transition-shadow no-underline">
            <div className={`w-9 h-9 rounded-lg bg-gradient-to-br ${c.color} flex items-center justify-center text-base mb-3`}>{c.icon}</div>
            <div className="text-xl font-bold text-slate-800">{c.value}</div>
            <div className="text-xs font-medium text-slate-600">{c.label}</div>
            <div className="text-xs text-slate-400 mt-0.5">{c.sub}</div>
          </Link>
        ))}
      </div>

      {/* Active trips */}
      {stats.active_trips.length > 0 && (
        <div className="mb-6">
          <h2 className="text-sm font-semibold text-slate-700 mb-3">চলমান ট্রিপ</h2>
          <div className="space-y-3">
            {stats.active_trips.map(trip => {
              const startMs = trip.actual_start_time ? new Date(trip.actual_start_time).getTime() : new Date(trip.start_date).getTime();
              return (
                <Link key={trip.id} to={`/manager/rentals?open=${trip.id}`}
                  className="block bg-white rounded-xl border border-emerald-200 shadow-sm p-4 hover:shadow-md transition-shadow no-underline">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                        <span className="text-sm font-semibold text-slate-800">
                          {trip.customer_first_name} {trip.customer_last_name}
                        </span>
                        <span className="text-xs text-slate-400">{trip.customer_phone}</span>
                      </div>
                      <div className="text-xs text-slate-500">{tripRoute(trip)}</div>
                      <div className="text-xs text-slate-400 mt-1">
                        {trip.vehicle_brand} {trip.vehicle_model} · {trip.driver_name || 'ড্রাইভার নেই'}
                      </div>
                    </div>
                    <div className="text-right flex-shrink-0">
                      <div className="text-xs font-mono text-emerald-700 bg-emerald-50 px-2 py-1 rounded-lg">
                        ⏱ {formatDuration(now - startMs)}
                      </div>
                      <div className="text-xs text-slate-400 mt-1">৳{trip.agreed_amount.toLocaleString('bn-BD')}</div>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      )}

      {/* Upcoming trips */}
      {stats.upcoming_trips.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold text-slate-700 mb-3">আসন্ন ট্রিপ</h2>
          <div className="space-y-3">
            {stats.upcoming_trips.map(trip => {
              const startMs = new Date(trip.start_date).getTime();
              const diff    = startMs - now;
              return (
                <Link key={trip.id} to={`/manager/rentals?open=${trip.id}`}
                  className="block bg-white rounded-xl border border-amber-200 shadow-sm p-4 hover:shadow-md transition-shadow no-underline">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="inline-block w-2 h-2 rounded-full bg-amber-400" />
                        <span className="text-sm font-semibold text-slate-800">
                          {trip.customer_first_name} {trip.customer_last_name}
                        </span>
                      </div>
                      <div className="text-xs text-slate-500">{tripRoute(trip)}</div>
                      <div className="text-xs text-slate-400 mt-1">
                        {trip.vehicle_brand} {trip.vehicle_model} · {trip.driver_name || 'ড্রাইভার নেই'}
                      </div>
                    </div>
                    <div className="text-right flex-shrink-0">
                      <div className="text-xs font-mono text-amber-700 bg-amber-50 px-2 py-1 rounded-lg">
                        {diff > 0 ? `⏳ ${formatDuration(diff)}` : '⚠️ বিলম্বিত'}
                      </div>
                      <div className="text-xs text-slate-400 mt-1">৳{trip.agreed_amount.toLocaleString('bn-BD')}</div>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      )}

      {stats.active_trips.length === 0 && stats.upcoming_trips.length === 0 && (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center">
          <div className="text-3xl mb-2">✅</div>
          <p className="text-slate-500 text-sm">কোনো সক্রিয় বা আসন্ন ট্রিপ নেই</p>
        </div>
      )}
    </div>
  );
}
