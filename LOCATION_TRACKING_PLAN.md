# লোকেশন টাইমলাইন ফিচার — সম্পূর্ণ পরিকল্পনা

**তৈরি:** 2026-06-26  
**স্ট্যাটাস:** ✅ সম্পূর্ণ বাস্তবায়িত (2026-06-26)

---

## সংক্ষিপ্ত বিবরণ

ড্রাইভার একটি ট্রিপ শুরু করার পর প্রতি **৫ মিনিট** অন্তর তার ফোনের GPS লোকেশন সার্ভারে পাঠাবে। ট্রিপ সম্পন্ন হলে লোকেশন আপডেট বন্ধ হবে। Admin ও Manager সেই ট্রিপের সম্পূর্ণ লোকেশন টাইমলাইন ম্যাপে দেখতে পাবে।

---

## আর্কিটেকচার

```
Driver App (Android)          PHP API                    Web (Admin/Manager)
────────────────────          ───────                    ───────────────────
LocationTrackingService       trip_locations table        Leaflet.js map
  └─ ForegroundService  →     POST /driver/location  →   polyline + markers
  └─ ৫ মিনিট loop             GET /admin/locations        timestamp tooltip
  └─ trip complete?            GET /manager/locations
     → service stop
```

---

## Phase 1 — Database ও PHP API

### নতুন টেবিল: `trip_locations`

```sql
CREATE TABLE trip_locations (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    rental_id   INT NOT NULL,
    latitude    DECIMAL(10, 7) NOT NULL,
    longitude   DECIMAL(10, 7) NOT NULL,
    accuracy    FLOAT NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rental_time (rental_id, recorded_at),
    FOREIGN KEY (rental_id) REFERENCES rentals(id)
);
```

**আকার অনুমান:** ৮ ঘণ্টার ট্রিপে ৫ মিনিট পরপর = মাত্র ৯৬ রো।

### নতুন API Endpoints

| Method | Path | Guard | কাজ |
|--------|------|-------|-----|
| `POST` | `/api/driver/location.php` | `require_driver()` | ড্রাইভারের লোকেশন সংরক্ষণ |
| `GET`  | `/api/admin/rentals/locations.php?id=X` | `require_role('admin')` | যেকোনো ট্রিপের টাইমলাইন |
| `GET`  | `/api/manager/rentals/locations.php?id=X` | `require_manager()` | assigned গাড়ির ট্রিপের টাইমলাইন |

### POST `/api/driver/location.php` — Logic

```
Input: rental_id, latitude, longitude, accuracy (optional)

Validation:
  1. require_driver() → driver_id পাই
  2. rental_id টি এই ড্রাইভারের কিনা verify
  3. rental_status = 'active' কিনা check (completed হলে reject)

Action:
  INSERT INTO trip_locations (rental_id, latitude, longitude, accuracy, recorded_at)
  VALUES (?, ?, ?, ?, NOW())

Response:
  { success: true, message: "লোকেশন সংরক্ষিত" }
```

### GET `locations.php?id=X` — Response Format

```json
{
  "success": true,
  "data": {
    "rental_id": 42,
    "pickup_location": "ঢাকা",
    "dropoff_location": "চট্টগ্রাম",
    "points": [
      { "lat": 23.8103, "lng": 90.4125, "recorded_at": "2026-06-26 10:00:00" },
      { "lat": 23.7500, "lng": 90.5000, "recorded_at": "2026-06-26 10:05:00" }
    ]
  }
}
```

---

## Phase 2 — Android App (Driver)

### কেন ForegroundService?

| পদ্ধতি | সমস্যা |
|--------|--------|
| Coroutine in Composable | ফোন lock / অ্যাপ background-এ গেলে মরে যায় |
| WorkManager | minimum interval ১৫ মিনিট — ৫ মিনিট সম্ভব নয় |
| **ForegroundService** ✅ | ফোন lock থাকলেও চলে, notification দেখায়, নির্ভরযোগ্য |

### নতুন ফাইল: `LocationTrackingService.kt`

```
android/app/src/main/java/com/rzzisan/carrental/service/
└── LocationTrackingService.kt
```

**Service কাঠামো:**
```kotlin
class LocationTrackingService : Service() {
    companion object {
        fun start(context: Context, rentalId: Int)
        fun stop(context: Context)
    }

    override fun onStartCommand(...): Int {
        // Persistent notification তৈরি
        // Coroutine scope শুরু
        // Loop: getLocation() → POST API → delay(5 min)
        return START_STICKY  // kill হলে OS restart করবে
    }

    override fun onDestroy() {
        // Coroutine cancel, tracking stop
    }
}
```

**Notification (persistent):**
```
🚗 ট্রিপ চলছে
লোকেশন শেয়ার হচ্ছে...
[ট্রিপ দেখুন]
```

**ট্রিগার — TripsScreen.kt-এ:**
```kotlin
// ট্রিপ active হলে
"active" → LocationTrackingService.start(context, rental.id)

// ট্রিপ completed হলে
"completed" → LocationTrackingService.stop(context)
```

### নতুন Permission (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".service.LocationTrackingService"
    android:foregroundServiceType="location" />
```

### নতুন API Interface (ApiService.kt)

```kotlin
@POST("driver/location.php")
suspend fun postLocation(@Body body: LocationBody): ApiResponse<Unit>

data class LocationBody(
    val rental_id: Int,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?
)
```

### অফলাইন কৌশল

প্রথম ভার্সনে **skip approach**: পাঠানো fail হলে সেই পয়েন্ট বাদ দিয়ে পরেরটায় যাও। বেশিরভাগ রুটে ইন্টারনেট থাকে, তাই এটি যথেষ্ট।

**ভবিষ্যতে (queue approach):** offline হলে local Room DB-তে জমা রাখো, internet ফিরলে batch পাঠাও।

---

## Phase 3 — Web Frontend (Admin + Manager)

### Map Library: Leaflet.js + OpenStreetMap

**কেন Leaflet.js?**
- সম্পূর্ণ বিনামূল্যে (Google Maps API key লাগে না)
- OpenStreetMap tile — বাংলাদেশের রোড ডেটা আছে
- React component-এ সহজে integrate করা যায়

### Frontend Install

```bash
cd frontend
npm install leaflet react-leaflet
npm install @types/leaflet
```

### নতুন Component: `TripLocationMap.tsx`

```
frontend/src/components/TripLocationMap.tsx
```

**Map UI:**
```
┌─────────────────────────────────────────┐
│  OpenStreetMap                          │
│                                         │
│   🟢 ────●────●────●────●──── 🔴       │
│  শুরু  (মার্কার, ক্লিক করলে সময়)  বর্তমান │
│                                         │
│  মোট পয়েন্ট: ৯৬ | প্রথম: ১০:০০ AM     │
└─────────────────────────────────────────┘
```

- **সবুজ মার্কার:** যাত্রা শুরু
- **লাল মার্কার:** সর্বশেষ/বর্তমান অবস্থান
- **নীল polyline:** সম্পূর্ণ পথ
- **ক্লিক করলে:** সময় দেখাবে (popup)

### Integration পয়েন্ট

**Admin:** `frontend/src/pages/admin/Rentals.tsx`
- Trip detail modal-এ নতুন ট্যাব: `[ বিবরণ ] [ খরচ ] [ 📍 লোকেশন ]`
- ট্যাব ক্লিক করলে `GET /api/admin/rentals/locations.php?id=X` call

**Manager:** `frontend/src/pages/manager/Rentals.tsx`
- একই কাঠামো, শুধু API endpoint আলাদা

---

## কাজের ক্রম

```
┌─────────────────────────────────────────────────────────────┐
│ Phase 1: Backend (সবচেয়ে আগে)                              │
│   ☐ trip_locations টেবিল তৈরি (SQL migration)               │
│   ☐ POST /api/driver/location.php                           │
│   ☐ GET /api/admin/rentals/locations.php                    │
│   ☐ GET /api/manager/rentals/locations.php                  │
├─────────────────────────────────────────────────────────────┤
│ Phase 2: Android (ড্রাইভার অ্যাপ)                           │
│   ☐ LocationTrackingService.kt তৈরি                         │
│   ☐ AndroidManifest.xml update                              │
│   ☐ ApiService.kt-এ postLocation যোগ                        │
│   ☐ TripsScreen-এ start/stop ট্রিগার                        │
│   ☐ Permission handling (Android 13+)                       │
│   ☐ APK build ও test                                        │
├─────────────────────────────────────────────────────────────┤
│ Phase 3: Web Frontend (Admin + Manager)                     │
│   ☐ leaflet + react-leaflet install                         │
│   ☐ TripLocationMap.tsx component                           │
│   ☐ Admin Rentals.tsx-এ map ট্যাব যোগ                       │
│   ☐ Manager Rentals.tsx-এ map ট্যাব যোগ                     │
│   ☐ Frontend build ও deploy                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## নির্ভরতা ও সীমাবদ্ধতা

- Android `minSdk` কমপক্ষে 26 হতে হবে (ForegroundService type "location" Android 10+ এ প্রয়োজন)
- GPS permission ইতিমধ্যে app-এ আছে (`ACCESS_FINE_LOCATION`)
- `POST_NOTIFICATIONS` permission Android 13 (API 33)-এ runtime-এ চাইতে হবে
- Web-এ Leaflet CSS import করতে হবে (`import 'leaflet/dist/leaflet.css'`)

---

## ভবিষ্যৎ উন্নয়ন (এই ফিচারের পরে)

- Offline queue: Room DB → batch upload when online
- Real-time tracking: WebSocket দিয়ে live map update (admin দেখতে পাবে গাড়ি চলছে)
- Geofencing: নির্ধারিত এলাকা থেকে বেরিয়ে গেলে alert
- Speed tracking: দ্রুতগতির সতর্কতা
