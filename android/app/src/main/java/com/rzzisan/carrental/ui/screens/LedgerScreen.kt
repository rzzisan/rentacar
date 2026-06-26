package com.rzzisan.carrental.ui.screens

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
import java.text.SimpleDateFormat
import java.util.Locale

// ── Shared helpers (used by HomeScreen too) ────────────────────────────────

fun parseDateTime(s: String?): Long {
    if (s.isNullOrBlank()) return 0L
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(s)?.time ?: 0L
    } catch (_: Exception) {
        try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)?.time ?: 0L }
        catch (_: Exception) { 0L }
    }
}

fun fmtDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h} ঘণ্টা ${m} মিনিট" else "${m} মিনিট"
}

fun tripTimeLabel(rental: Rental, now: Long): Pair<String, Color> {
    return when (rental.rentalStatus) {
        "pending" -> {
            val diff = parseDateTime(rental.startDate) - now
            if (diff > 0) Pair("শুরু হতে বাকি ${fmtDuration(diff)}", StatusPending)
            else Pair("নির্ধারিত সময় পেরিয়ে গেছে", StatusDue)
        }
        "active" -> {
            val fromMs = parseDateTime(rental.actualStartTime ?: rental.startDate)
            Pair("চলছে ${fmtDuration(now - fromMs)} ধরে", StatusActive)
        }
        else -> Pair("—", InkMuted)
    }
}

fun fmtBDT(v: Double) = "৳${String.format(Locale.US, "%,.0f", v)}"

// ── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen() {
    val s = LocalStrings.current
    var ledger  by remember { mutableStateOf<LedgerData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.service.getLedger()
            if (res.success) ledger = res.data else error = res.message ?: s.error
        } catch (_: Exception) { error = s.serverError }
        finally { loading = false }
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

                        if (data.settlements.isEmpty()) {
                            item { Text(s.noSettlements, color = InkMuted, modifier = Modifier.padding(16.dp)) }
                        } else {
                            items(data.settlements) { settlement ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Surface),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        // Location + status
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                settlement.pickupLocation ?: "-",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Ink,
                                                modifier = Modifier.weight(1f)
                                            )
                                            PayStatusChip(settlement.paymentStatus)
                                        }
                                        // Earned amount — big
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(s.earned, fontSize = 14.sp, color = InkMuted)
                                            Text(
                                                fmtBDT(settlement.driverCommission),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 22.sp,
                                                color = StatusPaid
                                            )
                                        }
                                        // Pending — visible
                                        if (settlement.remainingAmount > 0) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(s.pending, fontSize = 14.sp, color = InkMuted)
                                                Text(
                                                    fmtBDT(settlement.remainingAmount),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = StatusDue
                                                )
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
}

@Composable
private fun MonthCard(label: String, tripCount: Int, earned: Double, pending: Double, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Bold)
            Text("$tripCount ${s.trips}", fontSize = 13.sp, color = InkMuted)
            Text(fmtBDT(earned), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            if (pending > 0) Text("${s.pending}: ${fmtBDT(pending)}", fontSize = 13.sp, color = StatusDue, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PayStatusChip(status: String) {
    val (color, label) = when (status) {
        "paid"    -> StatusPaid    to "পরিশোধিত"
        "partial" -> StatusPartial to "আংশিক"
        else      -> StatusDue     to "বকেয়া"
    }
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(24.dp)) {
        Text(
            label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
