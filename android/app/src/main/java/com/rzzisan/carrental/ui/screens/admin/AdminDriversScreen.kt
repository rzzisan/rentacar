package com.rzzisan.carrental.ui.screens.admin

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
import com.rzzisan.carrental.data.network.AdminDriver
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.DriverCollectRequest
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDriversScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var drivers by remember { mutableStateOf<List<AdminDriver>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var collectTarget by remember { mutableStateOf<AdminDriver?>(null) }
    val snackState = remember { SnackbarHostState() }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getAdminDrivers()
                if (res.success) drivers = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.adminDriversTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                    Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.retry) }
                }
            }
            drivers.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(s.noDrivers, color = InkMuted)
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(drivers) { driver ->
                    DriverCard(driver, s, onCollect = { collectTarget = driver })
                }
            }
        }
    }

    // Collect dues bottom sheet
    collectTarget?.let { driver ->
        CollectDuesSheet(
            driver = driver,
            s = s,
            onDismiss = { collectTarget = null },
            onCollect = { amount, method, notes ->
                scope.launch {
                    try {
                        val res = ApiClient.service.collectDriverDues(
                            driver.id,
                            DriverCollectRequest(amount, method, notes.ifBlank { null })
                        )
                        val msg = if (res.success) s.paymentCollected else "ত্রুটি"
                        collectTarget = null
                        if (res.success) load()
                        snackState.showSnackbar(msg)
                    } catch (e: HttpException) {
                        val body = e.response()?.errorBody()?.string() ?: s.error
                        snackState.showSnackbar("HTTP ${e.code()}: $body")
                    } catch (e: Exception) {
                        snackState.showSnackbar("${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
private fun DriverCard(
    driver: AdminDriver,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onCollect: () -> Unit
) {
    val isActive = driver.status == "active"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp),
                        color = if (isActive) StatusActive.copy(alpha = 0.1f) else InkMuted.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Icon(Icons.Filled.Person, null,
                                tint = if (isActive) StatusActive else InkMuted,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                    Column {
                        Text(driver.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                        driver.mobile?.let { Text(it, fontSize = 12.sp, color = InkMuted) }
                    }
                }
                Surface(shape = RoundedCornerShape(6.dp),
                    color = if (isActive) StatusActive.copy(alpha = 0.1f) else InkMuted.copy(alpha = 0.1f)) {
                    Text(if (isActive) "সক্রিয়" else "নিষ্ক্রিয়",
                        fontSize = 11.sp, color = if (isActive) StatusActive else InkMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Percent, null, tint = InkMuted, modifier = Modifier.size(14.dp))
                    Text("${s.commissionRate}: ${String.format("%.1f", driver.commissionRate)}%",
                        fontSize = 12.sp, color = InkMuted)
                }
                driver.email?.let {
                    Text(it, fontSize = 11.sp, color = InkMuted, maxLines = 1)
                }
            }
            if (isActive) {
                Button(
                    onClick = onCollect,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(s.collectDues, fontSize = 13.sp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectDuesSheet(
    driver: AdminDriver,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onDismiss: () -> Unit,
    onCollect: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("cash") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val methods = listOf("cash" to s.cash, "bank_transfer" to s.bankTransfer, "mobile_banking" to s.mobileBanking)

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(s.collectDues, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(driver.name, color = InkMuted, fontSize = 13.sp)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(s.amount) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
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
                Text(if (submitting) s.loading else s.collectDues, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
