# Android App Feasibility Analysis — Car Rental Driver App

> তৈরির তারিখ: 2026-06-26  
> প্রজেক্ট: car.zisan.me Rent-A-Car Management System  
> লক্ষ্য: Driver-focused Android app — live GPS, camera, trip management

---

## সিদ্ধান্ত: সম্পূর্ণ সম্ভব ✓

বিদ্যমান PHP REST API Android app থেকে সরাসরি ব্যবহারযোগ্য।  
Authentication সমস্যা সমাধান হয়েছে — Token-based auth backend-এ যোগ করা হয়েছে।

---

## ১. Authentication — সমস্যা ও সমাধান

### সমস্যা ছিল
- PHP Session `samesite: Strict` + `secure: true` cookie — native Android app-এ session টিকিয়ে রাখা কঠিন
- App বন্ধ/খুললে session হারিয়ে যাওয়ার সম্ভাবনা

### সমাধান: Token-based Auth (বাস্তবায়িত)

**নতুন `api_tokens` টেবিল:**
```sql
id, token (64-char hex), role, user_id, driver_id, manager_id,
username, email, expires_at (৩০ দিন), created_at
```

**Login flow (mobile):**
```json
POST /api/auth/login.php
{ "email": "driver@example.com", "password": "pass", "source": "mobile" }

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "username": "আব্দুল করিম",
    "email": "driver@example.com",
    "role": "driver",
    "token": "a1b2c3d4...64chars..."  ← শুধু mobile request-এ আসবে
  }
}
```

**প্রতিটি API request-এ:**
```
Authorization: Bearer a1b2c3d4...64chars...
```

**Logout:**
```json
POST /api/auth/logout.php
Authorization: Bearer <token>
→ DB থেকে token delete হয়, তারপর কাজ করে না
```

**Token মেয়াদ:** ৩০ দিন — সাধারণ ব্যবহারে বার বার login করতে হবে না।

**Backward compatible:** Web browser session auth অপরিবর্তিত।

---

## ২. GPS Location — সম্পূর্ণ সাপোর্ট আছে

Database-এ ইতিমধ্যে সব field বিদ্যমান:

```
rentals table:
  start_location_name, start_latitude DECIMAL(10,7), start_longitude DECIMAL(10,7)
  end_location_name,   end_latitude   DECIMAL(10,7), end_longitude   DECIMAL(10,7)

trip_expenses table:
  location_name, latitude DECIMAL(10,7), longitude DECIMAL(10,7)
```

### Android GPS Flow

```
ট্রিপ শুরু (Start Trip):
  1. FusedLocationProviderClient.getCurrentLocation()
  2. Geocoder দিয়ে ঠিকানা → location_name
  3. POST /api/driver/rentals/update_status.php?id={id}
     Body: { status: "active", latitude: 23.8103, longitude: 90.4129, location_name: "Gulshan Circle" }

খরচ যোগ (Add Expense):
  1. বর্তমান GPS নাও
  2. Camera দিয়ে রসিদের ছবি তোলো
  3. POST /api/driver/rentals/expenses.php?rental_id={id} (multipart)
     Fields: expense_type, amount, description, latitude, longitude, location_name, receipt_image

ট্রিপ শেষ (Complete Trip):
  1. GPS নাও
  2. POST /api/driver/rentals/update_status.php?id={id}
     Body: { status: "completed", latitude, longitude, location_name }
  3. Settlement স্বয়ংক্রিয়ভাবে তৈরি হয়
```

**DECIMAL(10,7) precision:** ±1 সেন্টিমিটার নির্ভুলতা — Android GPS-এর জন্য যথেষ্ট।

---

## ৩. Camera ও File Upload

### বিদ্যমান Backend সাপোর্ট
- Upload path: `public/uploads/expenses/`
- Max size: 5 MB
- Allowed types: jpg, jpeg, png, gif
- Filename: `expense_{timestamp}_{random}.jpg`

### Android Implementation

```kotlin
// Kotlin — OkHttp multipart upload
val imageFile = File(imagePath)
val requestBody = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("expense_type", "fuel")
    .addFormDataPart("amount", "150.00")
    .addFormDataPart("location_name", "Mirpur Pump")
    .addFormDataPart("latitude", "23.8456")
    .addFormDataPart("longitude", "90.3678")
    .addFormDataPart(
        "receipt_image", imageFile.name,
        imageFile.asRequestBody("image/jpeg".toMediaType())
    )
    .build()

val request = Request.Builder()
    .url("$BASE_URL/api/driver/rentals/expenses.php?rental_id=$rentalId")
    .addHeader("Authorization", "Bearer $token")
    .post(requestBody)
    .build()
```

---

## ৪. Required Android Permissions

```xml
<!-- AndroidManifest.xml -->

<!-- GPS -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Camera -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />

<!-- Storage (receipt gallery থেকে বাছাই) -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />         <!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
                 android:maxSdkVersion="32" />                                   <!-- Android ≤12 -->

<!-- Internet -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## ৫. সম্পূর্ণ API Reference (Android App-এর জন্য)

| Endpoint | Method | কাজ |
|---|---|---|
| `/api/auth/login.php` | POST | Login + token পাওয়া (`source: "mobile"`) |
| `/api/auth/me.php` | GET | Session/token যাচাই |
| `/api/auth/logout.php` | POST | Token revoke + logout |
| `/api/driver/profile.php` | GET | Profile + assigned vehicles + stats |
| `/api/driver/profile.php` | POST | Profile/password/picture update |
| `/api/driver/vehicles.php` | GET | Assigned vehicles |
| `/api/driver/rentals/index.php` | GET | Trip list (filter: status, date_from, date_to) |
| `/api/driver/rentals/index.php` | POST | নতুন trip তৈরি |
| `/api/driver/rentals/show.php?id=` | GET | Trip details + expenses |
| `/api/driver/rentals/update_status.php?id=` | POST | Trip start/complete + GPS |
| `/api/driver/rentals/expenses.php?rental_id=` | GET | Trip expenses |
| `/api/driver/rentals/expenses.php?rental_id=` | POST | Expense যোগ + GPS + ছবি |
| `/api/driver/ledger.php` | GET | Settlement + monthly earnings |

**Base URL:** `https://car.zisan.me`  
**Auth Header:** `Authorization: Bearer <token>`  
**Response format:** `{ "success": bool, "data": any, "message": string }`

---

## ৬. Technology Recommendation

### React Native (Expo) — সুপারিশকৃত

**কারণ:**
- বিদ্যমান TypeScript types (`types/index.ts`) পুনর্ব্যবহারযোগ্য
- API client logic (`client.ts`) সহজে port করা যাবে
- `expo-location` → GPS (Google Play Services backed)
- `expo-camera` + `expo-image-picker` → Camera
- Expo Go দিয়ে দ্রুত test করা যায়
- একই কোড iOS-এও চলবে ভবিষ্যতে

**Key packages:**
```json
{
  "expo-location": "GPS — FusedLocationProvider wrapper",
  "expo-camera": "Camera capture",
  "expo-image-picker": "Gallery + camera combined",
  "@react-native-async-storage/async-storage": "Token storage",
  "axios": "HTTP client (interceptor দিয়ে token auto-inject)"
}
```

### Flutter (বিকল্প)
- Native performance বেশি
- Dart শিখতে হবে, বিদ্যমান কোড কাজে আসে না

### Native Android Kotlin (সবচেয়ে কম recommended)
- সর্বোচ্চ performance, সর্বোচ্চ সময়
- Android-only (iOS আলাদা)

---

## ৭. App Screen Plan

```
📱 Login Screen
    ↓ (token সংরক্ষণ AsyncStorage-এ)
📱 Dashboard
    ├── লাইভ ট্রিপ কার্ড (active trip থাকলে)
    ├── এই মাসের আয় সারসংক্ষেপ
    └── সাম্প্রতিক settlement

📱 My Trips (Trip List)
    ├── ফিল্টার: status, date range
    └── [+ নতুন ট্রিপ] বাটন

📱 Create Trip
    ├── যাত্রী নাম + মোবাইল
    ├── গাড়ি বাছাই (assigned vehicles)
    ├── pickup/dropoff location
    └── চুক্তির টাকা

📱 Trip Detail
    ├── [ট্রিপ শুরু] → GPS capture → API
    ├── Expense List
    ├── [খরচ যোগ] → Camera + GPS → Upload
    └── [ট্রিপ শেষ] → GPS capture → API

📱 Add Expense
    ├── ধরন (toll/fuel/parking/repair/other)
    ├── পরিমাণ
    ├── [রসিদের ছবি তোলো] → Camera
    └── GPS স্বয়ংক্রিয়ভাবে নেওয়া হবে

📱 Ledger (আমার লেজার)
    ├── Settlement তালিকা
    ├── মাসিক breakdown
    └── খরচের বিশ্লেষণ

📱 Profile
    ├── নাম/মোবাইল আপডেট
    ├── ছবি পরিবর্তন
    └── পাসওয়ার্ড পরিবর্তন
```

---

## ৮. Development Timeline

| Phase | কাজ | সময় |
|---|---|---|
| Phase 1 | Expo project setup, API client (axios + token interceptor), Login screen, AsyncStorage token | ২ দিন |
| Phase 2 | Dashboard, Trip List, Trip Detail screens | ৩ দিন |
| Phase 3 | GPS integration (start/end trip) — expo-location | ২ দিন |
| Phase 4 | Camera + file upload (expenses) — expo-camera | ২ দিন |
| Phase 5 | Ledger screen, Profile screen | ২ দিন |
| Phase 6 | Testing, APK build (EAS Build), refinement | ৩ দিন |
| **মোট** | | **~২ সপ্তাহ** |

---

## ৯. Backend পরিবর্তনের Summary (বাস্তবায়িত)

| পরিবর্তন | ফাইল | অবস্থা |
|---|---|---|
| `api_tokens` টেবিল তৈরি | MySQL | ✅ সম্পন্ন |
| Bearer token validation | `api/_helpers.php` | ✅ সম্পন্ন |
| Mobile login → token return | `api/auth/login.php` | ✅ সম্পন্ন |
| Logout → token revoke | `api/auth/logout.php` | ✅ সম্পন্ন |
| me.php token support | `api/auth/me.php` | ✅ সম্পন্ন |

**Web browser auth অপরিবর্তিত** — সম্পূর্ণ backward compatible।

---

## ১০. Android App-এ API Client Example (React Native)

```typescript
// api/client.ts (React Native version)
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';

const BASE_URL = 'https://car.zisan.me/api';

const apiClient = axios.create({ baseURL: BASE_URL });

// প্রতিটি request-এ token auto-inject
apiClient.interceptors.request.use(async (config) => {
  const token = await AsyncStorage.getItem('auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 401 হলে login screen-এ redirect
apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401) {
      await AsyncStorage.removeItem('auth_token');
      // Navigate to login
    }
    return Promise.reject(error);
  }
);

export const login = async (email: string, password: string) => {
  const res = await apiClient.post('/auth/login.php', {
    email, password, source: 'mobile'
  });
  if (res.data.success && res.data.data.token) {
    await AsyncStorage.setItem('auth_token', res.data.data.token);
  }
  return res.data;
};
```
