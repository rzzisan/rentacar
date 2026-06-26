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
import com.rzzisan.carrental.data.network.ManagerReport
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerReportsScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<ManagerReport?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getManagerReports()
                if (res.success) report = res.data else error = res.message ?: s.error
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.reportsTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                    Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                }
            }
            report != null -> {
                val r = report!!
                Column(Modifier.padding(padding)) {
                    // Summary card
                    Surface(color = Primary.copy(alpha = 0.06f)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            Arrangement.spacedBy(0.dp)
                        ) {
                            ReportSummaryCell(Modifier.weight(1f), s.totalTripsLabel, "${r.summary.totalTrips}", Color(0xFF3B82F6))
                            ReportSummaryCell(Modifier.weight(1f), s.totalRevenueLabel, "${s.taka}${String.format("%.0f", r.summary.totalRevenue)}", StatusPaid)
                            ReportSummaryCell(Modifier.weight(1f), s.totalExpensesLabel, "${s.taka}${String.format("%.0f", r.summary.totalExpenses)}", StatusDue)
                            ReportSummaryCell(Modifier.weight(1f), s.totalDueLabel, "${s.taka}${String.format("%.0f", r.summary.totalDue)}", Color(0xFFEF4444))
                        }
                    }

                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                        listOf(s.tabMonthly, s.tabVehicle, s.tabExpenses, s.tabDriverPerf).forEachIndexed { i, label ->
                            Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                                text = { Text(label, fontSize = 12.sp) })
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                if (r.monthlyRevenue.isEmpty()) {
                                    item { ReportEmpty(s.noReportData) }
                                } else {
                                    items(r.monthlyRevenue) { m ->
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                            elevation = CardDefaults.cardElevation(1.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                    Text(m.month, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    Text("${m.tripCount} ${s.tripCountLabel}", fontSize = 12.sp, color = InkMuted)
                                                }
                                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                    ReportDataPoint(s.totalRevenueLabel, "${s.taka}${String.format("%.0f", m.revenue)}", StatusPaid)
                                                    ReportDataPoint(s.expensesLabel, "${s.taka}${String.format("%.0f", m.expenses)}", StatusDue)
                                                    ReportDataPoint(s.netRevenueLabel, "${s.taka}${String.format("%.0f", m.net)}", Primary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                if (r.vehicleRevenue.isEmpty()) {
                                    item { ReportEmpty(s.noReportData) }
                                } else {
                                    items(r.vehicleRevenue) { v ->
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                            elevation = CardDefaults.cardElevation(1.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                    Column {
                                                        Text("${v.brand} ${v.model}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                        Text(v.registrationNumber, fontSize = 12.sp, color = InkMuted)
                                                    }
                                                    Surface(shape = RoundedCornerShape(6.dp),
                                                        color = when (v.status) {
                                                            "available" -> StatusActive.copy(alpha = 0.1f)
                                                            "rented" -> Primary.copy(alpha = 0.1f)
                                                            else -> InkMuted.copy(alpha = 0.1f)
                                                        }) {
                                                        Text(v.status, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                            color = when (v.status) {
                                                                "available" -> StatusActive
                                                                "rented" -> Primary
                                                                else -> InkMuted
                                                            })
                                                    }
                                                }
                                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                    ReportDataPoint(s.tripCountLabel, "${v.tripCount}", Color(0xFF3B82F6))
                                                    ReportDataPoint(s.totalRevenueLabel, "${s.taka}${String.format("%.0f", v.revenue)}", StatusPaid)
                                                    ReportDataPoint(s.expensesLabel, "${s.taka}${String.format("%.0f", v.expenses)}", StatusDue)
                                                    ReportDataPoint(s.netRevenueLabel, "${s.taka}${String.format("%.0f", v.net)}", Primary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                if (r.expenseBreakdown.isEmpty()) {
                                    item { ReportEmpty(s.noReportData) }
                                } else {
                                    val maxTotal = r.expenseBreakdown.maxOfOrNull { it.total } ?: 1.0
                                    items(r.expenseBreakdown) { e ->
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                            elevation = CardDefaults.cardElevation(1.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                    Text(expenseTypeLabel(e.expenseType, s), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                    Text("${s.taka}${String.format("%.0f", e.total)}", fontWeight = FontWeight.Bold, color = StatusDue)
                                                }
                                                LinearProgressIndicator(
                                                    progress = { (e.total / maxTotal).toFloat().coerceIn(0f, 1f) },
                                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                                    color = StatusDue,
                                                    trackColor = StatusDue.copy(alpha = 0.15f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                if (r.driverPerformance.isEmpty()) {
                                    item { ReportEmpty(s.noReportData) }
                                } else {
                                    items(r.driverPerformance) { d ->
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                            elevation = CardDefaults.cardElevation(1.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                    Column {
                                                        Text(d.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                        d.mobile?.let { Text(it, fontSize = 12.sp, color = InkMuted) }
                                                    }
                                                    Text("${d.commissionRate.toInt()}%", fontSize = 13.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                                                }
                                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                    ReportDataPoint(s.totalTripsLabel, "${d.totalTrips}", Color(0xFF3B82F6))
                                                    ReportDataPoint(s.thisMonthLabel, "${d.thisMonthTrips}", Primary)
                                                    ReportDataPoint(s.totalCommissionLabel, "${s.taka}${String.format("%.0f", d.totalCommission)}", Color(0xFF8B5CF6))
                                                    ReportDataPoint(s.totalDueLabel, "${s.taka}${String.format("%.0f", d.totalDue)}",
                                                        if (d.totalDue > 0) StatusDue else StatusPaid)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

private fun expenseTypeLabel(type: String, s: com.rzzisan.carrental.ui.strings.AppStrings): String = when (type) {
    "toll" -> s.expToll
    "fuel" -> s.expFuel
    "parking" -> s.expParking
    "repair" -> s.expRepair
    "driver_allowance" -> s.expDriverAllowance
    else -> s.expOther
}

@Composable
private fun ReportSummaryCell(modifier: Modifier, label: String, value: String, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        Text(label, fontSize = 10.sp, color = InkMuted, maxLines = 1)
    }
}

@Composable
private fun ReportDataPoint(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        Text(label, fontSize = 10.sp, color = InkMuted, maxLines = 1)
    }
}

@Composable
private fun ReportEmpty(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
        Text(text, color = InkMuted, fontSize = 13.sp)
    }
}
