package com.rzzisan.carrental.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.text.SimpleDateFormat
import java.util.Locale

// ── Trip time helpers ──────────────────────────────────────────────────────

private fun parseDateTime(s: String?): Long {
    if (s.isNullOrBlank()) return 0L
    return try {
        // Try datetime first, then date-only
        val fmtDt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        fmtDt.parse(s)?.time ?: 0L
    } catch (_: Exception) {
        try {
            val fmtD = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            fmtD.parse(s)?.time ?: 0L
        } catch (_: Exception) { 0L }
    }
}

private fun fmtDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h} ঘণ্টা ${m} মিনিট" else "${m} মিনিট"
}

private fun tripTimeLabel(rental: Rental, now: Long): Pair<String, Color> {
    val startMs = parseDateTime(rental.startDate)
    return when (rental.rentalStatus) {
        "pending" -> {
            val diff = startMs - now
            if (diff > 0)
                Pair("শুরু হতে বাকি ${fmtDuration(diff)}", StatusPending)
            else
                Pair("নির্ধারিত সময় পেরিয়ে গেছে", StatusDue)
        }
        "active" -> {
            val fromMs = parseDateTime(rental.actualStartTime ?: rental.startDate)
            Pair("চলছে ${fmtDuration(now - fromMs)} ধরে", StatusActive)
        }
        else -> Pair("—", InkMuted)
    }
}

// ── Main screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen() {
    val s = LocalStrings.current
    var ledger       by remember { mutableStateOf<LedgerData?>(null) }
    var activeTrips  by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var pendingTrips by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var loading      by remember { mutableStateOf(true) }
    var error        by remember { mutableStateOf("") }
    var now          by remember { mutableStateOf(System.currentTimeMillis()) }

    // Refresh clock every 60 seconds so time labels update
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        try {
            val (ledgerRes, activeRes, pendingRes) = coroutineScope {
                val l = async { ApiClient.service.getLedger() }
                val a = async { ApiClient.service.getRentals(status = "active") }
                val p = async { ApiClient.service.getRentals(status = "pending") }
                Triple(l.await(), a.await(), p.await())
            }
            if (ledgerRes.success) ledger = ledgerRes.data else error = ledgerRes.message ?: s.error
            if (activeRes.success) activeTrips = activeRes.data ?: emptyList()
            if (pendingRes.success) {
                pendingTrips = (pendingRes.data ?: emptyList())
                    .sortedBy { parseDateTime(it.startDate) }
                    .take(3)
            }
        } catch (_: Exception) {
            error = s.serverError
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(s.ledgerTitle, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error.isNotEmpty() -> Text(error, Modifier.align(Alignment.Center), color = StatusDue)
                else -> {
                    val data = ledger!!
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ── Active trips (green) ──────────────────────────
                        items(activeTrips) { trip ->
                            val (timeText, timeColor) = tripTimeLabel(trip, now)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusActive.copy(alpha = 0.06f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, StatusActive, RoundedCornerShape(12.dp))
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text(s.activeTripsTitle, fontWeight = FontWeight.Bold, color = StatusActive, fontSize = 13.sp)
                                        Text(timeText, fontSize = 12.sp, color = timeColor, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (!trip.pickupLocation.isNullOrBlank() || !trip.dropoffLocation.isNullOrBlank()) {
                                        Text(
                                            "${trip.pickupLocation ?: "?"} → ${trip.dropoffLocation ?: "?"}",
                                            fontSize = 13.sp, color = Ink
                                        )
                                    }
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text(
                                            "${trip.vehicleBrand ?: ""} ${trip.vehicleModel ?: ""}".trim(),
                                            fontSize = 12.sp, color = InkMuted
                                        )
                                        Text(fmtBDT(trip.agreedAmount), fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // ── Upcoming/pending trips (amber) ────────────────
                        items(pendingTrips) { trip ->
                            val (timeText, timeColor) = tripTimeLabel(trip, now)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusPending.copy(alpha = 0.06f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, StatusPending, RoundedCornerShape(12.dp))
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text(s.upcomingTripsTitle, fontWeight = FontWeight.Bold, color = StatusPending, fontSize = 13.sp)
                                        Text(timeText, fontSize = 12.sp, color = timeColor, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (!trip.pickupLocation.isNullOrBlank() || !trip.dropoffLocation.isNullOrBlank()) {
                                        Text(
                                            "${trip.pickupLocation ?: "?"} → ${trip.dropoffLocation ?: "?"}",
                                            fontSize = 13.sp, color = Ink
                                        )
                                    }
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text(
                                            "${trip.vehicleBrand ?: ""} ${trip.vehicleModel ?: ""}".trim(),
                                            fontSize = 12.sp, color = InkMuted
                                        )
                                        Text(fmtBDT(trip.agreedAmount), fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // ── This month vs last month ───────────────────────
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                data.thisMonth?.let { tm ->
                                    MonthCard(s.thisMonth, tm.tripCount, tm.earned, tm.pending, Modifier.weight(1f))
                                }
                                data.lastMonth?.let { lm ->
                                    MonthCard(s.lastMonth, lm.tripCount, lm.earned, lm.pending, Modifier.weight(1f))
                                }
                            }
                        }

                        // ── Settlements ───────────────────────────────────
                        if (data.settlements.isEmpty()) {
                            item { Text(s.noSettlements, color = InkMuted, modifier = Modifier.padding(16.dp)) }
                        } else {
                            items(data.settlements) { settlement ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Surface),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${s.pickup}: ${settlement.pickupLocation ?: "-"}", fontSize = 13.sp, color = InkMuted)
                                            StatusChip(settlement.paymentStatus)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(s.earned, fontSize = 12.sp, color = InkMuted)
                                            Text(fmtBDT(settlement.driverCommission), fontWeight = FontWeight.SemiBold, color = StatusPaid)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(s.pending, fontSize = 12.sp, color = InkMuted)
                                            Text(fmtBDT(settlement.remainingAmount), fontWeight = FontWeight.SemiBold, color = StatusDue)
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
}

@Composable
private fun MonthCard(label: String, tripCount: Int, earned: Double, pending: Double, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold)
            Text("$tripCount ${s.trips}", fontSize = 11.sp, color = InkMuted)
            Text(fmtBDT(earned), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            if (pending > 0) Text("${s.pending}: ${fmtBDT(pending)}", fontSize = 11.sp, color = StatusDue)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "paid"    -> StatusPaid
        "partial" -> StatusPartial
        else      -> StatusDue
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Text(status, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

fun fmtBDT(v: Double) = "৳${String.format(Locale.US, "%,.0f", v)}"
