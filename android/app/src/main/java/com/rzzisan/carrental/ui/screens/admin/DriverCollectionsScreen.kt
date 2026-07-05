package com.rzzisan.carrental.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.*
import com.rzzisan.carrental.ui.LocalAdminDrawerState
import com.rzzisan.carrental.ui.strings.AppStrings
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import com.rzzisan.carrental.util.errorMessageOf
import kotlinx.coroutines.launch

// ── local row shapes: admin ও manager-এর dues.php response দুটোই এই একই শেপে ম্যাপ করা হয় ──

private data class DriverDueRow(
    val id: Int,
    val name: String,
    val mobile: String?,
    val dueSettlementCount: Int,
    val totalPaid: Double,
    val totalDue: Double,
    val lastPaymentDate: String?
)

private data class DuesSummaryRow(
    val grandTotalDue: Double = 0.0,
    val grandTotalPaid: Double = 0.0,
    val driversWithDue: Int = 0
)

private fun taka(n: Double) = "৳${String.format("%.2f", n)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverCollectionsScreen(isAdmin: Boolean) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val drawerState = if (isAdmin) LocalAdminDrawerState.current else null

    var drivers by remember { mutableStateOf<List<DriverDueRow>>(emptyList()) }
    var summary by remember { mutableStateOf(DuesSummaryRow()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var detailTarget by remember { mutableStateOf<DriverDueRow?>(null) }
    var detailInitialTab by remember { mutableStateOf(0) }
    val snackState = remember { SnackbarHostState() }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                if (isAdmin) {
                    val res = ApiClient.service.getAdminDriverDues()
                    if (res.success && res.data != null) {
                        drivers = res.data.drivers.map {
                            DriverDueRow(it.id, it.name, it.mobile, it.dueSettlementCount, it.totalPaid, it.totalDue, it.lastPaymentDate)
                        }
                        summary = DuesSummaryRow(res.data.summary.grandTotalDue, res.data.summary.grandTotalPaid, res.data.summary.driversWithDue)
                    } else error = res.message ?: s.error
                } else {
                    val res = ApiClient.service.getManagerDriverDues()
                    if (res.success && res.data != null) {
                        drivers = res.data.drivers.map {
                            DriverDueRow(it.id, it.name, it.mobile, it.dueSettlementCount, it.totalPaid, it.totalDue, it.lastPaymentDate)
                        }
                        summary = DuesSummaryRow(res.data.summary.grandTotalDue, res.data.summary.grandTotalPaid, res.data.summary.driversWithDue)
                    } else error = res.message ?: s.error
                }
            } catch (e: Exception) { error = errorMessageOf(e, s.serverError) }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    val filtered = drivers.filter {
        search.isBlank() || it.name.contains(search, ignoreCase = true) || (it.mobile?.contains(search) == true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.navDriverCollections, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (isAdmin) {
                        IconButton(onClick = { scope.launch { drawerState?.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Summary cards
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryTile(s.grandTotalDueLabel, taka(summary.grandTotalDue), StatusDue, Modifier.weight(1f))
                SummaryTile(s.totalCollected, taka(summary.grandTotalPaid), StatusPaid, Modifier.weight(1f))
                SummaryTile(s.driversWithDueLabel, "${summary.driversWithDue}", Primary, Modifier.weight(1f))
            }

            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text(s.searchDriverPlaceholder, fontSize = 13.sp) },
                singleLine = true, shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )

            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
                error.isNotEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                        Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                    }
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(s.noDrivers, color = InkMuted) }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered) { d ->
                        DriverDuesCard(d, s,
                            onCollect = { detailTarget = d; detailInitialTab = 0 },
                            onHistory = { detailTarget = d; detailInitialTab = 1 })
                    }
                }
            }
        }
    }

    detailTarget?.let { target ->
        DriverDuesDetailSheet(
            driverId = target.id, driverName = target.name, driverMobile = target.mobile,
            isAdmin = isAdmin, initialTab = detailInitialTab, s = s,
            onDismiss = { detailTarget = null },
            onCollected = {
                scope.launch { snackState.showSnackbar(it) }
                load()
            }
        )
    }
}

@Composable
private fun SummaryTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier, shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = InkMuted, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun DriverDuesCard(
    d: DriverDueRow,
    s: AppStrings,
    onCollect: () -> Unit,
    onHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column {
                    Text(d.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                    d.mobile?.let { Text(it, fontSize = 12.sp, color = InkMuted) }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(s.dueAmount, fontSize = 10.sp, color = InkMuted)
                    Text(
                        taka(d.totalDue), fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = if (d.totalDue > 0) StatusDue else InkMuted
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column { Text(s.dueTripsCountLabel, fontSize = 10.sp, color = InkMuted); Text("${d.dueSettlementCount}", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                Column { Text(s.totalCollected, fontSize = 10.sp, color = InkMuted); Text(taka(d.totalPaid), fontSize = 13.sp, color = StatusPaid, fontWeight = FontWeight.Medium) }
                Column { Text(s.lastPaymentLabel, fontSize = 10.sp, color = InkMuted); Text(d.lastPaymentDate?.take(10) ?: "—", fontSize = 13.sp) }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                if (d.totalDue > 0) {
                    Button(
                        onClick = onCollect, modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPaid)
                    ) { Text(s.collectDues, fontSize = 12.sp) }
                }
                OutlinedButton(
                    onClick = onHistory, modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)
                ) { Text(s.paymentHistory, fontSize = 12.sp, color = Primary) }
            }
        }
    }
}

// ── Detail sheet: Collect / History tabs ────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverDuesDetailSheet(
    driverId: Int,
    driverName: String,
    driverMobile: String?,
    isAdmin: Boolean,
    initialTab: Int,
    s: AppStrings,
    onDismiss: () -> Unit,
    onCollected: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<DriverDuesDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(initialTab) }

    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("cash") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf("") }
    var lastResult by remember { mutableStateOf<DriverCollectResult?>(null) }
    val methods = listOf("cash" to s.cash, "bank_transfer" to s.bankTransfer, "mobile_banking" to s.mobileBanking)

    fun loadDetail() {
        loading = true; loadError = ""
        scope.launch {
            try {
                val res = if (isAdmin) ApiClient.service.getAdminDriverDuesDetail(driverId)
                          else ApiClient.service.getManagerDriverDuesDetail(driverId)
                if (res.success && res.data != null) detail = res.data else loadError = res.message ?: s.error
            } catch (e: Exception) { loadError = errorMessageOf(e, s.serverError) }
            finally { loading = false }
        }
    }
    LaunchedEffect(driverId) { loadDetail() }

    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0
    val totalDue = detail?.totalDue ?: 0.0
    val amountTooBig = enteredAmount > totalDue + 0.009

    // FIFO allocation preview — পুরাতন ট্রিপ থেকে ক্রমান্বয়ে কাটা হবে, ঠিক backend-এর মতোই
    val allocationPreview: List<Pair<DueSettlementItem, Double>> = run {
        val d = detail
        if (d == null || enteredAmount <= 0) emptyList()
        else {
            var left = enteredAmount
            d.dueSettlements.map { st ->
                val pay = minOf(st.remainingAmount, maxOf(left, 0.0))
                val rounded = Math.round(pay * 100) / 100.0
                left = Math.round((left - rounded) * 100) / 100.0
                st to rounded
            }
        }
    }

    fun submitCollect() {
        if (enteredAmount <= 0 || amountTooBig) return
        submitting = true; submitError = ""
        scope.launch {
            try {
                val req = DriverCollectRequest(enteredAmount, selectedMethod, notes.ifBlank { null })
                val res = if (isAdmin) ApiClient.service.collectDriverDues(driverId, req)
                          else ApiClient.service.collectManagerDriverDues(driverId, req)
                if (res.success) {
                    lastResult = res.data
                    amountText = ""; notes = ""
                    loadDetail()
                    onCollected(res.message ?: s.paymentCollected)
                } else {
                    submitError = res.message ?: s.error
                }
            } catch (e: Exception) {
                submitError = errorMessageOf(e, s.serverError)
            } finally {
                submitting = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(driverName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            driverMobile?.let { Text(it, color = InkMuted, fontSize = 13.sp) }

            when {
                loading -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(color = Primary) }
                loadError.isNotEmpty() -> Text(loadError, color = Color(0xFFDC2626), fontSize = 13.sp)
                detail != null -> {
                    val d = detail!!

                    // Status summary
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), color = StatusDue.copy(alpha = 0.1f), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp)) {
                                Text(s.remainingAmount, fontSize = 11.sp, color = InkMuted)
                                Text(taka(d.totalDue), fontWeight = FontWeight.Bold, color = StatusDue, fontSize = 15.sp)
                            }
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = StatusPaid.copy(alpha = 0.1f), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp)) {
                                Text(s.totalCollected, fontSize = 11.sp, color = InkMuted)
                                Text(taka(d.totalCollected), fontWeight = FontWeight.Bold, color = StatusPaid, fontSize = 15.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    TabRow(tab, containerColor = Color.White, contentColor = Primary) {
                        Tab(tab == 0, { tab = 0 }) { Text(s.collectDues, modifier = Modifier.padding(vertical = 12.dp), fontSize = 13.sp) }
                        Tab(tab == 1, { tab = 1 }) { Text("${s.paymentHistory} (${d.collections.size})", modifier = Modifier.padding(vertical = 12.dp), fontSize = 13.sp) }
                    }
                    Spacer(Modifier.height(4.dp))

                    when (tab) {
                        0 -> {
                            lastResult?.let { r ->
                                Surface(shape = RoundedCornerShape(8.dp), color = StatusPaid.copy(alpha = 0.1f)) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("✓ ${taka(r.collectedAmount ?: 0.0)}", fontWeight = FontWeight.Bold, color = StatusPaid, fontSize = 14.sp)
                                        r.allocations.forEach { a ->
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                Text("#${a.settlementId}", fontSize = 12.sp, color = InkMuted)
                                                Text(
                                                    if (a.paymentStatus == "paid") "${taka(a.allocated)} (${s.statusPaid})"
                                                    else "${taka(a.allocated)} (${s.remainingAmount}: ${taka(a.newRemainingAmount)})",
                                                    fontSize = 12.sp, color = StatusPaid
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (d.dueSettlements.isEmpty()) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), Alignment.Center) {
                                    Text("✓ ${s.allDuesCollectedMsg}", color = StatusPaid, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                if (submitError.isNotEmpty()) Text(submitError, color = Color(0xFFDC2626), fontSize = 12.sp)

                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        amountText, { amountText = it }, label = { Text("${s.amount} (${taka(d.totalDue)})") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true, shape = RoundedCornerShape(10.dp),
                                        isError = amountTooBig
                                    )
                                    OutlinedButton(onClick = { amountText = String.format("%.2f", d.totalDue) }, modifier = Modifier.height(56.dp)) {
                                        Text(s.fullDueQuickFill, fontSize = 12.sp)
                                    }
                                }
                                if (amountTooBig) Text(s.amountExceedsTotalDue, color = Color(0xFFDC2626), fontSize = 11.sp)

                                Text(s.paymentMethod, fontSize = 13.sp, color = InkMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    methods.forEach { (key, label) ->
                                        FilterChip(selectedMethod == key, { selectedMethod = key },
                                            label = { Text(label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White))
                                    }
                                }
                                OutlinedTextField(notes, { notes = it }, label = { Text(s.paymentNotes) },
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), maxLines = 2)

                                Button(
                                    onClick = ::submitCollect,
                                    enabled = !submitting && enteredAmount > 0 && !amountTooBig,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusPaid)
                                ) { Text(if (submitting) s.loading else s.collectDues, fontWeight = FontWeight.SemiBold) }

                                HorizontalDivider(color = Color(0xFFE5E7EB))
                                Text(
                                    "${s.dueSettlementsLabel} (${d.dueSettlements.size})",
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                                )
                                d.dueSettlements.forEach { st ->
                                    val preview = allocationPreview.find { it.first.id == st.id }?.second ?: 0.0
                                    val willPay = preview > 0 && !amountTooBig
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        Arrangement.SpaceBetween, Alignment.Top
                                    ) {
                                        Column {
                                            Text(
                                                "${st.customerFirstName ?: ""} ${st.customerLastName ?: ""}".trim().ifBlank { "—" },
                                                fontSize = 13.sp, fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "${st.vehicleBrand ?: ""} ${st.vehicleModel ?: ""} · #${st.id}${st.rentalStartDate?.let { " · ${it.take(10)}" } ?: ""}",
                                                fontSize = 11.sp, color = InkMuted
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(taka(st.remainingAmount), fontWeight = FontWeight.Medium, color = StatusDue, fontSize = 13.sp)
                                            if (willPay) {
                                                Text(
                                                    if (preview >= st.remainingAmount - 0.009) "✓ ${s.statusPaid}" else "-${taka(preview)}",
                                                    fontSize = 11.sp, color = StatusPaid, fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFF3F4F6))
                                }
                            }
                        }
                        1 -> {
                            if (d.collections.isEmpty()) {
                                Text(s.noPaymentHistory, color = InkMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
                            } else {
                                d.collections.forEach { p ->
                                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                            Text(taka(p.amount), fontWeight = FontWeight.Bold, color = StatusPaid, fontSize = 14.sp)
                                            Text(p.paymentDate?.take(10) ?: "—", fontSize = 11.sp, color = InkMuted)
                                        }
                                        Text(
                                            "${p.paymentMethod ?: ""} · ${p.customerFirstName ?: ""} ${p.customerLastName ?: ""} · #${p.settlementId}".trim(),
                                            fontSize = 11.sp, color = InkMuted
                                        )
                                        p.paymentNotes?.let { Text(it, fontSize = 11.sp, color = InkMuted) }
                                        p.recordedByName?.let { Text("${s.recordedBy}: $it", fontSize = 11.sp, color = InkMuted) }
                                        HorizontalDivider(Modifier.padding(top = 4.dp), color = Color(0xFFF3F4F6))
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
