# Android App Development Context — car.zisan.me

এই ফাইলটি Android app development-এর সম্পূর্ণ ইতিহাস, সিদ্ধান্ত, সমস্যা ও সমাধান ট্র্যাক করে।

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
│   │       │       └── Models.kt           — data classes (NO @JsonClass annotation)
│   │       ├── ui/
│   │       │   ├── strings/
│   │       │   │   ├── AppStrings.kt       — abstract class + Bangla/English objects
│   │       │   │   └── LocalStrings.kt     — CompositionLocal provider
│   │       │   ├── theme/
│   │       │   │   ├── Color.kt            — Primary=Indigo, Status colors
│   │       │   │   └── Theme.kt            — MaterialTheme lightColorScheme
│   │       │   └── screens/
│   │       │       ├── LoginScreen.kt
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
└── deploy-apk.sh                     — build + copy to /var/www/car-apk/
```

---

## Backend পরিবর্তন (Android সাপোর্টের জন্য)

### ১. Token-based Authentication

**কারণ:** PHP session cookie (`Set-Cookie` header) Android WebView-এর বাইরে কাজ করে না।  
Retrofit/OkHttp cookie jar থেকে same-site cookie অন্য request-এ পাঠানো যায় না reliably।

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
2. Server → returns `{"token": "abc123...", "role": "driver", ...}` in `data`
3. App stores token in `EncryptedSharedPreferences`
4. প্রতিটি পরবর্তী request-এ `Authorization: Bearer <token>` header

**ফাইল পরিবর্তন:**
- `api/_helpers.php` — `_validate_bearer_token()` function যোগ, `require_auth()` আপডেট
- `api/auth/login.php` — token generate করে response-এ পাঠায়
- `api/auth/logout.php` — Bearer token রিভোক করে
- `api/auth/me.php` — `require_auth()` যোগ (token সাপোর্ট)

### ২. Remember Me (৩ মাস)

**কারণ:** User-এর অনুরোধ — minimum ৩ মাস session।

- Web: Session cookie lifetime `90 days` (REMEMBER_ME_DURATION = 7776000)
- Mobile: Token `expires_at = now + 90 days` (remember_me=true হলে)
- Default mobile token: `30 days`

---

## Build Environment Setup

### Android SDK

```bash
ANDROID_HOME=/opt/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools
# cmdline-tools download + extract
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### Java (JDK required — JRE যথেষ্ট নয়)

```bash
apt-get install -y openjdk-21-jdk-headless
```

**গুরুত্বপূর্ণ:** শুধু JRE install করলে Gradle এর `jlink` টুল পাওয়া যায় না এবং `compileDebugJavaWithJavac` task ব্যর্থ হয়।

### Gradle Build

```bash
cd /var/www/html/car.zisan.me/android
ANDROID_HOME=/opt/android-sdk ./gradlew assembleDebug
```

`assembleRelease` ব্যবহার করা যাবে না — unsigned APK install হয় না।  
`assembleDebug` → debug-signed APK যেটি সরাসরি install করা যায়।

### APK Hosting

```
/var/www/car-apk/           → Apache serving at car.zisan.me/apk/
                              (Apache config-এ Alias যোগ করা হয়েছে)
gen-index.sh                → HTML index page তৈরি করে
deploy-apk.sh               → build + timestamp-named copy + gen-index
```

---

## সমস্যা ও সমাধান লগ

### সমস্যা ১: `android.useAndroidX` not enabled

**লক্ষণ:** Build error: `Dependency ... requires core library desugaring`  
**কারণ:** `gradle.properties`-এ `android.useAndroidX=true` ছিল না।  
**সমাধান:** `android/gradle.properties` তৈরি করে নিচের লাইন যোগ:
```
android.useAndroidX=true
android.enableJetifier=true
```
**ভবিষ্যতে:** নতুন Android project-এ সবসময় এই দুটি লাইন `gradle.properties`-এ রাখো।

---

### সমস্যা ২: Launcher icon not found (AAPT error)

**লক্ষণ:** `processDebugResources` fail — `mipmap/ic_launcher` resource not found  
**কারণ:** `res/mipmap-*/` ফোল্ডারে কোনো PNG ছিল না।  
**সমাধান:** Python3 দিয়ে minimal valid 1x1 PNG তৈরি করে সব density-তে রাখা:
```python
import struct, zlib
# 1x1 white pixel PNG — minimum valid PNG bytes
```
**ভবিষ্যতে:** Project তৈরির শুরুতেই placeholder icon PNG রাখো।  
Real icon পরে `res/mipmap-*/ic_launcher.png` replace করলেই হবে।

---

### সমস্যা ৩: `Unresolved reference: tasks` / `Unresolved reference: await`

**লক্ষণ:** Compile error in `TripDetailScreen.kt` ও `AddExpenseScreen.kt`  
**কারণ:** `fusedLocation.getCurrentLocation(...).await()` — এই `.await()` extension  
FusedLocationProvider-এর `Task<T>` object-এ কাজ করতে `kotlinx-coroutines-play-services` দরকার।  
**সমাধান:** `app/build.gradle.kts`-এ dependency যোগ:
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
```
**ভবিষ্যতে:** FusedLocationProvider ব্যবহার করলে এই dependency অবশ্যই লাগবে।

---

### সমস্যা ৪: `jlink executable does not exist`

**লক্ষণ:** `compileDebugJavaWithJavac` task fail  
**কারণ:** Server-এ শুধু JRE ছিল, JDK ছিল না। Gradle-এর `jlink` tool JDK-এর অংশ।  
**সমাধান:**
```bash
apt-get install -y openjdk-21-jdk-headless
```
**ভবিষ্যতে:** নতুন server-এ Android build করার আগে `java -version` ও `javac -version` দুটোই চেক করো।

---

### সমস্যা ৫: Login error — "সার্ভারের সাথে সংযোগ ব্যর্থ হয়েছে" ✅ সমাধান হয়েছে

**লক্ষণ:** App install করার পর login করলে error message দেখায়, সার্ভারে কোনো request পৌঁছায় না  
**কারণ:** `Models.kt`-এ সব data class-এ `@JsonClass(generateAdapter = true)` annotation ছিল।  
এই annotation Moshi-কে বলে "এই class-এর জন্য compile-time generated adapter ব্যবহার কর।"  
কিন্তু `app/build.gradle.kts`-এ kapt বা ksp plugin নেই, এবং `moshi-kotlin-codegen` dependency নেই।  
তাই runtime-এ `LoginRequestJsonAdapter.class` খোঁজে পায় না → `ClassNotFoundException` → catch block-এ ধরা পড়ে।

**সমাধান:** `Models.kt` থেকে সব `@JsonClass(generateAdapter = true)` সরিয়ে দেওয়া হয়েছে।  
`KotlinJsonAdapterFactory` (reflection-based) এখন সব serialization handle করে।  
`ApiClient.kt`-এ `.addLast(KotlinJsonAdapterFactory())` already ছিল — এটাই যথেষ্ট।

**ভবিষ্যতে:**
- `@JsonClass(generateAdapter = true)` শুধু তখনই ব্যবহার করো যখন kapt/ksp + `moshi-kotlin-codegen` configured থাকে।
- Reflection-based (KotlinJsonAdapterFactory) সহজ এবং driver app-এর size/performance trade-off acceptable।
- Code generation শুধু তখন দরকার হয় যখন reflection avoid করতে চাও (ProGuard বা R8 aggressiveness-এর কারণে)।

---

## Moshi Configuration — সঠিক Pattern

```kotlin
// ApiClient.kt — এইভাবে থাকবে
private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())  // reflection-based — annotation processor ছাড়াই কাজ করে
    .build()
```

```kotlin
// Models.kt — এইভাবে থাকবে (NO @JsonClass annotation)
data class LoginRequest(
    val email: String,
    val password: String,
    val source: String = "mobile",
    @Json(name = "remember_me") val rememberMe: Boolean = false
)
```

---

## i18n — Abstract Class Pattern (DEX Limit Fix)

**সমস্যা:** `data class AppStrings(val field1: String, ..., val field75: String)` → DEX register limit (74 args)।

**সমাধান:** Abstract class + object:
```kotlin
abstract class AppStrings {
    abstract val login: String
    abstract val serverError: String
    // ...
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

**ভবিষ্যতে:** i18n string class-এ যদি কখনো error আসে `Too many arguments for DEX method`, এই pattern-এই fix।

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
| POST | `/api/driver/rentals/update_status.php?id=` | Update status (active/completed) + GPS |
| GET | `/api/driver/rentals/expenses.php?rental_id=` | Expense list |
| POST | `/api/driver/rentals/expenses.php?rental_id=` | Add expense (multipart: type/amount/location/receipt) |
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

## Build ও Deploy Steps

```bash
# 1. Project directory
cd /var/www/html/car.zisan.me/android

# 2. Build (ANDROID_HOME অবশ্যই set করতে হবে)
ANDROID_HOME=/opt/android-sdk ./gradlew assembleDebug

# 3. APK copy + index update
APK_DIR="/var/www/car-apk"
TS=$(date +%Y%m%d-%H%M%S)
cp app/build/outputs/apk/debug/app-debug.apk "$APK_DIR/carrental-${TS}.apk"
bash "$APK_DIR/gen-index.sh"

# 4. Download করো: https://car.zisan.me/apk/
```

> **নোট:** `deploy-apk.sh` সরাসরি কাজ করে না কারণ script নিজেই build করে  
> এবং সেখানে `ANDROID_HOME` পাওয়া যায় না। ANDROID_HOME export করে  
> অথবা উপরের manual steps ব্যবহার করো।

---

## APK Info

| Item | Value |
|---|---|
| App ID | `com.rzzisan.carrental.debug` (debug build) |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 34 (Android 14) |
| Size | ~20MB |
| Build type | debug (self-signed) |

---

## সম্ভাব্য ভবিষ্যৎ সমস্যা ও সমাধান

### ProGuard/R8 (Release build)

Release build-এ R8 minification active থাকে। Moshi reflection-based serialization কাজ নাও করতে পারে কারণ R8 data class-এর field names obfuscate করে।

**সমাধান options:**
1. `proguard-rules.pro`-এ already `-keep class com.rzzisan.carrental.data.network.** { *; }` আছে — এটা যথেষ্ট হওয়া উচিত।
2. অথবা kapt + moshi-kotlin-codegen যোগ করে code generation ব্যবহার করো।

### Generic Type Deserialize

`ApiResponse<T>` generic। Retrofit-এর Moshi converter `ParameterizedType` ব্যবহার করে এটা handle করে। এটা নিয়ে সমস্যা হলে সরাসরি Moshi builder-এ `Types.newParameterizedType` দিয়ে adapter তৈরি করো।

### SSL Certificate Renewal

`car.zisan.me` SSL Let's Encrypt ব্যবহার করে। Certificate expire হলে Android app কাজ করবে না (SSL handshake fail)। Certificate renewal automate করতে `certbot renew` cron job দেওয়া উচিত।

### Camera Permission on Android 13+

Android 13 (API 33+) থেকে `READ_EXTERNAL_STORAGE` deprecated। `READ_MEDIA_IMAGES` ব্যবহার হচ্ছে। এটা সঠিকভাবে কনফিগার আছে।

---

## Commit History (Android-related)

- `1704383` — Android App: Token-based auth + feasibility report
- `8b8f808` — Login: Remember Me ফিচার (৩ মাস session)

---

*শেষ আপডেট: 2026-06-26 — Login Moshi annotation bug fix (APK v2)*
