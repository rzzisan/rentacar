import React, { useState, useCallback, useEffect } from 'react';
import { api } from '../../api/client';
import type { Rental, Vehicle, Driver, TripExpense } from '../../types';

const TRIP_TYPE_LABELS: Record<string, string> = {
  one_way: 'এক-দিকে',
  round_trip: 'রাউন্ড ট্রিপ',
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

export default function AdminRentals() {
  const [rentals, setRentals] = useState<Rental[]>([]);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [toast, setToast] = useState('');
  const [addOpen, setAddOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedRental, setSelectedRental] = useState<Rental | null>(null);
  const [saving, setSaving] = useState(false);
  const [addingExpense, setAddingExpense] = useState(false);

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

      const response = await api.get<Rental[]>(`/admin/rentals/index.php?${params}`);
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
  }, [filterStatus, search]);

  const loadVehicles = useCallback(async () => {
    try {
      const response = await api.get<Vehicle[]>('/vehicles/index.php?status=available');
      if (response.success) {
        setVehicles(response.data || []);
      }
    } catch (error) {
      console.error('Error loading vehicles:', error);
    }
  }, []);

  const loadDrivers = useCallback(async () => {
    try {
      const response = await api.get<Driver[]>('/admin/drivers/index.php?status=active');
      if (response.success) {
        setDrivers((response.data || []) as unknown as Driver[]);
      }
    } catch (error) {
      console.error('Error loading drivers:', error);
    }
  }, []);

  useEffect(() => {
    loadRentals();
  }, [loadRentals]);

  useEffect(() => {
    if (addOpen) {
      loadVehicles();
      loadDrivers();
    }
  }, [addOpen, loadVehicles, loadDrivers]);

  const handleAddRental = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSaving(true);
    const formData = new FormData(e.currentTarget);
    const data = {
      passenger_name: formData.get('passenger_name'),
      passenger_mobile: formData.get('passenger_mobile'),
      vehicle_id: formData.get('vehicle_id'),
      driver_id: formData.get('driver_id') || null,
      pickup_location: formData.get('pickup_location'),
      dropoff_location: formData.get('dropoff_location'),
      trip_type: formData.get('trip_type'),
      start_datetime: formData.get('start_datetime'),
      agreed_amount: formData.get('agreed_amount'),
      notes: formData.get('notes'),
    };

    try {
      const response = await api.post('/admin/rentals/index.php', data);
      if (response.success) {
        showToast('রেন্টাল সফলভাবে তৈরি হয়েছে');
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

  const handleOpenDetail = async (rental: Rental) => {
    try {
      const response = await api.get<Rental>(`/admin/rentals/show.php?id=${rental.id}`);
      if (response.success) {
        setSelectedRental(response.data || null);
        setDetailOpen(true);
      }
    } catch (error) {
      showToast('বিবরণ লোড ব্যর্থ');
    }
  };

  const handleUpdateStatus = async (rentalId: number, newStatus: string) => {
    setSaving(true);
    try {
      const response = await api.post(`/admin/rentals/update_status.php?id=${rentalId}`, {
        status: newStatus,
      });

      if (response.success) {
        showToast('স্ট্যাটাস সফলভাবে আপডেট হয়েছে');
        loadRentals();
        if (selectedRental) {
          const updated = await api.get<Rental>(`/admin/rentals/show.php?id=${rentalId}`);
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

    setAddingExpense(true);
    const formData = new FormData(e.currentTarget);

    try {
      const response = await fetch('/api/admin/rentals/expenses.php', {
        method: 'POST',
        credentials: 'include',
        body: new URLSearchParams({
          rental_id: selectedRental.id.toString(),
          expense_type: formData.get('expense_type') as string,
          amount: formData.get('amount') as string,
          description: formData.get('description') as string,
        }),
      });

      const data = (await response.json()) as any;
      if (data.success) {
        showToast('খরচ সফলভাবে যোগ করা হয়েছে');
        (e.target as HTMLFormElement).reset();
        const updated = await api.get<Rental>(`/admin/rentals/show.php?id=${selectedRental.id}`);
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

  const handleDeleteExpense = async (expenseId: number) => {
    if (!selectedRental) return;
    if (!confirm('খরচ ডিলিট করতে নিশ্চিত?')) return;

    try {
      const response = await fetch(`/api/admin/rentals/expenses_destroy.php?id=${expenseId}`, {
        method: 'DELETE',
        credentials: 'include',
      });

      const data = (await response.json()) as any;
      if (data.success) {
        showToast('খরচ ডিলিট করা হয়েছে');
        const updated = await api.get<Rental>(`/admin/rentals/show.php?id=${selectedRental.id}`);
        if (updated.success) {
          setSelectedRental(updated.data || null);
        }
      } else {
        showToast(data.message || 'ডিলিট ব্যর্থ');
      }
    } catch (error) {
      showToast('ত্রুটি: ' + (error instanceof Error ? error.message : 'Unknown'));
    }
  };

  const totalExpense = (selectedRental?.expenses || []).reduce((sum: number, exp: TripExpense) => sum + exp.amount, 0);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
        <h1 className="text-2xl font-bold text-gray-900">রেন্টাল ব্যবস্থাপনা</h1>
        <button
          onClick={() => setAddOpen(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-indigo-700 transition"
        >
          + নতুন রেন্টাল
        </button>
      </div>

      {/* Search & Filter */}
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

      {/* Table (Desktop) */}
      {!loading && (
        <div className="hidden md:block overflow-x-auto bg-white rounded-lg shadow">
          <table className="w-full text-sm">
            <thead className="bg-gray-100 border-b">
              <tr>
                <th className="px-4 py-2 text-left">যাত্রি</th>
                <th className="px-4 py-2 text-left">মোবাইল</th>
                <th className="px-4 py-2 text-left">গাড়ি</th>
                <th className="px-4 py-2 text-left">চালক</th>
                <th className="px-4 py-2 text-left">ধরন</th>
                <th className="px-4 py-2 text-right">চুক্তি টাকা</th>
                <th className="px-4 py-2 text-left">স্ট্যাটাস</th>
                <th className="px-4 py-2 text-left">তারিখ</th>
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
                  <td className="px-4 py-2">{rental.driver_name || '-'}</td>
                  <td className="px-4 py-2">{TRIP_TYPE_LABELS[rental.trip_type]}</td>
                  <td className="px-4 py-2 text-right">৳ {rental.agreed_amount.toFixed(0)}</td>
                  <td className="px-4 py-2">
                    <span className={`inline-block px-2 py-1 rounded-full text-xs font-medium ${STATUS_COLORS[rental.rental_status]}`}>
                      {STATUS_LABELS[rental.rental_status]}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-sm text-gray-600">
                    {new Date(rental.start_date).toLocaleDateString('bn-BD')}
                  </td>
                  <td className="px-4 py-2 text-center">
                    <button
                      onClick={() => handleOpenDetail(rental)}
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
            <div className="text-center py-6 text-gray-500">কোনো রেন্টাল পাওয়া যায়নি</div>
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
                <p>চালক: {rental.driver_name || '-'}</p>
                <p>ধরন: {TRIP_TYPE_LABELS[rental.trip_type]}</p>
                <p>চুক্তি: ৳ {rental.agreed_amount.toFixed(0)}</p>
              </div>
              <button
                onClick={() => handleOpenDetail(rental)}
                className="w-full bg-indigo-600 text-white py-2 rounded-lg font-medium hover:bg-indigo-700"
              >
                বিস্তারিত
              </button>
            </div>
          ))}
          {rentals.length === 0 && (
            <div className="text-center py-6 text-gray-500">কোনো রেন্টাল পাওয়া যায়নি</div>
          )}
        </div>
      )}

      {/* Loading */}
      {loading && <div className="text-center py-6 text-gray-500">লোড হচ্ছে...</div>}

      {/* Add Modal */}
      {addOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg max-w-lg w-full max-h-[90vh] overflow-y-auto">
            <div className="p-4 sm:p-6 border-b flex items-center justify-between">
              <h2 className="text-xl font-bold">নতুন রেন্টাল</h2>
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
                <select name="vehicle_id" required className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600">
                  <option value="">বেছে নিন...</option>
                  {vehicles.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.brand} {v.model} ({v.registration_number})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">চালক নির্বাচন</label>
                <select name="driver_id" className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-600">
                  <option value="">নির্বাচন করুন (ঐচ্ছিক)</option>
                  {drivers.map((d: Driver) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
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
                  disabled={saving}
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
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-4 sm:p-6 border-b flex items-center justify-between">
              <h2 className="text-xl font-bold">রেন্টাল বিস্তারিত</h2>
              <button onClick={() => setDetailOpen(false)} className="text-gray-400 hover:text-gray-600">
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
                    <p className="text-gray-600">চালক</p>
                    <p className="font-medium">{selectedRental.driver_name || '-'}</p>
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
                    <p className="text-gray-600">শুরু তারিখ</p>
                    <p className="font-medium">{new Date(selectedRental.start_date).toLocaleString('bn-BD')}</p>
                  </div>
                </div>

                {/* Status Actions */}
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
                    <>
                      <button
                        onClick={() => handleUpdateStatus(selectedRental.id, 'completed')}
                        disabled={saving}
                        className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 text-sm"
                      >
                        ট্রিপ সম্পন্ন করুন
                      </button>
                      <button
                        onClick={() => handleUpdateStatus(selectedRental.id, 'cancelled')}
                        disabled={saving}
                        className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400 text-sm"
                      >
                        বাতিল করুন
                      </button>
                    </>
                  )}
                </div>
              </div>

              {/* Expenses Section */}
              <div className="border-t pt-6">
                <h3 className="font-bold mb-3">খরচ তালিকা</h3>

                {(selectedRental.expenses || []).length > 0 ? (
                  <div className="space-y-2 mb-4">
                    {selectedRental.expenses!.map((exp: TripExpense) => (
                      <div key={exp.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg text-sm">
                        <div className="flex-1">
                          <p className="font-medium">{EXPENSE_TYPES[exp.expense_type]}</p>
                          <p className="text-gray-600">{exp.description || '-'}</p>
                          {exp.receipt_image && (
                            <a href={`/${exp.receipt_image}`} target="_blank" rel="noopener noreferrer" className="text-indigo-600 hover:text-indigo-700">
                              ছবি দেখুন
                            </a>
                          )}
                        </div>
                        <div className="text-right">
                          <p className="font-medium">৳ {exp.amount.toFixed(0)}</p>
                          <button
                            onClick={() => handleDeleteExpense(exp.id)}
                            className="text-red-600 hover:text-red-700 text-xs mt-1"
                          >
                            ডিলিট
                          </button>
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
                onClick={() => setDetailOpen(false)}
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
