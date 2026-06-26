import { useState, useEffect } from 'react';
import { api } from '@/api/client';
import type { Vehicle } from '@/types';

const statusLabel: Record<string, string> = {
  available: 'উপলব্ধ', rented: 'ভাড়ায়', maintenance: 'রক্ষণাবেক্ষণ', inactive: 'নিষ্ক্রিয়',
};
const statusColor: Record<string, string> = {
  available: 'bg-emerald-100 text-emerald-700',
  rented: 'bg-orange-100 text-orange-700',
  maintenance: 'bg-yellow-100 text-yellow-700',
  inactive: 'bg-slate-100 text-slate-500',
};
const typeLabel: Record<string, string> = {
  sedan: 'সেডান', suv: 'SUV', van: 'ভ্যান', truck: 'ট্রাক', premium: 'প্রিমিয়াম',
};

// Manager যে status-এ পরিবর্তন করতে পারবে (rented পারবে না)
const changeableStatuses = [
  { value: 'available',    label: 'উপলব্ধ',         icon: '✅' },
  { value: 'maintenance',  label: 'রক্ষণাবেক্ষণ',   icon: '🔧' },
  { value: 'inactive',     label: 'নিষ্ক্রিয়',      icon: '⛔' },
];

export default function ManagerVehicles() {
  const [vehicles, setVehicles]           = useState<Vehicle[]>([]);
  const [loading, setLoading]             = useState(true);
  const [error, setError]                 = useState('');
  const [toast, setToast]                 = useState('');
  const [changingId, setChangingId]       = useState<number | null>(null);
  const [confirmVehicle, setConfirmVehicle] = useState<{ vehicle: Vehicle; newStatus: string } | null>(null);
  const [saving, setSaving]               = useState(false);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(''), 3500);
  };

  const load = () => {
    setLoading(true);
    api.get<Vehicle[]>('/manager/vehicles.php')
      .then(res => {
        if (res.success) setVehicles(res.data ?? []);
        else setError(res.message ?? 'লোড ব্যর্থ');
      })
      .catch(() => setError('নেটওয়ার্ক সমস্যা'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleStatusChange = async (vehicle: Vehicle, newStatus: string) => {
    setSaving(true);
    try {
      const res = await api.put(`/manager/vehicles.php?id=${vehicle.id}`, { status: newStatus });
      if (res.success) {
        showToast(res.message ?? 'স্ট্যাটাস আপডেট হয়েছে');
        setVehicles(prev => prev.map(v => v.id === vehicle.id ? { ...v, status: newStatus as Vehicle['status'] } : v));
      } else {
        showToast(res.message ?? 'আপডেট ব্যর্থ');
      }
    } catch {
      showToast('নেটওয়ার্ক সমস্যা');
    } finally {
      setSaving(false);
      setChangingId(null);
      setConfirmVehicle(null);
    }
  };

  if (loading) return (
    <div className="space-y-3">
      <div className="h-8 w-48 bg-slate-200 rounded animate-pulse" />
      {[1, 2].map(i => <div key={i} className="h-24 bg-slate-100 rounded-xl animate-pulse" />)}
    </div>
  );

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-xl md:text-2xl font-bold text-slate-800">আমার গাড়ি</h1>
        <p className="text-sm text-slate-500 mt-0.5">আপনার অধীনস্থ {vehicles.length}টি গাড়ি • স্ট্যাটাস পরিবর্তন করা যাবে</p>
      </div>

      {error && <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-2 rounded-lg mb-4">{error}</div>}

      {vehicles.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="text-4xl mb-3">🚗</div>
          <p className="text-slate-500 text-sm">আপনার কোনো গাড়ি অ্যাসাইন করা নেই</p>
          <p className="text-slate-400 text-xs mt-1">অ্যাডমিনের সাথে যোগাযোগ করুন</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {vehicles.map(v => (
            <div key={v.id} className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
              {v.image_path ? (
                <img
                  src={`/public/${v.image_path.replace(/^\/+/, '')}`}
                  alt={`${v.brand} ${v.model}`}
                  className="w-full h-40 object-cover"
                />
              ) : (
                <div className="w-full h-40 bg-gradient-to-br from-slate-100 to-slate-200 flex items-center justify-center text-4xl">🚗</div>
              )}
              <div className="p-4">
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div>
                    <div className="font-semibold text-slate-800">{v.brand} {v.model}</div>
                    <div className="text-xs text-slate-400">{v.registration_number}</div>
                  </div>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium flex-shrink-0 ${statusColor[v.status] || 'bg-slate-100 text-slate-500'}`}>
                    {statusLabel[v.status] || v.status}
                  </span>
                </div>
                <div className="grid grid-cols-2 gap-2 text-xs text-slate-500">
                  <div>ধরন: {typeLabel[v.vehicle_type] || v.vehicle_type}</div>
                  <div>বছর: {v.year}</div>
                  <div>আসন: {v.seating_capacity}</div>
                  <div>জ্বালানি: {v.fuel_type}</div>
                </div>
                <div className="mt-3 pt-3 border-t border-slate-100 text-sm font-semibold text-indigo-700">
                  ৳{v.daily_rent_price.toLocaleString('bn-BD')} <span className="font-normal text-xs text-slate-400">/ দিন</span>
                </div>

                {/* Status change — rented গাড়ি বাদে */}
                {v.status !== 'rented' && (
                  <div className="mt-3 pt-3 border-t border-slate-100">
                    {changingId === v.id ? (
                      <div className="flex flex-col gap-1.5">
                        <p className="text-xs text-gray-500 mb-1">নতুন স্ট্যাটাস বেছে নিন:</p>
                        {changeableStatuses
                          .filter(s => s.value !== v.status)
                          .map(s => (
                            <button
                              key={s.value}
                              onClick={() => setConfirmVehicle({ vehicle: v, newStatus: s.value })}
                              className="w-full text-left text-sm px-3 py-1.5 rounded-lg border hover:bg-gray-50 flex items-center gap-2"
                            >
                              <span>{s.icon}</span> {s.label}
                            </button>
                          ))}
                        <button
                          onClick={() => setChangingId(null)}
                          className="text-xs text-gray-400 hover:text-gray-600 mt-1"
                        >
                          বাতিল
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={() => setChangingId(v.id)}
                        className="w-full text-sm text-slate-600 border border-slate-200 rounded-lg py-1.5 hover:bg-slate-50 hover:border-slate-300 transition"
                      >
                        🔄 স্ট্যাটাস পরিবর্তন
                      </button>
                    )}
                  </div>
                )}
                {v.status === 'rented' && (
                  <div className="mt-3 pt-3 border-t border-slate-100">
                    <p className="text-xs text-orange-500 text-center">চলমান ভাড়া — স্ট্যাটাস পরিবর্তন করা যাবে না</p>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Confirm Modal */}
      {confirmVehicle && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-sm w-full p-6">
            <h3 className="font-bold text-gray-900 mb-2">স্ট্যাটাস পরিবর্তন নিশ্চিত করুন</h3>
            <p className="text-sm text-gray-600 mb-4">
              <strong>{confirmVehicle.vehicle.brand} {confirmVehicle.vehicle.model}</strong> ({confirmVehicle.vehicle.registration_number}) কে{' '}
              <strong className="text-indigo-600">{statusLabel[confirmVehicle.newStatus]}</strong> করতে চান?
            </p>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => { setConfirmVehicle(null); setChangingId(null); }}
                disabled={saving}
                className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50"
              >
                বাতিল
              </button>
              <button
                onClick={() => handleStatusChange(confirmVehicle.vehicle, confirmVehicle.newStatus)}
                disabled={saving}
                className="px-4 py-2 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:bg-gray-400"
              >
                {saving ? 'আপডেট হচ্ছে...' : 'নিশ্চিত করুন'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-4 right-4 bg-gray-900 text-white px-4 py-3 rounded-lg shadow-lg z-50">
          {toast}
        </div>
      )}
    </div>
  );
}
