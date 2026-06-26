# Android App Development Context — car.zisan.me

এই ফাইলটি Android app development-এর সম্পূর্ণ ইতিহাস, সিদ্ধান্ত, সমস্যা ও সমাধান ট্র্যাক করে।

> **নিয়ম:** Android-সংক্রান্ত যেকোনো কাজ শেষে এই ফাইল আপডেট করতে হবে।
> নতুন সমস্যা, fix, feature, বা সিদ্ধান্ত — সবকিছু এখানে লিখতে হবে।
> ভবিষ্যতের developer (বা AI session) যেন শুধু এই ফাইল পড়েই পুরো context বুঝতে পারে।

---

## বর্তমান অবস্থা (2026-06-26)

| বিষয় | অবস্থা |
|---|---|
| Login | ✅ কাজ করছে |
| Ledger screen | কোড আছে, test হয়নি |
| Trips screen | কোড আছে, test হয়নি |
| Trip detail / start / complete | কোড আছে, test হয়নি |
| Expense add (GPS + camera) | কোড আছে, test হয়নি |
| Profile screen | কোড আছে, test হয়নি |
| APK | v3 — https://car.zisan.me/apk/ |

---

## প্রজেক্ট লক্ষ্য

Driver-focused Android app যেখানে driver:
- লগইন করতে পারবে (token-based auth)
- ট্রিপ তৈরি, শুরু, সম্পন্ন করতে পারবে
- GPS-সহ expense add করতে পারবে (ক্যামেরা দিয়ে receipt তুলতে পারবে)
- লেজার ও মাসিক আয় দেখতে পারবে
- প্রোফাইল আপডেট করতে পারবে

---

## Tech Stack

| উপাদান | পছন্দ | কারণ |
|---|---|---|
| UI | Jetpack Compose + Material3 | Modern declarative UI, Google recommended |
| HTTP | Retrofit2 + Moshi + OkHttp | Kotlin coroutines-native, type-safe |
| Auth | Bearer token (api_tokens table) | Cookie সরাসরি Android-এ কাজ করে না |
| GPS | FusedLocationProviderClient | Battery-efficient, Google Play Services |
| Camera | CameraX + FileProvider | Modern API, backward-compatible |
| i18n | Abstract class (BanglaStrings + EnglishStrings) | DEX 74-arg register limit এড়াতে |
| Storage | EncryptedSharedPreferences (security-crypto) | Token securely store করে |

---

## ফাইল কাঠামো

```
android/
├── app/
│   ├── build.gradle.kts              — dependencies, BuildConfig.API_BASE_URL
│   ├── proguard-rules.pro            — keep rules for Moshi reflection
│   ├── src/main/
│   │   ├── AndroidManifest.xml       — permissions, FileProvider
│   │   ├── res/xml/file_paths.xml    — FileProvider path config
│   │   └── java/com/rzzisan/carrental/
│   │       ├── CarRentalApp.kt       — Application class + AppContext singleton
│   │       ├── MainActivity.kt       — Entry point, language toggle
│   │       ├── MainAppShell.kt       — Bottom nav (Ledger/Trips/Profile), NavHost
│   │       ├── data/
│   │       │   ├── auth/
│   │       │   │   ├── AuthStorage.kt      — SharedPreferences wrapper
│   │       │   │   └── AuthTokenStore.kt   — token/role/username/id save/load
│   │       │   └── network/
│   │       │       ├── ApiClient.kt        — OkHttp + Retrofit + Moshi setup
│   │       │       ├── ApiService.kt       — Retrofit interface (driver endpoints)
│   │       │       └── Models.kt           — data classes (NO @JsonClass annotation — দেখো সমস্যা ৫)
│   │       ├── ui/
│   │       │   ├── strings/
│   │       │   │   ├── AppStrings.kt       — abstract class + Bangla/English objects
│   │       │   │   └── LocalStrings.kt     — CompositionLocal provider
│   │       │   ├── theme/
│   │       │   │   ├── Color.kt            — Primary=Indigo, Status colors
│   │       │   │   └── Theme.kt            — MaterialTheme lightColorScheme
│   │       │   └── screens/
│   │       │       ├── LoginScreen.kt      — ✅ কাজ করছে
│   │       │       ├── LedgerScreen.kt
│   │       │       ├── TripsScreen.kt
│   │       │       ├── TripDetailScreen.kt
│   │       │       ├── CreateTripScreen.kt
│   │       │       ├── AddExpenseScreen.kt
│   │       │       └── ProfileScreen.kt
│   │       └── util/
│   │           └── LocationUtils.kt        — reverseGeocode() via Android Geocoder
├── build.gradle.kts                  — root: AGP 8.3.0, Kotlin 1.9.22
├── settings.gradle.kts
├── gradle.properties                 — useAndroidX=true, enableJetifier=true
├── gradle/wrapper/gradle-wrapper.properties — Gradle 8.6
└── deploy-apk.sh                     — build + copy to /var/www/car-apk/ (ANDROID_HOME লাগবে)
```

---

## Backend পরিবর্তন (Android সাপোর্টের জন্য)

### ১. Token-based Authentication

**কারণ:** PHP session cookie (`Set-Cookie: samesite=Strict`) Android native app-এ reliable নয়।
Retrofit/OkHttp cookie jar থেকে same-site cookie অন্য request-এ পাঠানো যায় না।

**সমাধান:** `api_tokens` টেবিল + Bearer header সাপোর্ট।

```sql
CREATE TABLE api_tokens (
  id INT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(64) NOT NULL UNIQUE,  -- bin2hex(random_bytes(32))
  role VARCHAR(20) NOT NULL,
  user_id INT NOT NULL DEFAULT 0,
  driver_id INT NOT NULL DEFAULT 0,
  manager_id INT NOT NULL DEFAULT 0,
  username VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL,
  expires_at DATETIME NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_token (token)
);
```

**Flow:**
1. App → `POST /api/auth/login.php` with `{"source": "mobile", ...}`
2. Server → `{"success": true, "data": {"token": "abc123...", "role": "driver", ...}}`
3. App → token EncryptedSharedPreferences-এ save করে
4. প্রতিটি পরবর্তী request → `Authorization: Bearer <token>` header (OkHttp interceptor-এ)

**পরিবর্তিত ফাইল:**
- `api/_helpers.php` — `_helpers_db()` (lazy DB), `_validate_bearer_token()`, `require_auth()` আপডেট
- `api/auth/login.php` — token generate + return; credential failure-এ HTTP 200 (দেখো সমস্যা ৬)
- `api/auth/logout.php` — Bearer token DB থেকে DELETE করে (revoke)
- `api/auth/me.php` — `require_auth()` যোগ

### ২. Remember Me (৩ মাস)

**কারণ:** User-এর অনুরোধ — minimum ৩ মাস session।

- Web: Session cookie lifetime → `90 days` (`REMEMBER_ME_DURATION = 7776000` in `config.php`)
- Mobile: Token `expires_at = now + 90 days` (`remember_me: true` হলে), default `30 days`

---

## Build Environment Setup

### Android SDK

```bash
ANDROID_HOME=/opt/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools
# cmdline-tools download + extract করতে হবে
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### Java — JDK required (JRE যথেষ্ট নয়)

```bash
apt-get install -y openjdk-21-jdk-headless
```

শুধু JRE থাকলে `jlink` পাওয়া যায় না → `compileDebugJavaWithJavac` fail (দেখো সমস্যা ৪)।

### Build Command

```bash
cd /var/www/html/car.zisan.me/android
ANDROID_HOME=/opt/android-sdk ./gradlew assembleDebug
```

`assembleRelease` ব্যবহার করা যাবে না — unsigned APK install হয় না। `assembleDebug` → debug-signed APK।

### Deploy

```bash
APK_DIR="/var/www/car-apk"
TS=$(date +%Y%m%d-%H%M%S)
cp app/build/outputs/apk/debug/app-debug.apk "$APK_DIR/carrental-${TS}.apk"
bash "$APK_DIR/gen-index.sh"
# Download: https://car.zisan.me/apk/
```

`deploy-apk.sh` সরাসরি কাজ করে না — script-এ `ANDROID_HOME` নেই। উপরের steps manually run করো।

---

## সমস্যা ও সমাধান লগ

### সমস্যা ১: `android.useAndroidX` not enabled

**লক্ষণ:** Build error — `Dependency requires core library desugaring`
**কারণ:** `gradle.properties`-এ `android.useAndroidX=true` ছিল না।
**সমাধান:** `android/gradle.properties` তৈরি করে:
```
android.useAndroidX=true
android.enableJetifier=true
```
**ভবিষ্যতে:** নতুন Android project-এ সবসময় এই দুটি লাইন রাখো।

---

### সমস্যা ২: Launcher icon not found (AAPT error)

**লক্ষণ:** `processDebugResources` fail — `mipmap/ic_launcher` not found
**কারণ:** `res/mipmap-*/` ফোল্ডারে কোনো PNG ছিল না।
**সমাধান:** Python3 দিয়ে minimal 1x1 PNG তৈরি করে সব density-তে রাখা।
**ভবিষ্যতে:** Project শুরুতেই placeholder icon দাও। Real icon পরে replace করলেই হবে।

---

### সমস্যা ৩: `Unresolved reference: await` (FusedLocation)

**লক্ষণ:** Compile error in `TripDetailScreen.kt`, `AddExpenseScreen.kt`
**কারণ:** `fusedLocation.getCurrentLocation(...).await()` → Google Play Services `Task<T>`-এ `.await()` ব্যবহার করতে আলাদা dependency লাগে।
**সমাধান:** `app/build.gradle.kts`-এ:
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
```
**ভবিষ্যতে:** FusedLocationProvider ব্যবহার করলে এই dependency অবশ্যই লাগবে।

---

### সমস্যা ৪: `jlink executable does not exist`

**লক্ষণ:** `compileDebugJavaWithJavac` task fail
**কারণ:** Server-এ শুধু JRE ছিল, JDK ছিল না।
**সমাধান:** `apt-get install -y openjdk-21-jdk-headless`
**ভবিষ্যতে:** নতুন server-এ build করার আগে `javac -version` চেক করো।

---

### সমস্যা ৫: "সার্ভারের সাথে সংযোগ ব্যর্থ হয়েছে" ✅ সমাধান হয়েছে

**লক্ষণ:** App install-এর পর login-এ error, server-এ কোনো request পৌঁছায় না।
**কারণ:** `Models.kt`-এ সব data class-এ `@JsonClass(generateAdapter = true)` ছিল, কিন্তু `app/build.gradle.kts`-এ kapt/ksp plugin বা `moshi-kotlin-codegen` নেই। Runtime-এ `LoginRequestJsonAdapter.class` খোঁজে পায় না → `ClassNotFoundException` → catch block ধরে generic error।
**সমাধান:** `Models.kt` থেকে সব `@JsonClass(generateAdapter = true)` সরানো হয়েছে। `ApiClient.kt`-এ `KotlinJsonAdapterFactory` (reflection) যথেষ্ট।
**ভবিষ্যতে:**
- `@JsonClass(generateAdapter = true)` শুধু তখনই ব্যবহার করো যখন kapt/ksp + `moshi-kotlin-codegen` থাকে।
- Reflection-based approach এই app-এর জন্য যথেষ্ট।

---

### সমস্যা ৬: "HttpException: HTTP 401 Unauthorized" ✅ সমাধান হয়েছে

**লক্ষণ:** Moshi fix-এর পর request server পর্যন্ত পৌঁছায়, কিন্তু HttpException error।
**কারণ:**
- `login.php`-এ ভুল credentials-এ HTTP `401` status return হচ্ছিল।
- Retrofit `suspend` function-এ যেকোনো non-2xx response → `HttpException` throw; JSON body parse হয় না।
- HTTP 401 login endpoint-এ semantically ভুল ("এই resource-এ access করতে auth লাগবে" — কিন্তু login endpoint নিজেই public)।

**সমাধান:**
1. `api/auth/login.php` — সব credential failure থেকে `401` status সরানো → HTTP `200` (default)।
2. `LoginScreen.kt`-এ `HttpException` আলাদা catch block যোগ — debug mode-এ error body দেখায়।

**ভবিষ্যতে:**
- Login endpoint-এ credential failure → HTTP `200`, `success: false`, `message: "..."` — এটাই সঠিক।
- HTTP `401` শুধু `require_auth()` / `require_role()` — protected endpoint-এ।
- Retrofit `suspend` return type-এ non-2xx সবসময় exception। Error body পেতে `Response<T>` wrapper ব্যবহার করো, অথবা server `200` পাঠাক।

---

## Moshi Configuration — সঠিক Pattern

```kotlin
// ApiClient.kt
private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())  // kapt ছাড়াই কাজ করে
    .build()
```

```kotlin
// Models.kt — @JsonClass annotation নেই, @Json(name=) শুধু field rename-এর জন্য
data class LoginRequest(
    val email: String,
    val password: String,
    val source: String = "mobile",
    @Json(name = "remember_me") val rememberMe: Boolean = false
)
```

---

## i18n — Abstract Class Pattern (DEX Limit Fix)

**সমস্যা:** `data class AppStrings(75+ params)` → DEX 74-arg register limit error।
**সমাধান:**
```kotlin
abstract class AppStrings {
    abstract val login: String
    abstract val serverError: String
}
object BanglaStrings : AppStrings() {
    override val login = "লগইন"
    override val serverError = "সার্ভারের সাথে সংযোগ ব্যর্থ হয়েছে"
}
object EnglishStrings : AppStrings() {
    override val login = "Login"
    override val serverError = "Connection failed"
}
```
**ভবিষ্যতে:** String class-এ `Too many arguments for DEX method` error আসলে এই pattern-এই fix।

---

## API Endpoints (Driver)

| Method | URL | কাজ |
|---|---|---|
| POST | `/api/auth/login.php` | Login, returns token |
| GET | `/api/auth/me.php` | Current user check |
| POST | `/api/auth/logout.php` | Logout, revoke token |
| GET | `/api/driver/profile.php` | Profile + vehicles + stats |
| POST | `/api/driver/profile.php` | Update name/mobile/picture/password |
| GET | `/api/driver/vehicles.php` | Assigned vehicles |
| GET | `/api/driver/rentals/index.php` | Trip list (filter: status, date) |
| POST | `/api/driver/rentals/index.php` | Create trip |
| GET | `/api/driver/rentals/show.php?id=` | Trip detail |
| POST | `/api/driver/rentals/update_status.php?id=` | Update status + GPS location |
| GET | `/api/driver/rentals/expenses.php?rental_id=` | Expense list |
| POST | `/api/driver/rentals/expenses.php?rental_id=` | Add expense (multipart + receipt) |
| GET | `/api/driver/ledger.php` | Settlements + monthly breakdown |

---

## Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
```

---

## APK Info

| Item | Value |
|---|---|
| App ID | `com.rzzisan.carrental.debug` |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 34 (Android 14) |
| Size | ~20MB |
| Build type | debug (self-signed, সরাসরি install করা যায়) |
| Download | https://car.zisan.me/apk/ |

---

## সম্ভাব্য ভবিষ্যৎ সমস্যা

### ProGuard/R8 (Release build)
Release build-এ R8 reflection-based field names obfuscate করে। `proguard-rules.pro`-এ `-keep class com.rzzisan.carrental.data.network.** { *; }` আছে — যথেষ্ট হওয়া উচিত। না হলে kapt + `moshi-kotlin-codegen` যোগ করো।

### SSL Certificate Renewal
Let's Encrypt certificate expire হলে Android app-এ SSL handshake fail। `certbot renew` cron job দেওয়া উচিত।

### Camera (Android 13+)
`READ_MEDIA_IMAGES` ব্যবহার হচ্ছে — সঠিকভাবে configured আছে।

---

## Commit History (Android-related)

| Commit | বিবরণ |
|---|---|
| `1704383` | Android App: Token-based auth + feasibility report |
| `8b8f808` | Login: Remember Me ফিচার (৩ মাস session) |
| `4335129` | Moshi @JsonClass bug fix + ANDROID_CONTEXT.md তৈরি |
| `5d64221` | Login HTTP 401 → 200 fix + HttpException handler |
| `9b6049a` | ANDROID_CONTEXT.md: HTTP 401 bug documented |

---

*শেষ আপডেট: 2026-06-26 — Login সম্পূর্ণ কাজ করছে (APK v3)*
