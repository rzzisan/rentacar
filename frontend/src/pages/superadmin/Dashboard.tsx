import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '@/api/client';
import type { SuperAdminStats } from '@/types';

const statusLabel: Record<string, string> = {
  pending: 'বকেয়া', paid: 'পরিশোধিত', overdue: 'মেয়াদোত্তীর্ণ', waived: 'মওকুফ',
};
const statusColor: Record<string, string> = {
  pending: 'bg-amber-100 text-amber-700', paid: 'bg-emerald-100 text-emerald-700',
  overdue: 'bg-red-100 text-red-700', waived: 'bg-slate-100 text-slate-500',
};

export default function SuperAdminDashboard() {
  const [stats, setStats] = useState<SuperAdminStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<SuperAdminStats>('/superadmin/billing/stats.php').then(res => {
      setStats(res.success ? res.data ?? null : null);
      setLoading(false);
    });
  }, []);

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map(i => <div key={i} className="h-28 bg-slate-100 rounded-xl animate-pulse" />)}
        </div>
        <div className="h-64 bg-slate-100 rounded-xl animate-pulse" />
      </div>
    );
  }

  if (!stats) {
    return <div className="bg-white rounded-xl border border-slate-200 p-12 text-center text-slate-500 text-sm">ডেটা লোড করতে ব্যর্থ</div>;
  }

  const cards = [
    { label: 'মোট Tenant', value: stats.total_tenants, sub: `${stats.active_tenants} সক্রিয় · ${stats.trial_tenants} ট্রায়াল`, icon: '🏢', color: 'from-indigo-500 to-violet-600', href: '/superadmin/tenants' },
    { label: 'স্থগিত Tenant', value: stats.suspended_tenants, sub: `${stats.cancelled_tenants} বাতিল`, icon: '⛔', color: 'from-red-400 to-red-500', href: '/superadmin/tenants' },
    { label: 'এই মাসের আয়', value: '৳' + stats.this_month_total.toLocaleString('bn-BD'), sub: `৳${stats.this_month_paid.toLocaleString('bn-BD')} সংগৃহীত`, icon: '💰', color: 'from-green-500 to-emerald-600', href: '/superadmin/billing' },
    { label: 'মেয়াদোত্তীর্ণ Invoice', value: stats.overdue_count, sub: `৳${stats.overdue_amount.toLocaleString('bn-BD')}`, icon: '⚠️', color: 'from-orange-400 to-orange-500', href: '/superadmin/billing' },
  ];

  const maxTrend = Math.max(1, ...stats.monthly_trend.map(t => t.total));

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl md:text-2xl font-bold text-slate-800">SuperAdmin ড্যাশবোর্ড</h1>
        <p className="text-sm text-slate-500 mt-0.5">সব Tenant ও বিলিং ওভারভিউ</p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        {cards.map(c => (
          <Link key={c.label} to={c.href} className="bg-white rounded-xl border border-slate-200 shadow-sm p-4 hover:shadow-md transition-shadow no-underline">
            <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${c.color} flex items-center justify-center text-lg mb-3`}>{c.icon}</div>
            <div className="text-xl font-bold text-slate-800">{c.value}</div>
            <div className="text-xs text-slate-500 mt-0.5">{c.label}</div>
            <div className="text-[11px] text-slate-400 mt-1">{c.sub}</div>
          </Link>
        ))}
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        {/* Monthly trend */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5">
          <h2 className="text-sm font-semibold text-slate-700 mb-4">গত ৬ মাসের আয় ট্রেন্ড</h2>
          {stats.monthly_trend.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-8">কোনো ডেটা নেই</p>
          ) : (
            <div className="flex items-end gap-3 h-40">
              {stats.monthly_trend.map(t => (
                <div key={t.month} className="flex-1 flex flex-col items-center gap-1.5">
                  <div className="text-[11px] text-slate-500 font-medium">৳{t.total.toLocaleString('bn-BD')}</div>
                  <div
                    className="w-full bg-gradient-to-t from-indigo-500 to-violet-500 rounded-t-md min-h-[4px]"
                    style={{ height: `${Math.max(4, (t.total / maxTrend) * 100)}px` }}
                  />
                  <div className="text-[10px] text-slate-400">{t.month}</div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent invoices */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-slate-700">সাম্প্রতিক Invoice</h2>
            <Link to="/superadmin/billing" className="text-xs text-indigo-600 hover:underline">সব দেখুন</Link>
          </div>
          {stats.recent_invoices.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-8">কোনো invoice নেই</p>
          ) : (
            <div className="space-y-2">
              {stats.recent_invoices.map(inv => (
                <div key={inv.id} className="flex items-center justify-between py-2 border-b border-slate-50 last:border-0">
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-slate-700 truncate">{inv.tenant_name}</div>
                    <div className="text-xs text-slate-400">{inv.invoice_number} · {inv.invoice_date}</div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <span className="text-sm font-semibold text-slate-700">৳{inv.total_amount.toLocaleString('bn-BD')}</span>
                    <span className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${statusColor[inv.status] ?? ''}`}>
                      {statusLabel[inv.status] ?? inv.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
