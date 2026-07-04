package com.rzzisan.carrental.ui.screens.manager

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.Vehicle
import com.rzzisan.carrental.data.network.VehicleStatusRequest
import com.rzzisan.carrental.util.errorMessageOf
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerVehiclesScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var statusTarget by remember { mutableStateOf<Vehicle?>(null) }
    val snackState = remember { SnackbarHostState() }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getManagerVehicles()
                if (res.success) vehicles = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = errorMessageOf(e, s.serverError) }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.mgrVehiclesTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                    Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                }
            }
            vehicles.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(s.noVehicles, color = InkMuted)
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(vehicles) { v ->
                    MgrVehicleCard(v, s, onChangeStatus = { statusTarget = v })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // Status change sheet
    statusTarget?.let { v ->
        StatusChangeSheet(
            vehicle = v,
            s = s,
            onDismiss = { statusTarget = null },
            onChanged = { msg ->
                statusTarget = null; load()
                scope.launch { snackState.showSnackbar(msg) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChangeSheet(
    vehicle: Vehicle,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    val statuses = listOf(
        "available" to s.statusAvailable,
        "maintenance" to s.statusMaintenance,
        "inactive" to s.statusInactive
    )

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(s.changeStatus, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("${vehicle.brand} ${vehicle.model} (${vehicle.registrationNumber})",
                fontSize = 13.sp, color = InkMuted)
            Text("${s.currentStatus}: ${vehicle.status}", fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium)

            if (vehicle.status == "rented") {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEF2F2)) {
                    Text(s.cannotChangeRented, color = Color(0xFFDC2626), fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp))
                }
            } else {
                statuses.forEach { (statusKey, label) ->
                    if (statusKey != vehicle.status) {
                        Button(
                            onClick = {
                                submitting = true
                                scope.launch {
                                    try {
                                        val res = ApiClient.service.updateManagerVehicleStatus(
                                            vehicle.id, VehicleStatusRequest(statusKey)
                                        )
                                        onChanged(if (res.success) s.vehicleStatusChanged else res.message ?: s.error)
                                    } catch (e: Exception) {
                                        onChanged("${e.javaClass.simpleName}: ${e.message}")
                                    } finally { submitting = false }
                                }
                            },
                            enabled = !submitting,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (statusKey) {
                                    "available" -> StatusActive
                                    "maintenance" -> Color(0xFFF59E0B)
                                    else -> InkMuted
                                }
                            )
                        ) {
                            Text(label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MgrVehicleCard(
    v: Vehicle,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onChangeStatus: () -> Unit
) {
    val statusColor = when (v.status) {
        "available" -> StatusActive
        "rented" -> Primary
        "maintenance" -> Color(0xFFF59E0B)
        else -> InkMuted
    }
    val statusLabel = when (v.status) {
        "available" -> s.statusAvailable
        "rented" -> s.statusRented
        "maintenance" -> s.statusMaintenance
        "inactive" -> s.statusInactive
        else -> v.status
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("${v.brand} ${v.model}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                    Text(v.registrationNumber, fontSize = 12.sp, color = InkMuted)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(statusLabel, fontSize = 11.sp, color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(v.vehicleType.uppercase(), fontSize = 11.sp, color = InkMuted)
                v.seatingCapacity?.let { Text("$it ${s.seatsLabel}", fontSize = 11.sp, color = InkMuted) }
                Text("৳${String.format("%.0f", v.dailyRentPrice)}/দিন", fontSize = 11.sp, color = Primary)
            }
            if (v.status != "rented") {
                OutlinedButton(
                    onClick = onChangeStatus,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(16.dp), tint = Primary)
                    Spacer(Modifier.width(6.dp))
                    Text(s.changeStatus, fontSize = 13.sp, color = Primary)
                }
            }
        }
    }
}
