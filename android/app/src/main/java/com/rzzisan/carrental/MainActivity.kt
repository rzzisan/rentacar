package com.rzzisan.carrental

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.rzzisan.carrental.data.auth.AuthTokenStore
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.VersionInfo
import com.rzzisan.carrental.ui.AdminAppShell
import com.rzzisan.carrental.ui.ManagerAppShell
import com.rzzisan.carrental.ui.MainAppShell
import com.rzzisan.carrental.ui.screens.LoginScreen
import com.rzzisan.carrental.ui.strings.BanglaStrings
import com.rzzisan.carrental.ui.strings.EnglishStrings
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.CarRentalTheme
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.launch

private const val LANG_PREFS = "lang_store"
private const val LANG_KEY   = "lang_bn"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = getSharedPreferences(LANG_PREFS, MODE_PRIVATE)
            var isBangla by remember { mutableStateOf(prefs.getBoolean(LANG_KEY, true)) }

            CarRentalTheme {
                CompositionLocalProvider(
                    LocalStrings provides if (isBangla) BanglaStrings else EnglishStrings
                ) {
                    AppRoot(onLangToggle = {
                        isBangla = !isBangla
                        prefs.edit().putBoolean(LANG_KEY, isBangla).apply()
                    })
                }
            }
        }
    }
}

@Composable
fun AppRoot(onLangToggle: () -> Unit) {
    var isLoggedIn by remember { mutableStateOf(!AuthTokenStore.getToken().isNullOrBlank()) }
    val role = AuthTokenStore.getRole()

    UpdateCheckDialog()

    if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = { isLoggedIn = true })
    } else when (role) {
        "admin"   -> AdminAppShell(onLogout = { isLoggedIn = false })
        "manager" -> ManagerAppShell(onLogout = { isLoggedIn = false })
        "driver"  -> MainAppShell(onLogout = { isLoggedIn = false })
        else -> {
            // অজানা/অসমর্থিত role (superadmin/customer/employee/টাইপো) — এই অ্যাপ শুধু
            // admin/manager/driver-এর জন্য; আগে এখানে সাইলেন্টলি driver shell দেখানো হতো,
            // এখন explicit allow-list — ম্যাচ না হলে logout করে লগইন স্ক্রিনে ফেরত পাঠানো হয়
            LaunchedEffect(role) {
                AuthTokenStore.clear()
                isLoggedIn = false
            }
        }
    }
}

// Play Store-এ নেই বলে নিজস্ব update-check — অ্যাপ চালু হলে একবার /apk/version.json ফেচ করে
// BuildConfig.VERSION_CODE-এর সাথে তুলনা করে, নতুন থাকলে ডায়ালগ দেখায়। নেটওয়ার্ক/parse ব্যর্থ
// হলে নিরবে উপেক্ষা করে (এটা কোনো critical flow না, লগইন/ব্যবহারে বাধা দেওয়া উচিত না)
@Composable
private fun UpdateCheckDialog() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<VersionInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val info = ApiClient.service.getAppVersion()
                if (info.versionCode > BuildConfig.VERSION_CODE) updateInfo = info
            } catch (_: Exception) {
                // নীরবে উপেক্ষা — সার্ভারে version.json না থাকা বা নেটওয়ার্ক সমস্যা অ্যাপ ব্যবহারে বাধা দেবে না
            }
        }
    }

    val info = updateInfo
    if (info != null && !dismissed) {
        AlertDialog(
            onDismissRequest = { if (!info.forceUpdate) dismissed = true },
            title = { Text("নতুন আপডেট উপলব্ধ (${info.versionName})") },
            text = { Text(info.changelog?.takeIf { it.isNotBlank() } ?: "নতুন ভার্সনে বাগ ফিক্স ও উন্নতি আছে।") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl)))
                }) { Text("ডাউনলোড করুন") }
            },
            dismissButton = if (info.forceUpdate) null else {
                { TextButton(onClick = { dismissed = true }) { Text("পরে করব") } }
            }
        )
    }
}
