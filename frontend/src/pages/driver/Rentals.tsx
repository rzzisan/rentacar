import React, { useState, useCallback, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../../api/client';
import type { Rental, Vehicle, TripExpense } from '../../types';

const TRIP_TYPE_LABELS: Record<string, string> = {
  one_way: 'এক-দিকে',
  round_trip: 'রাউন্ড ট্রিপ',
};

// এক-দিকে: শুরু→শেষ, রাউন্ড ট্রিপ: শুরু→শেষ→শুরু
const tripRoute = (r: { trip_type?: string; pickup_location?: string; dropoff_location?: string }) => {
  if (!r.pickup_location && !r.dropoff_location) return '—';
  const pickup = r.pickup_location || '?';
  const dropoff = r.dropoff_location || '?';
  return r.trip_type === 'round_trip'
    ? `${pickup} → ${dropoff} → ${pickup}`
    : `${pickup} → ${dropoff}`;
};

const formatDuration = (ms: number): string => {
  const total = Math.max(0, Math.floor(ms / 1000));
  const d = Math.floor(total / 86400);
  const h = Math.floor((total % 86400) / 3600);
  const m = Math.floor((total % 3600) / 60);
  const sec = total % 60;
  const parts: string[] = [];
  if (d) parts.push(`${d} দিন`);
  if (h) parts.push(`${h} ঘণ্টা`);
  if (m) parts.push(`${m} মিনিট`);
  if (!d && !h) parts.push(`${sec} সেকেন্ড`);
  return parts.join(' ');
};

// pending: নির্ধারিত শুরুর কাউন্টডাউন, active: কতক্ষণ ধরে চলছে, completed: মোট সময়
const tripTimeInfo = (rental: Rental, now: number): { text: string; className: string } => {
  if (rental.rental_status === 'pending') {
    const diff = new Date(rental.start_date).getTime() - now;
    return diff > 0
      ? { text: `শুরু হতে বাকি ${formatDuration(diff)}`, className: 'text-amber-600' }
      : { text: 'নির্ধারিত সময় পেরিয়ে গেছে', className: 'text-red-600' };
  }
  if (rental.rental_status === 'active') {
    const from = rental.actual_start_time || rental.start_date;
    return { text: `চলছে ${formatDuration(now - new Date(from).getTime())} ধরে`, className: 'text-green-600' };
  }
  if (rental.rental_status === 'completed') {
    if (rental.actual_start_time && rental.actual_end_time) {
      const dur = new Date(rental.actual_end_time).getTime() - new Date(rental.actual_start_time).getTime();
      return { text: `মোট চলেছে ${formatDuration(dur)}`, className: 'text-gray-600' };
    }
    return { text: '—', className: 'text-gray-400' };
  }
  return { text: '—', className: 'text-gray-400' };
};

const EXPENSE_TYPES: Record<string, string> = {
  toll: 'টোল',
  fuel: 'জালানি',
  parking: 'পার্কিং',
  repair: 'মেরামত',
  driver_allowance: 'চালকের ভাতা',
  other: 'অন্যান্য',
};

const STATUS_LABELS: Record<string, string> = {
  pending: 'অপেক্ষমান',
  active: 'চলমান',
  completed: 'সম্পন্ন',
  cancelled: 'বাতিল',
};

const STATUS_COLORS: Record<string, string> = {
  pending: 'bg-yellow-100 text-yellow-800',
  active: 'bg-blue-100 text-blue-800',
  completed: 'bg-green-100 text-green-800',
  cancelled: 'bg-red-100 text-red-800',
};

export default function DriverRentals() {
  const [rentals, setRentals] = useState<Rental[]>([]);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [toast, setToast] = useState('');
  const [now, setNow] = useState(Date.now());

  // লাইভ কাউন্টডাউন/চলমান সময়ের জন্য প্রতি সেকেন্ডে টিক
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo]     = useState('');
  const [addOpen, setAddOpen]   = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedRental, setSelectedRental] = useState<Rental | null>(null);
  const [saving, setSaving] = useState(false);
  const [addingExpense, setAddingExpense] = useState(false);
  const [expenseLocation, setExpenseLocation] = useState('');
  const [expenseLatLng, setExpenseLatLng] = useState<{ lat: number; lng: number } | null>(null);
  const [gettingLocation, setGettingLocation] = useState(false);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(''), 3000);
  };

  const loadRentals = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filterStatus) params.append('status', filterStatus);
      if (search) params.append('search', search);
      if (dateFrom) params.append('date_from', dateFrom);
      if (dateTo) params.append('date_to', dateTo);

      const response = await api.get<Rental[]>(`/driver/rentals/index.php?${params}`);
      if (response.success) {
        setRentals(response.data || []);
      } else {
        showToast('তথ্য লোড ব্যর্থ');
      }
    } catch (error) {
      console.error('Error loading rentals:', error);
      showToast('ত্রুটি: ' + (error instanceof Error ? error.message : 'Unknown'));
    } finally {
      setLoading(false);
    }
  }, [filterStatus, search, dateFrom, dateTo]);

  const loadVehicles = useCallback(async () => {
    try {
      const response = await api.get<Vehicle[]>('/driver/vehicles.php');
      if (response.success) {
        setVehicles(response.data || []);
      }
    } catch (error) {
      console.error('Error loading vehicles:', error);
    }
  }, []);

  useEffect(() => {
    loadRentals();
  }, [loadRentals]);

  useEffect(() => {
    if (addOpen) {
      loadVehicles();
    }
  }, [addOpen, loadVehicles]);

  const handleAddRental = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSaving(true);
    const formData = new FormData(e.currentTarget);
    const data = {
      passenger_name: formData.get('passenger_name'),
      passenger_mobile: formData.get('passenger_mobile'),
      vehicle_id: formData.get('vehicle_id'),
      pickup_location: formData.get('pickup_location'),
      dropoff_location: formData.get('dropoff_location'),
      trip_type: formData.get('trip_type'),
      start_datetime: formData.get('start_datetime'),
      agreed_amount: formData.get('agreed_amount'),
      notes: formData.get('notes'),
    };

    try {
      const response = await api.post('/driver/rentals/index.php', data);
      if (response.success) {
        showToast('ট্রিপ সফলভাবে তৈরি হয়েছে');
        setAddOpen(false);
        (e.target as HTMLFormElement).reset();
        loadRentals();
      } else {
        showToast(response.message || 'তৈরি ব্যর্থ');
      }
    } catch (error) {
      showToast('ত্রুটি: ' + (error instanceof Error ? error.message : 'Unknown'));
    } finally {
      setSaving(false);
    }
  };

  const fetchCurrentLocation = useCallback(() => {
    if (!navigator.geolocation) {
      showToast('এই ব্রাউজারে GPS সাপোর্ট নেই');
      return;
    }
    setGettingLocation(true);
    setExpenseLocation('');
    setExpenseLatLng(null);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const { latitude, longitude } = pos.coords;
        setExpenseLatLng({ lat: latitude, lng: longitude });
        try {
          const res = await fetch(
            `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&accept-language=bn,en`,
            { headers: { 'User-Agent': 'car.zisan.me/1.0' } }
          );
          const geo = await res.json();
          setExpenseLocation(geo.display_name || `${latitude.toFixed(5)}, ${longitude.toFixed(5)}`);
        } catch {
          setExpenseLocation(`${latitude.toFixed(5)}, ${longitude.toFixed(5)}`);
        }
        setGettingLocation(false);
      },
      () => {
        showToast('GPS লোকেশন পাওয়া যায়নি');
        setGettingLocation(false);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  }, []);

  const handleOpenDetail = useCallback(async (rentalId: number) => {
    try {
      const response = await api.get<Rental>(`/driver/rentals/show.php?id=${rentalId}`);
      if (response.success) {
        const rental = response.data || null;
        setSelectedRental(rental);
        setExpenseLocation('');
        setExpenseLatLng(null);
        setDetailOpen(true);
        if (rental?.rental_status === 'active') {
          fetchCurrentLocation();
        }
      }
    } catch (error) {
      showToast('বিবরণ লোড ব্যর্থ');
    }
  }, [fetchCurrentLocation]);

  // ড্যাশবোর্ডের "খরচ যুক্ত করুন" থেকে ?open=<id> এলে সরাসরি ডিটেইল মডাল খুলবে
  const [searchParams, setSearchParams] = useSearchParams();
  useEffect(() => {
    const openId = Number(searchParams.get('open'));
    if (openId) {
      setSearchParams({}, { replace: true });
      handleOpenDetail(openId);
    }
  }, [searchParams, setSearchParams, handleOpenDetail]);

  const getGPSLocation = (): Promise<{ location_name: string; latitude: number; longitude: number }> =>
    new Promise((resolve, reject) => {
      if (!navigator.geolocation) { reject(new Error('GPS সাপোর্ট নেই')); return; }
      navigator.geolocation.getCurrentPosition(
        async (pos) => {
          const { latitude, longitude } = pos.coords;
          try {
            const res = await fetch(
              `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&accept-language=bn,en`,
              { headers: { 'User-Agent': 'car.zisan.me/1.0' } }
            );
            const geo = await res.json();
            resolve({ location_name: geo.display_name || `${latitude.toFixed(5)}, ${longitude.toFixed(5)}`, latitude, longitude });
          } catch {
            resolve({ location_name: `${latitude.toFixed(5)}, ${longitude.toFixed(5)}`, latitude, longitude });
          }
        },
        (err) => reject(err),
        { timeout: 10000, enableHighAccuracy: true }
      );
    });

  const handleUpdateStatus = async (rentalId: number, newStatus: string) => {
    setSaving(true);
    try {
      let locationData: { location_name: string; latitude: number; longitude: number } | null = null;
      try {
        locationData = await getGPSLocation();
      } catch {
        showToast('GPS লোকেশন পাওয়া যায়নি — স্ট্যাটাস পরিবর্তন করা যাবে না');
        setSaving(false);
        return;
      }

      const response = await api.post(`/driver/rentals/update_status.php?id=${rentalId}`, {
        status: newStatus,
        ...locationData,
      });

      if (response.success) {
        showToast('স্ট্যাটাস সফলভাবে আপডেট হয়েছে');
        loadRentals();
        if (selectedRental) {
          const updated = await api.get<Rental>(`/driver/rentals/show.php?id=${rentalId}`);
          if (updated.success) {
            setSelectedRental(updated.data || null);
          }
        }
      } else {
        showToast(response.message || 'আপডেট ব্যর্থ');
      }
    } catch (error) {
      showToast('ত্রুটি: ' + (error instanceof Error ? error.message : 'Unknown'));
    } finally {
      setSaving(false);
    }
  };

  const handleAddExpense = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedRental) return;

    if (!expenseLatLng) {
      showToast('GPS লোকেশন পাওয়া যায়নি — "পুনরায়" চাপুন');
      return;
    }

    setAddingExpense(true);
    const formData = new FormData(e.currentTarget);
    formData.append('location_name', expenseLocation);
    formData.append('latitude', String(expenseLatLng.lat));
    formData.append('longitude', String(expenseLatLng.lng));

    try {
      const response = await fetch(`/api/driver/rentals/expenses.php?rental_id=${selectedRental.id}`, {
        method: 'POST',
        credentials: 'include',
        body: formData,
      });

      const data = (await response.json()) as any;
      if (data.success) {
        showToast('খরচ সফলভাবে যোগ করা হয়েছে');
        (e.target as HTMLFormElement).reset();
        setExpenseLocation('');
        setExpenseLatLng(null);
        fetchCurrentLocation();
        const updated = await api.get<Rental>(`/driver/rentals/show.php?id=${selectedRental.id}`);
        if (updated.success) {
          setSelectedRental(updated.data || null);
        }
      } else {
        showToast(data.message || 'যোগ করা ব্যর্থ');
      }
    } catch (error) {
      showToast('ত্রুটি: ' + (error instanceof Error ? error.message : 'Unknown'));
    } finally {
      setAddingExpense(false);
    }
  };

  const totalExpense = (selectedRental?.expenses || []).reduce((sum: number, exp: TripExpense) => sum + exp.amount, 0);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
        <h1 className="text-2xl font-bold text-gray-900">আমার ট্রিপ</h1>
        <button
          onClick={() => setAddOpen(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-indigo-700 transition"
        >
          + নতুন ট্রিপ
        </button>
      </div>

      {/* Search & Filter */}
      <div className="flex flex-col gap-2">
        <div className="flex flex-col sm:flex-row gap-2">
          <input
            type="text"
            placeholder="যাত্রি বা মোবাইল খুঁজুন..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600"
          />
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600"
          >
            <option value="">সব স্ট্যাটাস</option>
            <option value="pending">অপেক্ষমান</option>
            <option value="active">চলমান</option>
            <option value="completed">সম্পন্ন</option>
            <option value="cancelled">বাতিল</option>
          </select>
        </div>
        <div className="flex flex-col sm:flex-row gap-2 items-center">
          <div className="flex items-center gap-2 flex-1">
            <label className="text-sm text-gray-600 whitespace-nowrap">তারিখ থেকে:</label>
            <input
              type="date"
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600 text-sm"
            />
          </div>
          <div className="flex items-center gap-2 flex-1">
            <label className="text-sm text-gray-600 whitespace-nowrap">তারিখ পর্যন্ত:</label>
            <input
              type="date"
              value={dateTo}
              onChange={(e) => setDateTo(e.target.value)}
              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600 text-sm"
            />
          </div>
          {(dateFrom || dateTo) && (
            <button
              onClick={() => { setDateFrom(''); setDateTo(''); }}
              className="px-3 py-2 text-sm text-red-600 hover:text-red-800 border border-red-200 rounded-lg hover:bg-red-50 whitespace-nowrap"
            >
              ফিল্টার মুছুন
            </button>
          )}
        </div>
      </div>

      {/* Table (Desktop) */}
      {!loading && (
        <div className="hidden md:block overflow-x-auto bg-white rounded-lg shadow">
          <table className="w-full text-sm">
            <thead className="bg-gray-100 border-b">
              <tr>
                <th className="px-4 py-2 text-left">যাত্রি</th>
                <th className="px-4 py-2 text-left">মোবাইল</th>
                <th className="px-4 py-2 text-left">গাড়ি</th>
                <th className="px-4 py-2 text-left">ধরন</th>
                <th className="px-4 py-2 text-left">ঠিকানা</th>
                <th className="px-4 py-2 text-right">চুক্তি টাকা</th>
                <th className="px-4 py-2 text-left">স্ট্যাটাস</th>
                <th className="px-4 py-2 text-left">তারিখ</th>
                <th className="px-4 py-2 text-left">সময়</th>
                <th className="px-4 py-2 text-center">অ্যাকশন</th>
              </tr>
            </thead>
            <tbody>
              {rentals.map((rental) => (
                <tr key={rental.id} className="border-b hover:bg-gray-50">
                  <td className="px-4 py-2 font-medium">
                    {rental.customer_first_name} {rental.customer_last_name}
                  </td>
                  <td className="px-4 py-2">{rental.customer_phone}</td>
                  <td className="px-4 py-2">
                    {rental.vehicle_brand} {rental.vehicle_model}
                  </td>
                  <td className="px-4 py-2">{TRIP_TYPE_LABELS[rental.trip_type]}</td>
                  <td className="px-4 py-2 text-gray-600 max-w-[220px]">{tripRoute(rental)}</td>
                  <td className="px-4 py-2 text-right">৳ {rental.agreed_amount.toFixed(0)}</td>
                  <td className="px-4 py-2">
                    <span className={`inline-block px-2 py-1 rounded-full text-xs font-medium ${STATUS_COLORS[rental.rental_status]}`}>
                      {STATUS_LABELS[rental.rental_status]}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-sm text-gray-600">
                    {new Date(rental.start_date).toLocaleDateString('bn-BD')}
                  </td>
                  <td className={`px-4 py-2 text-xs font-medium whitespace-nowrap ${tripTimeInfo(rental, now).className}`}>
                    {tripTimeInfo(rental, now).text}
                  </td>
                  <td className="px-4 py-2 text-center">
                    <button
                      onClick={() => handleOpenDetail(rental.id)}
                      className="text-indigo-600 hover:text-indigo-700 font-medium"
                    >
                      দেখুন
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {rentals.length === 0 && (
            <div className="text-center py-6 text-gray-500">কোনো ট্রিপ পাওয়া যায়নি</div>
          )}
        </div>
      )}

      {/* Cards (Mobile) */}
      {!loading && (
        <div className="md:hidden space-y-3">
          {rentals.map((rental) => (
            <div key={rental.id} className="bg-white p-4 rounded-lg border border-gray-200">
              <div className="flex items-start justify-between mb-2">
                <div>
                  <h3 className="font-medium">{rental.customer_first_name} {rental.customer_last_name}</h3>
                  <p className="text-sm text-gray-600">{rental.customer_phone}</p>
                </div>
                <span className={`inline-block px-2 py-1 rounded-full text-xs font-medium ${STATUS_COLORS[rental.rental_status]}`}>
                  {STATUS_LABELS[rental.rental_status]}
                </span>
              </div>
              <div className="text-sm text-gray-600 mb-3">
                <p>গাড়ি: {rental.vehicle_brand} {rental.vehicle_model}</p>
                <p>ধরন: {TRIP_TYPE_LABELS[rental.trip_type]}</p>
                <p>ঠিকানা: {tripRoute(rental)}</p>
                <p>চুক্তি: ৳ {rental.agreed_amount.toFixed(0)}</p>
                <p className={`font-medium ${tripTimeInfo(rental, now).className}`}>
                  {tripTimeInfo(rental, now).text}
                </p>
              </div>
              <button
                onClick={() => handleOpenDetail(rental.id)}
                className="w-full bg-indigo-600 text-white py-2 rounded-lg font-medium hover:bg-indigo-700"
              >
                বিস্তারিত
              </button>
            </div>
          ))}
          {rentals.length === 0 && (
            <div className="text-center py-6 text-gray-500">কোনো ট্রিপ পাওয়া যায়নি</div>
          )}
        </div>
      )}

      {/* Loading */}
      {loading && <div className="text-center py-6 text-gray-500">লোড হচ্ছে...</div>}

      {/* Add Modal */}
      {addOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 pt-20 sm:pt-4 overflow-y-auto">
          <div className="bg-white rounded-lg max-w-lg w-full max-h-[calc(100vh-120px)] overflow-y-auto">
            <div className="p-4 sm:p-6 border-b flex items-center justify-between">
              <h2 className="text-xl font-bold">নতুন ট্রিপ</h2>
              <button onClick={() => setAddOpen(false)} className="text-gray-400 hover:text-gray-600">
                ✕
              </button>
            </div>
            <form onSubmit={handleAddRental} className="p-4 sm:p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">যাত্রির নাম *</label>
                <input type="text" name="passenger_name" required className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">মোবাইল নম্বর *</label>
                <input type="tel" name="passenger_mobile" required className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">গাড়ি নির্বাচন *</label>
                {/* গাড়ি async লোড হয় — একটিমাত্র গাড়ি হলে preselect কার্যকর করতে remount */}
                <select
                  key={vehicles.length}
                  name="vehicle_id"
                  required
                  defaultValue={vehicles.length === 1 ? String(vehicles[0].id) : ''}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600"
                >
                  <option value="">বেছে নিন...</option>
                  {vehicles.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.brand} {v.model} ({v.registration_number})
                    </option>
                  ))}
                </select>
                <p className="text-xs text-gray-500 mt-1">
                  শুধুমাত্র আপনাকে অ্যাসাইন করা গাড়ি দেখানো হচ্ছে
                </p>
                {vehicles.length === 0 && (
                  <p className="text-xs text-red-600 mt-1">আপনার কোনো গাড়ি অ্যাসাইন করা নেই — অ্যাডমিনের সাথে যোগাযোগ করুন</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">শুরু ঠিকানা *</label>
                <input type="text" name="pickup_location" required className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">শেষ ঠিকানা *</label>
                <input type="text" name="dropoff_location" required className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">ট্রিপের ধরন *</label>
                <div className="flex gap-4">
                  <label className="flex items-center gap-2">
                    <input type="radio" name="trip_type" value="one_way" defaultChecked required />
                    এক-দিকে
                  </label>
                  <label className="flex items-center gap-2">
                    <input type="radio" name="trip_type" value="round_trip" required />
                    রাউন্ড ট্রিপ
                  </label>
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">শুরুর তারিখ ও সময় *</label>
                <input type="datetime-local" name="start_datetime" required className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">চুক্তির পরিমান (টাকা) *</label>
                <input type="number" name="agreed_amount" min="0" step="0.01" required className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">নোট</label>
                <textarea name="notes" rows={2} className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600" />
              </div>
              <div className="flex gap-2 justify-end">
                <button
                  type="button"
                  onClick={() => setAddOpen(false)}
                  className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
                >
                  বাতিল
                </button>
                <button
                  type="submit"
                  disabled={saving || vehicles.length === 0}
                  className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:bg-gray-400"
                >
                  {saving ? 'সংরক্ষণ হচ্ছে...' : 'সংরক্ষণ করুন'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {detailOpen && selectedRental && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 pt-20 sm:pt-4 overflow-y-auto">
          <div className="bg-white rounded-lg max-w-2xl w-full max-h-[calc(100vh-120px)] overflow-y-auto">
            <div className="p-4 sm:p-6 border-b flex items-center justify-between">
              <h2 className="text-xl font-bold">ট্রিপ বিস্তারিত</h2>
              <button onClick={() => { setDetailOpen(false); setExpenseLocation(''); setExpenseLatLng(null); }} className="text-gray-400 hover:text-gray-600">
                ✕
              </button>
            </div>

            <div className="p-4 sm:p-6 space-y-6">
              {/* Rental Info */}
              <div>
                <h3 className="font-bold mb-3">ট্রিপ তথ্য</h3>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <p className="text-gray-600">যাত্রি</p>
                    <p className="font-medium">{selectedRental.customer_first_name} {selectedRental.customer_last_name}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">মোবাইল</p>
                    <p className="font-medium">{selectedRental.customer_phone}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">গাড়ি</p>
                    <p className="font-medium">
                      {selectedRental.vehicle_brand} {selectedRental.vehicle_model}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-600">শুরু ঠিকানা</p>
                    <p className="font-medium">{selectedRental.pickup_location}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">শেষ ঠিকানা</p>
                    <p className="font-medium">{selectedRental.dropoff_location}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">ধরন</p>
                    <p className="font-medium">{TRIP_TYPE_LABELS[selectedRental.trip_type]}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">চুক্তি টাকা</p>
                    <p className="font-medium">৳ {selectedRental.agreed_amount.toFixed(0)}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">স্ট্যাটাস</p>
                    <p className={`font-medium inline-block px-2 py-1 rounded-full text-xs ${STATUS_COLORS[selectedRental.rental_status]}`}>
                      {STATUS_LABELS[selectedRental.rental_status]}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-600">নির্ধারিত শুরু (আনুমানিক)</p>
                    <p className="font-medium">{new Date(selectedRental.start_date).toLocaleString('bn-BD')}</p>
                  </div>
                  <div>
                    <p className="text-gray-600">প্রকৃত শুরু</p>
                    <p className="font-medium">
                      {selectedRental.actual_start_time
                        ? new Date(selectedRental.actual_start_time).toLocaleString('bn-BD')
                        : '—'}
                    </p>
                    {selectedRental.start_location_name && (
                      <p className="text-xs text-gray-500 mt-0.5">
                        {selectedRental.start_latitude && selectedRental.start_longitude ? (
                          <a href={`https://www.google.com/maps?q=${selectedRental.start_latitude},${selectedRental.start_longitude}`} target="_blank" rel="noopener noreferrer" className="text-indigo-600 hover:text-indigo-700">
                            {selectedRental.start_location_name}
                          </a>
                        ) : selectedRental.start_location_name}
                      </p>
                    )}
                  </div>
                  <div>
                    <p className="text-gray-600">প্রকৃত শেষ</p>
                    <p className="font-medium">
                      {selectedRental.actual_end_time
                        ? new Date(selectedRental.actual_end_time).toLocaleString('bn-BD')
                        : '—'}
                    </p>
                    {selectedRental.end_location_name && (
                      <p className="text-xs text-gray-500 mt-0.5">
                        {selectedRental.end_latitude && selectedRental.end_longitude ? (
                          <a href={`https://www.google.com/maps?q=${selectedRental.end_latitude},${selectedRental.end_longitude}`} target="_blank" rel="noopener noreferrer" className="text-indigo-600 hover:text-indigo-700">
                            {selectedRental.end_location_name}
                          </a>
                        ) : selectedRental.end_location_name}
                      </p>
                    )}
                  </div>
                  <div>
                    <p className="text-gray-600">সময়</p>
                    <p className={`font-medium ${tripTimeInfo(selectedRental, now).className}`}>
                      {tripTimeInfo(selectedRental, now).text}
                    </p>
                  </div>
                </div>

                {/* Status Actions — ড্রাইভার ট্রিপ শুরু ও সম্পন্ন করতে পারে, বাতিল নয় */}
                <div className="mt-4 flex flex-wrap gap-2">
                  {selectedRental.rental_status === 'pending' && (
                    <button
                      onClick={() => handleUpdateStatus(selectedRental.id, 'active')}
                      disabled={saving}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 text-sm"
                    >
                      ট্রিপ শুরু করুন
                    </button>
                  )}
                  {selectedRental.rental_status === 'active' && (
                    <button
                      onClick={() => handleUpdateStatus(selectedRental.id, 'completed')}
                      disabled={saving}
                      className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 text-sm"
                    >
                      ট্রিপ সম্পন্ন করুন
                    </button>
                  )}
                  {selectedRental.rental_status === 'completed' && (
                    <a
                      href="/driver"
                      className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 text-sm flex items-center gap-1"
                    >
                      লেজারে হিসাব দেখুন →
                    </a>
                  )}
                </div>
              </div>

              {/* Expenses Section */}
              <div className="border-t pt-6">
                <h3 className="font-bold mb-3">খরচ তালিকা</h3>

                {(selectedRental.expenses || []).length > 0 ? (
                  <div className="space-y-2 mb-4">
                    {selectedRental.expenses!.map((exp: TripExpense) => (
                      <div key={exp.id} className="flex items-start justify-between p-3 bg-gray-50 rounded-lg text-sm">
                        <div className="flex-1 min-w-0 pr-3">
                          <p className="font-medium">{EXPENSE_TYPES[exp.expense_type]}</p>
                          <p className="text-gray-500 text-xs">{new Date(exp.created_at).toLocaleString('bn-BD', { timeZone: 'Asia/Dhaka', year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</p>
                          {exp.description && <p className="text-gray-600 mt-0.5">{exp.description}</p>}
                          {exp.location_name && (
                            <p className="text-gray-500 text-xs mt-0.5 truncate">
                              {exp.latitude && exp.longitude ? (
                                <a href={`https://www.google.com/maps?q=${exp.latitude},${exp.longitude}`} target="_blank" rel="noopener noreferrer" className="text-indigo-600 hover:text-indigo-700">
                                  {exp.location_name}
                                </a>
                              ) : exp.location_name}
                            </p>
                          )}
                          {exp.receipt_image && (
                            <a href={`/public/${exp.receipt_image}`} target="_blank" rel="noopener noreferrer" className="text-indigo-600 hover:text-indigo-700 text-xs">
                              রসিদ দেখুন
                            </a>
                          )}
                        </div>
                        <div className="text-right shrink-0">
                          <p className="font-medium">৳ {exp.amount.toFixed(0)}</p>
                        </div>
                      </div>
                    ))}
                    <div className="p-3 bg-gray-100 rounded-lg font-bold flex justify-between">
                      <span>মোট খরচ:</span>
                      <span>৳ {totalExpense.toFixed(0)}</span>
                    </div>
                  </div>
                ) : (
                  <p className="text-gray-500 mb-4">কোনো খরচ যোগ করা হয়নি</p>
                )}

                {/* Add Expense Form */}
                {selectedRental.rental_status === 'active' && (
                  <form onSubmit={handleAddExpense} className="bg-gray-50 p-4 rounded-lg space-y-3">
                    <h4 className="font-medium">খরচ যোগ করুন</h4>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <select name="expense_type" required className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600 text-sm">
                        <option value="">ধরন নির্বাচন</option>
                        {Object.entries(EXPENSE_TYPES).map(([key, label]) => (
                          <option key={key} value={key}>
                            {label}
                          </option>
                        ))}
                      </select>
                      <input
                        type="number"
                        name="amount"
                        placeholder="পরিমান"
                        min="0"
                        step="0.01"
                        required
                        className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600 text-sm"
                      />
                    </div>
                    <input
                      type="text"
                      name="description"
                      placeholder="বিবরণ (ঐচ্ছিক)"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600 text-sm"
                    />
                    <div className="space-y-1">
                      <div className="flex gap-2 items-center">
                        <input
                          type="text"
                          value={gettingLocation ? 'GPS লোকেশন নেওয়া হচ্ছে...' : (expenseLocation || 'লোকেশন পাওয়া যায়নি')}
                          readOnly
                          className="flex-1 px-3 py-2 border border-gray-200 rounded-lg bg-gray-100 text-sm text-gray-700 cursor-default"
                        />
                        <button
                          type="button"
                          onClick={fetchCurrentLocation}
                          disabled={gettingLocation}
                          className="px-3 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 text-sm whitespace-nowrap"
                          title="আবার চেষ্টা করুন"
                        >
                          {gettingLocation ? '...' : 'পুনরায়'}
                        </button>
                      </div>
                      {expenseLatLng && (
                        <p className="text-xs text-green-700">GPS: {expenseLatLng.lat.toFixed(5)}, {expenseLatLng.lng.toFixed(5)}</p>
                      )}
                      {!gettingLocation && !expenseLatLng && (
                        <p className="text-xs text-red-600">লোকেশন ছাড়া খরচ যোগ হবে না — "পুনরায়" চাপুন</p>
                      )}
                    </div>
                    <input
                      type="file"
                      name="receipt_image"
                      accept="image/jpeg,image/png,image/gif"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600 text-sm"
                    />
                    <div className="flex gap-2 justify-end">
                      <button
                        type="submit"
                        disabled={addingExpense}
                        className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:bg-gray-400 text-sm font-medium"
                      >
                        {addingExpense ? 'যোগ হচ্ছে...' : 'খরচ যোগ করুন'}
                      </button>
                    </div>
                  </form>
                )}
              </div>
            </div>

            <div className="p-4 sm:p-6 border-t flex justify-end">
              <button
                onClick={() => { setDetailOpen(false); setExpenseLocation(''); setExpenseLatLng(null); }}
                className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300"
              >
                বন্ধ করুন
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-4 right-4 bg-gray-900 text-white px-4 py-3 rounded-lg shadow-lg z-50 animate-in slide-in-from-bottom-4">
          {toast}
        </div>
      )}
    </div>
  );
}
