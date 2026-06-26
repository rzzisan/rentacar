package com.rzzisan.carrental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.rzzisan.carrental.data.auth.AuthTokenStore
import com.rzzisan.carrental.ui.AdminAppShell
import com.rzzisan.carrental.ui.ManagerAppShell
import com.rzzisan.carrental.ui.MainAppShell
import com.rzzisan.carrental.ui.screens.LoginScreen
import com.rzzisan.carrental.ui.strings.BanglaStrings
import com.rzzisan.carrental.ui.strings.EnglishStrings
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.CarRentalTheme
import androidx.compose.runtime.CompositionLocalProvider

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

    if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = { isLoggedIn = true })
    } else when (role) {
        "admin"   -> AdminAppShell(onLogout = { isLoggedIn = false })
        "manager" -> ManagerAppShell(onLogout = { isLoggedIn = false })
        else      -> MainAppShell(onLogout = { isLoggedIn = false })
    }
}
