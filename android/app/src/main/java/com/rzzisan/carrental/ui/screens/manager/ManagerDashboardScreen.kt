package com.rzzisan.carrental.ui.screens.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.rzzisan.carrental.data.network.AdminStats
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.DashboardTrip
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(onLogout: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getManagerStats()
                if (res.success) stats = res.data else error = res.message ?: s.error
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.dashboardTitle, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, s.logout) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                    Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                }
            }
            stats != null -> {
                val st = stats!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MgrStatCard(Modifier.weight(1f), Icons.Filled.DirectionsCar,
                                    s.totalVehicles, "${st.totalVehicles}", Color(0xFF3B82F6))
                                MgrStatCard(Modifier.weight(1f), Icons.Filled.CheckCircle,
                                    s.availableVehicles, "${st.availableVehicles}", Color(0xFF10B981))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MgrStatCard(Modifier.weight(1f), Icons.Filled.PlayCircle,
                                    s.activeTrips, "${st.activeRentals}", StatusActive)
                                MgrStatCard(Modifier.weight(1f), Icons.Filled.Schedule,
                                    s.statusPending, "${st.pendingRentals}", Color(0xFFF59E0B))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MgrStatCard(Modifier.weight(1f), Icons.Filled.TrendingUp,
                                    s.monthlyRevenue, "${s.taka}${String.format("%.0f", st.monthlyRevenue)}", Primary)
                                MgrStatCard(Modifier.weight(1f), Icons.Filled.Warning,
                                    s.totalDues, "${s.taka}${String.format("%.0f", st.totalDues)}", Color(0xFFEF4444))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MgrStatCard(Modifier.weight(1f), Icons.Filled.Today,
                                    s.todayTrips, "${st.todayTrips}", Color(0xFF8B5CF6))
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    item { MgrSectionHeader(s.activeTripsTitle, st.activeTrips.size, StatusActive) }
                    if (st.activeTrips.isEmpty()) {
                        item { MgrEmptyNote(s.noActiveTrips) }
                    } else {
                        items(st.activeTrips) { trip -> MgrDashboardTripCard(trip, StatusActive) }
                    }

                    item { MgrSectionHeader(s.upcomingTripsTitle, st.upcomingTrips.size, Color(0xFFF59E0B)) }
                    if (st.upcomingTrips.isEmpty()) {
                        item { MgrEmptyNote(s.noUpcomingTrips) }
                    } else {
                        items(st.upcomingTrips) { trip -> MgrDashboardTripCard(trip, Color(0xFFF59E0B)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MgrStatCard(modifier: Modifier, icon: ImageVector, label: String, value: String, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Text(label, fontSize = 11.sp, color = InkMuted, maxLines = 1)
            }
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

@Composable
internal fun MgrSectionHeader(title: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(4.dp, 20.dp).background(color, RoundedCornerShape(2.dp)))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.15f)) {
            Text("$count", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}

@Composable
internal fun MgrDashboardTripCard(trip: DashboardTrip, statusColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("#${trip.id} · ${trip.vehicleBrand ?: ""} ${trip.vehicleModel ?: ""}",
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text("৳${String.format("%.0f", trip.agreedAmount)}",
                        fontSize = 13.sp, color = statusColor, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            if (!trip.pickupLocation.isNullOrBlank() || !trip.dropoffLocation.isNullOrBlank()) {
                Text("${trip.pickupLocation ?: "—"} → ${trip.dropoffLocation ?: "—"}",
                    fontSize = 12.sp, color = InkMuted, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val customer = listOfNotNull(trip.customerFirstName, trip.customerLastName).joinToString(" ")
                if (customer.isNotBlank()) Text("👤 $customer", fontSize = 12.sp, color = InkMuted)
                if (!trip.driverName.isNullOrBlank()) Text("🚗 ${trip.driverName}", fontSize = 12.sp, color = InkMuted)
            }
        }
    }
}

@Composable
internal fun MgrEmptyNote(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) {
        Text(text, fontSize = 13.sp, color = InkMuted)
    }
}
