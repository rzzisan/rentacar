import React, { useEffect, useState, useCallback } from 'react'
import { api } from '../../api/client'
import type { Customer, CustomerDetail } from '../../types'

// ── helpers ──────────────────────────────────────────────────────────────────

function fmtBDT(n?: number) {
  if (n == null) return '৳০'
  return '৳' + n.toLocaleString('en-BD')
}

function fmtDate(d?: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('bn-BD', { year: 'numeric', month: 'short', day: 'numeric' })
}

const statusLabel: Record<string, string> = {
  pending: 'অপেক্ষমান', active: 'চলমান', completed: 'সম্পন্ন', cancelled: 'বাতিল',
}
const payLabel: Record<string, string> = {
  pending: 'বকেয়া', paid: 'পরিশোধিত', partial: 'আংশিক', refunded: 'ফেরত',
}
const statusColor: Record<string, string> = {
  pending: 'bg-yellow-100 text-yellow-800',
  active: 'bg-emerald-100 text-emerald-800',
  completed: 'bg-indigo-100 text-indigo-800',
  cancelled: 'bg-red-100 text-red-800',
}
const payColor: Record<string, string> = {
  pending: 'bg-red-100 text-red-700',
  paid: 'bg-emerald-100 text-emerald-800',
  partial: 'bg-orange-100 text-orange-800',
  refunded: 'bg-gray-100 text-gray-600',
}

const emptyForm = {
  first_name: '', last_name: '', phone: '', email: '',
  nid: '', address: '', city: '', license_number: '', license_expiry: '',
}

type FormData = typeof emptyForm

// ── main component ────────────────────────────────────────────────────────────

export default function AdminCustomers() {
  const [customers, setCustomers]       = useState<Customer[]>([])
  const [loading, setLoading]           = useState(true)
  const [search, setSearch]             = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [saving, setSaving]             = useState(false)
  const [error, setError]               = useState('')

  // modals
  const [showCreate, setShowCreate]     = useState(false)
  const [editTarget, setEditTarget]     = useState<Customer | null>(null)
  const [detail, setDetail]             = useState<CustomerDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  // form
  const [form, setForm] = useState<FormData>(emptyForm)
  const [formErr, setFormErr] = useState('')

  const load = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams()
    if (search)       params.set('search', search)
    if (filterStatus) params.set('status', filterStatus)
    api.get<Customer[]>(`/admin/customers/index.php?${params}`)
      .then(r => { if (r.success) setCustomers(r.data ?? []) })
      .finally(() => setLoading(false))
  }, [search, filterStatus])

  useEffect(() => { load() }, [load])

  // ── detail modal ─────────────────────────────────────────────────────────
  const openDetail = (c: Customer) => {
    setDetail(null)
    setDetailLoading(true)
    api.get<CustomerDetail>(`/admin/customers/show.php?id=${c.id}`)
      .then(r => { if (r.success && r.data) setDetail(r.data) })
      .finally(() => setDetailLoading(false))
  }

  // ── create modal ─────────────────────────────────────────────────────────
  const openCreate = () => {
    setForm(emptyForm)
    setFormErr('')
    setShowCreate(true)
  }
  const handleCreate = async () => {
    if (!form.first_name.trim() || !form.phone.trim()) {
      setFormErr('নাম ও মোবাইল নম্বর আবশ্যক')
      return
    }
    setSaving(true); setFormErr('')
    const r = await api.post<{ id: number }>('/admin/customers/index.php', form)
    setSaving(false)
    if (r.success) { setShowCreate(false); load() }
    else setFormErr(r.message ?? 'ব্যর্থ হয়েছে')
  }

  // ── edit modal ───────────────────────────────────────────────────────────
  const openEdit = (c: Customer) => {
    setEditTarget(c)
    setForm({
      first_name: c.first_name, last_name: c.last_name ?? '',
      phone: c.phone, email: c.email ?? '',
      nid: c.nid ?? '', address: c.address ?? '',
      city: c.city ?? '', license_number: c.license_number ?? '',
      license_expiry: c.license_expiry ?? '',
    })
    setFormErr('')
  }
  const handleEdit = async () => {
    if (!editTarget) return
    if (!form.first_name.trim() || !form.phone.trim()) {
      setFormErr('নাম ও মোবাইল নম্বর আবশ্যক')
      return
    }
    setSaving(true); setFormErr('')
    const r = await api.put(`/admin/customers/update.php?id=${editTarget.id}`, form)
    setSaving(false)
    if (r.success) { setEditTarget(null); load() }
    else setFormErr(r.message ?? 'ব্যর্থ হয়েছে')
  }

  // ── status toggle ─────────────────────────────────────────────────────────
  const toggleStatus = async (c: Customer) => {
    const next = c.status === 'active' ? 'inactive' : 'active'
    const r = await api.put(`/admin/customers/status.php?id=${c.id}`, { status: next })
    if (r.success) load()
    else setError(r.message ?? 'ব্যর্থ হয়েছে')
  }

  // ── field helper ──────────────────────────────────────────────────────────
  const field = (key: keyof FormData, label: string, type = 'text', placeholder = '') => (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input
        type={type}
        value={form[key]}
        onChange={e => setForm(p => ({ ...p, [key]: e.target.value }))}
        placeholder={placeholder}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
      />
    </div>
  )

  // ─────────────────────────────────────────────────────────────────────────
  return (
    <div className="p-4 sm:p-6 space-y-5">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-gray-900">গ্রাহক ব্যবস্থাপনা</h1>
          <p className="text-sm text-gray-500 mt-0.5">{customers.length}জন গ্রাহক</p>
        </div>
        <button
          onClick={openCreate}
          className="inline-flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-indigo-700 transition-colors"
        >
          <span className="text-base">+</span> নতুন গ্রাহক
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
          {error}
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <input
          type="text"
          placeholder="নাম, মোবাইল বা NID দিয়ে খুঁজুন..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <select
          value={filterStatus}
          onChange={e => setFilterStatus(e.target.value)}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
        >
          <option value="">সব গ্রাহক</option>
          <option value="active">সক্রিয়</option>
          <option value="inactive">নিষ্ক্রিয়</option>
        </select>
      </div>

      {/* Table */}
      {loading ? (
        <div className="space-y-3">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="h-16 bg-gray-100 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : customers.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <div className="text-4xl mb-3">👥</div>
          <p>কোনো গ্রাহক পাওয়া যায়নি</p>
        </div>
      ) : (
        <>
          {/* Desktop table */}
          <div className="hidden md:block bg-white rounded-xl border border-gray-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3 font-semibold text-gray-600">নাম</th>
                  <th className="text-left px-4 py-3 font-semibold text-gray-600">মোবাইল</th>
                  <th className="text-left px-4 py-3 font-semibold text-gray-600">NID</th>
                  <th className="text-center px-4 py-3 font-semibold text-gray-600">মোট ট্রিপ</th>
                  <th className="text-right px-4 py-3 font-semibold text-gray-600">মোট ব্যয়</th>
                  <th className="text-left px-4 py-3 font-semibold text-gray-600">সর্বশেষ ট্রিপ</th>
                  <th className="text-center px-4 py-3 font-semibold text-gray-600">অবস্থা</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {customers.map(c => (
                  <tr key={c.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <button
                        onClick={() => openDetail(c)}
                        className="font-semibold text-indigo-600 hover:text-indigo-800 text-left"
                      >
                        {c.first_name} {c.last_name}
                      </button>
                    </td>
                    <td className="px-4 py-3 text-gray-700">{c.phone}</td>
                    <td className="px-4 py-3 text-gray-500">{c.nid || '—'}</td>
                    <td className="px-4 py-3 text-center">
                      <span className="font-semibold text-gray-900">{c.total_trips ?? 0}</span>
                      <span className="text-gray-400 text-xs ml-1">({c.completed_trips ?? 0} সম্পন্ন)</span>
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-gray-900">
                      {fmtBDT(c.total_spent)}
                    </td>
                    <td className="px-4 py-3 text-gray-500">{fmtDate(c.last_trip_date)}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-semibold ${
                        c.status === 'active' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-700'
                      }`}>
                        {c.status === 'active' ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2 justify-end">
                        <button
                          onClick={() => openDetail(c)}
                          className="text-indigo-600 hover:text-indigo-800 text-xs font-medium"
                        >
                          বিস্তারিত
                        </button>
                        <button
                          onClick={() => openEdit(c)}
                          className="text-gray-500 hover:text-gray-700 text-xs font-medium"
                        >
                          সম্পাদনা
                        </button>
                        <button
                          onClick={() => toggleStatus(c)}
                          className={`text-xs font-medium ${
                            c.status === 'active'
                              ? 'text-red-500 hover:text-red-700'
                              : 'text-emerald-600 hover:text-emerald-800'
                          }`}
                        >
                          {c.status === 'active' ? 'নিষ্ক্রিয়' : 'সক্রিয়'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile cards */}
          <div className="md:hidden space-y-3">
            {customers.map(c => (
              <div
                key={c.id}
                className="bg-white rounded-xl border border-gray-200 p-4"
              >
                <div className="flex justify-between items-start">
                  <div>
                    <button
                      onClick={() => openDetail(c)}
                      className="font-bold text-gray-900 text-base text-left"
                    >
                      {c.first_name} {c.last_name}
                    </button>
                    <p className="text-sm text-gray-500 mt-0.5">{c.phone}</p>
                    {c.nid && <p className="text-xs text-gray-400">NID: {c.nid}</p>}
                  </div>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
                    c.status === 'active' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-700'
                  }`}>
                    {c.status === 'active' ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
                  </span>
                </div>
                <div className="flex items-center gap-4 mt-3 text-sm text-gray-600">
                  <span><span className="font-semibold text-gray-900">{c.total_trips ?? 0}</span> ট্রিপ</span>
                  <span className="font-semibold text-gray-900">{fmtBDT(c.total_spent)}</span>
                  {c.last_trip_date && <span className="text-gray-400 text-xs">{fmtDate(c.last_trip_date)}</span>}
                </div>
                <div className="flex gap-4 mt-3 pt-3 border-t border-gray-100">
                  <button onClick={() => openDetail(c)} className="text-indigo-600 text-sm font-medium">বিস্তারিত</button>
                  <button onClick={() => openEdit(c)} className="text-gray-500 text-sm font-medium">সম্পাদনা</button>
                  <button
                    onClick={() => toggleStatus(c)}
                    className={`text-sm font-medium ${c.status === 'active' ? 'text-red-500' : 'text-emerald-600'}`}
                  >
                    {c.status === 'active' ? 'নিষ্ক্রিয় করুন' : 'সক্রিয় করুন'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* ── Create Modal ──────────────────────────────────────────────────── */}
      {showCreate && (
        <Modal title="নতুন গ্রাহক যোগ করুন" onClose={() => setShowCreate(false)}>
          <CustomerForm
            form={form} setForm={setForm} field={field}
            formErr={formErr} saving={saving}
            onSubmit={handleCreate} onCancel={() => setShowCreate(false)}
            submitLabel="গ্রাহক তৈরি করুন"
          />
        </Modal>
      )}

      {/* ── Edit Modal ────────────────────────────────────────────────────── */}
      {editTarget && (
        <Modal title="গ্রাহক তথ্য সম্পাদনা" onClose={() => setEditTarget(null)}>
          <CustomerForm
            form={form} setForm={setForm} field={field}
            formErr={formErr} saving={saving}
            onSubmit={handleEdit} onCancel={() => setEditTarget(null)}
            submitLabel="আপডেট করুন"
          />
        </Modal>
      )}

      {/* ── Detail Modal ──────────────────────────────────────────────────── */}
      {(detail || detailLoading) && (
        <Modal
          title={detail ? `${detail.customer.first_name} ${detail.customer.last_name}` : 'লোড হচ্ছে...'}
          onClose={() => { setDetail(null); setDetailLoading(false) }}
          wide
        >
          {detailLoading && (
            <div className="flex items-center justify-center h-40 text-gray-400">লোড হচ্ছে...</div>
          )}
          {detail && <CustomerDetail data={detail} onEdit={() => { setDetail(null); openEdit(detail.customer) }} />}
        </Modal>
      )}
    </div>
  )
}

// ── CustomerForm ──────────────────────────────────────────────────────────────

interface FormProps {
  form: FormData
  setForm: React.Dispatch<React.SetStateAction<FormData>>
  field: (key: keyof FormData, label: string, type?: string, placeholder?: string) => React.ReactElement
  formErr: string
  saving: boolean
  onSubmit: () => void
  onCancel: () => void
  submitLabel: string
}

function CustomerForm({ form, setForm, field, formErr, saving, onSubmit, onCancel, submitLabel }: FormProps) {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {field('first_name', 'প্রথম নাম *', 'text', 'রাকিব')}
        {field('last_name',  'শেষ নাম',    'text', 'আহমেদ')}
        {field('phone', 'মোবাইল নম্বর *', 'tel', '01XXXXXXXXX')}
        {field('email', 'ইমেইল', 'email', 'example@mail.com')}
        {field('nid', 'NID নম্বর', 'text', '১৯৯৩XXXXXXXXX')}
        {field('city', 'শহর', 'text', 'ঢাকা')}
        {field('license_number', 'লাইসেন্স নম্বর', 'text', '')}
        {field('license_expiry', 'লাইসেন্স মেয়াদ', 'date', '')}
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">ঠিকানা</label>
        <textarea
          value={form.address}
          onChange={e => setForm(p => ({ ...p, address: e.target.value }))}
          rows={2}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
          placeholder="সম্পূর্ণ ঠিকানা"
        />
      </div>

      {formErr && <p className="text-red-600 text-sm">{formErr}</p>}

      <div className="flex justify-end gap-3 pt-2">
        <button
          onClick={onCancel}
          className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
        >
          বাতিল
        </button>
        <button
          onClick={onSubmit}
          disabled={saving}
          className="px-5 py-2 text-sm font-semibold text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 disabled:opacity-50"
        >
          {saving ? 'সংরক্ষণ হচ্ছে...' : submitLabel}
        </button>
      </div>
    </div>
  )
}

// ── CustomerDetail ────────────────────────────────────────────────────────────

function CustomerDetail({ data, onEdit }: { data: CustomerDetail; onEdit: () => void }) {
  const { customer: c, stats, rentals } = data

  return (
    <div className="space-y-5">
      {/* Profile + Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* Profile */}
        <div className="bg-gray-50 rounded-xl p-4 space-y-2 text-sm">
          <div className="flex justify-between items-center mb-3">
            <span className="font-bold text-gray-900 text-base">{c.first_name} {c.last_name}</span>
            <button
              onClick={onEdit}
              className="text-indigo-600 text-xs font-medium hover:text-indigo-800"
            >
              সম্পাদনা
            </button>
          </div>
          <Row label="মোবাইল" value={c.phone} />
          {c.email && <Row label="ইমেইল" value={c.email} />}
          {c.nid && <Row label="NID" value={c.nid} />}
          {c.address && <Row label="ঠিকানা" value={[c.address, c.city].filter(Boolean).join(', ')} />}
          {c.license_number && <Row label="লাইসেন্স" value={c.license_number} />}
          {c.license_expiry && <Row label="মেয়াদ" value={fmtDate(c.license_expiry)} />}
          <Row label="নিবন্ধন" value={fmtDate(c.created_at)} />
          <Row
            label="অবস্থা"
            value={
              <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-semibold ${
                c.status === 'active' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-700'
              }`}>
                {c.status === 'active' ? 'সক্রিয়' : 'নিষ্ক্রিয়'}
              </span>
            }
          />
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 gap-3">
          <StatCard label="মোট ট্রিপ" value={String(stats.total_trips)} color="indigo" />
          <StatCard label="সম্পন্ন ট্রিপ" value={String(stats.completed_trips)} color="emerald" />
          <StatCard label="মোট ব্যয়" value={fmtBDT(stats.total_spent)} color="violet" />
          <StatCard label="বকেয়া পেমেন্ট" value={`${stats.pending_payment_trips}টি`} color="orange" />
        </div>
      </div>

      {/* Rental history */}
      <div>
        <h3 className="font-bold text-gray-800 mb-3">ট্রিপ ইতিহাস</h3>
        {rentals.length === 0 ? (
          <p className="text-gray-400 text-sm text-center py-8">কোনো ট্রিপ নেই</p>
        ) : (
          <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
            {rentals.map(r => (
              <div key={r.id} className="bg-white border border-gray-200 rounded-lg p-3 text-sm">
                <div className="flex justify-between items-start gap-2">
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-gray-900 truncate">
                      {r.pickup_location || '?'} → {r.dropoff_location || '?'}
                    </p>
                    <p className="text-gray-400 text-xs mt-0.5">
                      {fmtDate(r.start_date)}
                      {r.vehicle_brand && ` • ${r.vehicle_brand} ${r.vehicle_model}`}
                      {r.driver_name && ` • ${r.driver_name}`}
                    </p>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="font-bold text-gray-900">{fmtBDT(r.agreed_amount)}</p>
                    <div className="flex gap-1 mt-1 justify-end">
                      <span className={`px-1.5 py-0.5 rounded text-[11px] font-semibold ${statusColor[r.rental_status] ?? ''}`}>
                        {statusLabel[r.rental_status] ?? r.rental_status}
                      </span>
                      <span className={`px-1.5 py-0.5 rounded text-[11px] font-semibold ${payColor[r.payment_status] ?? ''}`}>
                        {payLabel[r.payment_status] ?? r.payment_status}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

// ── small components ──────────────────────────────────────────────────────────

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-2">
      <span className="text-gray-500 flex-shrink-0">{label}</span>
      <span className="text-gray-900 text-right">{value}</span>
    </div>
  )
}

const statColors = {
  indigo: 'bg-indigo-50 text-indigo-700',
  emerald: 'bg-emerald-50 text-emerald-700',
  violet: 'bg-violet-50 text-violet-700',
  orange: 'bg-orange-50 text-orange-700',
}
function StatCard({ label, value, color }: { label: string; value: string; color: keyof typeof statColors }) {
  return (
    <div className={`rounded-xl p-4 ${statColors[color]}`}>
      <p className="text-xs font-medium opacity-70">{label}</p>
      <p className="text-xl font-extrabold mt-1">{value}</p>
    </div>
  )
}

function Modal({ title, onClose, children, wide }: {
  title: string
  onClose: () => void
  children: React.ReactNode
  wide?: boolean
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center p-4 pt-16 overflow-y-auto">
      <div className="fixed inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className={`relative bg-white rounded-2xl shadow-xl w-full ${wide ? 'max-w-2xl' : 'max-w-lg'}`}>
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-bold text-gray-900">{title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">×</button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  )
}
