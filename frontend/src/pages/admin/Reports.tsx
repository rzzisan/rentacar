import React, { useState, useEffect } from 'react';
import { api } from '@/api/client';

// ── Types ──────────────────────────────────────────────────────────────────

interface MonthlyPL {
  month: string;
  trip_count: number;
  revenue: number;
  trip_expenses: number;
  commission: number;
  maintenance_cost: number;
  net: number;
}

interface VehicleProfitability {
  id: number;
  brand: string;
  model: string;
  registration_number: string;
  status: string;
  year: number;
  trip_count: number;
  revenue: number;
  trip_expenses: number;
  commission: number;
  maintenance_cost: number;
  total_expenses: number;
  net: number;
  rented_days: number;
  utilization_rate: number;
}

interface DuesItem {
  settlement_id: number;
  rental_id: number;
  start_date: string;
  end_date: string;
  driver_name: string;
  driver_mobile: string;
  vehicle: string;
  registration_number: string;
  agreed_amount: number;
  driver_commission: number;
  paid_amount: number;
  remaining_amount: number;
  payment_status: string;
  days_overdue: number;
}

interface DuesAging {
  current: DuesItem[];
  days30: DuesItem[];
  days60: DuesItem[];
  days90: DuesItem[];
  total_due: number;
}

interface DriverPerf {
  id: number;
  name: string;
  mobile: string;
  commission_rate: number;
  total_trips: number;
  year_trips: number;
  month_trips: number;
  total_revenue: number;
  total_commission: number;
  total_paid: number;
  total_due: number;
  avg_trip_value: number;
}

interface CustomerAnalytics {
  id: number;
  name: string;
  phone: string;
  total_trips: number;
  completed_trips: number;
  total_spent: number;
  last_trip_date: string;
  days_since_last: number;
  is_repeat: boolean;
  is_inactive: boolean;
}

interface Summary {
  total_trips: number;
  completed_trips: number;
  unique_customers: number;
  total_revenue: number;
  total_trip_expenses: number;
  total_commission: number;
  total_maintenance: number;
  total_expenses: number;
  net_profit: number;
  total_due: number;
}

interface ReportData {
  year: number;
  monthly_pl: MonthlyPL[];
  vehicle_profitability: VehicleProfitability[];
  dues_aging: DuesAging;
  driver_performance: DriverPerf[];
  customer_analytics: CustomerAnalytics[];
  summary: Summary;
}

// ── Helpers ────────────────────────────────────────────────────────────────

const MONTH_NAMES: Record<string, string> = {
  '01': 'জানু', '02': 'ফেব্রু', '03': 'মার্চ', '04': 'এপ্রি',
  '05': 'মে',   '06': 'জুন',    '07': 'জুলাই', '08': 'আগস্ট',
  '09': 'সেপ্টে','10': 'অক্টো', '11': 'নভে',  '12': 'ডিসে',
};

const BDT = (n: number) => `৳${n.toLocaleString('en-BD', { maximumFractionDigits: 0 })}`;
const fmtMonth = (ym: string) => { const [y, m] = ym.split('-'); return `${MONTH_NAMES[m] ?? m} ${y}`; };

const VEHICLE_STATUS: Record<string, string> = {
  available: 'উপলব্ধ', rented: 'ভাড়ায়', maintenance: 'রক্ষণাবেক্ষণ', inactive: 'নিষ্ক্রিয়',
};
const V_STATUS_CLS: Record<string, string> = {
  available: 'bg-emerald-100 text-emerald-700',
  rented: 'bg-orange-100 text-orange-700',
  maintenance: 'bg-yellow-100 text-yellow-700',
  inactive: 'bg-slate-100 text-slate-500',
};

type Tab = 'pl' | 'vehicles' | 'dues' | 'drivers' | 'customers';

// ── Bar chart (CSS-only) ──────────────────────────────────────────────────

function BarChart({ data, maxVal }: { data: { label: string; value: number; color: string }[]; maxVal: number }) {
  return (
    <div className="space-y-2">
      {data.map((d, i) => (
        <div key={i} className="flex items-center gap-3">
          <div className="w-16 text-right text-xs text-gray-500 shrink-0">{d.label}</div>
          <div className="flex-1 bg-gray-100 rounded-full h-5 overflow-hidden">
            <div
              className="h-full rounded-full transition-all duration-500"
              style={{ width: `${maxVal > 0 ? (d.value / maxVal) * 100 : 0}%`, backgroundColor: d.color }}
            />
          </div>
          <div className="w-24 text-right text-xs font-semibold text-gray-700 shrink-0">{BDT(d.value)}</div>
        </div>
      ))}
    </div>
  );
}

// ── Summary stat card ─────────────────────────────────────────────────────

function StatCard({ label, value, sub, color }: { label: string; value: string; sub?: string; color: string }) {
  return (
    <div className={`rounded-xl p-4 border ${color}`}>
      <div className="text-xs text-gray-500 mb-1">{label}</div>
      <div className="text-xl font-bold">{value}</div>
      {sub && <div className="text-xs text-gray-400 mt-0.5">{sub}</div>}
    </div>
  );
}

// ── Aging badge ───────────────────────────────────────────────────────────

function AgingBadge({ days }: { days: number }) {
  const cls = days <= 30 ? 'bg-blue-100 text-blue-700'
    : days <= 60 ? 'bg-yellow-100 text-yellow-700'
    : days <= 90 ? 'bg-orange-100 text-orange-700'
    : 'bg-red-100 text-red-700';
  return <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${cls}`}>{days} দিন</span>;
}

// ── Main Component ────────────────────────────────────────────────────────

export default function AdminReports(): React.ReactElement {
  const currentYear = new Date().getFullYear();
  const [year, setYear]       = useState(currentYear);
  const [data, setData]       = useState<ReportData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');
  const [activeTab, setTab]   = useState<Tab>('pl');
  const [duesFilter, setDuesFilter] = useState<'all' | 'current' | 'days30' | 'days60' | 'days90'>('all');

  useEffect(() => {
    setLoading(true);
    setError('');
    api.get<ReportData>(`/admin/reports.php?year=${year}`)
      .then(res => {
        if (res.success && res.data) setData(res.data);
        else setError(res.message ?? 'রিপোর্ট লোড ব্যর্থ');
      })
      .catch(() => setError('নেটওয়ার্ক সমস্যা'))
      .finally(() => setLoading(false));
  }, [year]);

  const yearOptions = Array.from({ length: 4 }, (_, i) => currentYear - i);

  if (loading) return (
    <div className="p-4 sm:p-6 space-y-4 animate-pulse">
      <div className="h-8 w-48 bg-slate-200 rounded" />
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        {[...Array(5)].map((_, i) => <div key={i} className="h-20 bg-slate-100 rounded-xl" />)}
      </div>
      <div className="h-72 bg-slate-100 rounded-xl" />
    </div>
  );

  if (error || !data) return (
    <div className="p-6 text-center text-red-600">{error || 'ডেটা পাওয়া যায়নি'}</div>
  );

  const { summary, monthly_pl, vehicle_profitability, dues_aging, driver_performance, customer_analytics } = data;
  const maxRevenue = Math.max(...monthly_pl.map(m => m.revenue), 1);
  const repeatCustomers = customer_analytics.filter(c => c.is_repeat).length;
  const inactiveCustomers = customer_analytics.filter(c => c.is_inactive).length;
  const totalDuesItems = dues_aging.current.length + dues_aging.days30.length + dues_aging.days60.length + dues_aging.days90.length;

  const tabs: { key: Tab; label: string; icon: string; badge?: number }[] = [
    { key: 'pl',        label: 'লাভ-ক্ষতি',       icon: '📊' },
    { key: 'vehicles',  label: 'গাড়ি বিশ্লেষণ',   icon: '🚗' },
    { key: 'dues',      label: 'বকেয়া',            icon: '⚠️', badge: totalDuesItems },
    { key: 'drivers',   label: 'ড্রাইভার',          icon: '🧑‍✈️' },
    { key: 'customers', label: 'গ্রাহক',            icon: '👥' },
  ];

  const duesItems: DuesItem[] = duesFilter === 'all'
    ? [...dues_aging.current, ...dues_aging.days30, ...dues_aging.days60, ...dues_aging.days90]
    : dues_aging[duesFilter] ?? [];

  return (
    <div className="p-4 sm:p-6 space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">রিপোর্ট ও বিশ্লেষণ</h1>
          <p className="text-gray-500 text-sm mt-0.5">সম্পূর্ণ ব্যবসার বিশ্লেষণ</p>
        </div>
        <select
          value={year}
          onChange={e => setYear(+e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        >
          {yearOptions.map(y => <option key={y} value={y}>{y} সাল</option>)}
        </select>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
        <StatCard label="মোট ট্রিপ" value={`${summary.completed_trips}`}
          sub={`${summary.total_trips} টি বুকিং`} color="bg-blue-50 border-blue-200 text-blue-700" />
        <StatCard label="মোট রাজস্ব" value={BDT(summary.total_revenue)}
          color="bg-green-50 border-green-200 text-green-700" />
        <StatCard label="মোট খরচ" value={BDT(summary.total_expenses)}
          sub={`কমিশন + ট্রিপ + রক্ষণাবেক্ষণ`} color="bg-red-50 border-red-200 text-red-600" />
        <StatCard
          label="নিট লাভ"
          value={BDT(summary.net_profit)}
          color={summary.net_profit >= 0 ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-red-50 border-red-200 text-red-700'}
        />
        <StatCard label="বকেয়া" value={BDT(summary.total_due)}
          sub={`${totalDuesItems}টি সেটেলমেন্ট`} color="bg-amber-50 border-amber-200 text-amber-600" />
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-xl border shadow-sm overflow-hidden">
        <div className="flex overflow-x-auto border-b">
          {tabs.map(tab => (
            <button
              key={tab.key}
              onClick={() => setTab(tab.key)}
              className={`relative flex items-center gap-1.5 px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors ${
                activeTab === tab.key
                  ? 'text-indigo-600 border-b-2 border-indigo-600 bg-indigo-50/50'
                  : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
              }`}
            >
              <span>{tab.icon}</span>
              {tab.label}
              {tab.badge && tab.badge > 0 ? (
                <span className="ml-1 bg-red-500 text-white text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
                  {tab.badge > 9 ? '9+' : tab.badge}
                </span>
              ) : null}
            </button>
          ))}
        </div>

        {/* ── Tab 1: মাসিক লাভ-ক্ষতি ───────────────────────────────────── */}
        {activeTab === 'pl' && (
          <div className="p-4">
            {monthly_pl.length === 0 ? (
              <div className="py-12 text-center text-gray-400">এই বছরের কোনো ডেটা নেই</div>
            ) : (
              <>
                {/* Bar chart for revenue */}
                <div className="mb-6">
                  <h3 className="text-sm font-semibold text-gray-700 mb-3">মাসিক রাজস্ব</h3>
                  <BarChart
                    maxVal={maxRevenue}
                    data={monthly_pl.map(m => ({ label: fmtMonth(m.month), value: m.revenue, color: '#4f46e5' }))}
                  />
                </div>
                {/* Table */}
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left font-medium text-gray-600">মাস</th>
                        <th className="px-4 py-3 text-center font-medium text-gray-600">ট্রিপ</th>
                        <th className="px-4 py-3 text-right font-medium text-gray-600">রাজস্ব</th>
                        <th className="px-4 py-3 text-right font-medium text-gray-600">ট্রিপ খরচ</th>
                        <th className="px-4 py-3 text-right font-medium text-gray-600">কমিশন</th>
                        <th className="px-4 py-3 text-right font-medium text-gray-600">রক্ষণাবেক্ষণ</th>
                        <th className="px-4 py-3 text-right font-medium text-gray-600">নিট লাভ</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {monthly_pl.map(m => (
                        <tr key={m.month} className="hover:bg-gray-50">
                          <td className="px-4 py-3 font-medium text-gray-800">{fmtMonth(m.month)}</td>
                          <td className="px-4 py-3 text-center text-gray-600">{m.trip_count}</td>
                          <td className="px-4 py-3 text-right text-green-700 font-semibold">{BDT(m.revenue)}</td>
                          <td className="px-4 py-3 text-right text-red-500">{BDT(m.trip_expenses)}</td>
                          <td className="px-4 py-3 text-right text-orange-500">{BDT(m.commission)}</td>
                          <td className="px-4 py-3 text-right text-yellow-600">{BDT(m.maintenance_cost)}</td>
                          <td className={`px-4 py-3 text-right font-bold ${m.net >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                            {m.net >= 0 ? '' : '−'}{BDT(Math.abs(m.net))}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot className="border-t-2 border-gray-200 bg-gray-50 font-semibold">
                      <tr>
                        <td className="px-4 py-3 text-gray-700">মোট</td>
                        <td className="px-4 py-3 text-center">{monthly_pl.reduce((s, m) => s + m.trip_count, 0)}</td>
                        <td className="px-4 py-3 text-right text-green-700">{BDT(summary.total_revenue)}</td>
                        <td className="px-4 py-3 text-right text-red-500">{BDT(summary.total_trip_expenses)}</td>
                        <td className="px-4 py-3 text-right text-orange-500">{BDT(summary.total_commission)}</td>
                        <td className="px-4 py-3 text-right text-yellow-600">{BDT(summary.total_maintenance)}</td>
                        <td className={`px-4 py-3 text-right font-bold ${summary.net_profit >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                          {BDT(summary.net_profit)}
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              </>
            )}
          </div>
        )}

        {/* ── Tab 2: গাড়ি বিশ্লেষণ ─────────────────────────────────────── */}
        {activeTab === 'vehicles' && (
          <div className="overflow-x-auto">
            {vehicle_profitability.length === 0 ? (
              <div className="p-8 text-center text-gray-400">কোনো গাড়ির তথ্য নেই</div>
            ) : (
              <table className="w-full text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">গাড়ি</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">স্ট্যাটাস</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">ট্রিপ</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">ব্যবহার %</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">রাজস্ব</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">মোট খরচ</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">নিট</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {vehicle_profitability.map(v => (
                    <tr key={v.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3">
                        <div className="font-medium text-gray-800">{v.brand} {v.model}</div>
                        <div className="text-xs text-gray-400">{v.registration_number} · {v.year}</div>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${V_STATUS_CLS[v.status] ?? 'bg-gray-100 text-gray-600'}`}>
                          {VEHICLE_STATUS[v.status] ?? v.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center font-semibold text-gray-700">{v.trip_count}</td>
                      <td className="px-4 py-3 text-center">
                        <div className="flex items-center gap-2">
                          <div className="flex-1 bg-gray-100 rounded-full h-1.5">
                            <div
                              className="h-1.5 rounded-full bg-indigo-500"
                              style={{ width: `${Math.min(v.utilization_rate, 100)}%` }}
                            />
                          </div>
                          <span className="text-xs font-semibold text-indigo-600 w-10 text-right">
                            {v.utilization_rate}%
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-right text-green-700 font-semibold">{BDT(v.revenue)}</td>
                      <td className="px-4 py-3 text-right text-red-500">
                        {BDT(v.total_expenses)}
                        <div className="text-[10px] text-gray-400">
                          খরচ:{BDT(v.trip_expenses)} কমি:{BDT(v.commission)} রক্ষণ:{BDT(v.maintenance_cost)}
                        </div>
                      </td>
                      <td className={`px-4 py-3 text-right font-bold ${v.net >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                        {BDT(v.net)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {/* ── Tab 3: বকেয়া Aging ────────────────────────────────────────── */}
        {activeTab === 'dues' && (
          <div className="p-4 space-y-4">
            {/* Aging summary */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {[
                { key: 'current' as const, label: '০–৩০ দিন', count: dues_aging.current.length, total: dues_aging.current.reduce((s,i)=>s+i.remaining_amount,0), color: 'border-blue-200 bg-blue-50 text-blue-700' },
                { key: 'days30'  as const, label: '৩১–৬০ দিন', count: dues_aging.days30.length,  total: dues_aging.days30.reduce((s,i)=>s+i.remaining_amount,0),  color: 'border-yellow-200 bg-yellow-50 text-yellow-700' },
                { key: 'days60'  as const, label: '৬১–৯০ দিন', count: dues_aging.days60.length,  total: dues_aging.days60.reduce((s,i)=>s+i.remaining_amount,0),  color: 'border-orange-200 bg-orange-50 text-orange-700' },
                { key: 'days90'  as const, label: '৯০+ দিন',   count: dues_aging.days90.length,  total: dues_aging.days90.reduce((s,i)=>s+i.remaining_amount,0),  color: 'border-red-200 bg-red-50 text-red-700' },
              ].map(g => (
                <button
                  key={g.key}
                  onClick={() => setDuesFilter(duesFilter === g.key ? 'all' : g.key)}
                  className={`rounded-xl p-3 border text-left transition-all ${g.color} ${duesFilter === g.key ? 'ring-2 ring-offset-1 ring-current' : 'hover:opacity-80'}`}
                >
                  <div className="text-xs mb-0.5">{g.label} · {g.count}টি</div>
                  <div className="font-bold text-base">{BDT(g.total)}</div>
                </button>
              ))}
            </div>

            <div className="text-xs text-gray-500">
              মোট বকেয়া: <strong className="text-gray-800">{BDT(dues_aging.total_due)}</strong>
              {duesFilter !== 'all' && (
                <button onClick={() => setDuesFilter('all')} className="ml-2 text-indigo-600 underline">সব দেখুন</button>
              )}
            </div>

            {duesItems.length === 0 ? (
              <div className="py-8 text-center text-gray-400">কোনো বকেয়া নেই</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-2 text-left font-medium text-gray-600">ড্রাইভার</th>
                      <th className="px-4 py-2 text-left font-medium text-gray-600">গাড়ি</th>
                      <th className="px-4 py-2 text-left font-medium text-gray-600">ট্রিপ তারিখ</th>
                      <th className="px-4 py-2 text-right font-medium text-gray-600">কমিশন</th>
                      <th className="px-4 py-2 text-right font-medium text-gray-600">পরিশোধ</th>
                      <th className="px-4 py-2 text-right font-medium text-gray-600">বাকি</th>
                      <th className="px-4 py-2 text-center font-medium text-gray-600">বয়স</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {duesItems.map(item => (
                      <tr key={item.settlement_id} className="hover:bg-gray-50">
                        <td className="px-4 py-2.5">
                          <div className="font-medium text-gray-800">{item.driver_name}</div>
                          <div className="text-xs text-gray-400">{item.driver_mobile}</div>
                        </td>
                        <td className="px-4 py-2.5">
                          <div className="text-gray-700">{item.vehicle}</div>
                          <div className="text-xs text-gray-400">{item.registration_number}</div>
                        </td>
                        <td className="px-4 py-2.5 text-gray-600">
                          <div>{item.start_date?.slice(0,10)}</div>
                          <div className="text-xs text-gray-400">{item.end_date?.slice(0,10)}</div>
                        </td>
                        <td className="px-4 py-2.5 text-right text-gray-700">{BDT(item.driver_commission)}</td>
                        <td className="px-4 py-2.5 text-right text-emerald-600">{BDT(item.paid_amount)}</td>
                        <td className="px-4 py-2.5 text-right font-bold text-red-600">{BDT(item.remaining_amount)}</td>
                        <td className="px-4 py-2.5 text-center"><AgingBadge days={item.days_overdue} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* ── Tab 4: ড্রাইভার পারফরম্যান্স ────────────────────────────── */}
        {activeTab === 'drivers' && (
          <div className="overflow-x-auto">
            {driver_performance.length === 0 ? (
              <div className="p-8 text-center text-gray-400">কোনো ড্রাইভার নেই</div>
            ) : (
              <table className="w-full text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">ড্রাইভার</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">এ মাসে</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">{year} সালে</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">মোট ট্রিপ</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">মোট রাজস্ব</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">কমিশন</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">পরিশোধ</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">বাকি</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">গড় ট্রিপ</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {driver_performance.map((d, idx) => (
                    <tr key={d.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-gray-400 w-5">#{idx + 1}</span>
                          <div>
                            <div className="font-medium text-gray-800">{d.name}</div>
                            <div className="text-xs text-gray-400">{d.mobile} · {d.commission_rate}%</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-center font-semibold text-blue-600">{d.month_trips}</td>
                      <td className="px-4 py-3 text-center font-semibold text-indigo-600">{d.year_trips}</td>
                      <td className="px-4 py-3 text-center text-gray-600">{d.total_trips}</td>
                      <td className="px-4 py-3 text-right text-green-700 font-semibold">{BDT(d.total_revenue)}</td>
                      <td className="px-4 py-3 text-right text-orange-500">{BDT(d.total_commission)}</td>
                      <td className="px-4 py-3 text-right text-emerald-600">{BDT(d.total_paid)}</td>
                      <td className={`px-4 py-3 text-right font-bold ${d.total_due > 0 ? 'text-red-600' : 'text-gray-400'}`}>
                        {BDT(d.total_due)}
                      </td>
                      <td className="px-4 py-3 text-right text-gray-600">{BDT(d.avg_trip_value)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {/* ── Tab 5: গ্রাহক বিশ্লেষণ ───────────────────────────────────── */}
        {activeTab === 'customers' && (
          <div className="p-4 space-y-4">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="bg-blue-50 border border-blue-200 rounded-xl p-3 text-center">
                <div className="text-2xl font-bold text-blue-600">{summary.unique_customers}</div>
                <div className="text-xs text-gray-500">মোট গ্রাহক</div>
              </div>
              <div className="bg-green-50 border border-green-200 rounded-xl p-3 text-center">
                <div className="text-2xl font-bold text-green-600">{repeatCustomers}</div>
                <div className="text-xs text-gray-500">নিয়মিত (৩+ ট্রিপ)</div>
              </div>
              <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 text-center">
                <div className="text-2xl font-bold text-amber-600">{inactiveCustomers}</div>
                <div className="text-xs text-gray-500">নিষ্ক্রিয় (৬০+ দিন)</div>
              </div>
              <div className="bg-indigo-50 border border-indigo-200 rounded-xl p-3 text-center">
                <div className="text-2xl font-bold text-indigo-600">{BDT(summary.total_revenue / Math.max(summary.unique_customers, 1))}</div>
                <div className="text-xs text-gray-500">গড় প্রতি গ্রাহক</div>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-2 text-left font-medium text-gray-600">গ্রাহক</th>
                    <th className="px-4 py-2 text-center font-medium text-gray-600">ট্রিপ</th>
                    <th className="px-4 py-2 text-right font-medium text-gray-600">মোট ব্যয়</th>
                    <th className="px-4 py-2 text-left font-medium text-gray-600">শেষ ট্রিপ</th>
                    <th className="px-4 py-2 text-center font-medium text-gray-600">ধরন</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {customer_analytics.map(c => (
                    <tr key={c.id} className="hover:bg-gray-50">
                      <td className="px-4 py-2.5">
                        <div className="font-medium text-gray-800">{c.name}</div>
                        <div className="text-xs text-gray-400">{c.phone}</div>
                      </td>
                      <td className="px-4 py-2.5 text-center font-semibold text-gray-700">{c.total_trips}</td>
                      <td className="px-4 py-2.5 text-right font-semibold text-green-700">{BDT(c.total_spent)}</td>
                      <td className="px-4 py-2.5">
                        <div className="text-gray-600">{c.last_trip_date?.slice(0,10) ?? '—'}</div>
                        {c.is_inactive && <div className="text-xs text-red-500">{c.days_since_last} দিন আগে</div>}
                      </td>
                      <td className="px-4 py-2.5 text-center">
                        {c.is_inactive ? (
                          <span className="bg-red-100 text-red-600 text-xs font-semibold px-2 py-0.5 rounded-full">নিষ্ক্রিয়</span>
                        ) : c.is_repeat ? (
                          <span className="bg-green-100 text-green-600 text-xs font-semibold px-2 py-0.5 rounded-full">নিয়মিত</span>
                        ) : (
                          <span className="bg-blue-100 text-blue-600 text-xs font-semibold px-2 py-0.5 rounded-full">সাধারণ</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
