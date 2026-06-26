package com.rzzisan.carrental.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rzzisan.carrental.data.network.*
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import com.rzzisan.carrental.util.reverseGeocode
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(rentalId: Int, navController: NavController) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocation = remember { LocationServices.getFusedLocationProviderClient(context) }

    var rental by remember { mutableStateOf<Rental?>(null) }
    var expenses by remember { mutableStateOf<List<TripExpense>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }

    fun reload() {
        scope.launch {
            try {
                val r = ApiClient.service.getRental(rentalId)
                if (r.success) rental = r.data
                val e = ApiClient.service.getExpenses(rentalId)
                if (e.success) expenses = e.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(rentalId) {
        loading = true
        try {
            val r = ApiClient.service.getRental(rentalId)
            if (r.success) rental = r.data else error = r.message ?: s.error
            val e = ApiClient.service.getExpenses(rentalId)
            if (e.success) expenses = e.data ?: emptyList()
        } catch (ex: Exception) {
            error = s.serverError
        } finally { loading = false }
    }

    fun updateStatus(newStatus: String) {
        actionLoading = true
        statusMsg = s.gettingLocation
        scope.launch {
            try {
                @Suppress("MissingPermission")
                val loc = fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                val locName = if (loc != null) reverseGeocode(context, loc.latitude, loc.longitude) else "Unknown"
                val res = ApiClient.service.updateStatus(
                    rentalId,
                    UpdateStatusRequest(
                        status = newStatus,
                        locationName = locName,
                        latitude = loc?.latitude ?: 0.0,
                        longitude = loc?.longitude ?: 0.0
                    )
                )
                statusMsg = res.message ?: s.success
                reload()
            } catch (ex: Exception) {
                statusMsg = s.serverError
            } finally { actionLoading = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.tripDetail, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, s.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error.isNotEmpty() -> Text(error, Modifier.align(Alignment.Center), color = StatusDue)
                rental == null -> Text(s.error, Modifier.align(Alignment.Center))
                else -> {
                    val r = rental!!
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Trip info card
                            Card(shape = RoundedCornerShape(12.dp), border = CardDefaults.outlinedCardBorder()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    InfoRow(s.passenger, "${r.customerFirstName ?: ""} ${r.customerLastName ?: ""}".trim())
                                    InfoRow(s.vehicle, "${r.vehicleBrand ?: ""} ${r.vehicleModel ?: ""}")
                                    InfoRow(s.pickup, r.pickupLocation ?: "-")
                                    InfoRow(s.dropoff, r.dropoffLocation ?: "-")
                                    InfoRow(s.agreedAmount, fmtBDT(r.agreedAmount))
                                    if (r.actualStartTime != null) InfoRow(s.startTime, r.actualStartTime)
                                    if (r.actualEndTime != null)   InfoRow(s.endTime,   r.actualEndTime)
                                }
                            }
                        }

                        // Status message
                        if (statusMsg.isNotEmpty()) {
                            item {
                                Surface(color = PrimaryLight, shape = RoundedCornerShape(8.dp)) {
                                    Text(statusMsg, color = Primary, fontSize = 13.sp,
                                        modifier = Modifier.padding(12.dp).fillMaxWidth())
                                }
                            }
                        }

                        // Action buttons
                        item {
                            when (r.rentalStatus) {
                                "pending" -> Button(
                                    onClick = { updateStatus("active") },
                                    enabled = !actionLoading,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusActive)
                                ) { Text(if (actionLoading) s.gettingLocation else s.startTrip, fontWeight = FontWeight.SemiBold) }

                                "active" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { navController.navigate("add_expense/$rentalId") },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                    ) { Text(s.addExpense, fontWeight = FontWeight.SemiBold) }

                                    OutlinedButton(
                                        onClick = { updateStatus("completed") },
                                        enabled = !actionLoading,
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) { Text(if (actionLoading) s.gettingLocation else s.completeTrip, fontWeight = FontWeight.SemiBold) }
                                }
                            }
                        }

                        // Expenses
                        if (expenses.isNotEmpty()) {
                            item { Text(s.addExpense, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink) }
                            items(expenses) { exp ->
                                Card(shape = RoundedCornerShape(10.dp), border = CardDefaults.outlinedCardBorder()) {
                                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text(exp.expenseType, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            if (!exp.description.isNullOrBlank()) Text(exp.description, fontSize = 12.sp, color = InkMuted)
                                            if (!exp.locationName.isNullOrBlank()) Text(exp.locationName, fontSize = 11.sp, color = InkLight)
                                        }
                                        Text(fmtBDT(exp.amount), fontWeight = FontWeight.Bold, color = StatusDue, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = InkMuted, modifier = Modifier.weight(0.4f))
        Text(value.ifBlank { "-" }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink, modifier = Modifier.weight(0.6f))
    }
}
