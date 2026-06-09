import React, { useState, useEffect } from 'react';
import { api } from '../../api/client';
import type { Settlement, TripExpense } from '../../types';

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
  const [paymentForm, setPaymentForm] = useState({
    payment_status: 'pending',
    payment_method: '',
    payment_notes: '',
  });

  // Load settlements
  const loadSettlements = async () => {
    setLoading(true);
    try {
      let url = '/admin/settlements/index.php';
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
      const response = await api.get<any[]>('/admin/rentals/index.php?status=completed');
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

  const showToast = (type: string, message: string) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 3000);
  };

  const handleShowDetail = async (settlement: Settlement) => {
    try {
      const response = await api.get<Settlement>(`/admin/settlements/show.php?id=${settlement.id}`);
      if (response.success && response.data) {
        setSelectedSettlement(response.data);
        setEditingAgreedAmount(response.data.agreed_amount.toString());
        setPaymentForm({
          payment_status: response.data.payment_status,
          payment_method: response.data.payment_method || '',
          payment_notes: response.data.payment_notes || '',
        });
        setShowDetailModal(true);
      }
    } catch (error) {
      showToast('error', 'বিস্তারিত লোড করতে ব্যর্থ');
    }
  };

  const handleCreateSettlement = async (rental: any) => {
    try {
      console.log('Creating settlement for rental:', rental.id);
      const response = await api.post('/admin/settlements/index.php', {
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
      const updateResponse = await api.post(`/admin/settlements/update.php?id=${selectedSettlement.id}`, {
        agreed_amount: updateAmount,
      });
      console.log('✓ Update response:', updateResponse);

      // Step 2: Immediately reload detail (to get fresh data with recalculations)
      console.log('📥 Reloading settlement detail...');
      const detailResponse = await api.get<Settlement>(`/admin/settlements/show.php?id=${selectedSettlement.id}`);
      console.log('📊 Detail response - agreed:', detailResponse.data?.agreed_amount);

      if (detailResponse.success && detailResponse.data) {
        console.log('✓ Updating modal with new data: agreed=' + detailResponse.data.agreed_amount);
        setSelectedSettlement(detailResponse.data);
        setEditingAgreedAmount(detailResponse.data.agreed_amount.toString());
        // Update payment form to reflect current state
        setPaymentForm({
          payment_status: detailResponse.data.payment_status,
          payment_method: detailResponse.data.payment_method || '',
          payment_notes: detailResponse.data.payment_notes || '',
        });
      }

      // Step 3: Reload settlements list
      console.log('📋 Reloading settlements list...');
      let listUrl = '/admin/settlements/index.php';
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

  const handleUpdatePaymentStatus = async () => {
    if (!selectedSettlement) return;
    try {
      console.log('💳 Updating payment status:', paymentForm.payment_status);

      // Step 1: Update payment
      await api.post(`/admin/settlements/update.php?id=${selectedSettlement.id}`, paymentForm);
      console.log('✓ Payment updated');

      // Step 2: Reload detail
      const response = await api.get<Settlement>(`/admin/settlements/show.php?id=${selectedSettlement.id}`);
      console.log('✓ Detail reloaded, status:', response.data?.payment_status);

      if (response.success && response.data) {
        setSelectedSettlement(response.data);
        // Update form to reflect latest state
        setPaymentForm({
          payment_status: response.data.payment_status,
          payment_method: response.data.payment_method || '',
          payment_notes: response.data.payment_notes || '',
        });
      }

      // Step 3: Reload list
      let listUrl = '/admin/settlements/index.php';
      const params = new URLSearchParams();
      if (filterStatus) params.append('status', filterStatus);
      if (searchQuery) params.append('search', searchQuery);
      if (params.toString()) listUrl += '?' + params.toString();

      const listResponse = await api.get<Settlement[]>(listUrl);
      if (listResponse.success && listResponse.data) {
        setSettlements(listResponse.data);
      }

      showToast('success', 'পেমেন্ট স্ট্যাটাস আপডেট হয়েছে');
    } catch (error) {
      console.error('❌ Payment update error:', error);
      showToast('error', 'আপডেট ব্যর্থ');
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
      {showDetailModal && selectedSettlement && (
        <div className="fixed inset-0 bg-black/50 flex items-start sm:items-center justify-center p-4 z-50 pt-20 sm:pt-4 overflow-y-auto">
          <div className="bg-white rounded-lg shadow-lg max-w-2xl w-full max-h-[calc(100vh-120px)] overflow-y-auto">
            <div className="flex justify-between items-center p-6 border-b sticky top-0 bg-white">
              <h2 className="text-xl font-bold">সেটেলমেন্ট বিস্তারিত</h2>
              <button
                onClick={() => {
                  setShowDetailModal(false);
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

              {/* Payment Status */}
              <div className="border rounded-lg p-4">
                <h3 className="font-bold mb-4">পেমেন্ট তথ্য</h3>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium mb-2">পেমেন্ট স্ট্যাটাস</label>
                    <select
                      value={paymentForm.payment_status}
                      onChange={(e) =>
                        setPaymentForm({ ...paymentForm, payment_status: e.target.value })
                      }
                      className="w-full border rounded-lg px-3 py-2 text-sm"
                    >
                      <option value="pending">অপেক্ষমান</option>
                      <option value="partial">আংশিক</option>
                      <option value="paid">পরিশোধিত</option>
                      <option value="refunded">ফেরত</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-2">পেমেন্ট পদ্ধতি</label>
                    <input
                      type="text"
                      placeholder="নগদ, চেক, ট্রান্সফার, ইত্যাদি"
                      value={paymentForm.payment_method}
                      onChange={(e) =>
                        setPaymentForm({ ...paymentForm, payment_method: e.target.value })
                      }
                      className="w-full border rounded-lg px-3 py-2 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-2">পেমেন্ট নোট</label>
                    <textarea
                      placeholder="যেকোনো অতিরিক্ত তথ্য..."
                      value={paymentForm.payment_notes}
                      onChange={(e) =>
                        setPaymentForm({ ...paymentForm, payment_notes: e.target.value })
                      }
                      className="w-full border rounded-lg px-3 py-2 text-sm"
                      rows={3}
                    />
                  </div>
                  <button
                    onClick={handleUpdatePaymentStatus}
                    className="w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded-lg transition"
                  >
                    পেমেন্ট আপডেট করুন
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminSettlements;
