package com.rzzisan.carrental.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.LedgerData
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen() {
    val s = LocalStrings.current
    var ledger by remember { mutableStateOf<LedgerData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.service.getLedger()
            if (res.success) ledger = res.data else error = res.message ?: s.error
        } catch (e: Exception) {
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
                        // This month vs last month
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

                        // Settlements
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
