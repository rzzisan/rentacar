import { useState, useEffect, useRef } from 'react';
import { api } from '../../api/client';

interface DriverProfile {
  id: number;
  name: string;
  mobile: string;
  email: string;
  commission_rate: number;
  profile_picture: string | null;
  status: 'active' | 'inactive';
  created_at: string;
}

interface AssignedVehicle {
  id: number;
  brand: string;
  model: string;
  registration_number: string;
  vehicle_status: string;
  vehicle_type: string;
}

interface ProfileStats {
  total_trips: number;
  completed_trips: number;
  active_trips: number;
  pending_trips: number;
}

interface ProfileData {
  driver: DriverProfile;
  vehicles: AssignedVehicle[];
  stats: ProfileStats;
}

const VEHICLE_STATUS_LABELS: Record<string, string> = {
  available: 'উপলব্ধ',
  rented: 'ভাড়ায়',
  maintenance: 'রক্ষণাবেক্ষণ',
  inactive: 'নিষ্ক্রিয়',
};
const VEHICLE_STATUS_COLORS: Record<string, string> = {
  available: 'bg-green-100 text-green-800',
  rented: 'bg-blue-100 text-blue-800',
  maintenance: 'bg-amber-100 text-amber-800',
  inactive: 'bg-gray-100 text-gray-600',
};
const VEHICLE_TYPE_LABELS: Record<string, string> = {
  sedan: 'সেডান',
  suv: 'এসইউভি',
  van: 'ভ্যান',
  pickup: 'পিকআপ',
  truck: 'ট্রাক',
  premium: 'প্রিমিয়াম',
};

export default function DriverProfile() {
  const [data, setData]         = useState<ProfileData | null>(null);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');
  const [toast, setToast]       = useState('');
  const [saving, setSaving]     = useState(false);
  const [tab, setTab]           = useState<'info' | 'password'>('info');
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(''), 3500);
  };

  const load = () => {
    setLoading(true);
    api.get<ProfileData>('/driver/profile.php')
      .then(res => {
        if (res.success && res.data) setData(res.data);
        else setError(res.message ?? 'লোড ব্যর্থ');
      })
      .catch(() => setError('নেটওয়ার্ক সমস্যা'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleInfoSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSaving(true);
    const fd = new FormData(e.currentTarget);
    try {
      const res = await fetch('/api/driver/profile.php', {
        method: 'POST',
        credentials: 'include',
        body: fd,
      });
      const json = await res.json() as { success: boolean; message?: string };
      if (json.success) {
        showToast('প্রোফাইল আপডেট হয়েছে');
        setPreviewUrl(null);
        load();
      } else {
        showToast(json.message ?? 'আপডেট ব্যর্থ');
      }
    } catch {
      showToast('নেটওয়ার্ক সমস্যা');
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    if (fd.get('new_password') !== fd.get('confirm_password')) {
      showToast('নতুন পাসওয়ার্ড দুটো মিলছে না');
      return;
    }
    setSaving(true);
    const body = new FormData();
    body.append('name', data?.driver.name ?? '');
    body.append('mobile', data?.driver.mobile ?? '');
    body.append('current_password', fd.get('current_password') as string);
    body.append('new_password', fd.get('new_password') as string);
    try {
      const res = await fetch('/api/driver/profile.php', {
        method: 'POST',
        credentials: 'include',
        body,
      });
      const json = await res.json() as { success: boolean; message?: string };
      if (json.success) {
        showToast('পাসওয়ার্ড পরিবর্তন হয়েছে');
        (e.target as HTMLFormElement).reset();
      } else {
        showToast(json.message ?? 'পরিবর্তন ব্যর্থ');
      }
    } catch {
      showToast('নেটওয়ার্ক সমস্যা');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return (
    <div className="p-4 sm:p-6 space-y-4 animate-pulse">
      <div className="h-8 w-48 bg-slate-200 rounded" />
      <div className="h-32 bg-slate-200 rounded-xl" />
      <div className="h-48 bg-slate-200 rounded-xl" />
    </div>
  );

  if (error || !data) return (
    <div className="p-6 text-center text-red-600">{error || 'তথ্য লোড হয়নি'}</div>
  );

  const { driver, vehicles, stats } = data;

  return (
    <div className="p-4 sm:p-6 max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">আমার প্রোফাইল</h1>
        <p className="text-gray-500 text-sm mt-1">ব্যক্তিগত তথ্য ও নিরাপত্তা সেটিংস</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[
          { label: 'মোট ট্রিপ', value: stats.total_trips, color: 'text-blue-600', bg: 'bg-blue-50 border-blue-200' },
          { label: 'সম্পন্ন', value: stats.completed_trips, color: 'text-green-600', bg: 'bg-green-50 border-green-200' },
          { label: 'চলমান', value: stats.active_trips, color: 'text-indigo-600', bg: 'bg-indigo-50 border-indigo-200' },
          { label: 'অপেক্ষমান', value: stats.pending_trips, color: 'text-amber-600', bg: 'bg-amber-50 border-amber-200' },
        ].map(s => (
          <div key={s.label} className={`${s.bg} border rounded-lg p-3 text-center`}>
            <div className={`text-2xl font-bold ${s.color}`}>{s.value}</div>
            <div className="text-xs text-gray-600 mt-0.5">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Profile Card */}
      <div className="bg-white rounded-xl border shadow-sm overflow-hidden">
        {/* Avatar + basic info */}
        <div className="bg-gradient-to-r from-indigo-500 to-violet-600 p-6 flex items-center gap-4">
          <div className="relative">
            {previewUrl || driver.profile_picture ? (
              <img
                src={previewUrl ?? `/public/${driver.profile_picture}`}
                alt="ছবি"
                className="w-20 h-20 rounded-full object-cover border-4 border-white/40"
              />
            ) : (
              <div className="w-20 h-20 rounded-full bg-white/20 border-4 border-white/40 flex items-center justify-center text-3xl font-bold text-white">
                {driver.name.charAt(0).toUpperCase()}
              </div>
            )}
          </div>
          <div className="text-white">
            <div className="text-xl font-bold">{driver.name}</div>
            <div className="text-indigo-100 text-sm">{driver.email}</div>
            <div className="flex items-center gap-2 mt-1.5">
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${driver.status === 'active' ? 'bg-green-400/30 text-green-100' : 'bg-gray-400/30 text-gray-200'}`}>
                {driver.status === 'active' ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
              </span>
              <span className="text-indigo-200 text-xs">কমিশন: {driver.commission_rate}%</span>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex border-b">
          <button
            onClick={() => setTab('info')}
            className={`flex-1 py-3 text-sm font-medium transition-colors ${tab === 'info' ? 'text-indigo-600 border-b-2 border-indigo-600' : 'text-gray-500 hover:text-gray-700'}`}
          >
            তথ্য সম্পাদনা
          </button>
          <button
            onClick={() => setTab('password')}
            className={`flex-1 py-3 text-sm font-medium transition-colors ${tab === 'password' ? 'text-indigo-600 border-b-2 border-indigo-600' : 'text-gray-500 hover:text-gray-700'}`}
          >
            পাসওয়ার্ড পরিবর্তন
          </button>
        </div>

        {/* Info Tab */}
        {tab === 'info' && (
          <form onSubmit={handleInfoSubmit} className="p-4 sm:p-6 space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">পূর্ণ নাম *</label>
                <input
                  type="text"
                  name="name"
                  defaultValue={driver.name}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">মোবাইল নম্বর *</label>
                <input
                  type="text"
                  name="mobile"
                  defaultValue={driver.mobile}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">ইমেইল</label>
              <input
                type="email"
                value={driver.email}
                disabled
                className="w-full px-3 py-2 border border-gray-200 rounded-lg bg-gray-50 text-gray-500 text-sm cursor-not-allowed"
              />
              <p className="text-xs text-gray-400 mt-1">ইমেইল পরিবর্তন করতে অ্যাডমিনের সাথে যোগাযোগ করুন</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">প্রোফাইল ছবি</label>
              <input
                ref={fileRef}
                type="file"
                name="profile_picture"
                accept="image/jpeg,image/png,image/gif"
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) setPreviewUrl(URL.createObjectURL(f));
                  else setPreviewUrl(null);
                }}
                className="w-full text-sm text-gray-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-sm file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
              />
              {previewUrl && (
                <div className="mt-2 flex items-center gap-2">
                  <img src={previewUrl} alt="preview" className="h-14 w-14 rounded-lg object-cover border" />
                  <button
                    type="button"
                    onClick={() => { setPreviewUrl(null); if (fileRef.current) fileRef.current.value = ''; }}
                    className="text-xs text-red-500 hover:text-red-700"
                  >
                    বাতিল
                  </button>
                </div>
              )}
            </div>

            <div className="grid grid-cols-2 gap-3 p-3 bg-gray-50 rounded-lg text-sm">
              <div>
                <span className="text-gray-500">কমিশন হার:</span>
                <span className="ml-1 font-semibold text-indigo-600">{driver.commission_rate}%</span>
              </div>
              <div>
                <span className="text-gray-500">যোগদান:</span>
                <span className="ml-1 font-medium">{new Date(driver.created_at).toLocaleDateString('bn-BD')}</span>
              </div>
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={saving}
                className="px-5 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:bg-gray-400 text-sm font-medium"
              >
                {saving ? 'সংরক্ষণ হচ্ছে...' : 'সংরক্ষণ করুন'}
              </button>
            </div>
          </form>
        )}

        {/* Password Tab */}
        {tab === 'password' && (
          <form onSubmit={handlePasswordSubmit} className="p-4 sm:p-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">বর্তমান পাসওয়ার্ড *</label>
              <input
                type="password"
                name="current_password"
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">নতুন পাসওয়ার্ড *</label>
              <input
                type="password"
                name="new_password"
                required
                minLength={6}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
              />
              <p className="text-xs text-gray-400 mt-1">কমপক্ষে ৬ অক্ষর</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">পাসওয়ার্ড নিশ্চিত করুন *</label>
              <input
                type="password"
                name="confirm_password"
                required
                minLength={6}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
              />
            </div>
            <div className="flex justify-end">
              <button
                type="submit"
                disabled={saving}
                className="px-5 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400 text-sm font-medium"
              >
                {saving ? 'পরিবর্তন হচ্ছে...' : 'পাসওয়ার্ড পরিবর্তন করুন'}
              </button>
            </div>
          </form>
        )}
      </div>

      {/* Assigned Vehicles */}
      <div className="bg-white rounded-xl border shadow-sm">
        <div className="p-4 border-b">
          <h2 className="font-semibold text-gray-800">আমার গাড়ি</h2>
          <p className="text-xs text-gray-500 mt-0.5">আপনাকে অ্যাসাইন করা গাড়ির তালিকা</p>
        </div>
        {vehicles.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            <div className="text-4xl mb-2">🚗</div>
            <p>কোনো গাড়ি অ্যাসাইন করা নেই</p>
            <p className="text-xs mt-1">অ্যাডমিনের সাথে যোগাযোগ করুন</p>
          </div>
        ) : (
          <div className="divide-y">
            {vehicles.map(v => (
              <div key={v.id} className="p-4 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-indigo-50 flex items-center justify-center text-xl flex-shrink-0">
                    🚗
                  </div>
                  <div>
                    <div className="font-medium text-gray-900">{v.brand} {v.model}</div>
                    <div className="text-sm text-gray-500">
                      {v.registration_number} • {VEHICLE_TYPE_LABELS[v.vehicle_type] ?? v.vehicle_type}
                    </div>
                  </div>
                </div>
                <span className={`px-2 py-1 rounded-full text-xs font-medium flex-shrink-0 ${VEHICLE_STATUS_COLORS[v.vehicle_status] ?? 'bg-gray-100 text-gray-600'}`}>
                  {VEHICLE_STATUS_LABELS[v.vehicle_status] ?? v.vehicle_status}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-4 right-4 bg-gray-900 text-white px-4 py-3 rounded-lg shadow-lg z-50">
          {toast}
        </div>
      )}
    </div>
  );
}
