package com.rzzisan.carrental.ui.screens.admin

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.*
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
    var showForm by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<AdminDriver?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminDriver?>(null) }
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
        floatingActionButton = {
            FloatingActionButton(onClick = { editTarget = null; showForm = true }, containerColor = Primary) {
                Icon(Icons.Filled.Add, s.addDriver, tint = Color.White)
            }
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
            drivers.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text(s.noDrivers, color = InkMuted) }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(drivers) { driver ->
                    DriverCard(driver, s,
                        onEdit = { editTarget = driver; showForm = true },
                        onDelete = { deleteTarget = driver },
                        onCollect = { collectTarget = driver })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    // Add/Edit form
    if (showForm) {
        DriverFormSheet(s = s, existing = editTarget, onDismiss = { showForm = false },
            onSaved = { msg ->
                showForm = false; load()
                scope.launch { snackState.showSnackbar(msg) }
            })
    }

    // Delete confirm
    deleteTarget?.let { d ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(s.deleteDriver, fontWeight = FontWeight.SemiBold) },
            text = { Text("${d.name}\n${s.deleteQuestion}") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val res = ApiClient.service.deleteDriver(d.id)
                            deleteTarget = null; load()
                            snackState.showSnackbar(if (res.success) s.driverDeleted else res.message ?: s.error)
                        } catch (e: HttpException) {
                            deleteTarget = null
                            snackState.showSnackbar("HTTP ${e.code()}: ${e.response()?.errorBody()?.string() ?: s.error}")
                        } catch (e: Exception) {
                            deleteTarget = null
                            snackState.showSnackbar("${e.javaClass.simpleName}: ${e.message}")
                        }
                    }
                }) { Text(s.yes, color = Color(0xFFEF4444)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(s.cancel) } }
        )
    }

    // Collect dues sheet
    collectTarget?.let { driver ->
        CollectDuesSheet(driver = driver, s = s, onDismiss = { collectTarget = null },
            onCollect = { amount, method, notes ->
                scope.launch {
                    try {
                        val res = ApiClient.service.collectDriverDues(driver.id,
                            DriverCollectRequest(amount, method, notes.ifBlank { null }))
                        val msg = if (res.success) s.paymentCollected else s.error
                        collectTarget = null
                        if (res.success) load()
                        snackState.showSnackbar(msg)
                    } catch (e: HttpException) {
                        snackState.showSnackbar("HTTP ${e.code()}: ${e.response()?.errorBody()?.string() ?: s.error}")
                    } catch (e: Exception) {
                        snackState.showSnackbar("${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            })
    }
}

// ── Driver Form Sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverFormSheet(
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    existing: AdminDriver?,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var mobile by remember { mutableStateOf(existing?.mobile ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var commission by remember { mutableStateOf(existing?.commissionRate?.let { "%.1f".format(it) } ?: "15.0") }
    var driverStatus by remember { mutableStateOf(existing?.status ?: "active") }
    var selectedVehicleIds by remember { mutableStateOf(existing?.vehicles?.map { it.id }?.toSet() ?: emptySet<Int>()) }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var submitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val res = ApiClient.service.getAdminVehicles()
                if (res.success) vehicles = res.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        LazyColumn(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(if (existing == null) s.addDriver else s.editDriver, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            if (formError.isNotEmpty()) {
                item { Text(formError, color = Color(0xFFDC2626), fontSize = 13.sp) }
            }
            item {
                OutlinedTextField(name, { name = it }, label = { Text(s.name) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
            }
            item {
                OutlinedTextField(mobile, { mobile = it }, label = { Text(s.mobile) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp))
            }
            item {
                OutlinedTextField(email, { email = it }, label = { Text(s.email) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp))
            }
            item {
                OutlinedTextField(password, { password = it },
                    label = { Text(if (existing == null) s.driverPassword else "${s.driverPassword} (খালি রাখলে পরিবর্তন হবে না)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(10.dp))
            }
            item {
                OutlinedTextField(commission, { commission = it },
                    label = { Text(s.commissionRateLabel) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp))
            }
            // Status
            item {
                Text(s.driverStatus, fontSize = 13.sp, color = InkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("active" to s.activeLabel, "inactive" to s.inactiveLabel).forEach { (k, lbl) ->
                        FilterChip(driverStatus == k, { driverStatus = k }, label = { Text(lbl, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White))
                    }
                }
            }
            // Vehicle assignment
            item {
                Text(s.vehicleAssignment, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            if (vehicles.isEmpty()) {
                item { Text(s.noVehicles, fontSize = 13.sp, color = InkMuted) }
            } else {
                items(vehicles) { v ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = v.id in selectedVehicleIds,
                            onCheckedChange = { checked ->
                                selectedVehicleIds = if (checked) selectedVehicleIds + v.id
                                else selectedVehicleIds - v.id
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                        Column {
                            Text("${v.brand} ${v.model}", fontSize = 13.sp)
                            Text(v.registrationNumber, fontSize = 11.sp, color = InkMuted)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (name.isBlank() || mobile.isBlank() || email.isBlank()) { formError = "নাম, মোবাইল ও ইমেইল আবশ্যক"; return@Button }
                        if (existing == null && password.length < 6) { formError = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষর"; return@Button }
                        val vehicleIdsJson = "[${selectedVehicleIds.joinToString(",")}]"
                        submitting = true; formError = ""
                        scope.launch {
                            try {
                                if (existing == null) {
                                    val res = ApiClient.service.createDriver(
                                        name.trim(), mobile.trim(), email.trim(), password,
                                        commission.ifBlank { "15.0" }, driverStatus, vehicleIdsJson
                                    )
                                    onSaved(if (res.success) s.driverAdded else res.message ?: s.error)
                                } else {
                                    val res = ApiClient.service.updateDriver(
                                        existing.id, name.trim(), mobile.trim(), email.trim(),
                                        commission.ifBlank { "15.0" }, driverStatus, vehicleIdsJson
                                    )
                                    onSaved(if (res.success) s.driverUpdated else res.message ?: s.error)
                                }
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
                ) { Text(if (submitting) s.loading else s.save, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ── Driver Card ────────────────────────────────────────────────────

@Composable
private fun DriverCard(
    driver: AdminDriver,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCollect: () -> Unit
) {
    val isActive = driver.status == "active"
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
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
                    Text(if (isActive) s.activeLabel else s.inactiveLabel, fontSize = 11.sp,
                        color = if (isActive) StatusActive else InkMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${s.commissionRateLabel}: ${String.format("%.1f", driver.commissionRate)}%",
                    fontSize = 12.sp, color = InkMuted)
                driver.email?.let { Text(it, fontSize = 11.sp, color = InkMuted, maxLines = 1) }
            }
            // Assigned vehicles
            if (driver.vehicles.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    driver.vehicles.forEach { v ->
                        Surface(shape = RoundedCornerShape(4.dp), color = Primary.copy(alpha = 0.08f)) {
                            Text("🚗 ${v.brand} ${v.model}", fontSize = 11.sp, color = Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(13.dp), tint = Primary)
                    Spacer(Modifier.width(4.dp))
                    Text(s.editDriver.take(5), fontSize = 11.sp, color = Primary)
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(13.dp), tint = Color(0xFFEF4444))
                    Spacer(Modifier.width(4.dp))
                    Text(s.deleteDriver.take(4), fontSize = 11.sp, color = Color(0xFFEF4444))
                }
                if (isActive) {
                    Button(onClick = onCollect, modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Text(s.collectDues, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ── Collect Dues Sheet ─────────────────────────────────────────────

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
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(s.collectDues, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(driver.name, color = InkMuted, fontSize = 13.sp)
            OutlinedTextField(amountText, { amountText = it }, label = { Text(s.amount) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, shape = RoundedCornerShape(10.dp))
            Text(s.paymentMethod, fontSize = 13.sp, color = InkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.forEach { (key, label) ->
                    FilterChip(selectedMethod == key, { selectedMethod = key }, label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White))
                }
            }
            OutlinedTextField(notes, { notes = it }, label = { Text(s.paymentNotes) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), maxLines = 2)
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
            ) { Text(if (submitting) s.loading else s.collectDues, fontWeight = FontWeight.SemiBold) }
        }
    }
}
