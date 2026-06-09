import React, { useState, useEffect } from 'react';
import { api } from '../../api/client';
import type { Settlement } from '../../types';

interface DriverLedger {
  settlements: Settlement[];
  summary: {
    total_trips: number;
    total_earned: number;
    total_paid: number;
    total_pending: number;
  };
}

const DriverDashboard: React.FC = () => {
  const [ledger, setLedger] = useState<DriverLedger | null>(null);
  const [loading, setLoading] = useState(false);
  const [expandedTrip, setExpandedTrip] = useState<number | null>(null);

  useEffect(() => {
    loadLedger();
  }, []);

  const loadLedger = async () => {
    setLoading(true);
    try {
      const response = await api.get<DriverLedger>('/driver/ledger.php');
      if (response.success && response.data) {
        setLedger(response.data);
      }
    } catch (error) {
      console.error('Failed to load ledger:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="p-4 sm:p-6">
        <div className="text-center py-8">লোড হচ্ছে...</div>
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

  const { settlements, summary } = ledger;

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold mb-2">আমার লেজার</h1>
        <p className="text-gray-600">ট্রিপ, কমিশন এবং পেমেন্ট হিসাব</p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="text-sm text-gray-600">মোট ট্রিপ</div>
          <div className="text-2xl font-bold text-blue-600">{summary.total_trips}</div>
        </div>
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="text-sm text-gray-600">মোট আয়</div>
          <div className="text-2xl font-bold text-green-600">৳{summary.total_earned.toFixed(2)}</div>
        </div>
        <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-4">
          <div className="text-sm text-gray-600">পরিশোধিত</div>
          <div className="text-2xl font-bold text-indigo-600">৳{summary.total_paid.toFixed(2)}</div>
        </div>
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <div className="text-sm text-gray-600">বকেয়া</div>
          <div className="text-2xl font-bold text-yellow-600">৳{summary.total_pending.toFixed(2)}</div>
        </div>
      </div>

      {/* Trips Table - Desktop */}
      <div className="hidden md:block overflow-x-auto bg-white rounded-lg border">
        <table className="w-full text-sm">
          <thead className="bg-gray-100 border-b">
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
                <tr className="border-b hover:bg-gray-50 cursor-pointer" onClick={() => setExpandedTrip(expandedTrip === trip.id ? null : trip.id)}>
                  <td className="p-3">
                    <div className="font-medium">
                      {trip.customer_first_name} {trip.customer_last_name}
                    </div>
                    <div className="text-xs text-gray-500">
                      {trip.pickup_location} → {trip.dropoff_location}
                    </div>
                    <div className="text-xs text-gray-500">
                      {new Date(trip.rental_start_date || '').toLocaleDateString('bn-BD')}
                    </div>
                  </td>
                  <td className="p-3 text-right">৳{trip.agreed_amount.toFixed(2)}</td>
                  <td className="p-3 text-right">৳{trip.total_expenses.toFixed(2)}</td>
                  <td className="p-3 text-right font-medium">৳{trip.driver_commission.toFixed(2)}</td>
                  <td className="p-3 text-right">৳{trip.paid_amount.toFixed(2)}</td>
                  <td className="p-3 text-right text-yellow-600 font-medium">৳{trip.remaining_amount.toFixed(2)}</td>
                  <td className="p-3 text-center">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      trip.payment_status === 'paid' ? 'bg-green-100 text-green-800' :
                      trip.payment_status === 'partial' ? 'bg-blue-100 text-blue-800' :
                      'bg-yellow-100 text-yellow-800'
                    }`}>
                      {trip.payment_status === 'paid' ? 'পরিশোধিত' :
                       trip.payment_status === 'partial' ? 'আংশিক' :
                       'অপেক্ষমান'}
                    </span>
                  </td>
                </tr>
                {expandedTrip === trip.id && (
                  <tr className="border-b bg-gray-50">
                    <td colSpan={7} className="p-4">
                      {/* Payment History */}
                      <div className="bg-white rounded p-4">
                        <h4 className="font-semibold text-sm mb-3">পেমেন্ট ইতিহাস</h4>
                        {trip.payments && trip.payments.length > 0 ? (
                          <div className="space-y-2">
                            {trip.payments.map((payment) => (
                              <div key={payment.id} className="flex justify-between items-start py-2 border-b last:border-0 text-sm">
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
                              </div>
                            ))}
                          </div>
                        ) : (
                          <p className="text-gray-500 text-sm">এখনও কোনো পেমেন্ট নেই</p>
                        )}
                      </div>
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </div>

      {/* Trips Cards - Mobile */}
      <div className="md:hidden space-y-4">
        {settlements.map((trip) => (
          <div key={trip.id} className="bg-white border rounded-lg overflow-hidden">
            <div
              className="p-4 cursor-pointer hover:bg-gray-50"
              onClick={() => setExpandedTrip(expandedTrip === trip.id ? null : trip.id)}
            >
              <div className="flex justify-between items-start mb-3">
                <div>
                  <div className="font-medium">
                    {trip.customer_first_name} {trip.customer_last_name}
                  </div>
                  <div className="text-xs text-gray-500">
                    {trip.pickup_location} → {trip.dropoff_location}
                  </div>
                </div>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                  trip.payment_status === 'paid' ? 'bg-green-100 text-green-800' :
                  trip.payment_status === 'partial' ? 'bg-blue-100 text-blue-800' :
                  'bg-yellow-100 text-yellow-800'
                }`}>
                  {trip.payment_status === 'paid' ? 'পরিশোধিত' :
                   trip.payment_status === 'partial' ? 'আংশিক' :
                   'অপেক্ষমান'}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-2 text-sm py-2 border-y">
                <div>
                  <div className="text-gray-500">কমিশন</div>
                  <div className="font-medium">৳{trip.driver_commission.toFixed(2)}</div>
                </div>
                <div>
                  <div className="text-gray-500">পাওয়া</div>
                  <div className="font-medium">৳{trip.paid_amount.toFixed(2)}</div>
                </div>
                <div>
                  <div className="text-gray-500">খরচ</div>
                  <div className="font-medium">৳{trip.total_expenses.toFixed(2)}</div>
                </div>
                <div>
                  <div className="text-gray-500">বাকি</div>
                  <div className="font-medium text-yellow-600">৳{trip.remaining_amount.toFixed(2)}</div>
                </div>
              </div>
            </div>

            {/* Expanded Payment History */}
            {expandedTrip === trip.id && (
              <div className="bg-gray-50 p-4 border-t">
                <h4 className="font-semibold text-sm mb-3">পেমেন্ট ইতিহাস</h4>
                {trip.payments && trip.payments.length > 0 ? (
                  <div className="space-y-2">
                    {trip.payments.map((payment) => (
                      <div key={payment.id} className="bg-white p-3 rounded text-sm">
                        <div className="font-medium">৳{payment.amount.toFixed(2)}</div>
                        <div className="text-xs text-gray-600">
                          {payment.payment_method && `${payment.payment_method} • `}
                          {new Date(payment.payment_date).toLocaleDateString('bn-BD')}
                        </div>
                        {payment.payment_notes && (
                          <div className="text-xs text-gray-500 mt-1">{payment.payment_notes}</div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-gray-500 text-sm">এখনও কোনো পেমেন্ট নেই</p>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      {settlements.length === 0 && (
        <div className="text-center py-12 text-gray-500">
          <p className="text-lg">এখনও কোনো ট্রিপ নেই</p>
        </div>
      )}
    </div>
  );
};

export default DriverDashboard;
