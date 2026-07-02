import { useState, useEffect } from 'react';
import { api } from '@/api/client';
import type { SaasSettings } from '@/types';

export default function SuperAdminSettings() {
  const [settings, setSettings] = useState<SaasSettings | null>(null);
  const [loading, setLoading]   = useState(true);
  const [pricePerVehicle, setPricePerVehicle] = useState('');
  const [trialDays, setTrialDays]             = useState('');
  const [invoiceDueDays, setInvoiceDueDays]   = useState('');
  const [saving, setSaving] = useState(false);
  const [err, setErr]       = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    api.get<SaasSettings>('/superadmin/settings/index.php').then(res => {
      if (res.success && res.data) {
        setSettings(res.data);
        setPricePerVehicle(res.data.price_per_vehicle.value);
        setTrialDays(res.data.trial_days.value);
        setInvoiceDueDays(res.data.invoice_due_days.value);
      }
      setLoading(false);
    });
  }, []);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true); setErr(''); setSuccess('');
    try {
      const res = await api.put('/superadmin/settings/index.php', {
        price_per_vehicle: pricePerVehicle,
        trial_days: trialDays,
        invoice_due_days: invoiceDueDays,
      });
      if (!res.success) { setErr(res.message ?? 'ব্যর্থ'); return; }
      setSuccess('সেটিংস সংরক্ষিত হয়েছে');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <div className="h-64 bg-slate-100 rounded-xl animate-pulse" />;
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl md:text-2xl font-bold text-slate-800">SaaS বিলিং সেটিংস</h1>
        <p className="text-sm text-slate-500 mt-0.5">এই মানগুলো নতুন Tenant তৈরি ও মাসিক invoice generate করার সময় ব্যবহৃত হয়</p>
      </div>

      <form onSubmit={handleSave} className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 max-w-lg space-y-5">
        {err && <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-2 rounded-lg">{err}</div>}
        {success && <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 text-sm px-4 py-2 rounded-lg">{success}</div>}

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">প্রতি গাড়ি মাসিক মূল্য (৳)</label>
          <input type="number" min="0" step="0.01" required value={pricePerVehicle} onChange={e => setPricePerVehicle(e.target.value)}
            className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          {settings && <p className="text-xs text-slate-400 mt-1">{settings.price_per_vehicle.description}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Trial Period (দিন)</label>
          <input type="number" min="0" step="1" required value={trialDays} onChange={e => setTrialDays(e.target.value)}
            className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          {settings && <p className="text-xs text-slate-400 mt-1">{settings.trial_days.description}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Invoice Due Days (দিন)</label>
          <input type="number" min="0" step="1" required value={invoiceDueDays} onChange={e => setInvoiceDueDays(e.target.value)}
            className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          {settings && <p className="text-xs text-slate-400 mt-1">{settings.invoice_due_days.description}</p>}
        </div>

        <div className="pt-2">
          <button type="submit" disabled={saving} className="px-5 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50">
            {saving ? 'সংরক্ষণ হচ্ছে…' : 'সংরক্ষণ করুন'}
          </button>
        </div>
      </form>
    </div>
  );
}
