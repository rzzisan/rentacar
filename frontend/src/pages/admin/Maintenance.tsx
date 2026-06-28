import React, { useState, useEffect, useCallback } from 'react';
import { api } from '@/api/client';

// ── Types ──────────────────────────────────────────────────────────────────

interface MaintenanceRecord {
  id: number;
  vehicle_id: number;
  vehicle: string;
  registration_number: string;
  maintenance_type: string;
  description: string | null;
  vendor: string | null;
  start_date: string;
  end_date: string | null;
  km_reading: number | null;
  next_due_date: string | null;
  next_due_km: number | null;
  cost: number | null;
  status: string;
  created_at: string;
}

interface VehicleDocument {
  id: number;
  vehicle_id: number;
  vehicle: string;
  registration_number: string;
  doc_type: string;
  doc_number: string | null;
  issue_date: string;
  expiry_date: string;
  days_remaining: number;
  note: string | null;
}

interface Vehicle {
  id: number;
  brand: string;
  model: string;
  registration_number: string;
}

// ── Constants ──────────────────────────────────────────────────────────────

const MAINT_TYPES: Record<string, string> = {
  oil_change: 'তেল পরিবর্তন',
  tire: 'টায়ার',
  brake: 'ব্রেক',
  engine: 'ইঞ্জিন',
  battery: 'ব্যাটারি',
  ac: 'এসি',
  body_repair: 'বডি মেরামত',
  electrical: 'বৈদ্যুতিক',
  inspection: 'পরিদর্শন',
  other: 'অন্যান্য',
};

const STATUS_LABELS: Record<string, string> = {
  pending: 'অপেক্ষমাণ',
  in_progress: 'চলছে',
  completed: 'সম্পন্ন',
};

const STATUS_COLORS: Record<string, string> = {
  pending: 'bg-yellow-100 text-yellow-700',
  in_progress: 'bg-blue-100 text-blue-700',
  completed: 'bg-green-100 text-green-700',
};

const DOC_TYPES: Record<string, string> = {
  fitness: 'ফিটনেস সার্টিফিকেট',
  insurance: 'বীমা',
  tax_token: 'ট্যাক্স টোকেন',
  route_permit: 'রুট পারমিট',
  registration: 'রেজিস্ট্রেশন',
  other: 'অন্যান্য',
};

const BDT = (n: number) => `৳${n.toLocaleString('en-BD', { maximumFractionDigits: 0 })}`;

function expiryColor(days: number) {
  if (days < 0) return 'bg-red-100 text-red-700 border-red-200';
  if (days <= 15) return 'bg-red-50 text-red-600 border-red-100';
  if (days <= 30) return 'bg-orange-50 text-orange-600 border-orange-100';
  if (days <= 60) return 'bg-yellow-50 text-yellow-700 border-yellow-100';
  return 'bg-green-50 text-green-700 border-green-100';
}

function expiryLabel(days: number) {
  if (days < 0) return `${Math.abs(days)} দিন আগে মেয়াদ শেষ`;
  if (days === 0) return 'আজ মেয়াদ শেষ!';
  return `${days} দিন বাকি`;
}

// ── Maintenance Form ───────────────────────────────────────────────────────

interface MaintForm {
  vehicle_id: number | '';
  maintenance_type: string;
  description: string;
  vendor: string;
  start_date: string;
  end_date: string;
  km_reading: string;
  next_due_date: string;
  next_due_km: string;
  cost: string;
  status: string;
}

const emptyMaintForm = (): MaintForm => ({
  vehicle_id: '', maintenance_type: 'oil_change', description: '', vendor: '',
  start_date: new Date().toISOString().slice(0,10), end_date: '', km_reading: '',
  next_due_date: '', next_due_km: '', cost: '', status: 'pending',
});

// ── Document Form ──────────────────────────────────────────────────────────

interface DocForm {
  vehicle_id: number | '';
  doc_type: string;
  doc_number: string;
  issue_date: string;
  expiry_date: string;
  note: string;
}

const emptyDocForm = (): DocForm => ({
  vehicle_id: '', doc_type: 'fitness', doc_number: '',
  issue_date: new Date().toISOString().slice(0,10), expiry_date: '', note: '',
});

// ── Main Component ─────────────────────────────────────────────────────────

type Tab = 'maintenance' | 'documents';

export default function AdminMaintenance(): React.ReactElement {
  const [activeTab, setTab] = useState<Tab>('maintenance');

  const [records, setRecords]   = useState<MaintenanceRecord[]>([]);
  const [documents, setDocuments] = useState<VehicleDocument[]>([]);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading]   = useState(true);
  const [saving, setSaving]     = useState(false);
  const [msg, setMsg]           = useState('');
  const [isError, setIsError]   = useState(false);

  // Maintenance
  const [showMaintModal, setShowMaintModal] = useState(false);
  const [editingMaint, setEditingMaint]     = useState<MaintenanceRecord | null>(null);
  const [maintForm, setMaintForm]           = useState<MaintForm>(emptyMaintForm());
  const [vehicleFilter, setVehicleFilter]   = useState(0);
  const [statusFilter, setStatusFilter]     = useState('');

  // Documents
  const [showDocModal, setShowDocModal]   = useState(false);
  const [editingDoc, setEditingDoc]       = useState<VehicleDocument | null>(null);
  const [docForm, setDocForm]             = useState<DocForm>(emptyDocForm());

  const flash = (text: string, err = false) => { setMsg(text); setIsError(err); setTimeout(() => setMsg(''), 3500); };

  const loadAll = useCallback(() => {
    setLoading(true);
    const q = vehicleFilter ? `?vehicle_id=${vehicleFilter}` : '';
    const sq = statusFilter ? `${q ? '&' : '?'}status=${statusFilter}` : '';
    Promise.all([
      api.get<MaintenanceRecord[]>(`/admin/maintenance/index.php${q}${sq}`),
      api.get<VehicleDocument[]>(`/admin/documents/index.php`),
      api.get<Vehicle[]>(`/vehicles/index.php`),
    ]).then(([mr, dr, vr]) => {
      if (mr.success) setRecords(mr.data ?? []);
      if (dr.success) setDocuments(dr.data ?? []);
      if (vr.success) setVehicles(vr.data ?? []);
    }).finally(() => setLoading(false));
  }, [vehicleFilter, statusFilter]);

  useEffect(() => { loadAll(); }, [loadAll]);

  // ── Maintenance handlers ───────────────────────────────────────────────

  function openAddMaint() {
    setEditingMaint(null);
    setMaintForm(emptyMaintForm());
    setShowMaintModal(true);
  }

  function openEditMaint(r: MaintenanceRecord) {
    setEditingMaint(r);
    setMaintForm({
      vehicle_id: r.vehicle_id,
      maintenance_type: r.maintenance_type,
      description: r.description ?? '',
      vendor: r.vendor ?? '',
      start_date: r.start_date,
      end_date: r.end_date ?? '',
      km_reading: r.km_reading?.toString() ?? '',
      next_due_date: r.next_due_date ?? '',
      next_due_km: r.next_due_km?.toString() ?? '',
      cost: r.cost?.toString() ?? '',
      status: r.status,
    });
    setShowMaintModal(true);
  }

  async function saveMaint() {
    if (!maintForm.vehicle_id || !maintForm.start_date) { flash('গাড়ি ও তারিখ আবশ্যক', true); return; }
    setSaving(true);
    try {
      const body = { ...maintForm, vehicle_id: Number(maintForm.vehicle_id), km_reading: maintForm.km_reading || null, next_due_km: maintForm.next_due_km || null, cost: maintForm.cost || null };
      const res = editingMaint
        ? await api.put(`/admin/maintenance/update.php?id=${editingMaint.id}`, body)
        : await api.post('/admin/maintenance/index.php', body);
      if (res.success) { flash(res.message || 'সংরক্ষিত হয়েছে'); setShowMaintModal(false); loadAll(); }
      else flash(res.message || 'ব্যর্থ হয়েছে', true);
    } catch { flash('সংযোগ সমস্যা', true); }
    finally { setSaving(false); }
  }

  async function deleteMaint(id: number) {
    if (!confirm('এই রেকর্ডটি মুছে ফেলবেন?')) return;
    const res = await api.delete(`/admin/maintenance/destroy.php?id=${id}`);
    if (res.success) { flash('মুছে ফেলা হয়েছে'); loadAll(); }
    else flash(res.message || 'ব্যর্থ', true);
  }

  // ── Document handlers ──────────────────────────────────────────────────

  function openAddDoc() {
    setEditingDoc(null);
    setDocForm(emptyDocForm());
    setShowDocModal(true);
  }

  function openEditDoc(d: VehicleDocument) {
    setEditingDoc(d);
    setDocForm({
      vehicle_id: d.vehicle_id, doc_type: d.doc_type, doc_number: d.doc_number ?? '',
      issue_date: d.issue_date, expiry_date: d.expiry_date, note: d.note ?? '',
    });
    setShowDocModal(true);
  }

  async function saveDoc() {
    if (!docForm.vehicle_id || !docForm.expiry_date) { flash('গাড়ি ও মেয়াদ তারিখ আবশ্যক', true); return; }
    setSaving(true);
    try {
      const body = { ...docForm, vehicle_id: Number(docForm.vehicle_id) };
      const res = await api.post('/admin/documents/index.php', body);
      if (res.success) { flash(res.message || 'সংরক্ষিত হয়েছে'); setShowDocModal(false); loadAll(); }
      else flash(res.message || 'ব্যর্থ হয়েছে', true);
    } catch { flash('সংযোগ সমস্যা', true); }
    finally { setSaving(false); }
  }

  async function deleteDoc(id: number) {
    if (!confirm('এই ডকুমেন্টটি মুছে ফেলবেন?')) return;
    const res = await api.delete(`/admin/documents/destroy.php?id=${id}`);
    if (res.success) { flash('মুছে ফেলা হয়েছে'); loadAll(); }
    else flash(res.message || 'ব্যর্থ', true);
  }

  // ── Expiry alerts ──────────────────────────────────────────────────────

  const urgentDocs = documents.filter(d => d.days_remaining <= 30).sort((a, b) => a.days_remaining - b.days_remaining);
  const totalMaintCost = records.filter(r => r.status === 'completed' && r.cost).reduce((s, r) => s + (r.cost ?? 0), 0);

  return (
    <div className="p-4 sm:p-6 space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">রক্ষণাবেক্ষণ ও ডকুমেন্ট</h1>
          <p className="text-gray-500 text-sm mt-0.5">গাড়ির সার্ভিস, বীমা ও কাগজপত্র ট্র্যাক করুন</p>
        </div>
        <div className="flex gap-2">
          {activeTab === 'maintenance'
            ? <button onClick={openAddMaint} className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-indigo-700">+ সার্ভিস যোগ</button>
            : <button onClick={openAddDoc} className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-indigo-700">+ ডকুমেন্ট যোগ</button>
          }
        </div>
      </div>

      {/* Flash message */}
      {msg && (
        <div className={`rounded-lg px-4 py-3 text-sm font-medium ${isError ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-green-50 text-green-700 border border-green-200'}`}>
          {msg}
        </div>
      )}

      {/* Urgent expiry alerts */}
      {urgentDocs.length > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-2">
          <div className="text-sm font-bold text-amber-800">⚠️ মেয়াদ শেষ হচ্ছে ({urgentDocs.length}টি)</div>
          <div className="flex flex-wrap gap-2">
            {urgentDocs.map(d => (
              <span key={d.id} className={`text-xs font-medium px-2.5 py-1 rounded-full border ${expiryColor(d.days_remaining)}`}>
                {d.vehicle} · {DOC_TYPES[d.doc_type] ?? d.doc_type} · {expiryLabel(d.days_remaining)}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Summary */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="bg-white border rounded-xl p-4">
          <div className="text-xs text-gray-500">মোট রক্ষণাবেক্ষণ</div>
          <div className="text-2xl font-bold text-gray-800 mt-1">{records.length}</div>
        </div>
        <div className="bg-white border rounded-xl p-4">
          <div className="text-xs text-gray-500">সম্পন্ন খরচ</div>
          <div className="text-xl font-bold text-red-600 mt-1">{BDT(totalMaintCost)}</div>
        </div>
        <div className="bg-white border rounded-xl p-4">
          <div className="text-xs text-gray-500">ডকুমেন্ট সংখ্যা</div>
          <div className="text-2xl font-bold text-gray-800 mt-1">{documents.length}</div>
        </div>
        <div className="bg-white border rounded-xl p-4">
          <div className="text-xs text-gray-500">মেয়াদ সংকটাপন্ন</div>
          <div className={`text-2xl font-bold mt-1 ${urgentDocs.length > 0 ? 'text-red-600' : 'text-gray-400'}`}>{urgentDocs.length}</div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-xl border shadow-sm overflow-hidden">
        <div className="flex border-b">
          {([['maintenance', '🔧 সার্ভিস রেকর্ড'], ['documents', '📄 ডকুমেন্ট']] as [Tab, string][]).map(([key, label]) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={`px-5 py-3 text-sm font-medium transition-colors ${
                activeTab === key ? 'text-indigo-600 border-b-2 border-indigo-600 bg-indigo-50/50' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {/* ── Maintenance Tab ─────────────────────────────────────────── */}
        {activeTab === 'maintenance' && (
          <>
            {/* Filters */}
            <div className="flex gap-3 p-4 border-b flex-wrap">
              <select
                value={vehicleFilter}
                onChange={e => setVehicleFilter(+e.target.value)}
                className="border rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value={0}>সব গাড়ি</option>
                {vehicles.map(v => <option key={v.id} value={v.id}>{v.brand} {v.model} ({v.registration_number})</option>)}
              </select>
              <select
                value={statusFilter}
                onChange={e => setStatusFilter(e.target.value)}
                className="border rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">সব স্ট্যাটাস</option>
                {Object.entries(STATUS_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>

            {loading ? (
              <div className="p-8 text-center text-gray-400">লোড হচ্ছে...</div>
            ) : records.length === 0 ? (
              <div className="p-8 text-center text-gray-400">কোনো রেকর্ড নেই। "+ সার্ভিস যোগ" করুন।</div>
            ) : (
              <div className="divide-y">
                {records.map(r => (
                  <div key={r.id} className="p-4 hover:bg-gray-50 flex flex-col sm:flex-row sm:items-start gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-gray-800">{MAINT_TYPES[r.maintenance_type] ?? r.maintenance_type}</span>
                        <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${STATUS_COLORS[r.status]}`}>
                          {STATUS_LABELS[r.status]}
                        </span>
                      </div>
                      <div className="text-sm text-gray-600 mt-0.5">{r.vehicle} · {r.registration_number}</div>
                      {r.description && <div className="text-sm text-gray-500 mt-0.5">{r.description}</div>}
                      <div className="flex gap-4 mt-1 text-xs text-gray-400 flex-wrap">
                        <span>তারিখ: {r.start_date}{r.end_date ? ` → ${r.end_date}` : ''}</span>
                        {r.km_reading && <span>কিমি: {r.km_reading.toLocaleString()}</span>}
                        {r.vendor && <span>গ্যারেজ: {r.vendor}</span>}
                        {r.cost != null && <span className="font-semibold text-red-500">খরচ: {BDT(r.cost)}</span>}
                        {r.next_due_date && <span className="text-amber-600">পরবর্তী: {r.next_due_date}</span>}
                      </div>
                    </div>
                    <div className="flex gap-2 shrink-0">
                      <button onClick={() => openEditMaint(r)} className="text-xs px-3 py-1.5 border rounded-lg hover:bg-gray-100">সম্পাদনা</button>
                      <button onClick={() => deleteMaint(r.id)} className="text-xs px-3 py-1.5 border border-red-200 text-red-600 rounded-lg hover:bg-red-50">মুছুন</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        {/* ── Documents Tab ───────────────────────────────────────────── */}
        {activeTab === 'documents' && (
          <>
            {loading ? (
              <div className="p-8 text-center text-gray-400">লোড হচ্ছে...</div>
            ) : documents.length === 0 ? (
              <div className="p-8 text-center text-gray-400">কোনো ডকুমেন্ট নেই। "+ ডকুমেন্ট যোগ" করুন।</div>
            ) : (
              <div className="divide-y">
                {documents.map(d => (
                  <div key={d.id} className={`p-4 flex flex-col sm:flex-row sm:items-start gap-3 hover:bg-gray-50 border-l-4 ${d.days_remaining < 0 ? 'border-l-red-500' : d.days_remaining <= 30 ? 'border-l-orange-400' : d.days_remaining <= 60 ? 'border-l-yellow-400' : 'border-l-green-400'}`}>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-gray-800">{DOC_TYPES[d.doc_type] ?? d.doc_type}</span>
                        <span className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${expiryColor(d.days_remaining)}`}>
                          {expiryLabel(d.days_remaining)}
                        </span>
                      </div>
                      <div className="text-sm text-gray-600 mt-0.5">{d.vehicle} · {d.registration_number}</div>
                      <div className="flex gap-4 mt-1 text-xs text-gray-400 flex-wrap">
                        {d.doc_number && <span>নং: {d.doc_number}</span>}
                        <span>ইস্যু: {d.issue_date}</span>
                        <span className={d.days_remaining <= 30 ? 'font-semibold text-red-500' : ''}>মেয়াদ: {d.expiry_date}</span>
                      </div>
                      {d.note && <div className="text-xs text-gray-400 mt-0.5">{d.note}</div>}
                    </div>
                    <div className="flex gap-2 shrink-0">
                      <button onClick={() => openEditDoc(d)} className="text-xs px-3 py-1.5 border rounded-lg hover:bg-gray-100">আপডেট</button>
                      <button onClick={() => deleteDoc(d.id)} className="text-xs px-3 py-1.5 border border-red-200 text-red-600 rounded-lg hover:bg-red-50">মুছুন</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>

      {/* ── Maintenance Modal ─────────────────────────────────────────────── */}
      {showMaintModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div className="px-5 py-4 border-b flex items-center justify-between">
              <h2 className="font-bold text-lg">{editingMaint ? 'সার্ভিস সম্পাদনা' : 'নতুন সার্ভিস রেকর্ড'}</h2>
              <button onClick={() => setShowMaintModal(false)} className="text-gray-400 hover:text-gray-600 text-xl leading-none">✕</button>
            </div>
            <div className="p-5 space-y-4">
              <div>
                <label className="text-sm font-medium text-gray-700">গাড়ি *</label>
                <select value={maintForm.vehicle_id} onChange={e => setMaintForm(f => ({ ...f, vehicle_id: +e.target.value }))}
                  className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
                  <option value="">গাড়ি নির্বাচন করুন</option>
                  {vehicles.map(v => <option key={v.id} value={v.id}>{v.brand} {v.model} ({v.registration_number})</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-sm font-medium text-gray-700">সার্ভিসের ধরন *</label>
                  <select value={maintForm.maintenance_type} onChange={e => setMaintForm(f => ({ ...f, maintenance_type: e.target.value }))}
                    className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
                    {Object.entries(MAINT_TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700">স্ট্যাটাস</label>
                  <select value={maintForm.status} onChange={e => setMaintForm(f => ({ ...f, status: e.target.value }))}
                    className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
                    {Object.entries(STATUS_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-sm font-medium text-gray-700">তারিখ *</label>
                  <input type="date" value={maintForm.start_date} onChange={e => setMaintForm(f => ({ ...f, start_date: e.target.value }))}
                    className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700">সম্পন্নের তারিখ</label>
                  <input type="date" value={maintForm.end_date} onChange={e => setMaintForm(f => ({ ...f, end_date: e.target.value }))}
                    className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-sm font-medium text-gray-700">কিমি রিডিং</label>
                  <input type="number" value={maintForm.km_reading} onChange={e => setMaintForm(f => ({ ...f, km_reading: e.target.value }))}
                    placeholder="যেমন: 45000" className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700">খরচ (টাকা)</label>
                  <input type="number" value={maintForm.cost} onChange={e => setMaintForm(f => ({ ...f, cost: e.target.value }))}
                    placeholder="যেমন: 5000" className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-700">গ্যারেজ/মেকানিক</label>
                <input type="text" value={maintForm.vendor} onChange={e => setMaintForm(f => ({ ...f, vendor: e.target.value }))}
                  placeholder="গ্যারেজের নাম বা ঠিকানা" className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-sm font-medium text-gray-700">পরবর্তী সার্ভিস তারিখ</label>
                  <input type="date" value={maintForm.next_due_date} onChange={e => setMaintForm(f => ({ ...f, next_due_date: e.target.value }))}
                    className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700">পরবর্তী কিমি</label>
                  <input type="number" value={maintForm.next_due_km} onChange={e => setMaintForm(f => ({ ...f, next_due_km: e.target.value }))}
                    placeholder="যেমন: 50000" className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-700">বিবরণ</label>
                <textarea value={maintForm.description} onChange={e => setMaintForm(f => ({ ...f, description: e.target.value }))}
                  rows={2} placeholder="বিস্তারিত বিবরণ (ঐচ্ছিক)"
                  className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none" />
              </div>
            </div>
            <div className="px-5 pb-5 flex gap-3">
              <button onClick={() => setShowMaintModal(false)} className="flex-1 border rounded-xl py-2.5 text-sm font-semibold hover:bg-gray-50">বাতিল</button>
              <button onClick={saveMaint} disabled={saving} className="flex-1 bg-indigo-600 text-white rounded-xl py-2.5 text-sm font-semibold hover:bg-indigo-700 disabled:opacity-50">
                {saving ? 'সংরক্ষণ হচ্ছে...' : 'সংরক্ষণ করুন'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Document Modal ─────────────────────────────────────────────────── */}
      {showDocModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
            <div className="px-5 py-4 border-b flex items-center justify-between">
              <h2 className="font-bold text-lg">{editingDoc ? 'ডকুমেন্ট আপডেট' : 'নতুন ডকুমেন্ট'}</h2>
              <button onClick={() => setShowDocModal(false)} className="text-gray-400 hover:text-gray-600 text-xl leading-none">✕</button>
            </div>
            <div className="p-5 space-y-4">
              <div>
                <label className="text-sm font-medium text-gray-700">গাড়ি *</label>
                <select value={docForm.vehicle_id} onChange={e => setDocForm(f => ({ ...f, vehicle_id: +e.target.value }))}
                  className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
                  <option value="">গাড়ি নির্বাচন করুন</option>
                  {vehicles.map(v => <option key={v.id} value={v.id}>{v.brand} {v.model} ({v.registration_number})</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-sm font-medium text-gray-700">ডকুমেন্টের ধরন *</label>
                  <select value={docForm.doc_type} onChange={e => setDocForm(f => ({ ...f, doc_type: e.target.value }))}
                    className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
                    {Object.entries(DOC_TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700">ডকুমেন্ট নম্বর</label>
                  <input type="text" value={docForm.doc_number} onChange={e => setDocForm(f => ({ ...f, doc_number: e.target.value }))}
                    placeholder="সার্টিফিকেট/পলিসি নং" className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-sm font-medium text-gray-700">ইস্যু তারিখ</label>
                  <input type="date" value={docForm.issue_date} onChange={e => setDocForm(f => ({ ...f, issue_date: e.target.value }))}
                    className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700">মেয়াদ উত্তীর্ণ *</label>
                  <input type="date" value={docForm.expiry_date} onChange={e => setDocForm(f => ({ ...f, expiry_date: e.target.value }))}
                    className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-700">নোট</label>
                <input type="text" value={docForm.note} onChange={e => setDocForm(f => ({ ...f, note: e.target.value }))}
                  placeholder="ঐচ্ছিক নোট" className="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
              </div>
            </div>
            <div className="px-5 pb-5 flex gap-3">
              <button onClick={() => setShowDocModal(false)} className="flex-1 border rounded-xl py-2.5 text-sm font-semibold hover:bg-gray-50">বাতিল</button>
              <button onClick={saveDoc} disabled={saving} className="flex-1 bg-indigo-600 text-white rounded-xl py-2.5 text-sm font-semibold hover:bg-indigo-700 disabled:opacity-50">
                {saving ? 'সংরক্ষণ হচ্ছে...' : 'সংরক্ষণ করুন'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
