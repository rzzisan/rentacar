---
name: android-app-build
description: 'Build, architect, and deploy a production Android app (Kotlin + Jetpack Compose) from a REST API backend. Use when starting a new Android app, wiring up token-based auth, adding localization, building/deploying APKs, or avoiding DEX/Compose pitfalls. Covers the full workflow derived from the CATV Android project at /var/www/catv-ui/android/.'
argument-hint: 'Describe the Android app task or the new project to build'
user-invocable: true
---

# Android App Build Skill

Derived from building the CATV Android app (Kotlin + Jetpack Compose + Material3) end-to-end.
Reference implementation: `/var/www/catv-ui/android/`
Feasibility report for a second app: `/var/www/car.zisan.me_android_app.md`

---

## When to Use

- Starting a new Android app from a REST API backend
- Wiring up Bearer token auth between Android and a PHP/Node/Laravel API
- Adding Bengali/English localization (i18n) to a Compose app
- Building and deploying APKs from this server
- Diagnosing DEX register limit crashes
- Troubleshooting Compose state bugs (variable shadowing, hardcoded strings)

---

## 1. Project Setup (New App Checklist)

### 1.1 Directory & Package

```
/var/www/<project-name>/android/
  app/src/main/java/com/<company>/<appname>/
    MainActivity.kt
    data/
      network/     ← ApiClient, ApiService, data models
      auth/        ← AuthRepository, AuthTokenStore, AuthStorage
    ui/
      strings/     ← AppStrings.kt, LocalStrings.kt  (if i18n needed)
      theme/       ← Color.kt, Theme.kt
      screens/     ← one file per screen
      MainAppShell.kt  ← nav drawer + routing
```

### 1.2 `app/build.gradle.kts` template

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.<company>.<appname>"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.<company>.<appname>"
        minSdk = 23          // Android 6.0 — safe floor
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "API_BASE_URL", "\"https://your-api.example.com/api/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // signingConfig = signingConfigs.getByName("release")  ← add when Play Store needed
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## 2. ⚠️ CRITICAL — APK Build নিয়ম

### সবসময় `assembleDebug` ব্যবহার করুন

```bash
cd /var/www/<project>/android
./gradlew assembleDebug
```

| Build type | APK size | স্বাক্ষর | ইনস্টলযোগ্য |
|---|---|---|---|
| `assembleDebug` | ~22 MB | debug key (auto-signed) | ✅ হ্যাঁ |
| `assembleRelease` | ~13 MB | unsigned | ❌ ইনস্টল হয় না |

`assembleRelease` unsigned হওয়ায় Android ইনস্টল করে না। **কখনো `assembleRelease` ব্যবহার করবেন না** যদি না keystore configure করা থাকে।

### APK Deploy পদ্ধতি (build-এর পর অবশ্যই)

```bash
TS=$(date +%Y%m%d-%H%M%S)
cp /var/www/<project>/android/app/build/outputs/apk/debug/app-debug.apk \
   /var/www/catv-apk/<appname>-${TS}.apk
bash /var/www/catv-apk/gen-index.sh
```

- APK filename format: `<appname>-YYYYMMDD-HHMMSS.apk`
- `gen-index.sh` চালালে `/var/www/catv-apk/index.html` আপডেট হয়
- APK serve হয় web এর মাধ্যমে — user সরাসরি download করতে পারে

---

## 3. ⚠️ CRITICAL — DEX 74-Argument Register Limit

Android DEX single method invocation-এ সর্বোচ্চ **74টি argument register** ব্যবহার করা যায়।

### লক্ষণ

App install হয়, খোলার সময় crash:
```
java.lang.VerifyError: Verifier rejected class ...AppStringsKt:
void AppStringsKt.<clinit>() failed to verify:
[0x37F] Rejecting invocation, expected 74 argument registers, method signature has 75 or more
```

### কারণ

`data class AppStrings(val a: String, val b: String, ...)` — 75+ constructor parameter → Kotlin top-level `val` → DEX `<clinit>` → crash।

### সমাধান — সবসময় abstract class + object pattern

```kotlin
// ✅ সঠিক — কোনো argument limit নেই
abstract class AppStrings {
    abstract val loading: String
    abstract val cancel: String
    // ... unlimited fields
}

object BengaliStrings : AppStrings() {
    override val loading = "লোড হচ্ছে..."
    override val cancel = "বাতিল"
}

object EnglishStrings : AppStrings() {
    override val loading = "Loading..."
    override val cancel = "Cancel"
}
```

```kotlin
// ❌ কখনো করবেন না
data class AppStrings(val loading: String, val cancel: String /* 75+ params */)
val BengaliStrings = AppStrings(...)  // DEX crash!
```

---

## 4. Token-based Authentication

### 4.1 Backend-এ কী থাকতে হবে

```sql
-- api_tokens table
CREATE TABLE api_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(20),
    user_id INT,
    expires_at DATETIME,
    created_at DATETIME DEFAULT NOW()
);
```

```
POST /api/auth/login.php
Body: { email, password, source: "mobile" }

Response (mobile only):
{ "success": true, "data": { "role": "driver", "token": "64-char-hex..." } }

All other requests:
Authorization: Bearer <token>

POST /api/auth/logout.php  → DB থেকে token delete
```

### 4.2 Android Token Storage

```kotlin
// data/auth/AuthStorage.kt
class AuthStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("auth_store", Context.MODE_PRIVATE)

    fun getToken(): String? = prefs.getString("jwt_token", null)
    fun saveToken(token: String) = prefs.edit().putString("jwt_token", token).apply()
    fun getUserRole(): String? = prefs.getString("user_role", null)
    fun getUserName(): String? = prefs.getString("user_name", null)
    fun saveUserInfo(role: String, name: String) = prefs.edit()
        .putString("user_role", role).putString("user_name", name).apply()
    fun clear() = prefs.edit().remove("jwt_token").remove("user_role").remove("user_name").apply()
}

// data/auth/AuthTokenStore.kt  (singleton — AppContext দিয়ে)
object AuthTokenStore {
    private val storage by lazy { AuthStorage(AppContext.app) }
    fun getToken(): String? = storage.getToken()
    fun saveToken(token: String) = storage.saveToken(token)
    fun getUserRole(): String? = storage.getUserRole()
    fun getUserName(): String? = storage.getUserName()
    fun saveUserInfo(role: String, name: String) = storage.saveUserInfo(role, name)
    fun clear() = storage.clear()
}
```

### 4.3 Retrofit ApiClient — Auto Bearer Token Injection

```kotlin
// data/network/ApiClient.kt
object ApiClient {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val httpClient: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        val authInterceptor = okhttp3.Interceptor { chain ->
            val token = AuthTokenStore.getToken()
            val req = if (token.isNullOrBlank()) chain.request()
                      else chain.request().newBuilder()
                          .addHeader("Authorization", "Bearer $token").build()
            chain.proceed(req)
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logger)
            .build()
    }

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
```

### 4.4 AppContext (Application class)

```kotlin
// CatvApp.kt (বা <AppName>App.kt)
class CatvApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
    }
}

object AppContext {
    lateinit var app: Application
    fun init(application: Application) { app = application }
}
```

```xml
<!-- AndroidManifest.xml -->
<application android:name=".CatvApp" ...>
```

### 4.5 MainActivity — Login Guard

```kotlin
@Composable
fun AppRoot() {
    val token = remember { AuthTokenStore.getToken() }
    var isLoggedIn by remember { mutableStateOf(!token.isNullOrBlank()) }

    if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = { isLoggedIn = true })
    } else {
        MainAppShell(onLogout = {
            AuthTokenStore.clear()
            isLoggedIn = false
        })
    }
}
```

---

## 5. i18n Architecture (বাংলা/English)

### 5.1 ফাইল কাঠামো

```kotlin
// ui/strings/LocalStrings.kt
val LocalStrings = compositionLocalOf<AppStrings> { BengaliStrings }

// ui/strings/AppStrings.kt  (abstract class + two objects — Section 3 দেখুন)
abstract class AppStrings {
    abstract val langToggleLabel: String  // "EN" বা "বাং"
    abstract val loading: String
    // ...
}
object BengaliStrings : AppStrings() { override val langToggleLabel = "EN" ... }
object EnglishStrings : AppStrings() { override val langToggleLabel = "বাং" ... }
```

### 5.2 MainActivity-তে Language State

```kotlin
private const val LANG_PREFS = "lang_store"
private const val LANG_KEY   = "lang_bn"

// setContent { } এর ভেতরে:
val prefs = getSharedPreferences(LANG_PREFS, Context.MODE_PRIVATE)
var isBengali by remember { mutableStateOf(prefs.getBoolean(LANG_KEY, true)) }

CompositionLocalProvider(
    LocalStrings provides if (isBengali) BengaliStrings else EnglishStrings
) {
    AppRoot(onLangToggle = {
        isBengali = !isBengali
        prefs.edit().putBoolean(LANG_KEY, isBengali).apply()
    })
}
```

### 5.3 Screen-এ ব্যবহার

```kotlin
@Composable
fun MyScreen() {
    val s = LocalStrings.current  // সবার প্রথম লাইন
    Text(s.loading)
    Button(onClick = {}) { Text(s.save) }
}
```

### 5.4 ভাষা শনাক্ত করা

```kotlin
val s = LocalStrings.current
val isBengali = s.langToggleLabel == "EN"
// বাংলায় আছি → toggle button "EN" দেখায়
```

---

## 6. Common Pitfalls (সাধারণ ভুল)

### 6a. Variable Shadowing — `val s`

```kotlin
// ❌ inner s outer s কে shadow করে
@Composable fun SomeScreen() {
    val s = LocalStrings.current
    LazyColumn { item {
        val s = summary!!      // ← outer s ঢেকে যায়
        Text(s.loading)        // crash বা wrong value
    }}
}

// ✅ আলাদা নাম দিন
@Composable fun SomeScreen() {
    val s = LocalStrings.current
    LazyColumn { item {
        val sm = summary!!
        Text(s.loading)        // s এখনও LocalStrings
    }}
}
```

### 6b. Success Status String Comparison

```kotlin
// ❌ English mode-এ "সফল" কখনো থাকবে না
if (submitStatus?.contains("সফল") == true) showGreen()

// ✅ আলাদা boolean রাখুন
var submitSuccess by remember { mutableStateOf(false) }
// API success → submitSuccess = true
if (submitSuccess) showGreen()
```

### 6c. Top-level Map-এ Hardcoded String

```kotlin
// ❌ ভাষা পরিবর্তনে map আপডেট হয় না
private val priorityLabel = mapOf("LOW" to "কম", "HIGH" to "বেশি")

// ✅ fun দিয়ে করুন
private fun priorityLabel(s: AppStrings) = mapOf(
    "LOW" to s.priorityLow, "HIGH" to s.priorityHigh
)
// ব্যবহার: val pLabel = priorityLabel(s)
```

### 6d. filterCollectorName Initial Value

```kotlin
// ❌ English mode-এ "সবাই" দেখা যাবে
var filter by remember { mutableStateOf("সবাই") }

// ✅ blank রাখুন, display-এ s.allCollectors ব্যবহার করুন
var filter by remember { mutableStateOf("") }
Text(filter.ifBlank { s.allCollectors })
```

### 6e. Amount Display — Comma-formatted, Full (কোনো K/L abbreviation না)

```kotlin
// ❌ 15K বা 1.5L — ছোট করা যাবে না
private fun fmtAmt(v: Number) = "${v.toLong() / 1000}K"

// ✅ full amount with commas
private fun fmtAmt(v: Number) = String.format(Locale.US, "%,d", v.toLong())
// Output: 15,000 বা 1,50,000
```

---

## 7. Theme Colors Pattern

```kotlin
// ui/theme/Color.kt
val Primary       = Color(0xFF0F7C7B)   // teal — main action
val PrimaryStrong = Color(0xFF0B5F5E)
val Accent        = Color(0xFFFF7A59)   // error/warning
val Background    = Color(0xFFF4F0E8)   // page bg
val Surface       = Color(0xFFFFFFFF)   // card bg
val BackgroundStrong = Color(0xFFEFE7D8) // divider
val CardBorder    = Color(0x0F111B2C)
val Ink           = Color(0xFF1B1F2A)   // body text
val Muted         = Color(0xFF657089)   // placeholder
val StatusPaid    = Color(0xFF48B45E)   // green
val StatusDue     = Color(0xFFFF7A59)   // red/orange
val StatusPartial = Color(0xFFF0A84B)   // amber
val StatusActive  = Color(0xFF1AA57B)   // teal-green
val StatusClosed  = Color(0xFF7B8496)   // grey
```

---

## 8. GPS ও Camera Support (প্রয়োজন হলে)

### AndroidManifest.xml permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />        <!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
                 android:maxSdkVersion="32" />                                  <!-- Android ≤12 -->
```

### Gradle dependencies (GPS + Camera)

```kotlin
implementation("com.google.android.gms:play-services-location:21.1.0")  // FusedLocationProvider
// Camera: CameraX বা ActivityResultContracts.TakePicture()
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")
```

### Multipart file upload (Retrofit)

```kotlin
val imageFile = File(imagePath)
val requestBody = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("amount", "150.00")
    .addFormDataPart("latitude", "23.8456")
    .addFormDataPart("longitude", "90.3678")
    .addFormDataPart(
        "receipt_image", imageFile.name,
        imageFile.asRequestBody("image/jpeg".toMediaType())
    )
    .build()
```

---

## 9. React Native (Expo) বিকল্প — car.zisan.me Pattern

যখন বিদ্যমান TypeScript/JavaScript codebase আছে এবং cross-platform (iOS+Android) লক্ষ্য, তখন React Native (Expo) বেশি উপযুক্ত।

### Key packages

```json
{
  "expo-location": "GPS — FusedLocationProvider wrapper",
  "expo-camera": "Camera capture",
  "expo-image-picker": "Gallery + camera combined",
  "@react-native-async-storage/async-storage": "Token storage",
  "axios": "HTTP client with interceptor"
}
```

### Token storage + auto-inject (React Native)

```typescript
// api/client.ts
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';

const apiClient = axios.create({ baseURL: 'https://your-api.com/api' });

apiClient.interceptors.request.use(async (config) => {
    const token = await AsyncStorage.getItem('auth_token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

apiClient.interceptors.response.use(
    (res) => res,
    async (error) => {
        if (error.response?.status === 401) {
            await AsyncStorage.removeItem('auth_token');
            // navigate to login
        }
        return Promise.reject(error);
    }
);

export const login = async (email: string, password: string) => {
    const res = await apiClient.post('/auth/login.php', { email, password, source: 'mobile' });
    if (res.data.success && res.data.data.token) {
        await AsyncStorage.setItem('auth_token', res.data.data.token);
    }
    return res.data;
};
```

### কখন React Native বেছে নেবেন

| ব্যবহার করুন | যখন |
|---|---|
| **Native Kotlin + Compose** | শুধু Android, complex UI, Bluetooth/printer, offline-first |
| **React Native (Expo)** | TypeScript codebase আছে, iOS+Android দুটোই, rapid prototype |
| **Flutter** | সর্বোচ্চ performance, Dart শিখতে ইচ্ছুক |

---

## 10. সম্পূর্ণ নতুন App তৈরির Step-by-step

```
1. Backend-এ api_tokens table তৈরি করুন
2. login.php-তে source=mobile হলে token return করুন
3. _helpers.php-তে Bearer token validate করুন
4. Android Studio-তে নতুন Empty Compose Activity project তৈরি করুন
5. build.gradle.kts-এ Section 1.2 অনুযায়ী dependencies যোগ করুন
6. data/auth/ folder: AuthStorage, AuthTokenStore, AuthRepository
7. data/network/: ApiClient (Section 4.3), ApiService (Retrofit interface)
8. ui/strings/: AppStrings.kt (abstract class pattern), LocalStrings.kt
9. ui/theme/: Color.kt (Section 7)
10. MainActivity.kt: language state + CompositionLocalProvider + login guard
11. ui/screens/LoginScreen.kt তৈরি করুন
12. ui/MainAppShell.kt: navigation drawer বা bottom nav
13. বাকি screens একে একে
14. ./gradlew assembleDebug → build
15. APK copy করুন → gen-index.sh চালান
```

---

## 11. Backend পরিবর্তনের Checklist (নতুন project)

| কাজ | ফাইল | অবস্থা |
|---|---|---|
| `api_tokens` table তৈরি | MySQL migration | করতে হবে |
| Bearer token validation | `api/_helpers.php` | করতে হবে |
| `source: "mobile"` হলে token return | `api/auth/login.php` | করতে হবে |
| Logout → token revoke | `api/auth/logout.php` | করতে হবে |
| me.php token support | `api/auth/me.php` | করতে হবে |

Web browser session auth অপরিবর্তিত রাখুন — backward compatible।

---

## 12. Guardrails for Future Agents

1. **সবসময় `assembleDebug`** — `assembleRelease` unsigned, ইনস্টল হয় না।
2. **APK deploy মানে দুটো কাজ**: copy → gen-index.sh (দুটোই করতে হবে)।
3. **String localization**: data class + top-level val নয়, abstract class + object।
4. **Boolean state**: Bengali string দিয়ে success check করবেন না।
5. **`val s` shadowing**: inner scope-এ `s` নামে অন্য variable রাখবেন না।
6. **Top-level map**: hardcoded বাংলা string রাখবেন না — fun দিয়ে s পাস করুন।
7. **Token auto-inject**: Retrofit Interceptor-এ করুন, প্রতিটি call-এ manually দেবেন না।
8. **`BuildConfig.API_BASE_URL`**: hardcode করবেন না, build.gradle.kts-এ রাখুন।
