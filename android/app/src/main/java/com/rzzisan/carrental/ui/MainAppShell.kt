package com.rzzisan.carrental.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rzzisan.carrental.data.auth.AuthTokenStore
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.ui.screens.*
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.Primary
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home    : Screen("home")
    object Trips   : Screen("trips")
    object Ledger  : Screen("ledger")
    object Profile : Screen("profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(onLogout: () -> Unit) {
    val s = LocalStrings.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Trip ID to auto-open when navigating from Home → Trips
    var pendingTripId by remember { mutableStateOf<Int?>(null) }

    val navItems = listOf(
        Triple(Screen.Home,    Icons.Filled.Home,                 s.navHome),
        Triple(Screen.Trips,   Icons.Filled.DirectionsCar,        s.navTrips),
        Triple(Screen.Ledger,  Icons.Filled.AccountBalanceWallet, s.navLedger),
        Triple(Screen.Profile, Icons.Filled.Person,               s.navProfile),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination
                navItems.forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, maxLines = 1) },
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Primary.copy(alpha = 0.15f))
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onTripClick = { tripId ->
                    pendingTripId = tripId
                    navController.navigate(Screen.Trips.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = false
                    }
                })
            }
            composable(Screen.Trips.route) {
                TripsScreen(
                    navController = navController,
                    openTripId = pendingTripId,
                    onTripOpened = { pendingTripId = null }
                )
            }
            composable(Screen.Ledger.route) { LedgerScreen() }
            composable(Screen.Profile.route) {
                ProfileScreen(onLogout = {
                    scope.launch {
                        try { ApiClient.service.logout() } catch (_: Exception) {}
                        AuthTokenStore.clear()
                        onLogout()
                    }
                })
            }
        }
    }
}
