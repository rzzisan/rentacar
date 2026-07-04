package com.rzzisan.carrental.ui.screens.manager

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.rzzisan.carrental.util.errorMessageOf
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerSettlementsScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var settlements by remember { mutableStateOf<List<AdminSettlement>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var detailTarget by remember { mutableStateOf<AdminSettlement?>(null) }
    var collectTarget by remember { mutableStateOf<AdminSettlement?>(null) }
    val snackState = remember { SnackbarHostState() }

    fun load(status: String? = selectedStatus) {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getManagerSettlements(status = status)
                if (res.success) settlements = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = errorMessageOf(e, s.serverError) }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settlementsTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(null to s.allStatus, "pending" to s.statusPending,
                    "partial" to s.statusPartial, "paid" to s.statusPaid
                ).forEach { (st, label) ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { selectedStatus = st; load(st) },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White)
                    )
                }
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
                error.isNotEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                        Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                    }
                }
                settlements.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(s.noAdminSettlements, color = InkMuted) }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(settlements) { st ->
                        MgrSettlementCard(st, s,
                            onDetail = { detailTarget = st },
                            onCollect = { collectTarget = st })
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    detailTarget?.let { st ->
        MgrSettlementDetailSheet(settlement = st, s = s, onDismiss = { detailTarget = null })
    }

    collectTarget?.let { st ->
        MgrCollectPaymentSheet(settlement = st, s = s, onDismiss = { collectTarget = null },
            onCollected = { msg ->
                collectTarget = null; load()
                scope.launch { snackState.showSnackbar(msg) }
            })
    }
}

// ── Settlement Detail Sheet ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MgrSettlementDetailSheet(
    settlement: AdminSettlement,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<AdminSettlementDetail?>(null) }
    var history by remember { mutableStateOf<SettlementPaymentHistoryData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(settlement.id) {
        scope.launch {
            try {
                val dDef = async { ApiClient.service.getManagerSettlementDetail(settlement.id) }
                val hDef = async { ApiClient.service.getManagerSettlementPaymentHistory(settlement.id) }
                val dr = dDef.await(); val hr = hDef.await()
                if (dr.success) detail = dr.data
                if (hr.success) history = hr.data
            } catch (_: Exception) {}
            finally { loading = false }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.settlementDetail, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            } else {
                val d = detail
                if (d == null) {
                    Text(s.error, color = Color(0xFFDC2626))
                } else {
                    // Header
                    val customer = listOfNotNull(d.customerFirstName, d.customerLastName).joinToString(" ")
                    if (customer.isNotBlank()) Text(customer, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("${d.vehicleBrand ?: ""} ${d.vehicleModel ?: ""}", fontSize = 13.sp, color = InkMuted)
                    if (!d.driverName.isNullOrBlank()) Text("🚗 ${d.driverName}", fontSize = 13.sp, color = InkMuted)

                    // Amounts
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFF)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MgrAmtRow(s.agreedAmount, d.agreedAmount, Ink, s)
                            MgrAmtRow(s.totalExpensesLabel, d.totalExpenses, StatusDue, s)
                            MgrAmtRow(s.commissionLabel, d.driverCommission, Color(0xFF8B5CF6), s)
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                            MgrAmtRow(s.amountToCollect, d.amountToCollect, Ink, s, bold = true)
                            MgrAmtRow(s.paidAmount, d.paidAmount, StatusPaid, s)
                            MgrAmtRow(s.remainingAmount, d.remainingAmount, StatusDue, s, bold = true)
                        }
                    }

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                            text = { Text(s.expensesLabel, fontSize = 12.sp) })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                            text = { Text(s.paymentHistory, fontSize = 12.sp) })
                    }

                    when (selectedTab) {
                        0 -> {
                            if (d.expenses.isEmpty()) {
                                Text(s.noExpenses, color = InkMuted, fontSize = 13.sp)
                            } else {
                                d.expenses.forEach { exp ->
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text(exp.expenseType, fontSize = 13.sp, color = InkMuted)
                                        Text("${s.taka}${String.format("%.0f", exp.amount)}", fontSize = 13.sp, color = Ink)
                                    }
                                }
                            }
                        }
                        1 -> {
                            val h = history
                            if (h == null || h.payments.isEmpty()) {
                                Text(s.noPaymentHistory, color = InkMuted, fontSize = 13.sp)
                            } else {
                                Surface(shape = RoundedCornerShape(8.dp), color = StatusPaid.copy(alpha = 0.08f)) {
                                    Row(Modifier.fillMaxWidth().padding(10.dp), Arrangement.SpaceBetween) {
                                        Text(s.totalCollected, fontSize = 13.sp, color = StatusPaid)
                                        Text("${s.taka}${String.format("%.0f", h.totalCollected)}", fontWeight = FontWeight.Bold, color = StatusPaid)
                                    }
                                }
                                h.payments.forEach { p ->
                                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))) {
                                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                Text("${s.taka}${String.format("%.0f", p.amount)}", fontWeight = FontWeight.Bold, color = StatusPaid)
                                                Text(p.paymentDate.take(10), fontSize = 11.sp, color = InkMuted)
                                            }
                                            Text(p.paymentMethod, fontSize = 12.sp, color = InkMuted)
                                            p.recordedByName?.let { Text("${s.recordedBy}: $it", fontSize = 11.sp, color = InkMuted) }
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
private fun MgrAmtRow(label: String, amount: Double, color: Color, s: com.rzzisan.carrental.ui.strings.AppStrings, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = InkMuted)
        Text("${s.taka}${String.format("%.0f", amount)}", fontSize = 13.sp, color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

// ── Collect Payment Sheet ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MgrCollectPaymentSheet(
    settlement: AdminSettlement,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onDismiss: () -> Unit,
    onCollected: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("cash") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.collectPayment, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("${s.remainingAmount}: ${s.taka}${String.format("%.0f", settlement.remainingAmount)}",
                fontSize = 13.sp, color = StatusDue, fontWeight = FontWeight.Medium)
            if (formError.isNotEmpty()) Text(formError, color = Color(0xFFDC2626), fontSize = 13.sp)
            OutlinedTextField(amount, { amount = it }, label = { Text(s.amount) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp))
            Text(s.paymentMethod, fontSize = 13.sp, color = InkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("cash" to s.cash, "bank_transfer" to s.bankTransfer, "mobile_banking" to s.mobileBanking)
                    .forEach { (k, lbl) ->
                        FilterChip(method == k, { method = k }, label = { Text(lbl, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White))
                    }
            }
            OutlinedTextField(notes, { notes = it }, label = { Text(s.paymentNotes) },
                modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(10.dp))
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt == null || amt <= 0) { formError = "${s.amount} সঠিক হতে হবে"; return@Button }
                    if (amt > settlement.remainingAmount + 0.01) { formError = "পরিমান বকেয়ার চেয়ে বেশি হতে পারে না"; return@Button }
                    submitting = true; formError = ""
                    scope.launch {
                        try {
                            val res = ApiClient.service.collectManagerSettlementPayment(
                                settlement.id, CollectPaymentRequest(amt, method, notes.ifBlank { null })
                            )
                            onCollected(if (res.success) s.paymentCollected else res.message ?: s.error)
                        } catch (e: HttpException) {
                            formError = "HTTP ${e.code()}: ${e.response()?.errorBody()?.string() ?: s.error}"
                        } catch (e: Exception) {
                            formError = "${e.javaClass.simpleName}: ${e.message}"
                        } finally { submitting = false }
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text(if (submitting) s.loading else s.collectPayment, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// ── Settlement Card ────────────────────────────────────────────────

@Composable
private fun MgrSettlementCard(
    st: AdminSettlement,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onDetail: () -> Unit,
    onCollect: () -> Unit
) {
    val (statusColor, statusLabel) = when (st.paymentStatus) {
        "paid"    -> StatusPaid    to s.statusPaid
        "partial" -> StatusPartial to s.statusPartial
        else      -> StatusDue     to s.statusPending
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val customer = listOfNotNull(st.customerFirstName, st.customerLastName).joinToString(" ").ifBlank { "#${st.id}" }
                    Text(customer, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                    st.driverName?.let { Text("🚗 $it", fontSize = 12.sp, color = InkMuted) }
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusLabel, fontSize = 11.sp, color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${s.agreedAmount}: ${s.taka}${String.format("%.0f", st.agreedAmount)}", fontSize = 12.sp, color = InkMuted)
                    Text("${s.remainingAmount}: ${s.taka}${String.format("%.0f", st.remainingAmount)}", fontSize = 13.sp,
                        color = if (st.remainingAmount > 0) StatusDue else StatusPaid, fontWeight = FontWeight.Medium)
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDetail, modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) {
                    Text(s.viewDetail, fontSize = 12.sp, color = Primary)
                }
                if (st.paymentStatus != "paid") {
                    Button(onClick = onCollect, modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Text(s.collectPayment, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
