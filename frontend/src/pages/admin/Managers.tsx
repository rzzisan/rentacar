import { useState, useEffect, useCallback, useRef } from 'react';
import { api } from '@/api/client';
import type { Manager } from '@/types';

interface VehicleOption {
  id: number;
  brand: string;
  model: string;
  registration_number: string;
  status: string;
}

interface FormState {
  name: string;
  mobile: string;
  email: string;
  password: string;
  status: 'active' | 'inactive';
  vehicle_id: number | null;
}

const emptyForm = (): FormState => ({
  name: '', mobile: '', email: '', password: '',
  status: 'active', vehicle_id: null,
});

const managerToForm = (m: Manager): FormState => ({
  name: m.name, mobile: m.mobile, email: m.email, password: '',
  status: m.status, vehicle_id: m.vehicles[0]?.id ?? null,
});

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

function Avatar({ src, name }: { src?: string; name: string }) {
  if (src) {
    let imgSrc = src.startsWith('/') ? src : `/${src}`;
    if (!imgSrc.startsWith('/public/')) imgSrc = `/public/${imgSrc.replace(/^\/+/, '')}`;
    return <img src={imgSrc} alt={name} className="w-10 h-10 rounded-full object-cover flex-shrink-0 border-2 border-white shadow-sm" />;
  }
  return (
    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-sm font-bold text-white flex-shrink-0">
      {name.charAt(0).toUpperCase()}
    </div>
  );
}

export default function AdminManagers() {
  const [managers, setManagers]   = useState<Manager[]>([]);
  const [vehicles, setVehicles]   = useState<VehicleOption[]>([]);
  const [loading, setLoading]     = useState(true);
  const [search, setSearch]       = useState('');
  const [statusFilter, setStatus] = useState('');
  const [modal, setModal]         = useState<'add' | 'edit' | 'delete' | null>(null);
  const [selected, setSelected]   = useState<Manager | null>(null);
  const [form, setForm]           = useState<FormState>(emptyForm());
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr]             = useState('');
  const [picFile, setPicFile]     = useState<File | null>(null);
  const fileRef                   = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    const params: Record<string, string> = {};
    if (search) params.search = search;
    if (statusFilter) params.status = statusFilter;
    const qs = new URLSearchParams(params).toString();
    const res = await api.get<Manager[]>(`/admin/managers/index.php${qs ? '?' + qs : ''}`);
    setManagers(res.success ? (res.data ?? []) : []);
    setLoading(false);
  }, [search, statusFilter]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    api.get<VehicleOption[]>('/vehicles/index.php').then(res => {
      if (res.success) setVehicles(res.data ?? []);
    });
  }, []);

  function openAdd() {
    setForm(emptyForm()); setPicFile(null); setErr(''); setModal('add');
  }
  function openEdit(m: Manager) {
    setSelected(m); setForm(managerToForm(m)); setPicFile(null); setErr(''); setModal('edit');
  }
  function openDelete(m: Manager) {
    setSelected(m); setModal('delete');
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true); setErr('');
    try {
      const fd = new FormData();
      fd.append('name', form.name);
      fd.append('mobile', form.mobile);
      fd.append('email', form.email);
      if (form.password) fd.append('password', form.password);
      fd.append('status', form.status);
      fd.append('vehicle_id', form.vehicle_id ? String(form.vehicle_id) : '');
      if (picFile) fd.append('profile_picture', picFile);

      let res;
      if (modal === 'add') {
        res = await api.post('/admin/managers/index.php', fd);
      } else {
        fd.append('id', String(selected!.id));
        res = await api.post('/admin/managers/update.php', fd);
      }
      if (!res.success) { setErr(res.message ?? 'ব্যর্থ'); return; }
      setModal(null); load();
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete() {
    if (!selected) return;
    setSubmitting(true);
    const res = await api.delete(`/admin/managers/destroy.php?id=${selected.id}`);
    setSubmitting(false);
    if (!res.success) { setErr(res.message ?? 'মুছতে ব্যর্থ'); return; }
    setModal(null); load();
  }

  const filteredVehicles = vehicles.filter(v => {
    // Show available vehicles + the currently assigned vehicle (if editing)
    const isAssigned = v.id === form.vehicle_id;
    const isFreeOrAvailable = !managers.some(m => m.vehicles.some(mv => mv.id === v.id) && m.id !== selected?.id);
    return isAssigned || isFreeOrAvailable;
  });

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl md:text-2xl font-bold text-slate-800">ম্যানেজার ব্যবস্থাপনা</h1>
          <p className="text-sm text-slate-500 mt-0.5">{managers.length}জন ম্যানেজার</p>
        </div>
        <button
          onClick={openAdd}
          className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 transition-colors"
        >
          <span>+</span> নতুন ম্যানেজার
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-4">
        <input
          type="text" placeholder="নাম / মোবাইল / ইমেইল খুঁজুন…"
          value={search} onChange={e => setSearch(e.target.value)}
          className="flex-1 px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <select
          value={statusFilter} onChange={e => setStatus(e.target.value)}
          className="px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        >
          <option value="">সব স্ট্যাটাস</option>
          <option value="active">সক্রিয়</option>
          <option value="inactive">নিষ্ক্রিয়</option>
        </select>
      </div>

      {/* List */}
      {loading ? (
        <div className="space-y-3">
          {[1,2,3].map(i => <div key={i} className="h-20 bg-slate-100 rounded-xl animate-pulse" />)}
        </div>
      ) : managers.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="text-4xl mb-3">👔</div>
          <p className="text-slate-500 text-sm">কোনো ম্যানেজার নেই</p>
        </div>
      ) : (
        <div className="space-y-3">
          {managers.map(m => (
            <div key={m.id} className="bg-white rounded-xl border border-slate-200 shadow-sm p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <Avatar src={m.profile_picture} name={m.name} />
                  <div className="min-w-0">
                    <div className="font-semibold text-slate-800 text-sm">{m.name}</div>
                    <div className="text-xs text-slate-500">{m.mobile} · {m.email}</div>
                    {m.vehicles.length > 0 ? (
                      <div className="mt-1 flex flex-wrap gap-1">
                        {m.vehicles.map(v => (
                          <span key={v.id} className="inline-flex items-center gap-1 text-xs bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full border border-indigo-100">
                            🚗 {v.brand} {v.model} <span className="text-indigo-400">({v.registration_number})</span>
                          </span>
                        ))}
                      </div>
                    ) : (
                      <span className="mt-1 inline-block text-xs text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">গাড়ি অ্যাসাইন নেই</span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${m.status === 'active' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                    {m.status === 'active' ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                  </span>
                  <button onClick={() => openEdit(m)} className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors text-xs">✏️</button>
                  <button onClick={() => openDelete(m)} className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors text-xs">🗑️</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add/Edit Modal */}
      {(modal === 'add' || modal === 'edit') && (
        <Modal onClose={() => setModal(null)}>
          <form onSubmit={handleSubmit}>
            <div className="p-6 border-b border-slate-100">
              <h2 className="text-lg font-bold text-slate-800">{modal === 'add' ? 'নতুন ম্যানেজার যোগ' : 'ম্যানেজার সম্পাদনা'}</h2>
            </div>
            <div className="p-6 space-y-4">
              {err && <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-2 rounded-lg">{err}</div>}

              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-slate-600 mb-1">নাম *</label>
                  <input type="text" required value={form.name} onChange={e => setForm(f => ({...f, name: e.target.value}))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">মোবাইল *</label>
                  <input type="text" required value={form.mobile} onChange={e => setForm(f => ({...f, mobile: e.target.value}))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">স্ট্যাটাস</label>
                  <select value={form.status} onChange={e => setForm(f => ({...f, status: e.target.value as 'active' | 'inactive'}))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
                    <option value="active">সক্রিয়</option>
                    <option value="inactive">নিষ্ক্রিয়</option>
                  </select>
                </div>
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-slate-600 mb-1">ইমেইল *</label>
                  <input type="email" required value={form.email} onChange={e => setForm(f => ({...f, email: e.target.value}))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-slate-600 mb-1">পাসওয়ার্ড {modal === 'edit' ? '(খালি রাখলে পরিবর্তন হবে না)' : '*'}</label>
                  <input type="password" required={modal === 'add'} minLength={6} value={form.password} onChange={e => setForm(f => ({...f, password: e.target.value}))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-slate-600 mb-1">গাড়ি অ্যাসাইন</label>
                  <select value={form.vehicle_id ?? ''} onChange={e => setForm(f => ({...f, vehicle_id: e.target.value ? Number(e.target.value) : null}))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
                    <option value="">গাড়ি অ্যাসাইন নেই</option>
                    {filteredVehicles.map(v => (
                      <option key={v.id} value={v.id}>{v.brand} {v.model} ({v.registration_number})</option>
                    ))}
                  </select>
                  <p className="text-xs text-slate-400 mt-1">প্রতিটি গাড়িতে শুধুমাত্র একজন ম্যানেজার অ্যাসাইন করা যাবে</p>
                </div>
                <div className="col-span-2">
                  <label className="block text-xs font-medium text-slate-600 mb-1">প্রোফাইল ছবি</label>
                  <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={e => setPicFile(e.target.files?.[0] ?? null)} />
                  <button type="button" onClick={() => fileRef.current?.click()}
                    className="px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition-colors">
                    {picFile ? picFile.name : '📷 ছবি বেছে নিন'}
                  </button>
                </div>
              </div>
            </div>
            <div className="p-6 border-t border-slate-100 flex justify-end gap-3">
              <button type="button" onClick={() => setModal(null)} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">বাতিল</button>
              <button type="submit" disabled={submitting} className="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50">
                {submitting ? 'সংরক্ষণ হচ্ছে…' : modal === 'add' ? 'যোগ করুন' : 'আপডেট করুন'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Delete Modal */}
      {modal === 'delete' && selected && (
        <Modal onClose={() => setModal(null)}>
          <div className="p-6">
            <h2 className="text-lg font-bold text-slate-800 mb-2">ম্যানেজার মুছুন</h2>
            <p className="text-slate-600 text-sm mb-6">
              <strong>{selected.name}</strong>-কে মুছে ফেলতে চান? এই কাজটি পূর্বাবস্থায় ফেরানো যাবে না।
            </p>
            {err && <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-2 rounded-lg mb-4">{err}</div>}
            <div className="flex justify-end gap-3">
              <button onClick={() => setModal(null)} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">বাতিল</button>
              <button onClick={handleDelete} disabled={submitting} className="px-4 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50">
                {submitting ? 'মুছছে…' : 'মুছে ফেলুন'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
