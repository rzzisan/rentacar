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
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerRentalsScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var rentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var actionRental by remember { mutableStateOf<Rental?>(null) }
    var detailRental by remember { mutableStateOf<Rental?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var actionLoading by remember { mutableStateOf(false) }
    val snackState = remember { SnackbarHostState() }

    fun load(status: String? = selectedStatus) {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getManagerRentals(status = status)
                if (res.success) rentals = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }

    fun changeStatus(rental: Rental, newStatus: String) {
        actionLoading = true
        scope.launch {
            try {
                val res = ApiClient.service.updateManagerRentalStatus(rental.id, AdminUpdateStatusRequest(newStatus))
                if (res.success) load()
                snackState.showSnackbar(if (res.success) res.message ?: s.success else res.message ?: s.error)
            } catch (e: HttpException) {
                snackState.showSnackbar("HTTP ${e.code()}: ${e.response()?.errorBody()?.string() ?: s.error}")
            } catch (e: Exception) {
                snackState.showSnackbar("${e.javaClass.simpleName}: ${e.message}")
            } finally { actionLoading = false; actionRental = null }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.adminRentalsTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }, containerColor = Primary) {
                Icon(Icons.Filled.Add, s.createRental, tint = Color.White)
            }
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(null to s.allStatus, "pending" to s.statusPending,
                    "active" to s.statusActive, "completed" to s.statusCompleted,
                    "cancelled" to s.statusCancelled
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
                rentals.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(s.noRentals, color = InkMuted) }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rentals) { r ->
                        MgrRentalCard(r, s, onAction = { actionRental = r }, onDetail = { detailRental = r })
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showCreate) {
        MgrCreateRentalSheet(s = s, onDismiss = { showCreate = false }, onCreated = { msg ->
            showCreate = false; load()
            scope.launch { snackState.showSnackbar(msg) }
        })
    }

    detailRental?.let { r ->
        MgrRentalDetailSheet(rental = r, s = s, onDismiss = { detailRental = null })
    }

    actionRental?.let { rental ->
        ModalBottomSheet(onDismissRequest = { if (!actionLoading) actionRental = null }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("#${rental.id} — ${rental.vehicleBrand ?: ""} ${rental.vehicleModel ?: ""}",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(s.statusChangeQuestion, color = InkMuted, fontSize = 13.sp)
                if (actionLoading) {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
                } else {
                    when (rental.rentalStatus) {
                        "pending" -> {
                            Button(onClick = { changeStatus(rental, "active") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusActive)) { Text(s.activateTrip) }
                            OutlinedButton(onClick = { changeStatus(rental, "cancelled") }, modifier = Modifier.fillMaxWidth()) {
                                Text(s.cancelTripBtn, color = Color(0xFFEF4444))
                            }
                        }
                        "active" -> {
                            Button(onClick = { changeStatus(rental, "completed") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text(s.completeTrip) }
                            OutlinedButton(onClick = { changeStatus(rental, "cancelled") }, modifier = Modifier.fillMaxWidth()) {
                                Text(s.cancelTripBtn, color = Color(0xFFEF4444))
                            }
                        }
                        else -> Text("এই ট্রিপের স্ট্যাটাস পরিবর্তন করা যাবে না", color = InkMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MgrCreateRentalSheet(
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var drivers by remember { mutableStateOf<List<ManagerDriver>>(emptyList()) }
    var passengerName by remember { mutableStateOf("") }
    var passengerMobile by remember { mutableStateOf("") }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var selectedDriver by remember { mutableStateOf<ManagerDriver?>(null) }
    var pickupLocation by remember { mutableStateOf("") }
    var dropoffLocation by remember { mutableStateOf("") }
    var tripType by remember { mutableStateOf("one_way") }
    var startDatetime by remember { mutableStateOf("") }
    var agreedAmount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }
    var vehicleExpanded by remember { mutableStateOf(false) }
    var driverExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val vr = ApiClient.service.getManagerVehicles()
                if (vr.success) vehicles = (vr.data ?: emptyList()).filter { it.status == "available" }
                val dr = ApiClient.service.getManagerDrivers(status = "active")
                if (dr.success) drivers = dr.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        LazyColumn(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(s.createRental, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            if (formError.isNotEmpty()) {
                item { Text(formError, color = Color(0xFFDC2626), fontSize = 13.sp) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(passengerName, { passengerName = it },
                        label = { Text(s.passengerName) }, modifier = Modifier.weight(1f),
                        singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(passengerMobile, { passengerMobile = it },
                        label = { Text(s.passengerMobile) }, modifier = Modifier.weight(1f),
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp))
                }
            }
            item {
                ExposedDropdownMenuBox(expanded = vehicleExpanded, onExpandedChange = { vehicleExpanded = it }) {
                    OutlinedTextField(
                        value = selectedVehicle?.let { "${it.brand} ${it.model} (${it.registrationNumber})" } ?: s.selectVehicle,
                        onValueChange = {}, readOnly = true,
                        label = { Text(s.vehicle) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vehicleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = vehicleExpanded, onDismissRequest = { vehicleExpanded = false }) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.brand} ${v.model} — ${v.registrationNumber}", fontSize = 13.sp) },
                                onClick = { selectedVehicle = v; vehicleExpanded = false })
                        }
                    }
                }
            }
            item {
                ExposedDropdownMenuBox(expanded = driverExpanded, onExpandedChange = { driverExpanded = it }) {
                    OutlinedTextField(
                        value = selectedDriver?.name ?: s.autoDriver,
                        onValueChange = {}, readOnly = true,
                        label = { Text(s.selectDriver) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(driverExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = driverExpanded, onDismissRequest = { driverExpanded = false }) {
                        DropdownMenuItem(text = { Text(s.autoDriver, fontSize = 13.sp) },
                            onClick = { selectedDriver = null; driverExpanded = false })
                        drivers.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("${d.name} (${d.mobile ?: "—"})", fontSize = 13.sp) },
                                onClick = { selectedDriver = d; driverExpanded = false })
                        }
                    }
                }
            }
            item {
                OutlinedTextField(pickupLocation, { pickupLocation = it }, label = { Text(s.pickupLabel) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
            }
            item {
                OutlinedTextField(dropoffLocation, { dropoffLocation = it }, label = { Text(s.dropoffLabel) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
            }
            item {
                Text(s.tripType, fontSize = 13.sp, color = InkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("one_way" to s.oneWay, "round_trip" to s.roundTrip).forEach { (k, label) ->
                        FilterChip(tripType == k, { tripType = k }, label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White))
                    }
                }
            }
            item {
                OutlinedTextField(startDatetime, { startDatetime = it },
                    label = { Text(s.startDatetime) },
                    placeholder = { Text("YYYY-MM-DD HH:MM", color = InkMuted) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
            }
            item {
                OutlinedTextField(agreedAmount, { agreedAmount = it },
                    label = { Text(s.amount) }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp))
            }
            item {
                OutlinedTextField(notes, { notes = it }, label = { Text(s.notes) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(10.dp))
            }
            item {
                Button(
                    onClick = {
                        val vid = selectedVehicle?.id
                        val amt = agreedAmount.toDoubleOrNull()
                        if (passengerName.isBlank() || passengerMobile.isBlank()) { formError = "যাত্রীর নাম ও মোবাইল আবশ্যক"; return@Button }
                        if (vid == null) { formError = "${s.selectVehicle} আবশ্যক"; return@Button }
                        if (startDatetime.isBlank()) { formError = "${s.startDatetime} আবশ্যক"; return@Button }
                        if (amt == null || amt <= 0) { formError = "${s.amount} সঠিক হতে হবে"; return@Button }
                        submitting = true; formError = ""
                        scope.launch {
                            try {
                                val res = ApiClient.service.createManagerRental(
                                    CreateAdminRentalRequest(
                                        passengerName.trim(), passengerMobile.trim(), vid,
                                        selectedDriver?.id, pickupLocation.trim(), dropoffLocation.trim(),
                                        tripType, startDatetime.trim(), amt, notes.ifBlank { null }
                                    )
                                )
                                onCreated(if (res.success) s.rentalCreated else res.message ?: s.error)
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
                ) { Text(if (submitting) s.loading else s.createRental, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MgrRentalDetailSheet(rental: Rental, s: com.rzzisan.carrental.ui.strings.AppStrings, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<AdminRentalDetail?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(rental.id) {
        scope.launch {
            try {
                val res = ApiClient.service.getManagerRentalDetail(rental.id)
                if (res.success) detail = res.data
            } catch (_: Exception) {}
            finally { loading = false }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.rentalDetail, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            } else {
                val d = detail
                if (d == null) {
                    Text(s.error, color = Color(0xFFDC2626))
                } else {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("#${d.id} · ${d.vehicleBrand ?: ""} ${d.vehicleModel ?: ""}",
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        val (sc, sl) = when (d.rentalStatus) {
                            "active"    -> StatusActive to s.statusActive
                            "completed" -> StatusPaid to s.statusCompleted
                            "cancelled" -> InkMuted to s.statusCancelled
                            else        -> Color(0xFFF59E0B) to s.statusPending
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = sc.copy(alpha = 0.15f)) {
                            Text(sl, fontSize = 12.sp, color = sc, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    val customer = listOfNotNull(d.customerFirstName, d.customerLastName).joinToString(" ")
                    if (customer.isNotBlank()) MgrDetailRow("👤", customer, d.customerPhone)
                    if (!d.driverName.isNullOrBlank()) MgrDetailRow("🚗", d.driverName, null)
                    if (!d.pickupLocation.isNullOrBlank()) MgrDetailRow("📍", "${d.pickupLocation} → ${d.dropoffLocation ?: "—"}", null)
                    d.startDate?.let { MgrDetailRow("📅", it.take(10), d.actualStartTime?.take(16)) }
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(s.agreedAmount, fontSize = 13.sp, color = InkMuted)
                        Text("${s.taka}${String.format("%.0f", d.agreedAmount)}", fontWeight = FontWeight.Bold, color = Primary)
                    }
                    if (d.expenses.isNotEmpty()) {
                        Text(s.tripExpenses, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        d.expenses.forEach { exp ->
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(exp.expenseType, fontSize = 13.sp, color = InkMuted)
                                Text("${s.taka}${String.format("%.0f", exp.amount)}", fontSize = 13.sp, color = Ink)
                            }
                        }
                        HorizontalDivider(color = Color(0xFFE5E7EB))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(s.totalExpensesLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${s.taka}${String.format("%.0f", d.expenses.sumOf { it.amount })}", fontWeight = FontWeight.Bold, color = StatusDue)
                        }
                    } else {
                        Text(s.noExpenses, color = InkMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MgrDetailRow(icon: String, primary: String, secondary: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 14.sp)
        Column {
            Text(primary, fontSize = 13.sp, color = Ink)
            secondary?.let { Text(it, fontSize = 11.sp, color = InkMuted) }
        }
    }
}

@Composable
private fun MgrRentalCard(
    r: Rental,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onAction: () -> Unit,
    onDetail: () -> Unit
) {
    val (statusColor, statusLabel) = when (r.rentalStatus) {
        "active"    -> StatusActive      to s.statusActive
        "pending"   -> Color(0xFFF59E0B) to s.statusPending
        "completed" -> StatusPaid        to s.statusCompleted
        else        -> InkMuted          to s.statusCancelled
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("#${r.id} · ${r.vehicleBrand ?: ""} ${r.vehicleModel ?: ""}",
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink)
                    Text(r.vehicleRegNumber ?: "", fontSize = 11.sp, color = InkMuted)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusLabel, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            if (!r.pickupLocation.isNullOrBlank()) {
                Text("${r.pickupLocation} → ${r.dropoffLocation ?: "—"}", fontSize = 12.sp, color = InkMuted, maxLines = 1)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val customer = listOfNotNull(r.customerFirstName, r.customerLastName).joinToString(" ")
                    if (customer.isNotBlank()) Text("👤 $customer", fontSize = 12.sp, color = InkMuted)
                    if (!r.driverName.isNullOrBlank()) Text("🚗 ${r.driverName}", fontSize = 12.sp, color = InkMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.taka}${String.format("%.0f", r.agreedAmount)}",
                        fontWeight = FontWeight.Bold, color = Primary, fontSize = 15.sp)
                    r.startDate?.let { Text(it.take(10), fontSize = 11.sp, color = InkMuted) }
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDetail, modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) {
                    Text(s.viewDetail, fontSize = 12.sp, color = Primary)
                }
                if (r.rentalStatus in listOf("pending", "active")) {
                    Button(onClick = onAction, modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Text(s.changeStatus, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
