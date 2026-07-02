import { useState, useEffect, useCallback } from 'react';
import { api } from '@/api/client';
import type { SubscriptionInvoice, Tenant } from '@/types';

const statusLabel: Record<string, string> = { pending: 'বকেয়া', paid: 'পরিশোধিত', overdue: 'মেয়াদোত্তীর্ণ', waived: 'মওকুফ' };
const statusColor: Record<string, string> = {
  pending: 'bg-amber-100 text-amber-700', paid: 'bg-emerald-100 text-emerald-700',
  overdue: 'bg-red-100 text-red-700', waived: 'bg-slate-100 text-slate-500',
};

function Modal({ onClose, children }: { onClose: () => void; children: React.ReactNode }) {
  return (
    <div
      className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      onClick={e => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md max-h-[92vh] overflow-y-auto">
        {children}
      </div>
    </div>
  );
}

export default function SuperAdminBilling() {
  const [invoices, setInvoices] = useState<SubscriptionInvoice[]>([]);
  const [tenants, setTenants]   = useState<Tenant[]>([]);
  const [loading, setLoading]   = useState(true);
  const [tenantFilter, setTenantFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [monthFilter, setMonthFilter]   = useState('');
  const [modal, setModal]       = useState<'mark-paid' | null>(null);
  const [selected, setSelected] = useState<SubscriptionInvoice | null>(null);
  const [paymentMethod, setPaymentMethod] = useState('bkash');
  const [paymentRef, setPaymentRef]       = useState('');
  const [notes, setNotes]                 = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr]               = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    const params: Record<string, string> = {};
    if (tenantFilter) params.tenant_id = tenantFilter;
    if (statusFilter) params.status = statusFilter;
    if (monthFilter) params.month = monthFilter;
    const qs = new URLSearchParams(params).toString();
    const res = await api.get<SubscriptionInvoice[]>(`/superadmin/billing/invoices.php${qs ? '?' + qs : ''}`);
    setInvoices(res.success ? (res.data ?? []) : []);
    setLoading(false);
  }, [tenantFilter, statusFilter, monthFilter]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    api.get<Tenant[]>('/superadmin/tenants/index.php').then(res => {
      if (res.success) setTenants(res.data ?? []);
    });
  }, []);

  function openMarkPaid(inv: SubscriptionInvoice) {
    setSelected(inv); setPaymentMethod('bkash'); setPaymentRef(''); setNotes(''); setErr(''); setModal('mark-paid');
  }

  async function handleMarkPaid(e: React.FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSubmitting(true); setErr('');
    try {
      const res = await api.post('/superadmin/billing/invoices.php', {
        invoice_id: selected.id,
        payment_method: paymentMethod,
        payment_ref: paymentRef,
        notes,
      });
      if (!res.success) { setErr(res.message ?? 'ব্যর্থ'); return; }
      setModal(null); load();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl md:text-2xl font-bold text-slate-800">বিলিং</h1>
        <p className="text-sm text-slate-500 mt-0.5">{invoices.length}টি invoice</p>
      </div>

      <div className="flex flex-col sm:flex-row gap-3 mb-4">
        <select value={tenantFilter} onChange={e => setTenantFilter(e.target.value)}
          className="px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
          <option value="">সব Tenant</option>
          {tenants.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>
        <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}
          className="px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
          <option value="">সব Status</option>
          <option value="pending">বকেয়া</option>
          <option value="paid">পরিশোধিত</option>
          <option value="overdue">মেয়াদোত্তীর্ণ</option>
          <option value="waived">মওকুফ</option>
        </select>
        <input type="month" value={monthFilter} onChange={e => setMonthFilter(e.target.value)}
          className="px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
      </div>

      {loading ? (
        <div className="space-y-3">{[1, 2, 3].map(i => <div key={i} className="h-20 bg-slate-100 rounded-xl animate-pulse" />)}</div>
      ) : invoices.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="text-4xl mb-3">🧾</div>
          <p className="text-slate-500 text-sm">কোনো invoice নেই</p>
        </div>
      ) : (
        <div className="space-y-3">
          {invoices.map(inv => (
            <div key={inv.id} className="bg-white rounded-xl border border-slate-200 shadow-sm p-4">
              <div className="flex items-start justify-between gap-3 flex-wrap">
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-semibold text-slate-800 text-sm">{inv.invoice_number}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${statusColor[inv.status]}`}>{statusLabel[inv.status]}</span>
                  </div>
                  <div className="text-xs text-slate-500 mt-0.5">{inv.tenant_name} ({inv.tenant_email})</div>
                  <div className="text-xs text-slate-400 mt-1">
                    {inv.vehicle_count}টি গাড়ি × ৳{inv.price_per_vehicle} · তারিখ {inv.invoice_date} · due {inv.due_date}
                    {inv.paid_at && <> · পরিশোধ {inv.paid_at} ({inv.payment_method}{inv.payment_ref ? ` · ${inv.payment_ref}` : ''})</>}
                  </div>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0">
                  <span className="text-lg font-bold text-slate-800">৳{inv.total_amount.toLocaleString('bn-BD')}</span>
                  {inv.status !== 'paid' && (
                    <button onClick={() => openMarkPaid(inv)} className="px-3 py-1.5 text-xs bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors">
                      ✓ Paid মার্ক করুন
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {modal === 'mark-paid' && selected && (
        <Modal onClose={() => setModal(null)}>
          <form onSubmit={handleMarkPaid}>
            <div className="p-6 border-b border-slate-100">
              <h2 className="text-lg font-bold text-slate-800">Invoice পরিশোধিত মার্ক করুন</h2>
              <p className="text-xs text-slate-500 mt-1">{selected.invoice_number} — {selected.tenant_name} — ৳{selected.total_amount.toLocaleString('bn-BD')}</p>
            </div>
            <div className="p-6 space-y-4">
              {err && <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-2 rounded-lg">{err}</div>}
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">পেমেন্ট পদ্ধতি *</label>
                <select required value={paymentMethod} onChange={e => setPaymentMethod(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
                  <option value="bkash">bKash</option>
                  <option value="nagad">Nagad</option>
                  <option value="bank_transfer">ব্যাংক ট্রান্সফার</option>
                  <option value="cash">নগদ</option>
                  <option value="other">অন্যান্য</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">TrxID / রেফারেন্স নম্বর</label>
                <input value={paymentRef} onChange={e => setPaymentRef(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="যেমন: 8N7X2K9P1Q" />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">নোট</label>
                <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={2}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
              </div>
              {selected.status === 'overdue' && (
                <div className="bg-indigo-50 border border-indigo-100 text-indigo-700 text-xs px-3 py-2 rounded-lg">
                  এই tenant বর্তমানে স্থগিত থাকলে, পরিশোধিত মার্ক করার সাথে সাথেই স্বয়ংক্রিয়ভাবে পুনরায় সক্রিয় হয়ে যাবে।
                </div>
              )}
            </div>
            <div className="p-6 border-t border-slate-100 flex justify-end gap-3">
              <button type="button" onClick={() => setModal(null)} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">বাতিল</button>
              <button type="submit" disabled={submitting} className="px-4 py-2 bg-emerald-600 text-white text-sm rounded-lg hover:bg-emerald-700 transition-colors disabled:opacity-50">
                {submitting ? 'সংরক্ষণ হচ্ছে…' : 'পরিশোধিত মার্ক করুন'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
