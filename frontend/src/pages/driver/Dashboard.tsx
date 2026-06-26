import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import type { Rental, Settlement } from '../../types';

interface MonthlyBreakdown {
  month: string;
  trip_count: number;
  earned: number;
  paid: number;
  pending: number;
}

interface DriverLedger {
  settlements: Settlement[];
  summary: {
    total_trips: number;
    total_earned: number;
    total_paid: number;
    total_pending: number;
  };
  monthly_breakdown: MonthlyBreakdown[];
  this_month: MonthlyBreakdown | null;
  last_month: MonthlyBreakdown | null;
}

const EXPENSE_LABELS: Record<string, string> = {
  toll: 'টোল',
  fuel: 'জ্বালানি',
  parking: 'পার্কিং',
  repair: 'মেরামত',
  driver_allowance: 'ভাতা',
  other: 'অন্যান্য',
};

const MONTH_NAMES: Record<string, string> = {
  '01': 'জানু', '02': 'ফেব্রু', '03': 'মার্চ', '04': 'এপ্রি',
  '05': 'মে', '06': 'জুন', '07': 'জুলাই', '08': 'আগস্ট',
  '09': 'সেপ্টে', '10': 'অক্টো', '11': 'নভে', '12': 'ডিসে',
};

const formatMonth = (ym: string) => {
  const [y, m] = ym.split('-');
  return `${MONTH_NAMES[m] ?? m} ${y}`;
};

const formatDuration = (ms: number): string => {
  const total = Math.max(0, Math.floor(ms / 1000));
  const d = Math.floor(total / 86400);
  const h = Math.floor((total % 86400) / 3600);
  const m = Math.floor((total % 3600) / 60);
  const parts: string[] = [];
  if (d) parts.push(`${d} দিন`);
  if (h) parts.push(`${h} ঘণ্টা`);
  parts.push(`${m} মিনিট`);
  return parts.join(' ');
};

const tripRoute = (r: { trip_type?: string; pickup_location?: string; dropoff_location?: string }) => {
  if (!r.pickup_location && !r.dropoff_location) return '—';
  const pickup = r.pickup_location || '?';
  const dropoff = r.dropoff_location || '?';
  return r.trip_type === 'round_trip'
    ? `${pickup} → ${dropoff} → ${pickup}`
    : `${pickup} → ${dropoff}`;
};

const DriverDashboard: React.FC = () => {
  const [ledger, setLedger]             = useState<DriverLedger | null>(null);
  const [activeTrips, setActiveTrips]   = useState<Rental[]>([]);
  const [upcomingTrips, setUpcomingTrips] = useState<Rental[]>([]);
  const [loading, setLoading]           = useState(false);
  const [expandedTrip, setExpandedTrip] = useState<number | null>(null);
  const [showMonthly, setShowMonthly]   = useState(false);
  const [now, setNow]                   = useState(Date.now());

  useEffect(() => { loadAll(); }, []);

  useEffect(() => {
    if (activeTrips.length === 0 && upcomingTrips.length === 0) return;
    const timer = setInterval(() => setNow(Date.now()), 60000);
    return () => clearInterval(timer);
  }, [activeTrips.length, upcomingTrips.length]);

  const loadAll = async () => {
    setLoading(true);
    try {
      const [ledgerRes, activeRes, pendingRes] = await Promise.all([
        api.get<DriverLedger>('/driver/ledger.php'),
        api.get<Rental[]>('/driver/rentals/index.php?status=active'),
        api.get<Rental[]>('/driver/rentals/index.php?status=pending'),
      ]);
      if (ledgerRes.success && ledgerRes.data) setLedger(ledgerRes.data);
      if (activeRes.success && activeRes.data) setActiveTrips(activeRes.data);
      if (pendingRes.success && pendingRes.data) {
        setUpcomingTrips(
          [...pendingRes.data].sort(
            (a, b) => new Date(a.start_date).getTime() - new Date(b.start_date).getTime()
          )
        );
      }
    } catch (error) {
      console.error('Failed to load:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="p-4 sm:p-6 space-y-4 animate-pulse">
        <div className="h-8 w-48 bg-slate-200 rounded" />
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => <div key={i} className="h-20 bg-slate-200 rounded-lg" />)}
        </div>
        <div className="h-40 bg-slate-200 rounded-lg" />
      </div>
    );
  }

  if (!ledger) {
    return (
      <div className="p-4 sm:p-6">
        <div className="text-center py-8 text-red-600">লেজার লোড করতে ব্যর্থ</div>
      </div>
    );
  }

  const { settlements, summary, monthly_breakdown, this_month, last_month } = ledger;

  // this month vs last month change
  const earnedChange = this_month && last_month
    ? (last_month.earned > 0 ? ((this_month.earned - last_month.earned) / last_month.earned * 100) : null)
    : null;

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">আমার লেজার</h1>
          <p className="text-gray-500 text-sm mt-0.5">ট্রিপ, কমিশন ও পেমেন্ট হিসাব</p>
        </div>
        <Link
          to="/driver/profile"
          className="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
        >
          প্রোফাইল →
        </Link>
      </div>

      {/* চলমান ট্রিপ হাইলাইট */}
      {activeTrips.map((trip) => (
        <div
          key={trip.id}
          className="bg-green-50 border-2 border-green-500 rounded-lg p-4 shadow-md ring-2 ring-green-200"
        >
          <div className="flex items-center gap-2 mb-3">
            <span className="relative flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75" />
              <span className="relative inline-flex rounded-full h-3 w-3 bg-green-600" />
            </span>
            <span className="font-bold text-green-800">চলমান ট্রিপ</span>
            <span className="ml-auto text-xs font-medium text-green-700">
              চলছে {formatDuration(now - new Date(trip.actual_start_time || trip.start_date).getTime())} ধরে
            </span>
          </div>
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div className="text-sm">
              <div className="font-medium text-gray-900">
                {trip.customer_first_name} {trip.customer_last_name}
                {trip.customer_phone && <span className="text-gray-600 font-normal"> • {trip.customer_phone}</span>}
              </div>
              <div className="text-gray-700 mt-1">{tripRoute(trip)}</div>
              <div className="text-gray-600 mt-1">
                {trip.vehicle_brand} {trip.vehicle_model} • চুক্তি ৳{trip.agreed_amount.toFixed(0)}
              </div>
            </div>
            <Link
              to={`/driver/rentals?open=${trip.id}`}
              className="shrink-0 text-center bg-green-600 text-white px-5 py-2.5 rounded-lg font-medium hover:bg-green-700 transition"
            >
              + খরচ যুক্ত করুন
            </Link>
          </div>
        </div>
      ))}

      {/* আপকামিং ট্রিপ */}
      {upcomingTrips.map((trip) => {
        const diff = new Date(trip.start_date).getTime() - now;
        return (
          <div
            key={trip.id}
            className="bg-amber-50 border-2 border-amber-400 rounded-lg p-4 shadow-sm"
          >
            <div className="flex items-center gap-2 mb-3">
              <span className="inline-flex rounded-full h-3 w-3 bg-amber-500" />
              <span className="font-bold text-amber-800">আপকামিং ট্রিপ</span>
              <span className={`ml-auto text-xs font-medium ${diff > 0 ? 'text-amber-700' : 'text-red-600'}`}>
                {diff > 0 ? `শুরু হতে বাকি ${formatDuration(diff)}` : 'নির্ধারিত সময় পেরিয়ে গেছে'}
              </span>
            </div>
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div className="text-sm">
                <div className="font-medium text-gray-900">
                  {trip.customer_first_name} {trip.customer_last_name}
                  {trip.customer_phone && <span className="text-gray-600 font-normal"> • {trip.customer_phone}</span>}
                </div>
                <div className="text-gray-700 mt-1">{tripRoute(trip)}</div>
                <div className="text-gray-600 mt-1">
                  {trip.vehicle_brand} {trip.vehicle_model} • চুক্তি ৳{trip.agreed_amount.toFixed(0)} •{' '}
                  {new Date(trip.start_date).toLocaleString('bn-BD')}
                </div>
              </div>
              <Link
                to={`/driver/rentals?open=${trip.id}`}
                className="shrink-0 text-center bg-amber-500 text-white px-5 py-2.5 rounded-lg font-medium hover:bg-amber-600 transition"
              >
                বিস্তারিত দেখুন
              </Link>
            </div>
          </div>
        );
      })}

      {/* সর্বকালীন সারসংক্ষেপ */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="text-xs text-gray-500 mb-1">মোট ট্রিপ</div>
          <div className="text-2xl font-bold text-blue-600">{summary.total_trips}</div>
        </div>
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="text-xs text-gray-500 mb-1">মোট আয়</div>
          <div className="text-2xl font-bold text-green-600">৳{summary.total_earned.toFixed(0)}</div>
        </div>
        <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-4">
          <div className="text-xs text-gray-500 mb-1">পরিশোধিত</div>
          <div className="text-2xl font-bold text-indigo-600">৳{summary.total_paid.toFixed(0)}</div>
        </div>
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
          <div className="text-xs text-gray-500 mb-1">বকেয়া</div>
          <div className="text-2xl font-bold text-amber-600">৳{summary.total_pending.toFixed(0)}</div>
        </div>
      </div>

      {/* এই মাস vs গত মাস */}
      {(this_month || last_month) && (
        <div className="bg-white border rounded-xl overflow-hidden shadow-sm">
          <div
            className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50"
            onClick={() => setShowMonthly(!showMonthly)}
          >
            <div className="flex items-center gap-3">
              <span className="text-xl">📅</span>
              <div>
                <div className="font-semibold text-gray-800">মাসিক আয়ের সারসংক্ষেপ</div>
                {this_month && (
                  <div className="text-sm text-gray-500">
                    এই মাসে ৳{this_month.earned.toFixed(0)} আয়
                    {earnedChange !== null && (
                      <span className={`ml-2 font-medium ${earnedChange >= 0 ? 'text-green-600' : 'text-red-500'}`}>
                        ({earnedChange >= 0 ? '+' : ''}{earnedChange.toFixed(1)}% গত মাসের চেয়ে)
                      </span>
                    )}
                  </div>
                )}
              </div>
            </div>
            <span className={`text-gray-400 transition-transform ${showMonthly ? 'rotate-180' : ''}`}>▼</span>
          </div>

          {showMonthly && (
            <>
              {/* এই মাস + গত মাস কার্ড */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 px-4 pb-4">
                {this_month && (
                  <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-3">
                    <div className="text-xs font-medium text-indigo-700 mb-2">{formatMonth(this_month.month)} (এই মাস)</div>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div><span className="text-gray-500">ট্রিপ:</span> <span className="font-semibold">{this_month.trip_count}</span></div>
                      <div><span className="text-gray-500">আয়:</span> <span className="font-semibold text-green-600">৳{this_month.earned.toFixed(0)}</span></div>
                      <div><span className="text-gray-500">পেয়েছি:</span> <span className="font-semibold text-indigo-600">৳{this_month.paid.toFixed(0)}</span></div>
                      <div><span className="text-gray-500">বাকি:</span> <span className="font-semibold text-amber-600">৳{this_month.pending.toFixed(0)}</span></div>
                    </div>
                  </div>
                )}
                {last_month && (
                  <div className="bg-gray-50 border border-gray-200 rounded-lg p-3">
                    <div className="text-xs font-medium text-gray-600 mb-2">{formatMonth(last_month.month)} (গত মাস)</div>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div><span className="text-gray-500">ট্রিপ:</span> <span className="font-semibold">{last_month.trip_count}</span></div>
                      <div><span className="text-gray-500">আয়:</span> <span className="font-semibold text-green-600">৳{last_month.earned.toFixed(0)}</span></div>
                      <div><span className="text-gray-500">পেয়েছি:</span> <span className="font-semibold text-indigo-600">৳{last_month.paid.toFixed(0)}</span></div>
                      <div><span className="text-gray-500">বাকি:</span> <span className="font-semibold text-amber-600">৳{last_month.pending.toFixed(0)}</span></div>
                    </div>
                  </div>
                )}
              </div>

              {/* সব মাসের টেবিল */}
              {monthly_breakdown.length > 0 && (
                <div className="overflow-x-auto border-t">
                  <table className="w-full text-sm">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-2 text-left font-medium text-gray-600">মাস</th>
                        <th className="px-4 py-2 text-center font-medium text-gray-600">ট্রিপ</th>
                        <th className="px-4 py-2 text-right font-medium text-gray-600">আয়</th>
                        <th className="px-4 py-2 text-right font-medium text-gray-600">পেয়েছি</th>
                        <th className="px-4 py-2 text-right font-medium text-gray-600">বাকি</th>
                      </tr>
                    </thead>
                    <tbody>
                      {monthly_breakdown.map(m => (
                        <tr key={m.month} className="border-t hover:bg-gray-50">
                          <td className="px-4 py-2 font-medium">{formatMonth(m.month)}</td>
                          <td className="px-4 py-2 text-center">{m.trip_count}</td>
                          <td className="px-4 py-2 text-right text-green-600 font-medium">৳{m.earned.toFixed(0)}</td>
                          <td className="px-4 py-2 text-right text-indigo-600">৳{m.paid.toFixed(0)}</td>
                          <td className="px-4 py-2 text-right text-amber-600">৳{m.pending.toFixed(0)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* ট্রিপ লেজার টেবিল — Desktop */}
      <div>
        <h2 className="font-semibold text-gray-800 mb-3">ট্রিপ ইতিহাস</h2>
        <div className="hidden md:block overflow-x-auto bg-white rounded-xl border shadow-sm">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="p-3 text-left">ট্রিপ তথ্য</th>
                <th className="p-3 text-right">চুক্তি</th>
                <th className="p-3 text-right">খরচ</th>
                <th className="p-3 text-right">কমিশন</th>
                <th className="p-3 text-right">পাওয়া</th>
                <th className="p-3 text-right">বাকি</th>
                <th className="p-3 text-center">স্ট্যাটাস</th>
              </tr>
            </thead>
            <tbody>
              {settlements.map((trip) => (
                <React.Fragment key={trip.id}>
                  <tr
                    className="border-b hover:bg-gray-50 cursor-pointer"
                    onClick={() => setExpandedTrip(expandedTrip === trip.id ? null : trip.id)}
                  >
                    <td className="p-3">
                      <div className="font-medium">
                        {trip.customer_first_name} {trip.customer_last_name}
                      </div>
                      <div className="text-xs text-gray-500">
                        {trip.pickup_location} → {trip.dropoff_location}
                      </div>
                      <div className="text-xs text-gray-400">
                        {trip.rental_start_date
                          ? new Date(trip.rental_start_date).toLocaleDateString('bn-BD')
                          : '—'}
                      </div>
                    </td>
                    <td className="p-3 text-right">৳{trip.agreed_amount.toFixed(0)}</td>
                    <td className="p-3 text-right">৳{trip.total_expenses.toFixed(0)}</td>
                    <td className="p-3 text-right font-medium text-green-600">৳{trip.driver_commission.toFixed(0)}</td>
                    <td className="p-3 text-right">৳{trip.paid_amount.toFixed(0)}</td>
                    <td className="p-3 text-right text-amber-600 font-medium">৳{trip.remaining_amount.toFixed(0)}</td>
                    <td className="p-3 text-center">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                        trip.payment_status === 'paid'    ? 'bg-green-100 text-green-800' :
                        trip.payment_status === 'partial' ? 'bg-blue-100 text-blue-800' :
                        'bg-amber-100 text-amber-800'
                      }`}>
                        {trip.payment_status === 'paid' ? 'পরিশোধিত' :
                         trip.payment_status === 'partial' ? 'আংশিক' : 'অপেক্ষমান'}
                      </span>
                    </td>
                  </tr>

                  {/* Expanded: payments + expense breakdown */}
                  {expandedTrip === trip.id && (
                    <tr className="border-b bg-gray-50">
                      <td colSpan={7} className="p-4">
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          {/* Expense Breakdown */}
                          {trip.expense_breakdown && Object.keys(trip.expense_breakdown).length > 0 && (
                            <div className="bg-white rounded-lg p-3 border">
                              <h4 className="font-semibold text-xs text-gray-500 uppercase mb-2">খরচের বিবরণ</h4>
                              <div className="space-y-1">
                                {Object.entries(trip.expense_breakdown).map(([type, amt]) => (
                                  <div key={type} className="flex justify-between text-sm">
                                    <span className="text-gray-600">{EXPENSE_LABELS[type] ?? type}</span>
                                    <span className="font-medium">৳{(amt as number).toFixed(0)}</span>
                                  </div>
                                ))}
                                <div className="flex justify-between text-sm font-bold border-t pt-1 mt-1">
                                  <span>মোট</span>
                                  <span>৳{trip.total_expenses.toFixed(0)}</span>
                                </div>
                              </div>
                            </div>
                          )}

                          {/* Payment History */}
                          <div className="bg-white rounded-lg p-3 border">
                            <h4 className="font-semibold text-xs text-gray-500 uppercase mb-2">পেমেন্ট ইতিহাস</h4>
                            {trip.payments && trip.payments.length > 0 ? (
                              <div className="space-y-2">
                                {trip.payments.map((p) => (
                                  <div key={p.id} className="flex justify-between items-start text-sm border-b last:border-0 pb-1">
                                    <div>
                                      <div className="font-medium">৳{p.amount.toFixed(0)}</div>
                                      <div className="text-xs text-gray-500">
                                        {p.payment_method && `${p.payment_method} • `}
                                        {new Date(p.payment_date).toLocaleDateString('bn-BD')}
                                      </div>
                                      {p.payment_notes && <div className="text-xs text-gray-400">{p.payment_notes}</div>}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            ) : (
                              <p className="text-gray-400 text-sm">কোনো পেমেন্ট নেই</p>
                            )}
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
          {settlements.length === 0 && (
            <div className="text-center py-12 text-gray-400">
              <div className="text-4xl mb-2">📋</div>
              <p>এখনও কোনো ট্রিপ নেই</p>
            </div>
          )}
        </div>

        {/* Mobile Cards */}
        <div className="md:hidden space-y-3">
          {settlements.map((trip) => (
            <div key={trip.id} className="bg-white border rounded-xl overflow-hidden shadow-sm">
              <div
                className="p-4 cursor-pointer hover:bg-gray-50"
                onClick={() => setExpandedTrip(expandedTrip === trip.id ? null : trip.id)}
              >
                <div className="flex justify-between items-start mb-2">
                  <div>
                    <div className="font-medium">{trip.customer_first_name} {trip.customer_last_name}</div>
                    <div className="text-xs text-gray-500">{trip.pickup_location} → {trip.dropoff_location}</div>
                    <div className="text-xs text-gray-400">
                      {trip.rental_start_date ? new Date(trip.rental_start_date).toLocaleDateString('bn-BD') : '—'}
                    </div>
                  </div>
                  <span className={`px-2 py-1 rounded-full text-xs font-medium flex-shrink-0 ${
                    trip.payment_status === 'paid'    ? 'bg-green-100 text-green-800' :
                    trip.payment_status === 'partial' ? 'bg-blue-100 text-blue-800' :
                    'bg-amber-100 text-amber-800'
                  }`}>
                    {trip.payment_status === 'paid' ? 'পরিশোধিত' :
                     trip.payment_status === 'partial' ? 'আংশিক' : 'অপেক্ষমান'}
                  </span>
                </div>
                <div className="grid grid-cols-2 gap-2 text-sm py-2 border-y">
                  <div><span className="text-gray-500">কমিশন: </span><span className="font-medium text-green-600">৳{trip.driver_commission.toFixed(0)}</span></div>
                  <div><span className="text-gray-500">পাওয়া: </span><span className="font-medium">৳{trip.paid_amount.toFixed(0)}</span></div>
                  <div><span className="text-gray-500">খরচ: </span><span className="font-medium">৳{trip.total_expenses.toFixed(0)}</span></div>
                  <div><span className="text-gray-500">বাকি: </span><span className="font-medium text-amber-600">৳{trip.remaining_amount.toFixed(0)}</span></div>
                </div>
              </div>

              {expandedTrip === trip.id && (
                <div className="bg-gray-50 p-4 border-t space-y-3">
                  {/* Expense breakdown */}
                  {trip.expense_breakdown && Object.keys(trip.expense_breakdown).length > 0 && (
                    <div>
                      <h4 className="font-semibold text-xs text-gray-500 uppercase mb-2">খরচের বিবরণ</h4>
                      <div className="bg-white rounded-lg p-3 space-y-1">
                        {Object.entries(trip.expense_breakdown).map(([type, amt]) => (
                          <div key={type} className="flex justify-between text-sm">
                            <span className="text-gray-600">{EXPENSE_LABELS[type] ?? type}</span>
                            <span className="font-medium">৳{(amt as number).toFixed(0)}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                  {/* Payment history */}
                  <div>
                    <h4 className="font-semibold text-xs text-gray-500 uppercase mb-2">পেমেন্ট ইতিহাস</h4>
                    {trip.payments && trip.payments.length > 0 ? (
                      <div className="space-y-2">
                        {trip.payments.map((p) => (
                          <div key={p.id} className="bg-white p-3 rounded-lg text-sm">
                            <div className="font-medium">৳{p.amount.toFixed(0)}</div>
                            <div className="text-xs text-gray-500">
                              {p.payment_method && `${p.payment_method} • `}
                              {new Date(p.payment_date).toLocaleDateString('bn-BD')}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-gray-400 text-sm">কোনো পেমেন্ট নেই</p>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
          {settlements.length === 0 && (
            <div className="text-center py-12 text-gray-400">
              <div className="text-4xl mb-2">📋</div>
              <p>এখনও কোনো ট্রিপ নেই</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DriverDashboard;
