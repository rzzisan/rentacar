import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { api } from '../api/client'

// Leaflet default icon fix for Vite bundler
delete (L.Icon.Default.prototype as any)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

const startIcon = L.divIcon({
  className: '',
  html: `<div style="width:14px;height:14px;background:#059669;border-radius:50%;border:2.5px solid white;box-shadow:0 1px 4px rgba(0,0,0,0.4)"></div>`,
  iconSize: [14, 14],
  iconAnchor: [7, 7],
})
const lastIcon = L.divIcon({
  className: '',
  html: `<div style="width:14px;height:14px;background:#DC2626;border-radius:50%;border:2.5px solid white;box-shadow:0 1px 4px rgba(0,0,0,0.4)"></div>`,
  iconSize: [14, 14],
  iconAnchor: [7, 7],
})
const midIcon = L.divIcon({
  className: '',
  html: `<div style="width:8px;height:8px;background:#4F46E5;border-radius:50%;border:1.5px solid white;box-shadow:0 1px 2px rgba(0,0,0,0.3)"></div>`,
  iconSize: [8, 8],
  iconAnchor: [4, 4],
})

interface LocationPoint {
  id: number
  lat: number
  lng: number
  accuracy: number | null
  recorded_at: string
}

interface LocationData {
  rental_id: number
  pickup_location: string | null
  dropoff_location: string | null
  rental_status: string
  actual_start_time: string | null
  actual_end_time: string | null
  driver_name: string | null
  driver_phone: string | null
  vehicle: string
  vehicle_reg: string | null
  total_points: number
  points: LocationPoint[]
}

function FitBounds({ points }: { points: [number, number][] }) {
  const map = useMap()
  useEffect(() => {
    if (points.length > 1) {
      map.fitBounds(points, { padding: [24, 24] })
    } else if (points.length === 1) {
      map.setView(points[0], 14)
    }
  }, [map, points])
  return null
}

function fmtTime(dt: string) {
  return new Date(dt).toLocaleString('bn-BD', {
    timeZone: 'Asia/Dhaka',
    month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

interface Props {
  rentalId: number
  apiPrefix: 'admin' | 'manager'
}

export default function TripLocationMap({ rentalId, apiPrefix }: Props) {
  const [data, setData]       = useState<LocationData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    api.get<LocationData>(`/${apiPrefix}/rentals/locations.php?id=${rentalId}`)
      .then(res => {
        if (res.success && res.data) setData(res.data)
        else setError(res.message || 'লোকেশন লোড ব্যর্থ')
      })
      .catch(() => setError('সার্ভার সংযোগ ব্যর্থ'))
      .finally(() => setLoading(false))
  }, [rentalId, apiPrefix])

  if (loading) return (
    <div className="flex items-center justify-center h-48 text-gray-500 text-sm">
      লোকেশন লোড হচ্ছে...
    </div>
  )

  if (error) return (
    <div className="flex items-center justify-center h-48 text-red-500 text-sm">{error}</div>
  )

  if (!data || data.points.length === 0) return (
    <div className="flex flex-col items-center justify-center h-48 text-gray-400 text-sm gap-2">
      <span className="text-3xl">📍</span>
      <p>এই ট্রিপের কোনো লোকেশন রেকর্ড নেই</p>
      {data?.rental_status === 'active' && (
        <p className="text-xs">ট্রিপ চলছে — ড্রাইভার অ্যাপ থেকে লোকেশন আসবে</p>
      )}
    </div>
  )

  const polyline: [number, number][] = data.points.map(p => [p.lat, p.lng])
  const first = data.points[0]
  const last  = data.points[data.points.length - 1]
  const mids  = data.points.slice(1, -1)
  const center: [number, number] = [first.lat, first.lng]

  return (
    <div className="space-y-3">
      {/* Stats bar */}
      <div className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-gray-600">
        <span>
          <span className="font-semibold text-gray-800">{data.total_points}</span>টি পয়েন্ট
        </span>
        <span>
          শুরু: <span className="font-medium">{fmtTime(first.recorded_at)}</span>
        </span>
        <span>
          শেষ: <span className="font-medium">{fmtTime(last.recorded_at)}</span>
        </span>
        {data.driver_name && (
          <span>চালক: <span className="font-medium">{data.driver_name}</span></span>
        )}
      </div>

      {/* Legend */}
      <div className="flex gap-4 text-xs text-gray-500">
        <span className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 rounded-full bg-emerald-600 border border-white shadow-sm"></span>
          শুরু
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-block w-2 h-2 rounded-full bg-indigo-600 border border-white shadow-sm"></span>
          মধ্যবর্তী
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 rounded-full bg-red-600 border border-white shadow-sm"></span>
          সর্বশেষ
        </span>
      </div>

      {/* Map */}
      <div className="rounded-xl overflow-hidden border border-gray-200" style={{ height: '380px' }}>
        <MapContainer
          center={center}
          zoom={12}
          style={{ height: '100%', width: '100%' }}
          zoomControl={true}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <FitBounds points={polyline} />

          {/* Route line */}
          <Polyline
            positions={polyline}
            pathOptions={{ color: '#4F46E5', weight: 3, opacity: 0.75 }}
          />

          {/* Start marker */}
          <Marker position={[first.lat, first.lng]} icon={startIcon}>
            <Popup>
              <div className="text-sm">
                <p className="font-semibold text-emerald-700">🟢 যাত্রা শুরু</p>
                <p>{fmtTime(first.recorded_at)}</p>
              </div>
            </Popup>
          </Marker>

          {/* Middle markers — only show every 3rd point to avoid clutter */}
          {mids.filter((_, i) => i % 3 === 0).map(pt => (
            <Marker key={pt.id} position={[pt.lat, pt.lng]} icon={midIcon}>
              <Popup>
                <p className="text-xs">{fmtTime(pt.recorded_at)}</p>
              </Popup>
            </Marker>
          ))}

          {/* Last marker */}
          {data.points.length > 1 && (
            <Marker position={[last.lat, last.lng]} icon={lastIcon}>
              <Popup>
                <div className="text-sm">
                  <p className="font-semibold text-red-700">
                    {data.rental_status === 'completed' ? '🏁 যাত্রা শেষ' : '📍 সর্বশেষ অবস্থান'}
                  </p>
                  <p>{fmtTime(last.recorded_at)}</p>
                </div>
              </Popup>
            </Marker>
          )}
        </MapContainer>
      </div>
    </div>
  )
}
