package com.rzzisan.carrental.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.Rental
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(navController: NavController) {
    val s = LocalStrings.current
    var rentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) }

    fun load() {
        loading = true; error = ""
    }

    LaunchedEffect(statusFilter) {
        loading = true; error = ""
        try {
            val res = ApiClient.service.getRentals(status = statusFilter)
            if (res.success) rentals = res.data ?: emptyList() else error = res.message ?: s.error
        } catch (e: Exception) {
            error = s.serverError
        } finally { loading = false }
    }

    val filters = listOf(null to s.allStatus, "pending" to s.statusPending,
        "active" to s.statusActive, "completed" to s.statusCompleted)

    Scaffold(
        topBar = { TopAppBar(title = { Text(s.tripsTitle, fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_trip") },
                containerColor = Primary
            ) { Icon(Icons.Filled.Add, contentDescription = s.newTrip, tint = androidx.compose.ui.graphics.Color.White) }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Status filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { (value, label) ->
                    FilterChip(
                        selected = statusFilter == value,
                        onClick = { statusFilter = value },
                        label = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    error.isNotEmpty() -> Text(error, Modifier.align(Alignment.Center), color = StatusDue)
                    rentals.isEmpty() -> Text(s.noTrips, Modifier.align(Alignment.Center), color = InkMuted)
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(rentals, key = { it.id }) { rental ->
                            RentalCard(rental, onClick = { navController.navigate("trip_detail/${rental.id}") })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RentalCard(rental: Rental, onClick: () -> Unit) {
    val s = LocalStrings.current
    val statusColor = when (rental.rentalStatus) {
        "active"    -> StatusActive
        "completed" -> StatusDone
        "cancelled" -> InkMuted
        else        -> StatusPending
    }
    val statusLabel = when (rental.rentalStatus) {
        "active"    -> s.statusActive
        "completed" -> s.statusCompleted
        "cancelled" -> s.statusCancelled
        else        -> s.statusPending
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${rental.customerFirstName ?: ""} ${rental.customerLastName ?: ""}".trim().ifEmpty { "-" },
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink
                )
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                    Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Text("${rental.vehicleBrand ?: ""} ${rental.vehicleModel ?: ""} • ${rental.vehicleRegNumber ?: ""}",
                fontSize = 12.sp, color = InkMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${rental.pickupLocation ?: "-"} → ${rental.dropoffLocation ?: "-"}",
                    fontSize = 12.sp, color = InkMuted, modifier = Modifier.weight(1f))
                Text(fmtBDT(rental.agreedAmount), fontWeight = FontWeight.Bold, color = Primary, fontSize = 14.sp)
            }
        }
    }
}
