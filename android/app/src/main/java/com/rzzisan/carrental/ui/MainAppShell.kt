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
    object Ledger  : Screen("ledger")
    object Trips   : Screen("trips")
    object Profile : Screen("profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(onLogout: () -> Unit) {
    val s = LocalStrings.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val navItems = listOf(
        Triple(Screen.Ledger,  Icons.Filled.AccountBalanceWallet, s.navLedger),
        Triple(Screen.Trips,   Icons.Filled.DirectionsCar,        s.navTrips),
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
            startDestination = Screen.Ledger.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Ledger.route)  { LedgerScreen() }
            composable(Screen.Trips.route)   { TripsScreen(navController) }
            composable(Screen.Profile.route) {
                ProfileScreen(onLogout = {
                    scope.launch {
                        try { ApiClient.service.logout() } catch (_: Exception) {}
                        AuthTokenStore.clear()
                        onLogout()
                    }
                })
            }
            composable("trip_detail/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                TripDetailScreen(rentalId = id, navController = navController)
            }
            composable("create_trip") {
                CreateTripScreen(navController = navController)
            }
            composable("add_expense/{rentalId}") { backStackEntry ->
                val rentalId = backStackEntry.arguments?.getString("rentalId")?.toIntOrNull() ?: return@composable
                AddExpenseScreen(rentalId = rentalId, navController = navController)
            }
        }
    }
}
