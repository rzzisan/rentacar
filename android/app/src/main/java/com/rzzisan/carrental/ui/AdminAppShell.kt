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
import com.rzzisan.carrental.ui.screens.admin.*
import com.rzzisan.carrental.ui.strings.AppStrings
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.Primary
import kotlinx.coroutines.launch

sealed class AdminScreen(val route: String) {
    object Dashboard    : AdminScreen("admin_dashboard")
    object Vehicles     : AdminScreen("admin_vehicles")
    object Rentals      : AdminScreen("admin_rentals")
    object Settlements  : AdminScreen("admin_settlements")
    object Drivers      : AdminScreen("admin_drivers")
    object Managers     : AdminScreen("admin_managers")
    object Customers    : AdminScreen("admin_customers")
    object Reports      : AdminScreen("admin_reports")
    object Maintenance  : AdminScreen("admin_maintenance")
}

@Composable
fun AdminAppShell(onLogout: () -> Unit) {
    val s = LocalStrings.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val navItems = listOf(
        Triple(AdminScreen.Dashboard,   Icons.Filled.Home,              s.navDashboard),
        Triple(AdminScreen.Vehicles,    Icons.Filled.DirectionsCar,     s.navVehicles),
        Triple(AdminScreen.Rentals,     Icons.Filled.Assignment,        s.navRentals),
        Triple(AdminScreen.Settlements, Icons.Filled.AccountBalance,    s.navSettlements),
        Triple(AdminScreen.Drivers,     Icons.Filled.People,            s.navDrivers),
        Triple(AdminScreen.Managers,    Icons.Filled.SupervisorAccount, s.navManagers),
        Triple(AdminScreen.Customers,   Icons.Filled.Group,             s.navCustomers),
        Triple(AdminScreen.Reports,     Icons.Filled.BarChart,          s.navAdminReports),
        Triple(AdminScreen.Maintenance, Icons.Filled.Build,             s.navAdminMaintenance),
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
            startDestination = AdminScreen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AdminScreen.Dashboard.route) {
                AdminDashboardScreen(onLogout = {
                    scope.launch {
                        try { ApiClient.service.logout() } catch (_: Exception) {}
                        AuthTokenStore.clear()
                        onLogout()
                    }
                })
            }
            composable(AdminScreen.Vehicles.route)    { AdminVehiclesScreen() }
            composable(AdminScreen.Rentals.route)     { AdminRentalsScreen() }
            composable(AdminScreen.Settlements.route) { AdminSettlementsScreen() }
            composable(AdminScreen.Drivers.route)     { AdminDriversScreen() }
            composable(AdminScreen.Managers.route)     { AdminManagersScreen() }
            composable(AdminScreen.Customers.route)    { AdminCustomersScreen(isAdmin = true) }
            composable(AdminScreen.Reports.route)      { AdminReportsScreen() }
            composable(AdminScreen.Maintenance.route)  { AdminMaintenanceScreen() }
        }
    }
}
