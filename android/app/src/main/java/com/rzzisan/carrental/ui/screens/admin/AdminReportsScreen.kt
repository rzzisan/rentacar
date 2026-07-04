package com.rzzisan.carrental.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.*
import com.rzzisan.carrental.ui.LocalAdminDrawerState
import com.rzzisan.carrental.ui.strings.AppStrings
import com.rzzisan.carrental.util.errorMessageOf
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val drawerState = LocalAdminDrawerState.current
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = (0..3).map { currentYear - it }

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var yearMenuOpen by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<AdminReport?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getAdminReports(selectedYear)
                if (res.success) report = res.data else error = res.message ?: s.error
            } catch (e: Exception) { error = errorMessageOf(e, s.serverError) }
            finally { loading = false }
        }
    }
    LaunchedEffect(selectedYear) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.adminReportsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState?.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = null)
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { yearMenuOpen = true }) {
                            Text("$selectedYear", fontWeight = FontWeight.SemiBold, color = Primary)
                            Icon(Icons.Filled.ArrowDropDown, null, tint = Primary)
                        }
                        DropdownMenu(expanded = yearMenuOpen, onDismissRequest = { yearMenuOpen = false }) {
                            years.forEach { y ->
                                DropdownMenuItem(
                                    text = { Text("$y") },
                                    onClick = { selectedYear = y; yearMenuOpen = false }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(error, color = StatusDue, fontSize = 13.sp)
                    Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                }
            }
            report != null -> {
                val r = report!!
                Column(Modifier.padding(padding)) {
                    // Summary bar
                    Surface(color = Primary.copy(alpha = 0.07f)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                            RSummaryCell(Modifier.weight(1f), s.totalRevenueLabel, r.summary.totalRevenue, StatusPaid)
                            RSummaryCell(Modifier.weight(1f), s.totalExpensesLabel, r.summary.totalExpenses, StatusDue)
                            RSummaryCell(Modifier.weight(1f), s.netProfitLabel, r.summary.netProfit,
                                if (r.summary.netProfit >= 0) Primary else StatusDue)
                            RSummaryCell(Modifier.weight(1f), s.totalDueLabel, r.summary.totalDue, StatusDue)
                        }
                    }

                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp,
                        containerColor = Color.White) {
                        listOf(s.tabPL, s.tabVehicle, s.tabAging, s.tabDriverPerf, s.tabAdminCustomers)
                            .forEachIndexed { i, label ->
                                Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                                    text = { Text(label, fontSize = 12.sp) })
                            }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (selectedTab) {

                            // ── Tab 0: লাভ-ক্ষতি ──────────────────────────────
                            0 -> {
                                if (r.monthlyPL.isEmpty()) {
                                    item { REmpty(s.noReportData) }
                                } else {
                                    items(r.monthlyPL) { m ->
                                        RCard {
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                Text(m.month, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text("${m.tripCount} ${s.tripCountLabel}", fontSize = 12.sp, color = InkMuted)
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                RDataPoint(s.totalRevenueLabel, m.revenue, StatusPaid)
                                                RDataPoint(s.tripExpensesLabel, m.tripExpenses, StatusDue)
                                                RDataPoint(s.commissionLabel, m.commission, Color(0xFF8B5CF6))
                                                RDataPoint(s.maintenanceCostLabel, m.maintenanceCost, StatusPending)
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                                                Surface(shape = RoundedCornerShape(6.dp),
                                                    color = if (m.net >= 0) StatusPaid.copy(alpha = 0.1f) else StatusDue.copy(alpha = 0.1f)) {
                                                    Text("${s.netProfitLabel}: ৳${String.format("%.0f", m.net)}",
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                        color = if (m.net >= 0) StatusPaid else StatusDue)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ── Tab 1: গাড়ি বিশ্লেষণ ──────────────────────────
                            1 -> {
                                if (r.vehicleProfitability.isEmpty()) {
                                    item { REmpty(s.noReportData) }
                                } else {
                                    items(r.vehicleProfitability) { v ->
                                        RCard {
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                                                Column(Modifier.weight(1f)) {
                                                    Text("${v.brand} ${v.model}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    Text(v.registrationNumber, fontSize = 11.sp, color = InkMuted)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("${v.utilizationRate}%", fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold, color = Primary)
                                                    Text(s.utilizationRate, fontSize = 10.sp, color = InkMuted)
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { (v.utilizationRate / 100).toFloat().coerceIn(0f, 1f) },
                                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                                color = Primary, trackColor = Primary.copy(alpha = 0.12f)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                RDataPoint(s.tripCountLabel, v.tripCount.toDouble(), Color(0xFF3B82F6), isInt = true)
                                                RDataPoint(s.totalRevenueLabel, v.revenue, StatusPaid)
                                                RDataPoint(s.totalExpensesLabel, v.totalExpenses, StatusDue)
                                                RDataPoint(s.netRevenueLabel, v.net,
                                                    if (v.net >= 0) Primary else StatusDue)
                                            }
                                            if (v.maintenanceCost > 0) {
                                                Spacer(Modifier.height(4.dp))
                                                Text("${s.maintenanceCostLabel}: ৳${String.format("%.0f", v.maintenanceCost)}",
                                                    fontSize = 11.sp, color = StatusPending)
                                            }
                                        }
                                    }
                                }
                            }

                            // ── Tab 2: বকেয়া Aging ────────────────────────────
                            2 -> {
                                val ag = r.duesAging
                                item {
                                    Surface(shape = RoundedCornerShape(10.dp),
                                        color = StatusDue.copy(alpha = 0.08f),
                                        modifier = Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(12.dp)) {
                                            AgingBucket(Modifier.weight(1f), s.bucket0_30,
                                                ag.current.size, ag.current.sumOf { it.remainingAmount }, Color(0xFF3B82F6))
                                            AgingBucket(Modifier.weight(1f), s.bucket31_60,
                                                ag.days30.size, ag.days30.sumOf { it.remainingAmount }, StatusPending)
                                            AgingBucket(Modifier.weight(1f), s.bucket61_90,
                                                ag.days60.size, ag.days60.sumOf { it.remainingAmount }, StatusPartial)
                                            AgingBucket(Modifier.weight(1f), s.bucket90plus,
                                                ag.days90.size, ag.days90.sumOf { it.remainingAmount }, StatusDue)
                                        }
                                    }
                                }
                                val allItems = ag.current + ag.days30 + ag.days60 + ag.days90
                                if (allItems.isEmpty()) {
                                    item { REmpty(s.noReportData) }
                                } else {
                                    items(allItems) { item ->
                                        val ageColor = when {
                                            item.daysOverdue <= 30 -> Color(0xFF3B82F6)
                                            item.daysOverdue <= 60 -> StatusPending
                                            item.daysOverdue <= 90 -> StatusPartial
                                            else -> StatusDue
                                        }
                                        RCard {
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(item.vehicle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                    Text(item.driverName ?: "—", fontSize = 11.sp, color = InkMuted)
                                                }
                                                Surface(shape = RoundedCornerShape(6.dp),
                                                    color = ageColor.copy(alpha = 0.12f)) {
                                                    Text("${item.daysOverdue}d", fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        color = ageColor, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                RDataPoint(s.agreedAmountLabel, item.agreedAmount, InkMuted)
                                                RDataPoint(s.paidLabel, item.paidAmount, StatusPaid)
                                                RDataPoint(s.totalDueLabel, item.remainingAmount, StatusDue)
                                            }
                                        }
                                    }
                                }
                            }

                            // ── Tab 3: ড্রাইভার পারফরম্যান্স ──────────────────
                            3 -> {
                                if (r.driverPerformance.isEmpty()) {
                                    item { REmpty(s.noReportData) }
                                } else {
                                    items(r.driverPerformance.sortedByDescending { it.totalRevenue }) { d ->
                                        RCard {
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(d.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    d.mobile?.let { Text(it, fontSize = 11.sp, color = InkMuted) }
                                                }
                                                Text("${d.commissionRate.toInt()}%",
                                                    fontSize = 13.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                RDataPoint(s.yearTripsLabel, d.yearTrips.toDouble(), Color(0xFF3B82F6), isInt = true)
                                                RDataPoint(s.monthTripsLabel, d.monthTrips.toDouble(), Primary, isInt = true)
                                                RDataPoint(s.totalRevenueLabel, d.totalRevenue, StatusPaid)
                                                RDataPoint(s.totalCommissionLabel, d.totalCommission, Color(0xFF8B5CF6))
                                            }
                                            if (d.totalDue > 0) {
                                                Spacer(Modifier.height(4.dp))
                                                Text("${s.totalDueLabel}: ৳${String.format("%.0f", d.totalDue)}",
                                                    fontSize = 11.sp, color = StatusDue)
                                            }
                                        }
                                    }
                                }
                            }

                            // ── Tab 4: গ্রাহক বিশ্লেষণ ────────────────────────
                            4 -> {
                                item {
                                    Surface(shape = RoundedCornerShape(10.dp),
                                        color = Primary.copy(alpha = 0.07f),
                                        modifier = Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(12.dp)) {
                                            RSummaryCell(Modifier.weight(1f), s.uniqueCustomers,
                                                r.summary.uniqueCustomers.toDouble(), Primary, isInt = true)
                                            RSummaryCell(Modifier.weight(1f), s.totalRevenueLabel,
                                                r.summary.totalRevenue, StatusPaid)
                                        }
                                    }
                                }
                                if (r.customerAnalytics.isEmpty()) {
                                    item { REmpty(s.noReportData) }
                                } else {
                                    items(r.customerAnalytics) { c ->
                                        RCard {
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(c.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    Text(c.phone, fontSize = 11.sp, color = InkMuted)
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    if (c.isRepeat == 1) {
                                                        Surface(shape = RoundedCornerShape(4.dp),
                                                            color = StatusPaid.copy(alpha = 0.12f)) {
                                                            Text(s.repeatCustomer, fontSize = 10.sp,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                color = StatusPaid)
                                                        }
                                                    }
                                                    if (c.isInactive == 1) {
                                                        Surface(shape = RoundedCornerShape(4.dp),
                                                            color = InkMuted.copy(alpha = 0.12f)) {
                                                            Text(s.inactiveCustomer, fontSize = 10.sp,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                color = InkMuted)
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                RDataPoint(s.totalTripsLabel, c.totalTrips.toDouble(), Color(0xFF3B82F6), isInt = true)
                                                RDataPoint(s.totalSpent, c.totalSpent, StatusPaid)
                                                c.lastTripDate?.let { RDataPoint(s.lastTrip, 0.0, InkMuted, text = it.take(10)) }
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

// ── Helpers ───────────────────────────────────────────────────────

@Composable
private fun RCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun RSummaryCell(modifier: Modifier, label: String, value: Double, color: Color, isInt: Boolean = false) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (isInt) "${value.toInt()}" else "৳${String.format("%.0f", value)}",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        Text(label, fontSize = 10.sp, color = InkMuted, maxLines = 1)
    }
}

@Composable
private fun RDataPoint(label: String, value: Double, color: Color, isInt: Boolean = false, text: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text ?: (if (isInt) "${value.toInt()}" else "৳${String.format("%.0f", value)}"),
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        Text(label, fontSize = 10.sp, color = InkMuted, maxLines = 1)
    }
}

@Composable
private fun AgingBucket(modifier: Modifier, label: String, count: Int, total: Double, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$count", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text("৳${String.format("%.0f", total)}", fontSize = 10.sp, color = color, maxLines = 1)
        Text(label, fontSize = 9.sp, color = InkMuted, maxLines = 1)
    }
}

@Composable
private fun REmpty(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
        Text(text, color = InkMuted, fontSize = 13.sp)
    }
}

private val AppStrings.agreedAmountLabel get() = "চুক্তি"
private val AppStrings.paidLabel get() = "পরিশোধ"
private val AppStrings.totalExpensesLabel get() = "মোট খরচ"
private val AppStrings.agreedAmountEN get() = "Agreed"
