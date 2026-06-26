package com.rzzisan.carrental.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.AdminSettlement
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.CollectPaymentRequest
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettlementsScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var settlements by remember { mutableStateOf<List<AdminSettlement>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var collectTarget by remember { mutableStateOf<AdminSettlement?>(null) }
    val snackState = remember { SnackbarHostState() }

    fun load(status: String? = selectedStatus) {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getAdminSettlements(status = status)
                if (res.success) settlements = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
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
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary, selectedLabelColor = Color.White)
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
                settlements.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(s.noAdminSettlements, color = InkMuted)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(settlements) { settlement ->
                        SettlementCard(settlement, s, onCollect = { collectTarget = settlement })
                    }
                }
            }
        }
    }

    // Collect payment bottom sheet
    collectTarget?.let { target ->
        CollectPaymentSheet(
            settlement = target,
            s = s,
            onDismiss = { collectTarget = null },
            onCollect = { amount, method, notes ->
                scope.launch {
                    try {
                        val res = ApiClient.service.collectSettlementPayment(
                            target.id,
                            CollectPaymentRequest(amount, method, notes.ifBlank { null })
                        )
                        val msg = if (res.success) s.paymentCollected else res.message ?: s.error
                        collectTarget = null
                        if (res.success) load()
                        snackState.showSnackbar(msg)
                    } catch (e: HttpException) {
                        snackState.showSnackbar("HTTP ${e.code()}: ${e.response()?.errorBody()?.string() ?: s.error}")
                    } catch (e: Exception) {
                        snackState.showSnackbar("${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
private fun SettlementCard(
    settlement: AdminSettlement,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onCollect: () -> Unit
) {
    val (statusColor, statusLabel) = when (settlement.paymentStatus) {
        "paid"    -> StatusPaid    to s.statusPaid
        "partial" -> StatusPartial to s.statusPartial
        else      -> StatusDue     to s.statusPending
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${settlement.driverName ?: "—"}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                    Text("#${settlement.rentalId} · ${settlement.vehicleBrand ?: ""} ${settlement.vehicleModel ?: ""}",
                        fontSize = 12.sp, color = InkMuted)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusLabel, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            if (!settlement.pickupLocation.isNullOrBlank()) {
                Text("${settlement.pickupLocation} → ${settlement.dropoffLocation ?: "—"}",
                    fontSize = 12.sp, color = InkMuted, maxLines = 1)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AmountRow(s.amountToCollect, settlement.amountToCollect, Primary)
                    AmountRow(s.paidAmount, settlement.paidAmount, StatusPaid)
                    AmountRow(s.remainingAmount, settlement.remainingAmount, StatusDue)
                }
            }
            if (settlement.paymentStatus != "paid") {
                Button(
                    onClick = onCollect,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(s.collectPayment, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, amount: Double, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", fontSize = 12.sp, color = InkMuted)
        Text("৳${String.format("%.2f", amount)}", fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectPaymentSheet(
    settlement: AdminSettlement,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onDismiss: () -> Unit,
    onCollect: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format("%.2f", settlement.remainingAmount)) }
    var selectedMethod by remember { mutableStateOf("cash") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val methods = listOf("cash" to s.cash, "bank_transfer" to s.bankTransfer, "mobile_banking" to s.mobileBanking)

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(s.collectPayment, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${settlement.driverName ?: ""} · #${settlement.rentalId}", color = InkMuted, fontSize = 13.sp)
            // Remaining info
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = StatusDue.copy(alpha = 0.1f)) {
                    Text("${s.remainingAmount}: ৳${String.format("%.2f", settlement.remainingAmount)}",
                        color = StatusDue, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            // Amount field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(s.amount) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            // Payment method
            Text(s.paymentMethod, fontSize = 13.sp, color = InkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedMethod == key,
                        onClick = { selectedMethod = key },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary, selectedLabelColor = Color.White)
                    )
                }
            }
            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(s.paymentNotes) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                maxLines = 2
            )
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: return@Button
                    submitting = true
                    onCollect(amt, selectedMethod, notes)
                },
                enabled = !submitting && amountText.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (submitting) s.loading else s.collectPayment, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
