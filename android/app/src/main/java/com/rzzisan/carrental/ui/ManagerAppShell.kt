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
import com.rzzisan.carrental.ui.screens.admin.AdminCustomersScreen
import com.rzzisan.carrental.ui.screens.manager.*
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.Primary
import kotlinx.coroutines.launch

sealed class ManagerScreen(val route: String) {
    object Dashboard   : ManagerScreen("mgr_dashboard")
    object Vehicles    : ManagerScreen("mgr_vehicles")
    object Rentals     : ManagerScreen("mgr_rentals")
    object Settlements : ManagerScreen("mgr_settlements")
    object Drivers     : ManagerScreen("mgr_drivers")
    object Reports     : ManagerScreen("mgr_reports")
    object Customers   : ManagerScreen("mgr_customers")
}

@Composable
fun ManagerAppShell(onLogout: () -> Unit) {
    val s = LocalStrings.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val navItems = listOf(
        Triple(ManagerScreen.Dashboard,   Icons.Filled.Home,              s.navDashboard),
        Triple(ManagerScreen.Vehicles,    Icons.Filled.DirectionsCar,     s.navMgrVehicles),
        Triple(ManagerScreen.Rentals,     Icons.Filled.Assignment,        s.navRentals),
        Triple(ManagerScreen.Settlements, Icons.Filled.AccountBalance,    s.navSettlements),
        Triple(ManagerScreen.Drivers,     Icons.Filled.People,            s.navMgrDrivers),
        Triple(ManagerScreen.Customers,   Icons.Filled.Group,             s.navCustomers),
        Triple(ManagerScreen.Reports,     Icons.Filled.BarChart,          s.navReports),
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
            startDestination = ManagerScreen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ManagerScreen.Dashboard.route) {
                ManagerDashboardScreen(onLogout = {
                    scope.launch {
                        try { ApiClient.service.logout() } catch (_: Exception) {}
                        AuthTokenStore.clear()
                        onLogout()
                    }
                })
            }
            composable(ManagerScreen.Vehicles.route)    { ManagerVehiclesScreen() }
            composable(ManagerScreen.Rentals.route)     { ManagerRentalsScreen() }
            composable(ManagerScreen.Settlements.route) { ManagerSettlementsScreen() }
            composable(ManagerScreen.Drivers.route)     { ManagerDriversScreen() }
            composable(ManagerScreen.Customers.route)   { AdminCustomersScreen(isAdmin = false) }
            composable(ManagerScreen.Reports.route)     { ManagerReportsScreen() }
        }
    }
}
