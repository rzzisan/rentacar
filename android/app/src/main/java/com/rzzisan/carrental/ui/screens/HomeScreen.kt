package com.rzzisan.carrental.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.LedgerData
import com.rzzisan.carrental.data.network.Rental
import com.rzzisan.carrental.service.LocationTrackingService
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onTripClick: (Int) -> Unit) {
    val s       = LocalStrings.current
    val context = LocalContext.current
    var activeTrips  by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var pendingTrips by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var ledger       by remember { mutableStateOf<LedgerData?>(null) }
    var loading      by remember { mutableStateOf(true) }
    var now          by remember { mutableStateOf(System.currentTimeMillis()) }

    // Clock refresh every 60s
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

    // অ্যাপ খুললে active trip থাকলে LocationTrackingService নিশ্চিত করো
    LaunchedEffect(activeTrips) {
        activeTrips.firstOrNull()?.let { trip ->
            LocationTrackingService.start(context, trip.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.navHome, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Background
    ) { padding ->
        if (loading) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)
        ) {
            // ── Active trips ─────────────────────────────────────────
            if (activeTrips.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            Modifier
                                .border(2.dp, StatusActive.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                .padding(vertical = 32.dp)
                                .fillMaxWidth(),
                            Alignment.Center
                        ) {
                            Text(s.noActiveTrips, color = InkMuted, fontSize = 15.sp)
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
                    Text(
                        s.upcomingTripsTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Ink,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(pendingTrips) { trip ->
                    UpcomingTripCard(trip = trip, now = now, onClick = { onTripClick(trip.id) })
                }
            }

            // ── Monthly summary ──────────────────────────────────────
            ledger?.let { data ->
                item {
                    Text(
                        "${s.thisMonth} / ${s.lastMonth}",
                        fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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

// ── Active trip card — solid green, white text, glance-readable ──────────────

@Composable
private fun ActiveTripCard(trip: Rental, now: Long, onClick: () -> Unit) {
    val s = LocalStrings.current
    val (timeText, _) = tripTimeLabel(trip, now)

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f, label = "scale",
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse)
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StatusActive),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: pulsing dot + label + timer
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(12.dp).scale(scale),
                        shape = CircleShape,
                        color = Color.White
                    ) {}
                    Text(
                        s.activeTripsTitle,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        timeText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            // Route — hero element, largest text
            if (!trip.pickupLocation.isNullOrBlank() || !trip.dropoffLocation.isNullOrBlank()) {
                Text(
                    "${trip.pickupLocation ?: "?"} → ${trip.dropoffLocation ?: "?"}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 30.sp
                )
            }

            // Vehicle + Amount
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "${trip.vehicleBrand ?: ""} ${trip.vehicleModel ?: ""}".trim(),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    trip.vehicleRegNumber?.let {
                        Text(it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Text(
                    fmtBDT(trip.agreedAmount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = Color.White
                )
            }

            // CTA
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    s.viewTripBtn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(Modifier.border(2.dp, StatusPending.copy(alpha = 0.5f), RoundedCornerShape(16.dp))) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(Modifier.size(10.dp), CircleShape, StatusPending) {}
                        Text(
                            s.upcomingTripsTitle,
                            fontWeight = FontWeight.Bold,
                            color = StatusPending,
                            fontSize = 14.sp
                        )
                    }
                    Surface(
                        color = timeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            timeText,
                            color = timeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Route
                if (!trip.pickupLocation.isNullOrBlank() || !trip.dropoffLocation.isNullOrBlank()) {
                    Text(
                        "${trip.pickupLocation ?: "?"} → ${trip.dropoffLocation ?: "?"}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }

                // Vehicle + Amount
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(
                        "${trip.vehicleBrand ?: ""} ${trip.vehicleModel ?: ""}".trim(),
                        fontSize = 13.sp,
                        color = InkMuted
                    )
                    Text(
                        fmtBDT(trip.agreedAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Ink
                    )
                }
            }
        }
    }
}

// ── Month summary card ─────────────────────────────────────────────────────

@Composable
private fun HomeMonthCard(
    label: String,
    tripCount: Int,
    earned: Double,
    pending: Double,
    modifier: Modifier
) {
    val s = LocalStrings.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Bold)
            Text("$tripCount ${s.trips}", fontSize = 13.sp, color = InkMuted)
            Text(
                fmtBDT(earned),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            if (pending > 0) {
                Text(
                    "${s.pending}: ${fmtBDT(pending)}",
                    fontSize = 12.sp,
                    color = StatusDue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
