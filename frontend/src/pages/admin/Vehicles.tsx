import { useState, useEffect, useCallback } from 'react';
import { api } from '@/api/client';

interface Vehicle {
  id: number;
  registration_number: string;
  brand: string;
  model: string;
  year: number;
  vehicle_type: string;
  color: string;
  fuel_type: string;
  seating_capacity: number;
  mileage: number;
  daily_rent_price: number;
  status: string;
}

const typeLabel: Record<string, string> = {
  sedan: 'সেডান', suv: 'এসইউভি', van: 'ভ্যান', truck: 'ট্রাক', premium: 'প্রিমিয়াম',
};
const fuelLabel: Record<string, string> = {
  petrol: 'পেট্রোল', diesel: 'ডিজেল', hybrid: 'হাইব্রিড', electric: 'ইলেকট্রিক',
};
const statusConfig: Record<string, { label: string; cls: string }> = {
  available:   { label: 'উপলব্ধ',     cls: 'bg-green-100 text-green-700' },
  rented:      { label: 'ভাড়ায়',     cls: 'bg-blue-100 text-blue-700' },
  maintenance: { label: 'রক্ষণাবেক্ষণ', cls: 'bg-yellow-100 text-yellow-700' },
  inactive:    { label: 'নিষ্ক্রিয়',   cls: 'bg-slate-100 text-slate-500' },
};

type FormData = Omit<Vehicle, 'id' | 'mileage'>;
const emptyForm = (): FormData => ({
  registration_number: '',
  brand: '',
  model: '',
  year: new Date().getFullYear(),
  vehicle_type: 'sedan',
  color: '',
  fuel_type: 'petrol',
  seating_capacity: 5,
  daily_rent_price: 0,
  status: 'available',
});

// ── Modal backdrop ────────────────────────────────────────────
function Modal({ onClose, children }: { onClose: () => void; children: React.ReactNode }) {
  return (
    <div
      className="fixed inset-0 z-[500] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      onClick={e => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        {children}
      </div>
    </div>
  );
}

// ── Vehicle Form ──────────────────────────────────────────────
function VehicleForm({
  initial,
  onSave,
  onCancel,
  saving,
}: {
  initial: FormData;
  onSave: (data: FormData) => void;
  onCancel: () => void;
  saving: boolean;
}) {
  const [form, setForm] = useState<FormData>(initial);
  const set = (k: keyof FormData, v: string | number) => setForm(p => ({ ...p, [k]: v }));

  const field = (label: string, children: React.ReactNode) => (
    <div>
      <label className="block text-xs font-medium text-slate-600 mb-1">{label}</label>
      {children}
    </div>
  );
  const input = (k: keyof FormData, type = 'text', extra = {}) => (
    <input
      type={type}
      value={String(form[k])}
      onChange={e => set(k, type === 'number' ? Number(e.target.value) : e.target.value)}
      className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
      {...extra}
    />
  );
  const select = (k: keyof FormData, opts: Record<string, string>) => (
    <select
      value={String(form[k])}
      onChange={e => set(k, e.target.value)}
      className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
    >
      {Object.entries(opts).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
    </select>
  );

  return (
    <form onSubmit={e => { e.preventDefault(); onSave(form); }} className="p-6 space-y-4">
      <div className="grid grid-cols-2 gap-4">
        {field('রেজিস্ট্রেশন নম্বর *', input('registration_number', 'text', { required: true, placeholder: 'ঢাকা মেট্রো ক ১২-৩৪৫৬' }))}
        {field('বছর *', input('year', 'number', { required: true, min: 1990, max: new Date().getFullYear() + 1 }))}
      </div>
      <div className="grid grid-cols-2 gap-4">
        {field('ব্র্যান্ড *', input('brand', 'text', { required: true, placeholder: 'Toyota' }))}
        {field('মডেল *', input('model', 'text', { required: true, placeholder: 'Corolla' }))}
      </div>
      <div className="grid grid-cols-2 gap-4">
        {field('ধরন *', select('vehicle_type', typeLabel))}
        {field('জ্বালানি', select('fuel_type', fuelLabel))}
      </div>
      <div className="grid grid-cols-2 gap-4">
        {field('রঙ', input('color', 'text', { placeholder: 'সাদা' }))}
        {field('আসন সংখ্যা', input('seating_capacity', 'number', { min: 1, max: 50 }))}
      </div>
      <div className="grid grid-cols-2 gap-4">
        {field('দৈনিক ভাড়া (৳) *', input('daily_rent_price', 'number', { required: true, min: 0, step: '0.01' }))}
        {field('স্ট্যাটাস', select('status', {
          available: 'উপলব্ধ', rented: 'ভাড়ায়', maintenance: 'রক্ষণাবেক্ষণ', inactive: 'নিষ্ক্রিয়',
        }))}
      </div>
      <div className="flex gap-3 pt-2">
        <button type="button" onClick={onCancel} className="flex-1 py-2 border border-slate-300 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
          বাতিল
        </button>
        <button type="submit" disabled={saving} className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white rounded-lg text-sm font-medium transition">
          {saving ? 'সংরক্ষণ হচ্ছে…' : 'সংরক্ষণ করুন'}
        </button>
      </div>
    </form>
  );
}

// ── Main Vehicles Page ────────────────────────────────────────
export default function AdminVehicles() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterType, setFilterType] = useState('');

  const [addOpen, setAddOpen] = useState(false);
  const [editVehicle, setEditVehicle] = useState<Vehicle | null>(null);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState('');

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(''), 3000);
  };

  const load = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (filterStatus) params.set('status', filterStatus);
    if (filterType)   params.set('vehicle_type', filterType);
    if (search)       params.set('search', search);
    const res = await api.get<Vehicle[]>(`/vehicles/index.php?${params}`).catch(() => null);
    if (res?.success && res.data) setVehicles(res.data);
    setLoading(false);
  }, [filterStatus, filterType, search]);

  useEffect(() => { load(); }, [load]);

  async function handleAdd(data: FormData) {
    setSaving(true);
    const res = await api.post<{ vehicle_id: number }>('/vehicles/index.php', data).catch(() => null);
    if (res?.success) {
      setAddOpen(false);
      showToast('গাড়ি সফলভাবে যোগ করা হয়েছে');
      load();
    } else {
      showToast(res?.message ?? 'গাড়ি যোগ করতে ব্যর্থ');
    }
    setSaving(false);
  }

  async function handleEdit(data: FormData) {
    if (!editVehicle) return;
    setSaving(true);
    const res = await api.put(`/vehicles/update.php?id=${editVehicle.id}`, data).catch(() => null);
    if (res?.success) {
      setEditVehicle(null);
      showToast('গাড়ির তথ্য আপডেট হয়েছে');
      load();
    } else {
      showToast(res?.message ?? 'আপডেট ব্যর্থ');
    }
    setSaving(false);
  }

  async function handleDelete() {
    if (!deleteId) return;
    setDeleting(true);
    const res = await api.delete(`/vehicles/destroy.php?id=${deleteId}`).catch(() => null);
    if (res?.success) {
      setDeleteId(null);
      showToast('গাড়ি মুছে ফেলা হয়েছে');
      load();
    } else {
      showToast(res?.message ?? 'মুছতে ব্যর্থ');
    }
    setDeleting(false);
  }

  return (
    <div>
      {/* Toast */}
      {toast && (
        <div className="fixed bottom-4 right-4 z-[600] bg-slate-800 text-white text-sm px-4 py-2.5 rounded-lg shadow-lg animate-in slide-in-from-bottom-4 duration-200">
          {toast}
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
        <div>
          <h1 className="text-xl md:text-2xl font-bold text-slate-800">গাড়ি ব্যবস্থাপনা</h1>
          <p className="text-sm text-slate-500 mt-0.5">{vehicles.length} টি গাড়ি</p>
        </div>
        <button
          onClick={() => setAddOpen(true)}
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition self-start sm:self-auto"
        >
          <span className="text-lg leading-none">+</span> নতুন গাড়ি যোগ
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm mb-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <input
            type="text"
            placeholder="ব্র্যান্ড, মডেল বা নম্বর খুঁজুন…"
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
            {Object.entries(statusConfig).map(([v, c]) => (
              <option key={v} value={v}>{c.label}</option>
            ))}
          </select>
          <select
            value={filterType}
            onChange={e => setFilterType(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
          >
            <option value="">সব ধরন</option>
            {Object.entries(typeLabel).map(([v, l]) => (
              <option key={v} value={v}>{l}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-400 text-sm">লোড হচ্ছে…</div>
        ) : vehicles.length === 0 ? (
          <div className="p-12 text-center">
            <div className="text-4xl mb-3">🚗</div>
            <p className="text-slate-500 text-sm">কোনো গাড়ি পাওয়া যায়নি</p>
          </div>
        ) : (
          <>
            {/* Desktop table */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 border-b border-slate-200">
                  <tr>
                    {['গাড়ি', 'নম্বর', 'ধরন / জ্বালানি', 'দৈনিক ভাড়া', 'স্ট্যাটাস', ''].map(h => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {vehicles.map(v => {
                    const sc = statusConfig[v.status] ?? { label: v.status, cls: 'bg-slate-100 text-slate-500' };
                    return (
                      <tr key={v.id} className="hover:bg-slate-50 transition-colors">
                        <td className="px-4 py-3">
                          <div className="font-semibold text-slate-800">{v.brand} {v.model}</div>
                          <div className="text-xs text-slate-400">{v.year} • {v.color}</div>
                        </td>
                        <td className="px-4 py-3 text-slate-600 font-mono text-xs">{v.registration_number}</td>
                        <td className="px-4 py-3">
                          <div className="text-slate-700">{typeLabel[v.vehicle_type] ?? v.vehicle_type}</div>
                          <div className="text-xs text-slate-400">{fuelLabel[v.fuel_type] ?? v.fuel_type} • {v.seating_capacity} আসন</div>
                        </td>
                        <td className="px-4 py-3 font-semibold text-indigo-700">৳{Number(v.daily_rent_price).toLocaleString()}</td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium ${sc.cls}`}>{sc.label}</span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2 justify-end">
                            <button
                              onClick={() => setEditVehicle(v)}
                              className="px-3 py-1.5 text-xs bg-indigo-50 text-indigo-600 hover:bg-indigo-100 rounded-lg transition"
                            >
                              সম্পাদনা
                            </button>
                            <button
                              onClick={() => setDeleteId(v.id)}
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
            <div className="md:hidden divide-y divide-slate-100">
              {vehicles.map(v => {
                const sc = statusConfig[v.status] ?? { label: v.status, cls: 'bg-slate-100 text-slate-500' };
                return (
                  <div key={v.id} className="p-4">
                    <div className="flex justify-between items-start mb-2">
                      <div>
                        <div className="font-semibold text-slate-800">{v.brand} {v.model}</div>
                        <div className="text-xs text-slate-400">{v.year} • {v.registration_number}</div>
                      </div>
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${sc.cls}`}>{sc.label}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="text-sm text-indigo-700 font-semibold">৳{Number(v.daily_rent_price).toLocaleString()}/দিন</div>
                      <div className="flex gap-2">
                        <button onClick={() => setEditVehicle(v)} className="px-3 py-1 text-xs bg-indigo-50 text-indigo-600 rounded-lg">সম্পাদনা</button>
                        <button onClick={() => setDeleteId(v.id)} className="px-3 py-1 text-xs bg-red-50 text-red-600 rounded-lg">মুছুন</button>
                      </div>
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
          <div className="px-6 pt-6 pb-2 border-b border-slate-100">
            <h2 className="text-lg font-bold text-slate-800">নতুন গাড়ি যোগ করুন</h2>
          </div>
          <VehicleForm
            initial={emptyForm()}
            onSave={handleAdd}
            onCancel={() => setAddOpen(false)}
            saving={saving}
          />
        </Modal>
      )}

      {/* Edit Modal */}
      {editVehicle && (
        <Modal onClose={() => setEditVehicle(null)}>
          <div className="px-6 pt-6 pb-2 border-b border-slate-100">
            <h2 className="text-lg font-bold text-slate-800">গাড়ি সম্পাদনা</h2>
            <p className="text-sm text-slate-400">{editVehicle.brand} {editVehicle.model}</p>
          </div>
          <VehicleForm
            initial={{
              registration_number: editVehicle.registration_number,
              brand: editVehicle.brand,
              model: editVehicle.model,
              year: editVehicle.year,
              vehicle_type: editVehicle.vehicle_type,
              color: editVehicle.color,
              fuel_type: editVehicle.fuel_type,
              seating_capacity: editVehicle.seating_capacity,
              daily_rent_price: editVehicle.daily_rent_price,
              status: editVehicle.status,
            }}
            onSave={handleEdit}
            onCancel={() => setEditVehicle(null)}
            saving={saving}
          />
        </Modal>
      )}

      {/* Delete confirm */}
      {deleteId && (
        <Modal onClose={() => setDeleteId(null)}>
          <div className="p-6 text-center">
            <div className="text-4xl mb-3">⚠️</div>
            <h2 className="text-lg font-bold text-slate-800 mb-2">গাড়ি মুছবেন?</h2>
            <p className="text-sm text-slate-500 mb-6">এই কাজটি পূর্বাবস্থায় ফেরানো যাবে না।</p>
            <div className="flex gap-3">
              <button onClick={() => setDeleteId(null)} className="flex-1 py-2 border border-slate-300 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">বাতিল</button>
              <button onClick={handleDelete} disabled={deleting} className="flex-1 py-2 bg-red-600 hover:bg-red-700 disabled:bg-red-400 text-white rounded-lg text-sm font-medium transition">
                {deleting ? 'মুছছে…' : 'হ্যাঁ, মুছুন'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
