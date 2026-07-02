import { useState, useEffect, useCallback } from 'react';
import { api } from '@/api/client';
import type { Tenant } from '@/types';

interface CreateForm {
  name: string;
  email: string;
  phone: string;
  address: string;
  admin_name: string;
  admin_email: string;
  admin_phone: string;
  admin_password: string;
}
const emptyCreateForm = (): CreateForm => ({
  name: '', email: '', phone: '', address: '', admin_name: '', admin_email: '', admin_phone: '', admin_password: '',
});

interface EditForm {
  name: string;
  email: string;
  phone: string;
  address: string;
}
const tenantToEditForm = (t: Tenant): EditForm => ({
  name: t.name, email: t.email, phone: t.phone ?? '', address: t.address ?? '',
});

const statusLabel: Record<string, string> = { trial: 'ট্রায়াল', active: 'সক্রিয়', suspended: 'স্থগিত', cancelled: 'বাতিল' };
const statusColor: Record<string, string> = {
  trial: 'bg-sky-100 text-sky-700', active: 'bg-emerald-100 text-emerald-700',
  suspended: 'bg-red-100 text-red-700', cancelled: 'bg-slate-100 text-slate-500',
};

function Modal({ onClose, children }: { onClose: () => void; children: React.ReactNode }) {
  return (
    <div
      className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      onClick={e => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xl max-h-[92vh] overflow-y-auto">
        {children}
      </div>
    </div>
  );
}

export default function SuperAdminTenants() {
  const [tenants, setTenants]   = useState<Tenant[]>([]);
  const [loading, setLoading]   = useState(true);
  const [modal, setModal]       = useState<'add' | 'edit' | 'status' | null>(null);
  const [selected, setSelected] = useState<Tenant | null>(null);
  const [createForm, setCreateForm] = useState<CreateForm>(emptyCreateForm());
  const [editForm, setEditForm]     = useState<EditForm>({ name: '', email: '', phone: '', address: '' });
  const [newStatus, setNewStatus]   = useState<Tenant['status']>('active');
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr]               = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    const res = await api.get<Tenant[]>('/superadmin/tenants/index.php');
    setTenants(res.success ? (res.data ?? []) : []);
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  function openAdd() { setCreateForm(emptyCreateForm()); setErr(''); setModal('add'); }
  function openEdit(t: Tenant) { setSelected(t); setEditForm(tenantToEditForm(t)); setErr(''); setModal('edit'); }
  function openStatus(t: Tenant) { setSelected(t); setNewStatus(t.status); setErr(''); setModal('status'); }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true); setErr('');
    try {
      const res = await api.post('/superadmin/tenants/index.php', createForm);
      if (!res.success) { setErr(res.message ?? 'ব্যর্থ'); return; }
      setModal(null); load();
    } finally {
      setSubmitting(false);
    }
  }

  async function handleEdit(e: React.FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSubmitting(true); setErr('');
    try {
      const res = await api.put(`/superadmin/tenants/update.php?id=${selected.id}`, editForm);
      if (!res.success) { setErr(res.message ?? 'ব্যর্থ'); return; }
      setModal(null); load();
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStatusChange() {
    if (!selected) return;
    setSubmitting(true); setErr('');
    try {
      const res = await api.put(`/superadmin/tenants/status.php?id=${selected.id}`, { status: newStatus });
      if (!res.success) { setErr(res.message ?? 'ব্যর্থ'); return; }
      setModal(null); load();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl md:text-2xl font-bold text-slate-800">Tenant ব্যবস্থাপনা</h1>
          <p className="text-sm text-slate-500 mt-0.5">{tenants.length}টি tenant</p>
        </div>
        <button
          onClick={openAdd}
          className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 transition-colors"
        >
          <span>+</span> নতুন Tenant
        </button>
      </div>

      {loading ? (
        <div className="space-y-3">{[1, 2, 3].map(i => <div key={i} className="h-24 bg-slate-100 rounded-xl animate-pulse" />)}</div>
      ) : tenants.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="text-4xl mb-3">🏢</div>
          <p className="text-slate-500 text-sm">কোনো tenant নেই</p>
        </div>
      ) : (
        <div className="space-y-3">
          {tenants.map(t => (
            <div key={t.id} className="bg-white rounded-xl border border-slate-200 shadow-sm p-4">
              <div className="flex items-start justify-between gap-3 flex-wrap">
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-semibold text-slate-800 text-sm">{t.name}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${statusColor[t.status]}`}>{statusLabel[t.status]}</span>
                  </div>
                  <div className="text-xs text-slate-500 mt-0.5">{t.email} {t.phone ? `· ${t.phone}` : ''}</div>
                  <div className="text-xs text-slate-400 mt-1">
                    🚗 {t.vehicle_count}টি গাড়ি · trial শেষ {t.trial_ends_at}
                    {t.latest_invoice_status && (
                      <> · সর্বশেষ invoice: {t.latest_invoice_status === 'paid' ? 'পরিশোধিত' : t.latest_invoice_status === 'overdue' ? 'মেয়াদোত্তীর্ণ' : 'বকেয়া'} ৳{t.latest_invoice_amount?.toLocaleString('bn-BD')}</>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <button onClick={() => openEdit(t)} className="px-3 py-1.5 text-xs text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors">✏️ সম্পাদনা</button>
                  <button onClick={() => openStatus(t)} className="px-3 py-1.5 text-xs text-indigo-600 border border-indigo-200 rounded-lg hover:bg-indigo-50 transition-colors">🔄 Status</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create modal */}
      {modal === 'add' && (
        <Modal onClose={() => setModal(null)}>
          <form onSubmit={handleCreate}>
            <div className="p-6 border-b border-slate-100">
              <h2 className="text-lg font-bold text-slate-800">নতুন Tenant তৈরি</h2>
              <p className="text-xs text-slate-500 mt-1">Tenant-এর সাথে একটি প্রথম Admin অ্যাকাউন্টও তৈরি হবে</p>
            </div>
            <div className="p-6 space-y-4">
              {err && <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-2 rounded-lg">{err}</div>}

              <div className="text-xs font-semibold text-slate-500 uppercase tracking-wide">ব্যবসার তথ্য</div>
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-slate-600 mb-1">ব্যবসার নাম *</label>
                  <input required value={createForm.name} onChange={e => setCreateForm(f => ({ ...f, name: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">ইমেইল *</label>
                  <input type="email" required value={createForm.email} onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">মোবাইল</label>
                  <input value={createForm.phone} onChange={e => setCreateForm(f => ({ ...f, phone: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-slate-600 mb-1">ঠিকানা</label>
                  <input value={createForm.address} onChange={e => setCreateForm(f => ({ ...f, address: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
              </div>

              <div className="text-xs font-semibold text-slate-500 uppercase tracking-wide pt-2">প্রথম Admin অ্যাকাউন্ট</div>
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-slate-600 mb-1">Admin নাম *</label>
                  <input required value={createForm.admin_name} onChange={e => setCreateForm(f => ({ ...f, admin_name: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">Admin ইমেইল *</label>
                  <input type="email" required value={createForm.admin_email} onChange={e => setCreateForm(f => ({ ...f, admin_email: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">Admin মোবাইল</label>
                  <input value={createForm.admin_phone} onChange={e => setCreateForm(f => ({ ...f, admin_phone: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" placeholder="01XXXXXXXXX" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">পাসওয়ার্ড *</label>
                  <input type="password" required minLength={6} value={createForm.admin_password} onChange={e => setCreateForm(f => ({ ...f, admin_password: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
              </div>
            </div>
            <div className="p-6 border-t border-slate-100 flex justify-end gap-3">
              <button type="button" onClick={() => setModal(null)} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">বাতিল</button>
              <button type="submit" disabled={submitting} className="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50">
                {submitting ? 'তৈরি হচ্ছে…' : 'তৈরি করুন'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Edit modal */}
      {modal === 'edit' && selected && (
        <Modal onClose={() => setModal(null)}>
          <form onSubmit={handleEdit}>
            <div className="p-6 border-b border-slate-100">
              <h2 className="text-lg font-bold text-slate-800">Tenant সম্পাদনা</h2>
            </div>
            <div className="p-6 space-y-4">
              {err && <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-2 rounded-lg">{err}</div>}
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">ব্যবসার নাম</label>
                <input required value={editForm.name} onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">ইমেইল</label>
                <input type="email" required value={editForm.email} onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">মোবাইল</label>
                <input value={editForm.phone} onChange={e => setEditForm(f => ({ ...f, phone: e.target.value }))}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">ঠিকানা</label>
                <input value={editForm.address} onChange={e => setEditForm(f => ({ ...f, address: e.target.value }))}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
              </div>
            </div>
            <div className="p-6 border-t border-slate-100 flex justify-end gap-3">
              <button type="button" onClick={() => setModal(null)} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">বাতিল</button>
              <button type="submit" disabled={submitting} className="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50">
                {submitting ? 'সংরক্ষণ হচ্ছে…' : 'আপডেট করুন'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Status modal */}
      {modal === 'status' && selected && (
        <Modal onClose={() => setModal(null)}>
          <div className="p-6 border-b border-slate-100">
            <h2 className="text-lg font-bold text-slate-800">Status পরিবর্তন — {selected.name}</h2>
          </div>
          <div className="p-6 space-y-3">
            {err && <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-2 rounded-lg">{err}</div>}
            {(['trial', 'active', 'suspended', 'cancelled'] as const).map(s => (
              <label key={s} className={`flex items-center gap-3 px-4 py-3 rounded-lg border cursor-pointer transition-colors ${newStatus === s ? 'border-indigo-400 bg-indigo-50' : 'border-slate-200 hover:bg-slate-50'}`}>
                <input type="radio" name="status" checked={newStatus === s} onChange={() => setNewStatus(s)} className="accent-indigo-600" />
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${statusColor[s]}`}>{statusLabel[s]}</span>
              </label>
            ))}
          </div>
          <div className="p-6 border-t border-slate-100 flex justify-end gap-3">
            <button onClick={() => setModal(null)} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">বাতিল</button>
            <button onClick={handleStatusChange} disabled={submitting} className="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50">
              {submitting ? 'সংরক্ষণ হচ্ছে…' : 'সংরক্ষণ করুন'}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
