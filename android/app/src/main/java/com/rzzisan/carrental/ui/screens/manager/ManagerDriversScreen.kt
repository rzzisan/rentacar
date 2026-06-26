package com.rzzisan.carrental.ui.screens.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDriversScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var drivers by remember { mutableStateOf<List<ManagerDriver>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var collectTarget by remember { mutableStateOf<ManagerDriver?>(null) }
    val snackState = remember { SnackbarHostState() }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getManagerDrivers()
                if (res.success) drivers = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.mgrDriversTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                    Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                }
            }
            drivers.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text(s.noMgrDrivers, color = InkMuted) }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(drivers) { d ->
                    MgrDriverCard(d, s, onCollect = { collectTarget = d })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    collectTarget?.let { d ->
        MgrCollectDuesSheet(driver = d, s = s, onDismiss = { collectTarget = null },
            onCollected = { msg ->
                collectTarget = null; load()
                scope.launch { snackState.showSnackbar(msg) }
            })
    }
}

// ── Collect Dues Sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MgrCollectDuesSheet(
    driver: ManagerDriver,
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
            Text(s.collectDues, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(driver.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("${s.totalDueLabel}: ${s.taka}${String.format("%.0f", driver.totalDue)}",
                fontSize = 13.sp, color = StatusDue, fontWeight = FontWeight.Medium)
            if (formError.isNotEmpty()) Text(formError, color = Color(0xFFDC2626), fontSize = 13.sp)
            OutlinedTextField(amount, { amount = it }, label = { Text(s.dueAmount) },
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
                    submitting = true; formError = ""
                    scope.launch {
                        try {
                            val res = ApiClient.service.collectManagerDriverDues(
                                driver.id, DriverCollectRequest(amt, method, notes.ifBlank { null })
                            )
                            onCollected(if (res.success) res.data?.let { "৳${String.format("%.0f", it.collectedAmount)} জমা হয়েছে" } ?: s.success else res.message ?: s.error)
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
            ) { Text(if (submitting) s.loading else s.collectDues, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// ── Driver Card ────────────────────────────────────────────────────

@Composable
private fun MgrDriverCard(
    d: ManagerDriver,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onCollect: () -> Unit
) {
    val isActive = d.status == "active"
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp),
                        color = if (isActive) Primary.copy(alpha = 0.1f) else InkMuted.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Icon(Icons.Filled.Person, null,
                                tint = if (isActive) Primary else InkMuted,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                    Column {
                        Text(d.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                        d.mobile?.let { Text(it, fontSize = 12.sp, color = InkMuted) }
                    }
                }
                Surface(shape = RoundedCornerShape(6.dp),
                    color = if (isActive) StatusActive.copy(alpha = 0.1f) else InkMuted.copy(alpha = 0.1f)) {
                    Text(if (isActive) s.activeLabel else s.inactiveLabel, fontSize = 11.sp,
                        color = if (isActive) StatusActive else InkMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            // Performance stats row
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(0.dp)) {
                MgrPerfStat(Modifier.weight(1f), s.totalTripsLabel, "${d.totalTrips}", Color(0xFF3B82F6))
                MgrPerfStat(Modifier.weight(1f), s.thisMonthLabel, "${d.thisMonthTrips}", Primary)
                MgrPerfStat(Modifier.weight(1f), s.commissionRateLabel, "${d.commissionRate.toInt()}%", Color(0xFF8B5CF6))
                MgrPerfStat(Modifier.weight(1f), s.totalDueLabel, "${s.taka}${String.format("%.0f", d.totalDue)}",
                    if (d.totalDue > 0) StatusDue else StatusPaid)
            }
            // Assigned vehicles chips
            if (d.vehicles.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    d.vehicles.forEach { v ->
                        Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.08f)) {
                            Text("${v.brand} ${v.model}", fontSize = 11.sp, color = Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            if (d.totalDue > 0) {
                Button(
                    onClick = onCollect,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDue)
                ) {
                    Icon(Icons.Filled.Payments, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(s.collectDues, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun MgrPerfStat(modifier: Modifier, label: String, value: String, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = InkMuted, maxLines = 1)
    }
}
