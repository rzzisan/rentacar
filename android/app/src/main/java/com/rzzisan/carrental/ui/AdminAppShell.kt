package com.rzzisan.carrental.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rzzisan.carrental.data.auth.AuthTokenStore
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.ui.screens.admin.*
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.Primary
import kotlinx.coroutines.launch

val LocalAdminDrawerState = staticCompositionLocalOf<DrawerState?> { null }

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

private data class NavItem(
    val screen: AdminScreen,
    val icon: ImageVector,
    val label: String,
    val group: Int = 0,
)

private val SidebarBg          = Color(0xFF0F172A)
private val SidebarHeader      = Color(0xFF1E293B)
private val SidebarDivider     = Color(0xFF1E293B)
private val SidebarText        = Color(0xFFCBD5E1)
private val SidebarActiveText  = Color.White
private val SidebarActiveBg    = Color(0xFF312E81)   // indigo-900
private val SidebarActiveIcon  = Color(0xFF818CF8)   // indigo-400
private val SidebarLogoutColor = Color(0xFFEF4444)

@Composable
fun AdminAppShell(onLogout: () -> Unit) {
    val s = LocalStrings.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val navItems = listOf(
        NavItem(AdminScreen.Dashboard,   Icons.Filled.Home,              s.navDashboard,         0),
        NavItem(AdminScreen.Vehicles,    Icons.Filled.DirectionsCar,     s.navVehicles,          0),
        NavItem(AdminScreen.Rentals,     Icons.Filled.Assignment,        s.navRentals,           0),
        NavItem(AdminScreen.Settlements, Icons.Filled.AccountBalance,    s.navSettlements,       0),
        NavItem(AdminScreen.Drivers,     Icons.Filled.People,            s.navDrivers,           0),
        NavItem(AdminScreen.Managers,    Icons.Filled.SupervisorAccount, s.navManagers,          0),
        NavItem(AdminScreen.Customers,   Icons.Filled.Group,             s.navCustomers,         0),
        NavItem(AdminScreen.Reports,     Icons.Filled.BarChart,          s.navAdminReports,      1),
        NavItem(AdminScreen.Maintenance, Icons.Filled.Build,             s.navAdminMaintenance,  1),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    fun navigate(screen: AdminScreen) {
        scope.launch { drawerState.close() }
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalAdminDrawerState provides drawerState) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(272.dp),
                    drawerContainerColor = SidebarBg,
                    drawerContentColor = SidebarText,
                ) {
                    // ── Header ─────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SidebarHeader)
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.DirectionsCar, null, tint = SidebarActiveIcon, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("কার রেন্টাল", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp)
                                Text("Admin Panel", color = SidebarText, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Main nav items ──────────────────────────────────────
                    navItems.filter { it.group == 0 }.forEach { item ->
                        val selected = currentDest?.hierarchy?.any { it.route == item.screen.route } == true
                        SidebarItem(item.icon, item.label, selected, onClick = { navigate(item.screen) })
                    }

                    // ── Analytics group ─────────────────────────────────────
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = SidebarDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "বিশ্লেষণ",
                        color = Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                    navItems.filter { it.group == 1 }.forEach { item ->
                        val selected = currentDest?.hierarchy?.any { it.route == item.screen.route } == true
                        SidebarItem(item.icon, item.label, selected, onClick = { navigate(item.screen) })
                    }

                    // ── Logout ──────────────────────────────────────────────
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(color = SidebarDivider, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(4.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Logout, null, modifier = Modifier.size(20.dp)) },
                        label = { Text(s.logout, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                try { ApiClient.service.logout() } catch (_: Exception) {}
                                AuthTokenStore.clear()
                                onLogout()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = SidebarLogoutColor,
                            unselectedTextColor = SidebarLogoutColor,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = AdminScreen.Dashboard.route,
            ) {
                composable(AdminScreen.Dashboard.route)    { AdminDashboardScreen() }
                composable(AdminScreen.Vehicles.route)     { AdminVehiclesScreen() }
                composable(AdminScreen.Rentals.route)      { AdminRentalsScreen() }
                composable(AdminScreen.Settlements.route)  { AdminSettlementsScreen() }
                composable(AdminScreen.Drivers.route)      { AdminDriversScreen() }
                composable(AdminScreen.Managers.route)     { AdminManagersScreen() }
                composable(AdminScreen.Customers.route)    { AdminCustomersScreen(isAdmin = true) }
                composable(AdminScreen.Reports.route)      { AdminReportsScreen() }
                composable(AdminScreen.Maintenance.route)  { AdminMaintenanceScreen() }
            }
        }
    }
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        icon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
        label = { Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = SidebarActiveBg,
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = SidebarActiveIcon,
            unselectedIconColor = SidebarText,
            selectedTextColor = SidebarActiveText,
            unselectedTextColor = SidebarText,
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
    )
}
