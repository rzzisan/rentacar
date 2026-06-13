import React, { useState, useEffect } from 'react';
import { api } from '../../api/client';
import type { Settlement, TripExpense, SettlementPayment } from '../../types';

// এক-দিকে: শুরু→শেষ, রাউন্ড ট্রিপ: শুরু→শেষ→শুরু
const tripRoute = (s: { trip_type?: string; pickup_location?: string; dropoff_location?: string }) => {
  if (!s.pickup_location && !s.dropoff_location) return '—';
  const pickup = s.pickup_location || '?';
  const dropoff = s.dropoff_location || '?';
  return s.trip_type === 'round_trip'
    ? `${pickup} → ${dropoff} → ${pickup}`
    : `${pickup} → ${dropoff}`;
};

const AdminSettlements: React.FC = () => {
  const [settlements, setSettlements] = useState<Settlement[]>([]);
  const [completedRentals, setCompletedRentals] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedSettlement, setSelectedSettlement] = useState<Settlement | null>(null);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [toast, setToast] = useState<{ type: string; message: string } | null>(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingAgreedAmount, setEditingAgreedAmount] = useState<string>('');
  const [collectionForm, setCollectionForm] = useState({
    amount: '',
    payment_method: '',
    payment_notes: '',
  });
  const [paymentHistory, setPaymentHistory] = useState<SettlementPayment[]>([]);
  const [showPaymentHistory, setShowPaymentHistory] = useState(false);
  const [detailError, setDetailError] = useState<string>('');

  // Load settlements
  const loadSettlements = async () => {
    setLoading(true);
    try {
      let url = '/manager/settlements/index.php';
      const params = new URLSearchParams();
      if (filterStatus) params.append('status', filterStatus);
      if (searchQuery) params.append('search', searchQuery);
      if (params.toString()) url += '?' + params.toString();

      console.log('📡 Loading settlements from:', url, 'Filter:', filterStatus, 'Search:', searchQuery);
      const response = await api.get<Settlement[]>(url);
      console.log('📊 Received settlements count:', response.data?.length);
      if (response.data && response.data.length > 0) {
        console.log('First settlement:', response.data[0].id, 'agreed_amount:', response.data[0].agreed_amount);
      }
      if (response.success && response.data) {
        setSettlements(response.data);
      }
    } catch (error) {
      console.error('Load error:', error);
      showToast('error', 'সেটেলমেন্ট লোড করতে ব্যর্থ');
    } finally {
      setLoading(false);
    }
  };

  // Load completed rentals (for creating new settlements)
  const loadCompletedRentals = async () => {
    try {
      const response = await api.get<any[]>('/manager/rentals/index.php?status=completed');
      if (response.success && response.data) {
        // Filter out rentals that already have settlements
        const filtered = response.data.filter(
          (r) =>
            !settlements.some((s) => s.rental_id === r.id)
        );
        setCompletedRentals(filtered);
      }
    } catch (error) {
      console.error('Failed to load completed rentals');
    }
  };

  useEffect(() => {
    loadSettlements();
  }, [filterStatus, searchQuery]);

  useEffect(() => {
    loadCompletedRentals();
  }, [settlements]);

  useEffect(() => {
    if (selectedSettlement && showDetailModal) {
      loadPaymentHistory();
    }
  }, [selectedSettlement, showDetailModal]);

  const showToast = (type: string, message: string) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 3000);
  };

  const handleShowDetail = async (settlement: Settlement) => {
    try {
      setShowDetailModal(true);
      setDetailError('');
      const response = await api.get<Settlement>(`/manager/settlements/show.php?id=${settlement.id}`);
      if (response.success && response.data) {
        setSelectedSettlement(response.data);
        setEditingAgreedAmount(response.data.agreed_amount.toString());
      } else {
        setDetailError(response.message || 'ডেটা লোড হয়নি');
      }
    } catch (error: any) {
      const errorMsg = error.message || 'বিস্তারিত লোড করতে ব্যর্থ';
      setDetailError(errorMsg);
      console.error('Detail loading error:', error);
    }
  };

  const handleCreateSettlement = async (rental: any) => {
    try {
      console.log('Creating settlement for rental:', rental.id);
      const response = await api.post('/manager/settlements/index.php', {
        rental_id: rental.id,
      });
      console.log('Settlement response:', response);
      if (response.success) {
        showToast('success', 'সেটেলমেন্ট সফলভাবে তৈরি হয়েছে');
        setShowCreateModal(false);
        await loadSettlements();
      } else {
        showToast('error', response.message || 'সেটেলমেন্ট তৈরি করতে ব্যর্থ');
      }
    } catch (error: any) {
      console.error('Settlement creation error:', error);
      showToast('error', error.message || 'সেটেলমেন্ট তৈরি করতে ব্যর্থ');
    }
  };

  const handleUpdateAgreedAmount = async (amount?: number) => {
    if (!selectedSettlement) return;
    try {
      const updateAmount = amount !== undefined ? amount : parseFloat(editingAgreedAmount);
      console.log('🔄 Updating agreed_amount to:', updateAmount);

      // Step 1: Update settlement
      const updateResponse = await api.post(`/manager/settlements/update.php?id=${selectedSettlement.id}`, {
        agreed_amount: updateAmount,
      });
      console.log('✓ Update response:', updateResponse);

      // Step 2: Immediately reload detail (to get fresh data with recalculations)
      console.log('📥 Reloading settlement detail...');
      const detailResponse = await api.get<Settlement>(`/manager/settlements/show.php?id=${selectedSettlement.id}`);
      console.log('📊 Detail response - agreed:', detailResponse.data?.agreed_amount);

      if (detailResponse.success && detailResponse.data) {
        console.log('✓ Updating modal with new data: agreed=' + detailResponse.data.agreed_amount);
        setSelectedSettlement(detailResponse.data);
        setEditingAgreedAmount(detailResponse.data.agreed_amount.toString());
      }

      // Step 3: Reload settlements list
      console.log('📋 Reloading settlements list...');
      let listUrl = '/manager/settlements/index.php';
      const params = new URLSearchParams();
      if (filterStatus) params.append('status', filterStatus);
      if (searchQuery) params.append('search', searchQuery);
      if (params.toString()) listUrl += '?' + params.toString();

      const listResponse = await api.get<Settlement[]>(listUrl);
      console.log('✓ List reloaded with', listResponse.data?.length, 'items');
      if (listResponse.success && listResponse.data) {
        setSettlements(listResponse.data);
      }

      showToast('success', 'চুক্তির পরিমান আপডেট হয়েছে');
    } catch (error) {
      console.error('❌ Error:', error);
      showToast('error', 'আপডেট ব্যর্থ');
    }
  };

  const handleCollectPayment = async () => {
    if (!selectedSettlement) return;

    const amount = parseFloat(collectionForm.amount);
    if (!amount || amount <= 0) {
      showToast('error', 'পেমেন্ট পরিমান ০ থেকে বেশি হতে হবে');
      return;
    }

    try {
      console.log('💰 Collecting payment:', amount);
      const response = await api.post(
        `/manager/settlements/collect-payment.php?id=${selectedSettlement.id}`,
        {
          amount: amount,
          payment_method: collectionForm.payment_method || null,
          payment_notes: collectionForm.payment_notes || null,
        }
      );
      console.log('✓ Payment collected:', response.data);

      if (response.success) {
        const paymentData = response.data as any;
        showToast('success', `✓ ৳${amount.toFixed(2)} সফলভাবে সংগ্রহ করা হয়েছেছে${paymentData?.auto_paid ? ' (সম্পূর্ণ পরিশোধিত!)' : ''}`);

        // Reset form
        setCollectionForm({
          amount: '',
          payment_method: '',
          payment_notes: '',
        });

        // Reload detail
        const detailResponse = await api.get<Settlement>(`/manager/settlements/show.php?id=${selectedSettlement.id}`);
        if (detailResponse.success && detailResponse.data) {
          setSelectedSettlement(detailResponse.data);
        }

        // Reload list
        await loadSettlements();

        // Reload payment history
        await loadPaymentHistory();
      }
    } catch (error: any) {
      console.error('Payment collection error:', error);
      showToast('error', error.message || 'পেমেন্ট সংগ্রহ ব্যর্থ');
    }
  };

  const loadPaymentHistory = async () => {
    if (!selectedSettlement) return;
    try {
      const response = await api.get<{
        total_collected: number;
        payment_count: number;
        payments: SettlementPayment[];
      }>(`/manager/settlements/payment-history.php?id=${selectedSettlement.id}`);

      if (response.success && response.data) {
        setPaymentHistory(response.data.payments);
      }
    } catch (error) {
      console.error('Failed to load payment history:', error);
    }
  };

  // Status badge styling
  const getStatusBadge = (status: string) => {
    const styles: Record<string, string> = {
      pending: 'bg-yellow-100 text-yellow-800',
      paid: 'bg-green-100 text-green-800',
      partial: 'bg-blue-100 text-blue-800',
      refunded: 'bg-red-100 text-red-800',
    };
    const labels: Record<string, string> = {
      pending: 'অপেক্ষমান',
      paid: 'পরিশোধিত',
      partial: 'আংশিক',
      refunded: 'ফেরত',
    };
    return (
      <span className={`px-3 py-1 rounded-full text-sm font-medium ${styles[status] || ''}`}>
        {labels[status] || status}
      </span>
    );
  };

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <h1 className="text-2xl font-bold">পেমেন্ট সেটেলমেন্ট</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="w-full sm:w-auto bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg transition"
        >
          নতুন সেটেলমেন্ট
        </button>
      </div>

      {/* Filters */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
        <div>
          <label className="block text-sm font-medium mb-2">স্ট্যাটাস</label>
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="w-full border rounded-lg px-3 py-2"
          >
            <option value="">সব</option>
            <option value="pending">অপেক্ষমান</option>
            <option value="partial">আংশিক</option>
            <option value="paid">পরিশোধিত</option>
            <option value="refunded">ফেরত</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium mb-2">অনুসন্ধান</label>
          <input
            type="text"
            placeholder="যাত্রি বা মোবাইল..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full border rounded-lg px-3 py-2"
          />
        </div>
      </div>

      {/* Toast */}
      {toast && (
        <div
          className={`mb-4 p-4 rounded-lg ${
            toast.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
          }`}
        >
          {toast.message}
        </div>
      )}

      {/* Loading */}
      {loading && <div className="text-center py-8">লোড হচ্ছে...</div>}

      {/* Desktop Table */}
      {!loading && (
        <div className="hidden md:block overflow-x-auto">
          <table className="w-full border-collapse text-sm">
            <thead className="bg-gray-100">
              <tr>
                <th className="border p-3 text-left">যাত্রি</th>
                <th className="border p-3 text-left">চালক ও ঠিকানা</th>
                <th className="border p-3 text-right">চুক্তির টাকা</th>
                <th className="border p-3 text-right">খরচ</th>
                <th className="border p-3 text-right">নেট</th>
                <th className="border p-3 text-right">চালকের কমিশন</th>
                <th className="border p-3 text-right">বকেয়া</th>
                <th className="border p-3 text-center">স্ট্যাটাস</th>
                <th className="border p-3">অ্যাকশন</th>
              </tr>
            </thead>
            <tbody>
              {settlements.map((s) => (
                <tr key={s.id} className="border hover:bg-gray-50">
                  <td className="border p-3">
                    <div className="font-medium">
                      {s.customer_first_name} {s.customer_last_name}
                    </div>
                    <div className="text-xs text-gray-500">{s.customer_phone}</div>
                    <div className="text-xs text-gray-500">
                      {s.vehicle_brand} {s.vehicle_model}
                    </div>
                  </td>
                  <td className="border p-3 max-w-[220px]">
                    <div className="font-medium">{s.driver_name || '—'}</div>
                    <div className="text-xs text-gray-500">{tripRoute(s)}</div>
                  </td>
                  <td className="border p-3 text-right">৳{s.agreed_amount.toFixed(2)}</td>
                  <td className="border p-3 text-right">৳{s.total_expenses.toFixed(2)}</td>
                  <td className="border p-3 text-right font-medium">৳{s.net_amount.toFixed(2)}</td>
                  <td className="border p-3 text-right">৳{s.driver_commission.toFixed(2)}</td>
                  <td className="border p-3 text-right text-indigo-600 font-medium">
                    ৳{s.amount_to_collect.toFixed(2)}
                  </td>
                  <td className="border p-3 text-center">{getStatusBadge(s.payment_status)}</td>
                  <td className="border p-3">
                    <button
                      onClick={() => handleShowDetail(s)}
                      className="text-indigo-600 hover:underline text-sm"
                    >
                      বিস্তারিত
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Mobile Cards */}
      <div className="md:hidden space-y-4">
        {settlements.map((s) => (
          <div key={s.id} className="border rounded-lg p-4 hover:shadow-lg transition">
            <div className="flex justify-between items-start mb-3">
              <div>
                <div className="font-medium">
                  {s.customer_first_name} {s.customer_last_name}
                </div>
                <div className="text-xs text-gray-500">{s.customer_phone}</div>
              </div>
              <div>{getStatusBadge(s.payment_status)}</div>
            </div>
            <div className="text-xs text-gray-600 mb-2">
              <div>চালক: {s.driver_name || '—'}</div>
              <div>ঠিকানা: {tripRoute(s)}</div>
            </div>
            <div className="grid grid-cols-2 gap-2 text-sm mb-3 py-2 border-y">
              <div>
                <div className="text-gray-500">চুক্তির টাকা</div>
                <div className="font-medium">৳{s.agreed_amount.toFixed(2)}</div>
              </div>
              <div>
                <div className="text-gray-500">খরচ</div>
                <div className="font-medium">৳{s.total_expenses.toFixed(2)}</div>
              </div>
              <div>
                <div className="text-gray-500">চালক কমিশন</div>
                <div className="font-medium">৳{s.driver_commission.toFixed(2)}</div>
              </div>
              <div>
                <div className="text-gray-500">বকেয়া</div>
                <div className="font-medium text-indigo-600">৳{s.amount_to_collect.toFixed(2)}</div>
              </div>
            </div>
            <button
              onClick={() => handleShowDetail(s)}
              className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-2 rounded transition"
            >
              বিস্তারিত
            </button>
          </div>
        ))}
      </div>

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg shadow-lg max-w-md w-full">
            <div className="flex justify-between items-center p-6 border-b">
              <h2 className="text-xl font-bold">নতুন সেটেলমেন্ট</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>
            <div className="p-6 max-h-96 overflow-y-auto">
              {completedRentals.length === 0 ? (
                <div className="text-center text-gray-500">
                  সম্পন্ন করা হয়নি এমন কোনো রেন্টাল নেই
                </div>
              ) : (
                <div className="space-y-3">
                  {completedRentals.map((rental) => (
                    <div
                      key={rental.id}
                      className="border rounded-lg p-3 hover:bg-gray-50 cursor-pointer"
                      onClick={() => handleCreateSettlement(rental)}
                    >
                      <div className="font-medium">
                        {rental.customer_first_name} {rental.customer_last_name}
                      </div>
                      <div className="text-sm text-gray-500">
                        {rental.vehicle_brand} {rental.vehicle_model}
                      </div>
                      <div className="text-sm font-medium text-indigo-600 mt-1">
                        ৳{rental.agreed_amount.toFixed(2)}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {showDetailModal && (
        <div className="fixed inset-0 bg-black/50 flex items-start sm:items-center justify-center p-4 z-50 pt-20 sm:pt-4 overflow-y-auto">
          {detailError ? (
            <div className="bg-white rounded-lg shadow-lg p-8 w-full max-w-md text-center">
              <div className="text-red-600 text-lg mb-4">⚠️ ত্রুটি</div>
              <p className="text-gray-700 mb-6">{detailError}</p>
              <button
                onClick={() => {
                  setShowDetailModal(false);
                  setSelectedSettlement(null);
                  setDetailError('');
                }}
                className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
              >
                বন্ধ করুন
              </button>
            </div>
          ) : selectedSettlement ? (
            <div className="bg-white rounded-lg shadow-lg max-w-2xl w-full max-h-[calc(100vh-120px)] overflow-y-auto">
            <div className="flex justify-between items-center p-6 border-b sticky top-0 bg-white">
              <h2 className="text-xl font-bold">সেটেলমেন্ট বিস্তারিত</h2>
              <button
                onClick={() => {
                  setShowDetailModal(false);
                  setSelectedSettlement(null);
                  loadSettlements();
                }}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>

            <div className="p-6 space-y-6">
              {/* Trip Info */}
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 className="font-bold mb-3">ট্রিপ তথ্য</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <div className="text-gray-600">যাত্রি</div>
                    <div className="font-medium">
                      {selectedSettlement.customer_first_name} {selectedSettlement.customer_last_name}
                    </div>
                  </div>
                  <div>
                    <div className="text-gray-600">মোবাইল</div>
                    <div className="font-medium">{selectedSettlement.customer_phone}</div>
                  </div>
                  <div>
                    <div className="text-gray-600">গাড়ি</div>
                    <div className="font-medium">
                      {selectedSettlement.vehicle_brand} {selectedSettlement.vehicle_model}
                    </div>
                  </div>
                  <div>
                    <div className="text-gray-600">চালক</div>
                    <div className="font-medium">{selectedSettlement.driver_name || '—'}</div>
                  </div>
                  <div className="col-span-2">
                    <div className="text-gray-600">শুরু ঠিকানা</div>
                    <div className="font-medium">{selectedSettlement.pickup_location}</div>
                  </div>
                  <div className="col-span-2">
                    <div className="text-gray-600">শেষ ঠিকানা</div>
                    <div className="font-medium">{selectedSettlement.dropoff_location}</div>
                  </div>
                </div>
              </div>

              {/* Financial Summary */}
              <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-4">
                <h3 className="font-bold mb-4">আর্থিক সারাংশ</h3>
                <div className="space-y-3 text-sm">
                  <div className="flex justify-between items-center border-b pb-2">
                    <span className="text-gray-600">চুক্তির পরিমান</span>
                    <div className="flex items-center gap-2">
                      <span className="font-medium">৳{selectedSettlement.agreed_amount.toFixed(2)}</span>
                      <button
                        onClick={() => {
                          const newAmount = prompt(
                            'নতুন চুক্তির পরিমান:',
                            selectedSettlement.agreed_amount.toString()
                          );
                          if (newAmount && parseFloat(newAmount) > 0) {
                            handleUpdateAgreedAmount(parseFloat(newAmount));
                          }
                        }}
                        className="text-indigo-600 hover:underline"
                      >
                        সম্পাদন
                      </button>
                    </div>
                  </div>
                  <div className="flex justify-between border-b pb-2">
                    <span className="text-gray-600">মোট খরচ</span>
                    <span className="font-medium">৳{selectedSettlement.total_expenses.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between border-b pb-2">
                    <span className="text-gray-600">নেট পরিমান</span>
                    <span className="font-medium">৳{selectedSettlement.net_amount.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between border-b pb-2">
                    <span className="text-gray-600">
                      চালক কমিশন ({selectedSettlement.commission_percent}%)
                    </span>
                    <span className="font-medium">৳{selectedSettlement.driver_commission.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between pt-2 bg-white p-2 rounded">
                    <span className="font-bold">বকেয়া পরিমান</span>
                    <span className="font-bold text-indigo-600 text-lg">
                      ৳{selectedSettlement.amount_to_collect.toFixed(2)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Expenses */}
              {selectedSettlement.expenses && selectedSettlement.expenses.length > 0 && (
                <div className="border rounded-lg p-4">
                  <h3 className="font-bold mb-3">খরচ তালিকা</h3>
                  <div className="space-y-2 text-sm">
                    {selectedSettlement.expenses.map((exp: TripExpense) => (
                      <div key={exp.id} className="flex justify-between items-start py-2 border-b last:border-0">
                        <div>
                          <div className="font-medium">
                            {exp.expense_type === 'toll' && 'টোল'}
                            {exp.expense_type === 'fuel' && 'জালানি'}
                            {exp.expense_type === 'parking' && 'পার্কিং'}
                            {exp.expense_type === 'repair' && 'মেরামত'}
                            {exp.expense_type === 'driver_allowance' && 'চালকের ভাতা'}
                            {exp.expense_type === 'other' && 'অন্যান্য'}
                          </div>
                          {exp.description && (
                            <div className="text-gray-600">{exp.description}</div>
                          )}
                        </div>
                        <span className="font-medium">৳{exp.amount.toFixed(2)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Payment Collection */}
              <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-4">
                <div className="flex justify-between items-center mb-4">
                  <h3 className="font-bold">💰 পেমেন্ট জমা নিন</h3>
                  <button
                    onClick={() => setShowPaymentHistory(!showPaymentHistory)}
                    className="text-xs bg-emerald-600 text-white px-2 py-1 rounded hover:bg-emerald-700"
                  >
                    {showPaymentHistory ? 'লুকান' : 'ইতিহাস দেখুন'}
                  </button>
                </div>

                {/* Payment Status Summary */}
                <div className="grid grid-cols-3 gap-3 mb-4 text-sm">
                  <div className="bg-white p-3 rounded">
                    <div className="text-gray-600">মোট বকেয়া</div>
                    <div className="font-bold text-lg">৳{selectedSettlement.amount_to_collect.toFixed(2)}</div>
                  </div>
                  <div className="bg-white p-3 rounded">
                    <div className="text-gray-600">সংগৃহীত</div>
                    <div className="font-bold text-lg text-green-600">৳{selectedSettlement.paid_amount.toFixed(2)}</div>
                  </div>
                  <div className={`p-3 rounded ${selectedSettlement.remaining_amount <= 0 ? 'bg-green-100' : 'bg-yellow-100'}`}>
                    <div className="text-gray-600">বাকি</div>
                    <div className={`font-bold text-lg ${selectedSettlement.remaining_amount <= 0 ? 'text-green-700' : 'text-yellow-700'}`}>
                      ৳{selectedSettlement.remaining_amount.toFixed(2)}
                    </div>
                  </div>
                </div>

                {/* Payment History */}
                {showPaymentHistory && paymentHistory.length > 0 && (
                  <div className="mb-4 bg-white p-3 rounded border">
                    <h4 className="font-semibold text-sm mb-3">সংগৃহীত পেমেন্ট</h4>
                    <div className="space-y-2 text-sm max-h-48 overflow-y-auto">
                      {paymentHistory.map((payment) => (
                        <div key={payment.id} className="flex justify-between items-start py-2 border-b last:border-0">
                          <div>
                            <div className="font-medium">৳{payment.amount.toFixed(2)}</div>
                            <div className="text-xs text-gray-600">
                              {payment.payment_method && `${payment.payment_method} • `}
                              {new Date(payment.payment_date).toLocaleDateString('bn-BD')}
                            </div>
                            {payment.payment_notes && (
                              <div className="text-xs text-gray-500 mt-1">{payment.payment_notes}</div>
                            )}
                          </div>
                          <div className="text-right">
                            {payment.paid_by_driver_name && (
                              <div className="text-xs text-gray-600">{payment.paid_by_driver_name}</div>
                            )}
                            {payment.recorded_by_name && (
                              <div className="text-xs text-gray-500">{payment.recorded_by_name}</div>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Payment Collection Form */}
                {selectedSettlement.remaining_amount > 0 && (
                  <div className="space-y-3">
                    <div>
                      <label className="block text-sm font-medium mb-2">জমাদানকারী</label>
                      <input
                        type="text"
                        readOnly
                        value={selectedSettlement.driver_name || 'ড্রাইভার নির্দিষ্ট নেই'}
                        className="w-full border rounded-lg px-3 py-2 text-sm bg-gray-50"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-2">পেমেন্ট পরিমান (সর্বোচ্চ: ৳{selectedSettlement.remaining_amount.toFixed(2)})</label>
                      <input
                        type="number"
                        placeholder="০"
                        min="0"
                        max={selectedSettlement.remaining_amount}
                        step="0.01"
                        value={collectionForm.amount}
                        onChange={(e) =>
                          setCollectionForm({ ...collectionForm, amount: e.target.value })
                        }
                        className="w-full border rounded-lg px-3 py-2 text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-2">পেমেন্ট পদ্ধতি</label>
                      <select
                        value={collectionForm.payment_method}
                        onChange={(e) =>
                          setCollectionForm({ ...collectionForm, payment_method: e.target.value })
                        }
                        className="w-full border rounded-lg px-3 py-2 text-sm"
                      >
                        <option value="">নির্বাচন করুন</option>
                        <option value="ক্যাশ">ক্যাশ</option>
                        <option value="বিকাশ">বিকাশ</option>
                        <option value="নগদ">নগদ</option>
                        <option value="অন্যান্য">অন্যান্য</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-2">নোট (ঐচ্ছিক)</label>
                      <input
                        type="text"
                        placeholder="যেমন: চেক নম্বর, রেফারেন্স ID ইত্যাদি"
                        value={collectionForm.payment_notes}
                        onChange={(e) =>
                          setCollectionForm({ ...collectionForm, payment_notes: e.target.value })
                        }
                        className="w-full border rounded-lg px-3 py-2 text-sm"
                      />
                    </div>
                    <button
                      onClick={handleCollectPayment}
                      className="w-full bg-emerald-600 hover:bg-emerald-700 text-white py-2 rounded-lg transition font-medium"
                    >
                      পেমেন্ট সংগ্রহ করুন
                    </button>
                  </div>
                )}

                {selectedSettlement.remaining_amount <= 0 && (
                  <div className="bg-green-100 border border-green-300 rounded p-3 text-center">
                    <div className="font-bold text-green-700">✓ সম্পূর্ণ পরিশোধিত</div>
                    <div className="text-sm text-green-600">এই সেটেলমেন্টের সকল টাকা সংগ্রহ করা হয়েছে</div>
                  </div>
                )}
              </div>
            </div>
            </div>
          ) : (
            <div className="bg-white rounded-lg shadow-lg p-8 w-full max-w-md text-center">
              <div className="animate-spin inline-block w-8 h-8 border-4 border-gray-300 border-t-indigo-600 rounded-full mb-4"></div>
              <p className="text-gray-600">বিস্তারিত লোড করছি...</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default AdminSettlements;
