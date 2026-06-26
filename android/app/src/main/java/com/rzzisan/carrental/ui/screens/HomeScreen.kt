package com.rzzisan.carrental.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.LedgerData
import com.rzzisan.carrental.data.network.Rental
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onTripClick: (Int) -> Unit) {
    val s = LocalStrings.current
    var activeTrips  by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var pendingTrips by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var ledger       by remember { mutableStateOf<LedgerData?>(null) }
    var loading      by remember { mutableStateOf(true) }
    var now          by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) { delay(60_000); now = System.currentTimeMillis() }
    }

    LaunchedEffect(Unit) {
        try {
            coroutineScope {
                val a = async { ApiClient.service.getRentals(status = "active") }
                val p = async { ApiClient.service.getRentals(status = "pending") }
                val l = async { ApiClient.service.getLedger() }
                val ar = a.await(); val pr = p.await(); val lr = l.await()
                if (ar.success) activeTrips = ar.data ?: emptyList()
                if (pr.success) pendingTrips = (pr.data ?: emptyList())
                    .sortedBy { parseDateTime(it.startDate) }.take(3)
                if (lr.success) ledger = lr.data
            }
        } catch (_: Exception) {}
        finally { loading = false }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(s.navHome, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        if (loading) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(padding)
        ) {
            // ── Active trips ─────────────────────────────────────────
            if (activeTrips.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        modifier = Modifier.fillMaxWidth()
                            .border(1.5.dp, StatusActive.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    ) {
                        Box(Modifier.padding(20.dp).fillMaxWidth(), Alignment.Center) {
                            Text(s.noActiveTrips, color = InkMuted, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(activeTrips) { trip ->
                    ActiveTripCard(trip = trip, now = now, onClick = { onTripClick(trip.id) })
                }
            }

            // ── Upcoming/pending trips ───────────────────────────────
            if (pendingTrips.isNotEmpty()) {
                item {
                    Text(s.upcomingTripsTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                }
                items(pendingTrips) { trip ->
                    UpcomingTripCard(trip = trip, now = now, onClick = { onTripClick(trip.id) })
                }
            }

            // ── Monthly summary ──────────────────────────────────────
            ledger?.let { data ->
                item {
                    Text(s.thisMonth + " / " + s.lastMonth,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        data.thisMonth?.let { tm ->
                            HomeMonthCard(s.thisMonth, tm.tripCount, tm.earned, tm.pending, Modifier.weight(1f))
                        }
                        data.lastMonth?.let { lm ->
                            HomeMonthCard(s.lastMonth, lm.tripCount, lm.earned, lm.pending, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── Active trip card ───────────────────────────────────────────────────────

@Composable
private fun ActiveTripCard(trip: Rental, now: Long, onClick: () -> Unit) {
    val s = LocalStrings.current
    val (timeText, _) = tripTimeLabel(trip, now)

    // Pulsing dot animation
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f, label = "scale",
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse)
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(Modifier.border(2.dp, StatusActive, RoundedCornerShape(16.dp))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header row
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier.size(10.dp).scale(scale),
                            shape = CircleShape,
                            color = StatusActive
                        ) {}
                        Text(s.activeTripsTitle, fontWeight = FontWeight.Bold, color = StatusActive, fontSize = 14.sp)
                    }
                    Surface(color = StatusActive.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                        Text(timeText, color = StatusActive, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }

                // Route
                if (!trip.pickupLocation.isNullOrBlank() || !trip.dropoffLocation.isNullOrBlank()) {
                    Text(
                        "${trip.pickupLocation ?: "?"} → ${trip.dropoffLocation ?: "?"}",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink
                    )
                }

                // Vehicle + amount
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(
                        "${trip.vehicleBrand ?: ""} ${trip.vehicleModel ?: ""}".trim(),
                        fontSize = 13.sp, color = InkMuted
                    )
                    Text(fmtBDT(trip.agreedAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                }

                // CTA button
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusActive)
                ) {
                    Text(s.viewTripBtn, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

// ── Upcoming trip card ─────────────────────────────────────────────────────

@Composable
private fun UpcomingTripCard(trip: Rental, now: Long, onClick: () -> Unit) {
    val s = LocalStrings.current
    val (timeText, timeColor) = tripTimeLabel(trip, now)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(Modifier.border(1.5.dp, StatusPending, RoundedCornerShape(14.dp))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(Modifier.size(8.dp), CircleShape, StatusPending) {}
                        Text(s.upcomingTripsTitle, fontWeight = FontWeight.Bold, color = StatusPending, fontSize = 13.sp)
                    }
                    Surface(color = timeColor.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                        Text(timeText, color = timeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }

                if (!trip.pickupLocation.isNullOrBlank() || !trip.dropoffLocation.isNullOrBlank()) {
                    Text(
                        "${trip.pickupLocation ?: "?"} → ${trip.dropoffLocation ?: "?"}",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink
                    )
                }

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(
                        "${trip.vehicleBrand ?: ""} ${trip.vehicleModel ?: ""}".trim(),
                        fontSize = 12.sp, color = InkMuted
                    )
                    Text(fmtBDT(trip.agreedAmount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                }
            }
        }
    }
}

// ── Month summary card ─────────────────────────────────────────────────────

@Composable
private fun HomeMonthCard(label: String, tripCount: Int, earned: Double, pending: Double, modifier: Modifier) {
    val s = LocalStrings.current
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold)
            Text("$tripCount ${s.trips}", fontSize = 11.sp, color = InkMuted)
            Text(fmtBDT(earned), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            if (pending > 0) Text("${s.pending}: ${fmtBDT(pending)}", fontSize = 11.sp, color = StatusDue)
        }
    }
}
