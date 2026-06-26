import { useState, useEffect } from 'react';
import { api } from '@/api/client';

interface MonthlyRevenue {
  month: string;
  trip_count: number;
  revenue: number;
  expenses: number;
  net: number;
}

interface VehicleRevenue {
  id: number;
  brand: string;
  model: string;
  registration_number: string;
  status: string;
  trip_count: number;
  revenue: number;
  expenses: number;
  net: number;
}

interface ExpenseBreakdown {
  expense_type: string;
  total: number;
}

interface DriverPerformance {
  id: number;
  name: string;
  mobile: string;
  commission_rate: number;
  status: string;
  total_trips: number;
  this_month_trips: number;
  total_commission: number;
  total_paid: number;
  total_due: number;
}

interface ReportData {
  monthly_revenue: MonthlyRevenue[];
  vehicle_revenue: VehicleRevenue[];
  expense_breakdown: ExpenseBreakdown[];
  driver_performance: DriverPerformance[];
  summary: {
    total_trips: number;
    total_revenue: number;
    total_expenses: number;
    total_due: number;
  };
}

const MONTH_NAMES: Record<string, string> = {
  '01': 'জানু', '02': 'ফেব্রু', '03': 'মার্চ', '04': 'এপ্রি',
  '05': 'মে', '06': 'জুন', '07': 'জুলাই', '08': 'আগস্ট',
  '09': 'সেপ্টে', '10': 'অক্টো', '11': 'নভে', '12': 'ডিসে',
};
const EXPENSE_LABELS: Record<string, string> = {
  toll: 'টোল', fuel: 'জ্বালানি', parking: 'পার্কিং',
  repair: 'মেরামত', driver_allowance: 'ড্রাইভার ভাতা', other: 'অন্যান্য',
};
const VEHICLE_STATUS_LABELS: Record<string, string> = {
  available: 'উপলব্ধ', rented: 'ভাড়ায়', maintenance: 'রক্ষণাবেক্ষণ', inactive: 'নিষ্ক্রিয়',
};
const VEHICLE_STATUS_COLORS: Record<string, string> = {
  available: 'bg-emerald-100 text-emerald-700',
  rented: 'bg-orange-100 text-orange-700',
  maintenance: 'bg-yellow-100 text-yellow-700',
  inactive: 'bg-slate-100 text-slate-500',
};

const formatMonth = (ym: string) => {
  const [y, m] = ym.split('-');
  return `${MONTH_NAMES[m] ?? m} ${y}`;
};

type Tab = 'monthly' | 'vehicles' | 'expenses' | 'drivers';

export default function ManagerReports() {
  const [data, setData]       = useState<ReportData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');
  const [activeTab, setTab]   = useState<Tab>('monthly');

  useEffect(() => {
    api.get<ReportData>('/manager/reports.php')
      .then(res => {
        if (res.success && res.data) setData(res.data);
        else setError(res.message ?? 'রিপোর্ট লোড ব্যর্থ');
      })
      .catch(() => setError('নেটওয়ার্ক সমস্যা'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return (
    <div className="p-4 sm:p-6 space-y-4 animate-pulse">
      <div className="h-8 w-40 bg-slate-200 rounded" />
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[...Array(4)].map((_, i) => <div key={i} className="h-20 bg-slate-100 rounded-xl" />)}
      </div>
      <div className="h-64 bg-slate-100 rounded-xl" />
    </div>
  );

  if (error || !data) return (
    <div className="p-6 text-center text-red-600">{error || 'ডেটা পাওয়া যায়নি'}</div>
  );

  const { summary, monthly_revenue, vehicle_revenue, expense_breakdown, driver_performance } = data;
  const totalExpense = expense_breakdown.reduce((s, e) => s + e.total, 0);

  const tabs: { key: Tab; label: string; icon: string }[] = [
    { key: 'monthly',  label: 'মাসিক রাজস্ব',   icon: '📅' },
    { key: 'vehicles', label: 'গাড়িভিত্তিক',    icon: '🚗' },
    { key: 'expenses', label: 'খরচের বিশ্লেষণ', icon: '💸' },
    { key: 'drivers',  label: 'ড্রাইভার',        icon: '🧑‍✈️' },
  ];

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">রিপোর্ট</h1>
        <p className="text-gray-500 text-sm mt-0.5">আপনার অধীনস্থ গাড়ির সম্পূর্ণ বিশ্লেষণ</p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
          <div className="text-xs text-gray-500 mb-1">মোট ট্রিপ</div>
          <div className="text-2xl font-bold text-blue-600">{summary.total_trips}</div>
        </div>
        <div className="bg-green-50 border border-green-200 rounded-xl p-4">
          <div className="text-xs text-gray-500 mb-1">মোট রাজস্ব</div>
          <div className="text-xl font-bold text-green-600">৳{summary.total_revenue.toFixed(0)}</div>
        </div>
        <div className="bg-red-50 border border-red-200 rounded-xl p-4">
          <div className="text-xs text-gray-500 mb-1">মোট খরচ</div>
          <div className="text-xl font-bold text-red-500">৳{summary.total_expenses.toFixed(0)}</div>
        </div>
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
          <div className="text-xs text-gray-500 mb-1">বকেয়া</div>
          <div className="text-xl font-bold text-amber-600">৳{summary.total_due.toFixed(0)}</div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-xl border shadow-sm overflow-hidden">
        <div className="flex overflow-x-auto border-b">
          {tabs.map(tab => (
            <button
              key={tab.key}
              onClick={() => setTab(tab.key)}
              className={`flex items-center gap-1.5 px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors ${
                activeTab === tab.key
                  ? 'text-indigo-600 border-b-2 border-indigo-600 bg-indigo-50/50'
                  : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
              }`}
            >
              <span>{tab.icon}</span> {tab.label}
            </button>
          ))}
        </div>

        {/* Monthly Revenue Tab */}
        {activeTab === 'monthly' && (
          <div className="overflow-x-auto">
            {monthly_revenue.length === 0 ? (
              <div className="p-8 text-center text-gray-400">কোনো ডেটা পাওয়া যায়নি</div>
            ) : (
              <table className="w-full text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">মাস</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">ট্রিপ</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">রাজস্ব</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">খরচ</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">নেট</th>
                  </tr>
                </thead>
                <tbody>
                  {monthly_revenue.map(m => (
                    <tr key={m.month} className="border-t hover:bg-gray-50">
                      <td className="px-4 py-3 font-medium">{formatMonth(m.month)}</td>
                      <td className="px-4 py-3 text-center">{m.trip_count}</td>
                      <td className="px-4 py-3 text-right text-green-600 font-medium">৳{m.revenue.toFixed(0)}</td>
                      <td className="px-4 py-3 text-right text-red-500">৳{m.expenses.toFixed(0)}</td>
                      <td className={`px-4 py-3 text-right font-bold ${m.net >= 0 ? 'text-indigo-600' : 'text-red-600'}`}>
                        ৳{m.net.toFixed(0)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {/* Vehicle Revenue Tab */}
        {activeTab === 'vehicles' && (
          <div className="overflow-x-auto">
            {vehicle_revenue.length === 0 ? (
              <div className="p-8 text-center text-gray-400">কোনো গাড়ি পাওয়া যায়নি</div>
            ) : (
              <table className="w-full text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">গাড়ি</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">স্ট্যাটাস</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">ট্রিপ</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">রাজস্ব</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">খরচ</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">নেট</th>
                  </tr>
                </thead>
                <tbody>
                  {vehicle_revenue.map(v => (
                    <tr key={v.id} className="border-t hover:bg-gray-50">
                      <td className="px-4 py-3">
                        <div className="font-medium">{v.brand} {v.model}</div>
                        <div className="text-xs text-gray-400">{v.registration_number}</div>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${VEHICLE_STATUS_COLORS[v.status] ?? 'bg-gray-100 text-gray-600'}`}>
                          {VEHICLE_STATUS_LABELS[v.status] ?? v.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center">{v.trip_count}</td>
                      <td className="px-4 py-3 text-right text-green-600 font-medium">৳{v.revenue.toFixed(0)}</td>
                      <td className="px-4 py-3 text-right text-red-500">৳{v.expenses.toFixed(0)}</td>
                      <td className={`px-4 py-3 text-right font-bold ${v.net >= 0 ? 'text-indigo-600' : 'text-red-600'}`}>
                        ৳{v.net.toFixed(0)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {/* Expense Breakdown Tab */}
        {activeTab === 'expenses' && (
          <div className="p-4 sm:p-6">
            {expense_breakdown.length === 0 ? (
              <div className="text-center text-gray-400 py-8">কোনো খরচের তথ্য নেই</div>
            ) : (
              <div className="space-y-3">
                {expense_breakdown.map(e => {
                  const pct = totalExpense > 0 ? (e.total / totalExpense * 100) : 0;
                  return (
                    <div key={e.expense_type}>
                      <div className="flex justify-between text-sm mb-1">
                        <span className="font-medium text-gray-700">
                          {EXPENSE_LABELS[e.expense_type] ?? e.expense_type}
                        </span>
                        <span className="font-semibold">৳{e.total.toFixed(0)} ({pct.toFixed(1)}%)</span>
                      </div>
                      <div className="w-full bg-gray-100 rounded-full h-2.5">
                        <div
                          className="bg-indigo-500 h-2.5 rounded-full"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
                <div className="pt-3 border-t flex justify-between font-semibold text-sm">
                  <span>মোট খরচ</span>
                  <span className="text-red-600">৳{totalExpense.toFixed(0)}</span>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Driver Performance Tab */}
        {activeTab === 'drivers' && (
          <div className="overflow-x-auto">
            {driver_performance.length === 0 ? (
              <div className="p-8 text-center text-gray-400">কোনো ড্রাইভার পাওয়া যায়নি</div>
            ) : (
              <table className="w-full text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium text-gray-600">ড্রাইভার</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">এই মাস</th>
                    <th className="px-4 py-3 text-center font-medium text-gray-600">মোট ট্রিপ</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">কমিশন</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">পরিশোধিত</th>
                    <th className="px-4 py-3 text-right font-medium text-gray-600">বকেয়া</th>
                  </tr>
                </thead>
                <tbody>
                  {driver_performance.map(d => (
                    <tr key={d.id} className="border-t hover:bg-gray-50">
                      <td className="px-4 py-3">
                        <div className="font-medium">{d.name}</div>
                        <div className="text-xs text-gray-400">{d.mobile} • {d.commission_rate}% কমিশন</div>
                        <span className={`text-xs px-1.5 py-0.5 rounded-full ${d.status === 'active' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                          {d.status === 'active' ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className="font-semibold text-indigo-600">{d.this_month_trips}</span>
                        <span className="text-gray-400 text-xs"> ট্রিপ</span>
                      </td>
                      <td className="px-4 py-3 text-center font-medium">{d.total_trips}</td>
                      <td className="px-4 py-3 text-right font-medium text-green-600">৳{d.total_commission.toFixed(0)}</td>
                      <td className="px-4 py-3 text-right text-indigo-600">৳{d.total_paid.toFixed(0)}</td>
                      <td className={`px-4 py-3 text-right font-semibold ${d.total_due > 0 ? 'text-amber-600' : 'text-gray-400'}`}>
                        ৳{d.total_due.toFixed(0)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
