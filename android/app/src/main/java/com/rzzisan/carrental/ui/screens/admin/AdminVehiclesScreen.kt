package com.rzzisan.carrental.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.Vehicle
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVehiclesScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    fun load(status: String? = selectedStatus) {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getAdminVehicles(status = status)
                if (res.success) vehicles = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.vehiclesTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Status filter chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(null to s.allStatus, "available" to s.statusAvailable,
                    "rented" to s.statusRented, "maintenance" to s.statusMaintenance,
                    "inactive" to s.statusInactive
                ).forEach { (status, label) ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status; load(status) },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                error.isNotEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                        Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                    }
                }
                vehicles.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(s.noVehicles, color = InkMuted)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vehicles) { v -> VehicleCard(v, s) }
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(v: Vehicle, s: com.rzzisan.carrental.ui.strings.AppStrings) {
    val (statusColor, statusLabel) = when (v.status) {
        "available"   -> Color(0xFF10B981) to s.statusAvailable
        "rented"      -> StatusActive      to s.statusRented
        "maintenance" -> Color(0xFFF59E0B) to s.statusMaintenance
        else          -> InkMuted          to s.statusInactive
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("${v.brand} ${v.model}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                    Text(v.registrationNumber, fontSize = 12.sp, color = InkMuted)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusLabel, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoChip(Icons.Filled.Category, v.vehicleType.replaceFirstChar { it.uppercase() })
                v.color?.let { InfoChip(Icons.Filled.Palette, it) }
                v.seatingCapacity?.let { InfoChip(Icons.Filled.AirlineSeatReclineNormal, "${it} ${s.seatingCapacity}") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                v.year?.let { Text("${it}", fontSize = 12.sp, color = InkMuted) }
                Text("${s.taka}${String.format("%.0f", v.dailyRentPrice)}/${s.dailyRate}",
                    fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = InkMuted, modifier = Modifier.size(13.dp))
        Text(text, fontSize = 12.sp, color = InkMuted)
    }
}
