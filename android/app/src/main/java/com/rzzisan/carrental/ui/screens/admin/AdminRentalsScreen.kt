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
import com.rzzisan.carrental.data.network.AdminUpdateStatusRequest
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.Rental
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRentalsScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var rentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var actionRental by remember { mutableStateOf<Rental?>(null) }
    var actionLoading by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf("") }
    val snackState = remember { SnackbarHostState() }

    fun load(status: String? = selectedStatus) {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getAdminRentals(status = status)
                if (res.success) rentals = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }

    fun changeStatus(rental: Rental, newStatus: String) {
        actionLoading = true
        scope.launch {
            try {
                val res = ApiClient.service.updateAdminRentalStatus(rental.id, AdminUpdateStatusRequest(newStatus))
                snackMsg = if (res.success) res.message ?: s.success else res.message ?: s.error
                if (res.success) load()
            } catch (e: HttpException) {
                snackMsg = "HTTP ${e.code()}: ${e.response()?.errorBody()?.string() ?: s.error}"
            } catch (e: Exception) {
                snackMsg = "${e.javaClass.simpleName}: ${e.message}"
            } finally {
                actionLoading = false
                actionRental = null
                snackState.showSnackbar(snackMsg)
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.adminRentalsTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Status filter
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(null to s.allStatus, "pending" to s.statusPending,
                    "active" to s.statusActive, "completed" to s.statusCompleted,
                    "cancelled" to s.statusCancelled
                ).forEach { (st, label) ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { selectedStatus = st; load(st) },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary, selectedLabelColor = Color.White)
                    )
                }
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
                error.isNotEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                        Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                    }
                }
                rentals.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(s.noRentals, color = InkMuted) }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rentals) { r ->
                        AdminRentalCard(r, s, onAction = { actionRental = r })
                    }
                }
            }
        }
    }

    // Status action bottom sheet
    actionRental?.let { rental ->
        ModalBottomSheet(onDismissRequest = { if (!actionLoading) actionRental = null }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("#${rental.id} — ${rental.vehicleBrand ?: ""} ${rental.vehicleModel ?: ""}",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${s.statusChangeQuestion}", color = InkMuted, fontSize = 13.sp)
                if (actionLoading) {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
                } else {
                    when (rental.rentalStatus) {
                        "pending" -> {
                            Button(onClick = { changeStatus(rental, "active") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusActive)) {
                                Text(s.activateTrip)
                            }
                            OutlinedButton(onClick = { changeStatus(rental, "cancelled") },
                                modifier = Modifier.fillMaxWidth()) {
                                Text(s.cancelTripBtn, color = Color(0xFFEF4444))
                            }
                        }
                        "active" -> {
                            Button(onClick = { changeStatus(rental, "completed") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                                Text(s.completeTrip)
                            }
                            OutlinedButton(onClick = { changeStatus(rental, "cancelled") },
                                modifier = Modifier.fillMaxWidth()) {
                                Text(s.cancelTripBtn, color = Color(0xFFEF4444))
                            }
                        }
                        else -> {
                            Text("এই ট্রিপের স্ট্যাটাস পরিবর্তন করা যাবে না", color = InkMuted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRentalCard(r: Rental, s: com.rzzisan.carrental.ui.strings.AppStrings, onAction: () -> Unit) {
    val (statusColor, statusLabel) = when (r.rentalStatus) {
        "active"    -> StatusActive      to s.statusActive
        "pending"   -> Color(0xFFF59E0B) to s.statusPending
        "completed" -> StatusPaid        to s.statusCompleted
        else        -> InkMuted          to s.statusCancelled
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("#${r.id} · ${r.vehicleBrand ?: ""} ${r.vehicleModel ?: ""}",
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                    Text(r.vehicleRegNumber ?: "", fontSize = 11.sp, color = InkMuted)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusLabel, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            if (!r.pickupLocation.isNullOrBlank()) {
                Text("${r.pickupLocation} → ${r.dropoffLocation ?: "—"}", fontSize = 12.sp, color = InkMuted, maxLines = 1)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val customer = listOfNotNull(r.customerFirstName, r.customerLastName).joinToString(" ")
                    if (customer.isNotBlank()) Text("👤 $customer", fontSize = 12.sp, color = InkMuted)
                    if (!r.driverName.isNullOrBlank()) Text("🚗 ${r.driverName}", fontSize = 12.sp, color = InkMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.taka}${String.format("%.0f", r.agreedAmount)}",
                        fontWeight = FontWeight.Bold, color = Primary, fontSize = 15.sp)
                    r.startDate?.let { Text(it.take(10), fontSize = 11.sp, color = InkMuted) }
                }
            }
            if (r.rentalStatus in listOf("pending", "active")) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(s.confirmStatusChange, fontSize = 13.sp)
                }
            }
        }
    }
}
