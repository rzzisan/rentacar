import React, { useState, useEffect, useMemo } from 'react';
import { api } from '../../api/client';
import type { DriverDues, DriverDuesDetail, CollectionAllocation } from '../../types';

const taka = (n: number) => '৳' + n.toLocaleString('bn-BD', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const dateBn = (d?: string) => (d ? new Date(d).toLocaleDateString('bn-BD') : '—');

const DriverCollections: React.FC = () => {
  const [drivers, setDrivers] = useState<DriverDues[]>([]);
  const [summary, setSummary] = useState({ grand_total_due: 0, grand_total_paid: 0, drivers_with_due: 0 });
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Detail / collect modal
  const [detail, setDetail] = useState<DriverDuesDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [activeTab, setActiveTab] = useState<'collect' | 'history'>('collect');
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({ amount: '', payment_method: '', notes: '' });
  const [lastResult, setLastResult] = useState<{ collected: number; allocations: CollectionAllocation[] } | null>(null);

  const showToast = (type: 'success' | 'error', message: string) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 4000);
  };

  const loadDrivers = async () => {
    setLoading(true);
    try {
      const res = await api.get<{ drivers: DriverDues[]; summary: typeof summary }>('/manager/drivers/dues.php');
      if (res.success && res.data) {
        setDrivers(res.data.drivers);
        setSummary(res.data.summary);
      } else {
        showToast('error', res.message || 'ড্রাইভারের বকেয়া লোড করতে ব্যর্থ');
      }
    } catch {
      showToast('error', 'ড্রাইভারের বকেয়া লোড করতে ব্যর্থ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDrivers();
  }, []);

  const loadDetail = async (driverId: number) => {
    setDetailLoading(true);
    setDetailError('');
    try {
      const res = await api.get<DriverDuesDetail>(`/manager/drivers/dues.php?id=${driverId}`);
      if (res.success && res.data) {
        setDetail(res.data);
      } else {
        setDetailError(res.message || 'বিস্তারিত লোড করতে ব্যর্থ');
      }
    } catch {
      setDetailError('বিস্তারিত লোড করতে ব্যর্থ');
    } finally {
      setDetailLoading(false);
    }
  };

  const openModal = (driver: DriverDues, tab: 'collect' | 'history') => {
    setShowModal(true);
    setActiveTab(tab);
    setForm({ amount: '', payment_method: '', notes: '' });
    setLastResult(null);
    setDetail(null);
    loadDetail(driver.id);
  };

  const closeModal = () => {
    setShowModal(false);
    setDetail(null);
    setDetailError('');
    setLastResult(null);
  };

  // Live FIFO allocation preview: entered amount distributed oldest trip first
  const allocationPreview = useMemo(() => {
    if (!detail) return [];
    const amount = parseFloat(form.amount);
    if (!amount || amount <= 0) return [];
    let left = Math.round(amount * 100) / 100;
    return detail.due_settlements.map((s) => {
      const pay = Math.min(s.remaining_amount, Math.max(left, 0));
      const rounded = Math.round(pay * 100) / 100;
      left = Math.round((left - rounded) * 100) / 100;
      return { settlement: s, pay: rounded, fullyPaid: rounded >= s.remaining_amount - 0.009 };
    });
  }, [detail, form.amount]);

  const enteredAmount = parseFloat(form.amount) || 0;
  const totalDue = detail?.total_due ?? 0;
  const amountTooBig = enteredAmount > totalDue + 0.009;

  const handleCollect = async () => {
    if (!detail) return;
    if (!enteredAmount || enteredAmount <= 0) {
      showToast('error', 'জমার পরিমান ০ থেকে বেশি হতে হবে');
      return;
    }
    if (amountTooBig) {
      showToast('error', `জমার পরিমান মোট বকেয়ার (${taka(totalDue)}) চেয়ে বেশি হতে পারে না`);
      return;
    }
    setSubmitting(true);
    try {
      const res = await api.post<{ collected_amount: number; allocations: CollectionAllocation[] }>(
        `/manager/drivers/collect.php?id=${detail.driver.id}`,
        {
          amount: enteredAmount,
          payment_method: form.payment_method || null,
          notes: form.notes || null,
        }
      );
      if (res.success && res.data) {
        showToast('success', res.message || 'টাকা সফলভাবে জমা হয়েছে');
        setLastResult({ collected: res.data.collected_amount, allocations: res.data.allocations });
        setForm({ amount: '', payment_method: '', notes: '' });
        await Promise.all([loadDetail(detail.driver.id), loadDrivers()]);
      } else {
        showToast('error', res.message || 'টাকা জমা ব্যর্থ');
      }
    } catch {
      showToast('error', 'টাকা জমা ব্যর্থ');
    } finally {
      setSubmitting(false);
    }
  };

  const filteredDrivers = drivers.filter((d) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return d.name.toLowerCase().includes(q) || d.mobile.includes(q);
  });

  return (
    <div className="p-4 sm:p-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <h1 className="text-2xl font-bold">ড্রাইভার বকেয়া জমা</h1>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm">
          <div className="text-sm text-gray-500">মোট বকেয়া (ড্রাইভারদের কাছে)</div>
          <div className="text-2xl font-bold text-red-600 mt-1">{taka(summary.grand_total_due)}</div>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm">
          <div className="text-sm text-gray-500">মোট সংগৃহীত</div>
          <div className="text-2xl font-bold text-green-600 mt-1">{taka(summary.grand_total_paid)}</div>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm">
          <div className="text-sm text-gray-500">বকেয়া আছে এমন ড্রাইভার</div>
          <div className="text-2xl font-bold text-indigo-600 mt-1">{summary.drivers_with_due} জন</div>
        </div>
      </div>

      {/* Search */}
      <div className="mb-6">
        <input
          type="text"
          placeholder="ড্রাইভারের নাম বা মোবাইল খুঁজুন..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full sm:max-w-sm border rounded-lg px-3 py-2"
        />
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

      {loading && <div className="text-center py-8">লোড হচ্ছে...</div>}

      {/* Desktop table */}
      {!loading && (
        <div className="hidden md:block overflow-x-auto bg-white rounded-xl border border-slate-200 shadow-sm">
          <table className="w-full border-collapse text-sm">
            <thead className="bg-gray-100">
              <tr>
                <th className="border-b p-3 text-left">ড্রাইভার</th>
                <th className="border-b p-3 text-center">বকেয়া ট্রিপ</th>
                <th className="border-b p-3 text-right">মোট সংগৃহীত</th>
                <th className="border-b p-3 text-right">মোট বকেয়া</th>
                <th className="border-b p-3 text-center">শেষ জমা</th>
                <th className="border-b p-3 text-center">অ্যাকশন</th>
              </tr>
            </thead>
            <tbody>
              {filteredDrivers.map((d) => (
                <tr key={d.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="p-3">
                    <div className="font-medium">{d.name}</div>
                    <div className="text-xs text-gray-500">{d.mobile}</div>
                  </td>
                  <td className="p-3 text-center">{d.due_settlement_count}টি</td>
                  <td className="p-3 text-right text-green-600">{taka(d.total_paid)}</td>
                  <td className={`p-3 text-right font-bold ${d.total_due > 0 ? 'text-red-600' : 'text-gray-400'}`}>
                    {taka(d.total_due)}
                  </td>
                  <td className="p-3 text-center text-gray-500">{dateBn(d.last_payment_date)}</td>
                  <td className="p-3 text-center">
                    <div className="flex justify-center gap-2">
                      {d.total_due > 0 && (
                        <button
                          onClick={() => openModal(d, 'collect')}
                          className="bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1.5 rounded-lg text-xs font-medium transition"
                        >
                          টাকা জমা নিন
                        </button>
                      )}
                      <button
                        onClick={() => openModal(d, 'history')}
                        className="border border-indigo-600 text-indigo-600 hover:bg-indigo-50 px-3 py-1.5 rounded-lg text-xs font-medium transition"
                      >
                        ইতিহাস
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {filteredDrivers.length === 0 && (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-gray-500">কোনো ড্রাইভার পাওয়া যায়নি</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Mobile cards */}
      {!loading && (
        <div className="md:hidden space-y-4">
          {filteredDrivers.map((d) => (
            <div key={d.id} className="bg-white border rounded-lg p-4 shadow-sm">
              <div className="flex justify-between items-start mb-3">
                <div>
                  <div className="font-medium">{d.name}</div>
                  <div className="text-xs text-gray-500">{d.mobile}</div>
                </div>
                <div className="text-right">
                  <div className="text-xs text-gray-500">মোট বকেয়া</div>
                  <div className={`font-bold ${d.total_due > 0 ? 'text-red-600' : 'text-gray-400'}`}>
                    {taka(d.total_due)}
                  </div>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-2 text-xs mb-3 py-2 border-y">
                <div>
                  <div className="text-gray-500">বকেয়া ট্রিপ</div>
                  <div className="font-medium">{d.due_settlement_count}টি</div>
                </div>
                <div>
                  <div className="text-gray-500">সংগৃহীত</div>
                  <div className="font-medium text-green-600">{taka(d.total_paid)}</div>
                </div>
                <div>
                  <div className="text-gray-500">শেষ জমা</div>
                  <div className="font-medium">{dateBn(d.last_payment_date)}</div>
                </div>
              </div>
              <div className="flex gap-2">
                {d.total_due > 0 && (
                  <button
                    onClick={() => openModal(d, 'collect')}
                    className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white py-2 rounded-lg text-sm font-medium transition"
                  >
                    টাকা জমা নিন
                  </button>
                )}
                <button
                  onClick={() => openModal(d, 'history')}
                  className="flex-1 border border-indigo-600 text-indigo-600 hover:bg-indigo-50 py-2 rounded-lg text-sm font-medium transition"
                >
                  ইতিহাস
                </button>
              </div>
            </div>
          ))}
          {filteredDrivers.length === 0 && (
            <div className="text-center py-8 text-gray-500">কোনো ড্রাইভার পাওয়া যায়নি</div>
          )}
        </div>
      )}

      {/* Collect / History modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-start sm:items-center justify-center p-4 z-50 pt-20 sm:pt-4 overflow-y-auto">
          <div className="bg-white rounded-lg shadow-lg max-w-2xl w-full max-h-[calc(100vh-120px)] overflow-y-auto">
            <div className="flex justify-between items-center p-6 border-b sticky top-0 bg-white z-10">
              <div>
                <h2 className="text-xl font-bold">{detail?.driver.name || 'লোড হচ্ছে...'}</h2>
                {detail && <div className="text-sm text-gray-500">{detail.driver.mobile}</div>}
              </div>
              <button onClick={closeModal} className="text-gray-400 hover:text-gray-600 text-xl">✕</button>
            </div>

            {detailLoading && !detail && (
              <div className="p-12 text-center">
                <div className="animate-spin inline-block w-8 h-8 border-4 border-gray-300 border-t-indigo-600 rounded-full mb-4"></div>
                <p className="text-gray-600">লোড হচ্ছে...</p>
              </div>
            )}

            {detailError && (
              <div className="p-8 text-center">
                <div className="text-red-600 mb-4">⚠️ {detailError}</div>
                <button onClick={closeModal} className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700">
                  বন্ধ করুন
                </button>
              </div>
            )}

            {detail && (
              <div className="p-6 space-y-5">
                {/* Status summary */}
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div className="bg-red-50 border border-red-200 p-3 rounded-lg">
                    <div className="text-gray-600">মোট বকেয়া</div>
                    <div className="font-bold text-lg text-red-600">{taka(detail.total_due)}</div>
                  </div>
                  <div className="bg-green-50 border border-green-200 p-3 rounded-lg">
                    <div className="text-gray-600">মোট সংগৃহীত</div>
                    <div className="font-bold text-lg text-green-600">{taka(detail.total_collected)}</div>
                  </div>
                </div>

                {/* Tabs */}
                <div className="flex border-b">
                  <button
                    onClick={() => setActiveTab('collect')}
                    className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition ${
                      activeTab === 'collect'
                        ? 'border-emerald-600 text-emerald-700'
                        : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                  >
                    💰 টাকা জমা নিন
                  </button>
                  <button
                    onClick={() => setActiveTab('history')}
                    className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition ${
                      activeTab === 'history'
                        ? 'border-indigo-600 text-indigo-700'
                        : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                  >
                    📜 জমার ইতিহাস ({detail.collections.length})
                  </button>
                </div>

                {/* Collect tab */}
                {activeTab === 'collect' && (
                  <div className="space-y-4">
                    {/* Last collection result */}
                    {lastResult && (
                      <div className="bg-green-50 border border-green-300 rounded-lg p-4 text-sm">
                        <div className="font-bold text-green-700 mb-2">
                          ✓ {taka(lastResult.collected)} জমা হয়েছে
                        </div>
                        <div className="space-y-1 text-green-800">
                          {lastResult.allocations.map((a) => (
                            <div key={a.payment_id} className="flex justify-between">
                              <span>সেটেলমেন্ট #{a.settlement_id}</span>
                              <span>
                                {taka(a.allocated)}{' '}
                                {a.payment_status === 'paid' ? '(পরিশোধিত ✓)' : `(বাকি ${taka(a.new_remaining_amount)})`}
                              </span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {detail.due_settlements.length === 0 ? (
                      <div className="bg-green-100 border border-green-300 rounded-lg p-4 text-center">
                        <div className="font-bold text-green-700">✓ কোনো বকেয়া নেই</div>
                        <div className="text-sm text-green-600">এই ড্রাইভারের সকল টাকা সংগ্রহ করা হয়েছে</div>
                      </div>
                    ) : (
                      <>
                        {/* Collection form */}
                        <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-4 space-y-3">
                          <div>
                            <label className="block text-sm font-medium mb-2">
                              জমার পরিমান (সর্বোচ্চ: {taka(detail.total_due)})
                            </label>
                            <div className="flex gap-2">
                              <input
                                type="number"
                                placeholder="০"
                                min="0"
                                max={detail.total_due}
                                step="0.01"
                                value={form.amount}
                                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                                className={`flex-1 border rounded-lg px-3 py-2 text-sm ${
                                  amountTooBig ? 'border-red-500 bg-red-50' : ''
                                }`}
                              />
                              <button
                                onClick={() => setForm({ ...form, amount: detail.total_due.toFixed(2) })}
                                className="bg-emerald-100 hover:bg-emerald-200 text-emerald-800 px-3 py-2 rounded-lg text-sm whitespace-nowrap transition"
                              >
                                পুরো বকেয়া
                              </button>
                            </div>
                            {amountTooBig && (
                              <div className="text-xs text-red-600 mt-1">
                                মোট বকেয়ার ({taka(detail.total_due)}) চেয়ে বেশি জমা নেওয়া যাবে না
                              </div>
                            )}
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2">পেমেন্ট পদ্ধতি</label>
                            <select
                              value={form.payment_method}
                              onChange={(e) => setForm({ ...form, payment_method: e.target.value })}
                              className="w-full border rounded-lg px-3 py-2 text-sm"
                            >
                              <option value="">নির্বাচন করুন</option>
                              <option value="ক্যাশ">ক্যাশ</option>
                              <option value="বিকাশ">বিকাশ</option>
                              <option value="নগদ">নগদ</option>
                              <option value="ব্যাংক">ব্যাংক</option>
                              <option value="অন্যান্য">অন্যান্য</option>
                            </select>
                          </div>
                          <div>
                            <label className="block text-sm font-medium mb-2">নোট (ঐচ্ছিক)</label>
                            <input
                              type="text"
                              placeholder="যেমন: ট্রানজেকশন ID, রেফারেন্স ইত্যাদি"
                              value={form.notes}
                              onChange={(e) => setForm({ ...form, notes: e.target.value })}
                              className="w-full border rounded-lg px-3 py-2 text-sm"
                            />
                          </div>
                          <button
                            onClick={handleCollect}
                            disabled={submitting || !enteredAmount || enteredAmount <= 0 || amountTooBig}
                            className="w-full bg-emerald-600 hover:bg-emerald-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white py-2.5 rounded-lg transition font-medium"
                          >
                            {submitting ? 'জমা হচ্ছে...' : 'টাকা জমা নিন'}
                          </button>
                        </div>

                        {/* Due settlements + allocation preview */}
                        <div className="border rounded-lg p-4">
                          <h4 className="font-bold text-sm mb-3">
                            বকেয়া সেটেলমেন্ট ({detail.due_settlements.length}টি)
                            {enteredAmount > 0 && !amountTooBig && (
                              <span className="font-normal text-gray-500"> — পুরাতন ট্রিপ থেকে ক্রমান্বয়ে কাটা হবে</span>
                            )}
                          </h4>
                          <div className="space-y-2 text-sm max-h-64 overflow-y-auto">
                            {detail.due_settlements.map((s, i) => {
                              const preview = allocationPreview[i];
                              const willPay = preview && preview.pay > 0 && !amountTooBig;
                              return (
                                <div
                                  key={s.id}
                                  className={`flex justify-between items-start py-2 px-2 rounded border-b last:border-0 ${
                                    willPay ? 'bg-emerald-50' : ''
                                  }`}
                                >
                                  <div>
                                    <div className="font-medium">
                                      {s.customer_first_name} {s.customer_last_name}
                                      <span className="text-xs text-gray-500"> · {dateBn(s.rental_start_date)}</span>
                                    </div>
                                    <div className="text-xs text-gray-500">
                                      {s.vehicle_brand} {s.vehicle_model} · সেটেলমেন্ট #{s.id}
                                    </div>
                                  </div>
                                  <div className="text-right">
                                    <div className="font-medium text-red-600">{taka(s.remaining_amount)}</div>
                                    {willPay && (
                                      <div className="text-xs text-emerald-700 font-medium">
                                        {preview.fullyPaid ? '✓ পরিশোধ হবে' : `${taka(preview.pay)} কাটা হবে`}
                                      </div>
                                    )}
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                )}

                {/* History tab */}
                {activeTab === 'history' && (
                  <div className="space-y-2 text-sm">
                    {detail.collections.length === 0 ? (
                      <div className="text-center text-gray-500 py-8">এখনো কোনো জমা নেওয়া হয়নি</div>
                    ) : (
                      detail.collections.map((p) => (
                        <div key={p.id} className="flex justify-between items-start py-3 px-2 border-b last:border-0">
                          <div>
                            <div className="font-bold text-green-700">{taka(p.amount)}</div>
                            <div className="text-xs text-gray-600">
                              {p.payment_method && `${p.payment_method} · `}
                              {dateBn(p.payment_date)}
                            </div>
                            {p.payment_notes && (
                              <div className="text-xs text-gray-500 mt-0.5">{p.payment_notes}</div>
                            )}
                          </div>
                          <div className="text-right text-xs text-gray-500">
                            <div>
                              {p.customer_first_name} {p.customer_last_name} · #{p.settlement_id}
                            </div>
                            <div>{p.vehicle_brand} {p.vehicle_model}</div>
                            {p.recorded_by_name && <div>গ্রহণকারী: {p.recorded_by_name}</div>}
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default DriverCollections;
