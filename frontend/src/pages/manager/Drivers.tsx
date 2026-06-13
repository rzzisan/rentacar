import { useState, useEffect, useCallback, useRef } from 'react';
import { api } from '@/api/client';

interface VehicleOption {
  id: number;
  brand: string;
  model: string;
  registration_number: string;
}

interface AssignedVehicle extends VehicleOption {}

interface Driver {
  id: number;
  name: string;
  mobile: string;
  profile_picture: string | null;
  email: string;
  commission_rate: number;
  status: 'active' | 'inactive';
  vehicles: AssignedVehicle[];
}

interface FormState {
  name: string;
  mobile: string;
  email: string;
  password: string;
  commission_rate: number;
  status: 'active' | 'inactive';
  vehicle_ids: number[];
}

const emptyForm = (): FormState => ({
  name: '',
  mobile: '',
  email: '',
  password: '',
  commission_rate: 0,
  status: 'active',
  vehicle_ids: [],
});

const driverToForm = (d: Driver): FormState => ({
  name: d.name,
  mobile: d.mobile,
  email: d.email,
  password: '',
  commission_rate: d.commission_rate,
  status: d.status,
  vehicle_ids: d.vehicles.map(v => v.id),
});

// ── Modal ─────────────────────────────────────────────────────────
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

// ── Avatar ────────────────────────────────────────────────────────
function Avatar({ src, name, size = 'md' }: { src: string | null; name: string; size?: 'sm' | 'md' | 'lg' }) {
  const cls = size === 'sm' ? 'w-8 h-8 text-xs' : size === 'lg' ? 'w-16 h-16 text-xl' : 'w-10 h-10 text-sm';
  if (src) {
    let imgSrc = src.startsWith('/') ? src : `/${src}`;
    if (!imgSrc.startsWith('/public/')) {
      imgSrc = `/public/${imgSrc.replace(/^\/+/, '')}`;
    }
    return (
      <img
        src={imgSrc}
        alt={name}
        className={`${cls} rounded-full object-cover flex-shrink-0 border-2 border-white shadow-sm`}
        onError={e => {
          (e.target as HTMLImageElement).style.display = 'none';
        }}
      />
    );
  }
  return (
    <div className={`${cls} rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center font-bold text-white flex-shrink-0`}>
      {name.charAt(0).toUpperCase()}
    </div>
  );
}

// ── Driver Form ───────────────────────────────────────────────────
function DriverForm({
  initial,
  currentPicture,
  isEdit,
  vehicles,
  vehiclesLoading,
  onSave,
  onCancel,
  saving,
}: {
  initial: FormState;
  currentPicture?: string | null;
  isEdit: boolean;
  vehicles: VehicleOption[];
  vehiclesLoading: boolean;
  onSave: (data: FormData) => void;
  onCancel: () => void;
  saving: boolean;
}) {
  const [form, setForm] = useState<FormState>(initial);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [showPw, setShowPw] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const set = <K extends keyof FormState>(k: K, v: FormState[K]) =>
    setForm(p => ({ ...p, [k]: v }));

  function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (!f) return;
    setFile(f);
    setPreview(URL.createObjectURL(f));
  }

  function toggleVehicle(id: number) {
    setForm(p => ({
      ...p,
      vehicle_ids: p.vehicle_ids.includes(id)
        ? p.vehicle_ids.filter(x => x !== id)
        : [...p.vehicle_ids, id],
    }));
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const fd = new FormData();
    fd.append('name', form.name);
    fd.append('mobile', form.mobile);
    fd.append('email', form.email);
    fd.append('commission_rate', String(form.commission_rate));
    fd.append('status', form.status);
    fd.append('vehicle_ids', JSON.stringify(form.vehicle_ids));
    if (form.password) fd.append('password', form.password);
    if (file) fd.append('profile_picture', file);
    onSave(fd);
  }

  const inputCls =
    'w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all';
  const labelCls = 'block text-xs font-semibold text-slate-600 mb-1.5';

  const getPictureSrc = (path: string | null | undefined) => {
    if (!path) return null;
    let src = path.startsWith('/') ? path : `/${path}`;
    if (!src.startsWith('/public/')) {
      src = `/public/${src.replace(/^\/+/, '')}`;
    }
    return src;
  };
  const picSrc = preview ?? getPictureSrc(currentPicture);

  return (
    <form onSubmit={handleSubmit} className="p-6 space-y-5">
      {/* Profile picture */}
      <div className="flex items-center gap-4">
        <div
          onClick={() => fileRef.current?.click()}
          className="relative cursor-pointer group"
        >
          {picSrc ? (
            <img src={picSrc} alt="preview" className="w-16 h-16 rounded-full object-cover border-2 border-indigo-200" />
          ) : (
            <div className="w-16 h-16 rounded-full bg-slate-100 border-2 border-dashed border-slate-300 flex items-center justify-center text-2xl group-hover:border-indigo-400 transition-colors">
              👤
            </div>
          )}
          <div className="absolute inset-0 rounded-full bg-black/30 hidden group-hover:flex items-center justify-center text-white text-lg">
            📷
          </div>
        </div>
        <div>
          <button
            type="button"
            onClick={() => fileRef.current?.click()}
            className="text-sm text-indigo-600 hover:text-indigo-800 font-medium"
          >
            ছবি {picSrc ? 'পরিবর্তন' : 'আপলোড'} করুন
          </button>
          <p className="text-[11px] text-slate-400 mt-0.5">JPG, PNG, GIF · সর্বোচ্চ ৫ MB</p>
          <input
            ref={fileRef}
            type="file"
            accept="image/jpeg,image/png,image/gif"
            className="hidden"
            onChange={handleFile}
          />
        </div>
      </div>

      {/* Name + Mobile */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className={labelCls}>নাম *</label>
          <input
            type="text"
            value={form.name}
            onChange={e => set('name', e.target.value)}
            className={inputCls}
            placeholder="ড্রাইভারের নাম"
            required
          />
        </div>
        <div>
          <label className={labelCls}>মোবাইল নম্বর *</label>
          <input
            type="tel"
            value={form.mobile}
            onChange={e => set('mobile', e.target.value)}
            className={inputCls}
            placeholder="01XXXXXXXXX"
            required
          />
        </div>
      </div>

      {/* Email */}
      <div>
        <label className={labelCls}>লগইন ইমেইল *</label>
        <input
          type="email"
          value={form.email}
          onChange={e => set('email', e.target.value)}
          className={inputCls}
          placeholder="driver@example.com"
          required
          autoComplete="off"
        />
      </div>

      {/* Password */}
      <div>
        <label className={labelCls}>
          পাসওয়ার্ড {isEdit && <span className="text-slate-400 font-normal">(পরিবর্তন না করলে খালি রাখুন)</span>}
          {!isEdit && '*'}
        </label>
        <div className="relative">
          <input
            type={showPw ? 'text' : 'password'}
            value={form.password}
            onChange={e => set('password', e.target.value)}
            className={inputCls + ' pr-10'}
            placeholder={isEdit ? 'নতুন পাসওয়ার্ড (ঐচ্ছিক)' : 'কমপক্ষে ৬ অক্ষর'}
            required={!isEdit}
            minLength={form.password ? 6 : undefined}
            autoComplete="new-password"
          />
          <button
            type="button"
            onClick={() => setShowPw(p => !p)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 text-sm"
            tabIndex={-1}
          >
            {showPw ? '🙈' : '👁️'}
          </button>
        </div>
      </div>

      {/* Commission + Status */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className={labelCls}>কমিশন (%) *</label>
          <div className="relative">
            <input
              type="number"
              value={form.commission_rate}
              onChange={e => set('commission_rate', parseFloat(e.target.value) || 0)}
              className={inputCls + ' pr-8'}
              min="0"
              max="100"
              step="0.01"
              required
            />
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">%</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">প্রতি রেন্টালে এই হারে কমিশন প্রযোজ্য</p>
        </div>
        <div>
          <label className={labelCls}>স্ট্যাটাস</label>
          <select
            value={form.status}
            onChange={e => set('status', e.target.value as 'active' | 'inactive')}
            className={inputCls + ' bg-white'}
          >
            <option value="active">সক্রিয়</option>
            <option value="inactive">নিষ্ক্রিয়</option>
          </select>
        </div>
      </div>

      {/* Vehicle assignment */}
      <div>
        <label className={labelCls}>
          গাড়ি অ্যাসাইন করুন
          {form.vehicle_ids.length > 0 && (
            <span className="ml-2 px-1.5 py-0.5 bg-indigo-100 text-indigo-700 rounded text-[10px] font-semibold">
              {form.vehicle_ids.length} টি
            </span>
          )}
        </label>
        <div className="border border-slate-200 rounded-lg max-h-44 overflow-y-auto">
          {vehiclesLoading ? (
            <div className="p-4 text-center text-slate-400 text-sm">লোড হচ্ছে…</div>
          ) : vehicles.length === 0 ? (
            <div className="p-4 text-center text-slate-400 text-sm">কোনো গাড়ি পাওয়া যায়নি</div>
          ) : (
            <ul className="divide-y divide-slate-100">
              {vehicles.map(v => {
                const checked = form.vehicle_ids.includes(v.id);
                return (
                  <li
                    key={v.id}
                    onClick={() => toggleVehicle(v.id)}
                    className={`flex items-center gap-3 px-3 py-2.5 cursor-pointer hover:bg-slate-50 transition-colors ${checked ? 'bg-indigo-50/60' : ''}`}
                  >
                    <div className={`w-4 h-4 rounded border-2 flex-shrink-0 flex items-center justify-center transition-colors ${
                      checked ? 'bg-indigo-600 border-indigo-600' : 'border-slate-300'
                    }`}>
                      {checked && <span className="text-white text-[10px] leading-none">✓</span>}
                    </div>
                    <div className="flex-1 min-w-0">
                      <span className="text-sm font-medium text-slate-800">{v.brand} {v.model}</span>
                      <span className="text-xs text-slate-400 ml-2 font-mono">{v.registration_number}</span>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>

      {/* Buttons */}
      <div className="flex gap-3 pt-1">
        <button
          type="button"
          onClick={onCancel}
          className="flex-1 py-2.5 border border-slate-300 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition"
        >
          বাতিল
        </button>
        <button
          type="submit"
          disabled={saving}
          className="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-lg text-sm font-semibold transition flex items-center justify-center gap-2"
        >
          {saving ? (
            <>
              <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              সংরক্ষণ হচ্ছে…
            </>
          ) : (isEdit ? 'আপডেট করুন' : 'ড্রাইভার যোগ করুন')}
        </button>
      </div>
    </form>
  );
}

// ── Main Page ─────────────────────────────────────────────────────
export default function AdminDrivers() {
  const [drivers, setDrivers]           = useState<Driver[]>([]);
  const [loading, setLoading]           = useState(true);
  const [search, setSearch]             = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  const [addOpen, setAddOpen]           = useState(false);
  const [editDriver, setEditDriver]     = useState<Driver | null>(null);
  const [deleteDriver, setDeleteDriver] = useState<Driver | null>(null);

  const [vehicles, setVehicles]         = useState<VehicleOption[]>([]);
  const [vehiclesLoading, setVehiclesLoading] = useState(false);

  const [saving, setSaving]   = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast]     = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

  function showToast(msg: string, type: 'success' | 'error' = 'success') {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  }

  const load = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (filterStatus) params.set('status', filterStatus);
    if (search)       params.set('search', search);
    const res = await api.get<Driver[]>(`/manager/drivers/index.php?${params}`).catch(() => null);
    if (res?.success && res.data) setDrivers(res.data);
    setLoading(false);
  }, [filterStatus, search]);

  useEffect(() => { load(); }, [load]);

  async function loadVehicles() {
    if (vehicles.length > 0) return; // Already loaded
    setVehiclesLoading(true);
    const res = await api.get<VehicleOption[]>('/manager/vehicles.php').catch(() => null);
    if (res?.success && res.data) setVehicles(res.data);
    setVehiclesLoading(false);
  }

  function openAdd() { loadVehicles(); setAddOpen(true); }
  function openEdit(d: Driver) { loadVehicles(); setEditDriver(d); }

  async function handleAdd(fd: FormData) {
    setSaving(true);
    const res = await api.post('/manager/drivers/index.php', fd).catch(() => null);
    if (res?.success) {
      setAddOpen(false);
      showToast('ড্রাইভার সফলভাবে যোগ করা হয়েছে');
      load();
    } else {
      showToast(res?.message ?? 'যোগ করতে ব্যর্থ', 'error');
    }
    setSaving(false);
  }

  async function handleEdit(fd: FormData) {
    if (!editDriver) return;
    setSaving(true);
    const res = await api.post(`/manager/drivers/update.php?id=${editDriver.id}`, fd).catch(() => null);
    if (res?.success) {
      setEditDriver(null);
      showToast('ড্রাইভার তথ্য আপডেট হয়েছে');
      load();
    } else {
      showToast(res?.message ?? 'আপডেট ব্যর্থ', 'error');
    }
    setSaving(false);
  }

  async function handleDelete() {
    if (!deleteDriver) return;
    setDeleting(true);
    const res = await api.delete(`/manager/drivers/destroy.php?id=${deleteDriver.id}`).catch(() => null);
    if (res?.success) {
      setDeleteDriver(null);
      showToast('ড্রাইভার মুছে ফেলা হয়েছে');
      load();
    } else {
      showToast(res?.message ?? 'মুছতে ব্যর্থ', 'error');
    }
    setDeleting(false);
  }

  const statusCfg = {
    active:   { label: 'সক্রিয়',   cls: 'bg-green-100 text-green-700' },
    inactive: { label: 'নিষ্ক্রিয়', cls: 'bg-slate-100 text-slate-500' },
  };

  return (
    <div>
      {/* Toast */}
      {toast && (
        <div className={`fixed bottom-4 right-4 z-[600] text-white text-sm px-4 py-2.5 rounded-lg shadow-lg animate-in slide-in-from-bottom-4 duration-200 ${
          toast.type === 'error' ? 'bg-red-600' : 'bg-slate-800'
        }`}>
          {toast.msg}
        </div>
      )}

      {/* Page header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
        <div>
          <h1 className="text-xl md:text-2xl font-bold text-slate-800">ড্রাইভার ব্যবস্থাপনা</h1>
          <p className="text-sm text-slate-500 mt-0.5">{drivers.length} জন ড্রাইভার</p>
        </div>
        <button
          onClick={openAdd}
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition self-start sm:self-auto"
        >
          <span className="text-lg leading-none">+</span> নতুন ড্রাইভার
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm mb-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <input
            type="text"
            placeholder="নাম, মোবাইল বা ইমেইল দিয়ে খুঁজুন…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="flex-1 px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <select
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
          >
            <option value="">সব স্ট্যাটাস</option>
            <option value="active">সক্রিয়</option>
            <option value="inactive">নিষ্ক্রিয়</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-400 text-sm">লোড হচ্ছে…</div>
        ) : drivers.length === 0 ? (
          <div className="p-12 text-center">
            <div className="text-5xl mb-3">🧑‍✈️</div>
            <p className="text-slate-500 text-sm">কোনো ড্রাইভার পাওয়া যায়নি</p>
          </div>
        ) : (
          <>
            {/* Desktop table */}
            <div className="hidden lg:block overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 border-b border-slate-200">
                  <tr>
                    {['ড্রাইভার', 'যোগাযোগ', 'কমিশন', 'অ্যাসাইন করা গাড়ি', 'স্ট্যাটাস', ''].map(h => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {drivers.map(d => {
                    const sc = statusCfg[d.status];
                    return (
                      <tr key={d.id} className="hover:bg-slate-50 transition-colors">
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            <Avatar src={d.profile_picture} name={d.name} size="sm" />
                            <div>
                              <div className="font-semibold text-slate-800">{d.name}</div>
                              <div className="text-xs text-slate-400">{d.email}</div>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-slate-600">{d.mobile}</td>
                        <td className="px-4 py-3">
                          <span className="font-semibold text-indigo-700">{d.commission_rate}%</span>
                        </td>
                        <td className="px-4 py-3">
                          {d.vehicles.length === 0 ? (
                            <span className="text-slate-400 text-xs">কোনো গাড়ি নেই</span>
                          ) : (
                            <div className="flex flex-wrap gap-1">
                              {d.vehicles.slice(0, 2).map(v => (
                                <span key={v.id} className="inline-flex px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs">
                                  {v.brand} {v.model}
                                </span>
                              ))}
                              {d.vehicles.length > 2 && (
                                <span className="inline-flex px-2 py-0.5 bg-indigo-50 text-indigo-600 rounded text-xs">
                                  +{d.vehicles.length - 2}
                                </span>
                              )}
                            </div>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium ${sc.cls}`}>
                            {sc.label}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2 justify-end">
                            <button
                              onClick={() => openEdit(d)}
                              className="px-3 py-1.5 text-xs bg-indigo-50 text-indigo-600 hover:bg-indigo-100 rounded-lg transition"
                            >
                              সম্পাদনা
                            </button>
                            <button
                              onClick={() => setDeleteDriver(d)}
                              className="px-3 py-1.5 text-xs bg-red-50 text-red-600 hover:bg-red-100 rounded-lg transition"
                            >
                              মুছুন
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Mobile cards */}
            <div className="lg:hidden divide-y divide-slate-100">
              {drivers.map(d => {
                const sc = statusCfg[d.status];
                return (
                  <div key={d.id} className="p-4">
                    <div className="flex items-start justify-between gap-3 mb-3">
                      <div className="flex items-center gap-3">
                        <Avatar src={d.profile_picture} name={d.name} />
                        <div>
                          <div className="font-semibold text-slate-800">{d.name}</div>
                          <div className="text-xs text-slate-500">{d.mobile}</div>
                          <div className="text-xs text-slate-400 truncate max-w-[180px]">{d.email}</div>
                        </div>
                      </div>
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium flex-shrink-0 ${sc.cls}`}>
                        {sc.label}
                      </span>
                    </div>

                    <div className="flex items-center gap-2 flex-wrap mb-3">
                      <span className="text-xs text-slate-500">কমিশন:</span>
                      <span className="text-xs font-semibold text-indigo-700">{d.commission_rate}%</span>
                      {d.vehicles.length > 0 && (
                        <>
                          <span className="text-slate-300">·</span>
                          <span className="text-xs text-slate-500">{d.vehicles.length} টি গাড়ি</span>
                        </>
                      )}
                    </div>

                    <div className="flex gap-2">
                      <button onClick={() => openEdit(d)} className="flex-1 py-1.5 text-xs bg-indigo-50 text-indigo-600 rounded-lg hover:bg-indigo-100 transition">
                        সম্পাদনা
                      </button>
                      <button onClick={() => setDeleteDriver(d)} className="flex-1 py-1.5 text-xs bg-red-50 text-red-600 rounded-lg hover:bg-red-100 transition">
                        মুছুন
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </div>

      {/* Add Modal */}
      {addOpen && (
        <Modal onClose={() => setAddOpen(false)}>
          <div className="px-6 pt-5 pb-3 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h2 className="text-base font-bold text-slate-800">নতুন ড্রাইভার যোগ করুন</h2>
              <p className="text-xs text-slate-400 mt-0.5">সকল তথ্য পূরণ করুন</p>
            </div>
            <button onClick={() => setAddOpen(false)} className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-slate-100 text-slate-400 text-lg">✕</button>
          </div>
          <DriverForm
            initial={emptyForm()}
            isEdit={false}
            vehicles={vehicles}
            vehiclesLoading={vehiclesLoading}
            onSave={handleAdd}
            onCancel={() => setAddOpen(false)}
            saving={saving}
          />
        </Modal>
      )}

      {/* Edit Modal */}
      {editDriver && (
        <Modal onClose={() => setEditDriver(null)}>
          <div className="px-6 pt-5 pb-3 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Avatar src={editDriver.profile_picture} name={editDriver.name} size="sm" />
              <div>
                <h2 className="text-base font-bold text-slate-800">{editDriver.name}</h2>
                <p className="text-xs text-slate-400">{editDriver.email}</p>
              </div>
            </div>
            <button onClick={() => setEditDriver(null)} className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-slate-100 text-slate-400 text-lg">✕</button>
          </div>
          <DriverForm
            initial={driverToForm(editDriver)}
            currentPicture={editDriver.profile_picture}
            isEdit={true}
            vehicles={vehicles}
            vehiclesLoading={vehiclesLoading}
            onSave={handleEdit}
            onCancel={() => setEditDriver(null)}
            saving={saving}
          />
        </Modal>
      )}

      {/* Delete Modal */}
      {deleteDriver && (
        <Modal onClose={() => setDeleteDriver(null)}>
          <div className="p-6 text-center">
            <div className="flex justify-center mb-4">
              <Avatar src={deleteDriver.profile_picture} name={deleteDriver.name} size="lg" />
            </div>
            <h2 className="text-lg font-bold text-slate-800 mb-1">{deleteDriver.name}</h2>
            <p className="text-sm text-slate-500 mb-1">{deleteDriver.mobile}</p>
            <p className="text-sm text-slate-400 mb-5">এই ড্রাইভারকে স্থায়ীভাবে মুছে ফেলতে চান?</p>
            <div className="flex gap-3">
              <button
                onClick={() => setDeleteDriver(null)}
                className="flex-1 py-2.5 border border-slate-300 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition"
              >
                বাতিল
              </button>
              <button
                onClick={handleDelete}
                disabled={deleting}
                className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-lg text-sm font-semibold transition flex items-center justify-center gap-2"
              >
                {deleting ? (
                  <>
                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    মুছছে…
                  </>
                ) : '🗑️ হ্যাঁ, মুছুন'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
