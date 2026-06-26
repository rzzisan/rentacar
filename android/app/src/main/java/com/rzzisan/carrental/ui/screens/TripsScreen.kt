package com.rzzisan.carrental.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rzzisan.carrental.data.network.*
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import com.rzzisan.carrental.util.reverseGeocode
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter

// ── Helpers ────────────────────────────────────────────────────────────────

private fun expTypeLabel(s: com.rzzisan.carrental.ui.strings.AppStrings, key: String) = when (key) {
    "toll"             -> s.expToll
    "fuel"             -> s.expFuel
    "parking"          -> s.expParking
    "repair"           -> s.expRepair
    "driver_allowance" -> s.expAllowance
    else               -> s.expOther
}

private fun statusColor(status: String) = when (status) {
    "active"    -> StatusActive
    "completed" -> StatusDone
    "cancelled" -> InkMuted
    else        -> StatusPending
}

// ── Main screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(navController: NavController) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fusedLocation = remember { LocationServices.getFusedLocationProviderClient(context) }

    var rentals      by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var loading      by remember { mutableStateOf(true) }
    var error        by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var snackMsg     by remember { mutableStateOf("") }
    val snackState   = remember { SnackbarHostState() }

    // Sheet state
    var showAddSheet    by remember { mutableStateOf(false) }
    var detailRentalId  by remember { mutableStateOf<Int?>(null) }

    // Location permission launcher
    var permCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) permCallback?.invoke()
        permCallback = null
    }

    fun hasLocationPerm() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Get location — returns null if unavailable (never blocks the user flow)
    suspend fun getLocation(): Triple<String?, Double?, Double?> {
        if (!hasLocationPerm()) return Triple(null, null, null)
        return try {
            @Suppress("MissingPermission")
            val loc = fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            if (loc != null) {
                val name = reverseGeocode(context, loc.latitude, loc.longitude)
                Triple(name, loc.latitude, loc.longitude)
            } else Triple(null, null, null)
        } catch (_: Exception) { Triple(null, null, null) }
    }

    fun load() {
        scope.launch {
            loading = true; error = ""
            try {
                val res = ApiClient.service.getRentals(status = statusFilter)
                if (res.success) rentals = res.data ?: emptyList()
                else error = res.message ?: s.error
            } catch (_: Exception) { error = s.serverError }
            finally { loading = false }
        }
    }

    LaunchedEffect(statusFilter) { load() }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) {
            snackState.showSnackbar(snackMsg)
            snackMsg = ""
        }
    }

    val filters = listOf(
        null       to s.allStatus,
        "pending"  to s.statusPending,
        "active"   to s.statusActive,
        "completed" to s.statusCompleted
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.tripsTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = Primary
            ) { Icon(Icons.Filled.Add, s.newTrip, tint = Color.White) }
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Status filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { (value, label) ->
                    FilterChip(
                        selected = statusFilter == value,
                        onClick  = { statusFilter = value },
                        label    = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)
                    error.isNotEmpty() -> Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(error, color = StatusDue, fontSize = 13.sp)
                        Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text(s.retry)
                        }
                    }
                    rentals.isEmpty() -> Text(
                        s.noTrips, Modifier.align(Alignment.Center), color = InkMuted
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(rentals, key = { it.id }) { rental ->
                            TripCard(rental, s, onClick = { detailRentalId = rental.id })
                        }
                    }
                }
            }
        }
    }

    // ── Add Trip Sheet ──────────────────────────────────────────────────────
    if (showAddSheet) {
        AddTripSheet(
            s = s,
            onDismiss = { showAddSheet = false },
            onCreated = { msg ->
                showAddSheet = false
                snackMsg = msg
                load()
            }
        )
    }

    // ── Trip Detail Sheet ───────────────────────────────────────────────────
    detailRentalId?.let { rentalId ->
        TripDetailSheet(
            rentalId   = rentalId,
            s          = s,
            onDismiss  = { detailRentalId = null },
            getLocation = ::getLocation,
            requestPerm = { callback ->
                permCallback = callback
                locPermLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            },
            hasLocationPerm = ::hasLocationPerm,
            onStatusChanged = { msg ->
                snackMsg = msg
                load()
            }
        )
    }
}

// ── Trip Card ──────────────────────────────────────────────────────────────

@Composable
private fun TripCard(
    rental: Rental,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onClick: () -> Unit
) {
    val color = statusColor(rental.rentalStatus)
    val statusLabel = when (rental.rentalStatus) {
        "active"    -> s.statusActive
        "completed" -> s.statusCompleted
        "cancelled" -> s.statusCancelled
        else        -> s.statusPending
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    "${rental.customerFirstName ?: ""} ${rental.customerLastName ?: ""}".trim().ifEmpty { "-" },
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink
                )
                Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.12f)) {
                    Text(statusLabel, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Text(
                "${rental.vehicleBrand ?: ""} ${rental.vehicleModel ?: ""} • ${rental.vehicleRegNumber ?: ""}",
                fontSize = 12.sp, color = InkMuted
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    "${rental.pickupLocation ?: "-"} → ${rental.dropoffLocation ?: "-"}",
                    fontSize = 12.sp, color = InkMuted, modifier = Modifier.weight(1f)
                )
                Text(fmtBDT(rental.agreedAmount), fontWeight = FontWeight.Bold, color = Primary, fontSize = 14.sp)
            }
            rental.startDate?.let {
                Text(it.take(10), fontSize = 11.sp, color = InkLight)
            }
        }
    }
}

// ── Add Trip Sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTripSheet(
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var passengerName   by remember { mutableStateOf("") }
    var passengerPhone  by remember { mutableStateOf("") }
    var pickup          by remember { mutableStateOf("") }
    var dropoff         by remember { mutableStateOf("") }
    var amount          by remember { mutableStateOf("") }
    var notes           by remember { mutableStateOf("") }
    var tripType        by remember { mutableStateOf("one_way") }
    var selectedVehicle by remember { mutableStateOf<AssignedVehicle?>(null) }
    var vehicles        by remember { mutableStateOf<List<AssignedVehicle>>(emptyList()) }
    var vehicleExpanded by remember { mutableStateOf(false) }
    var submitting      by remember { mutableStateOf(false) }
    var formError       by remember { mutableStateOf("") }

    var selectedDate    by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime    by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var showDatePicker  by remember { mutableStateOf(false) }
    var showTimePicker  by remember { mutableStateOf(false) }

    val displayDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    val displayTime = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    val apiDatetime = LocalDateTime.of(selectedDate, selectedTime)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.service.getVehicles()
            if (res.success) {
                vehicles = res.data ?: emptyList()
                if (vehicles.size == 1) selectedVehicle = vehicles[0]
            }
        } catch (_: Exception) {}
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { ms ->
                        selectedDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(s.confirm) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(s.cancel) } }
        ) { DatePicker(state = dateState) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = selectedTime.hour, initialMinute = selectedTime.minute, is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text(s.confirm) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(s.cancel) } },
            text = { TimePicker(state = timeState) }
        )
    }

    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(s.createTripTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            if (formError.isNotEmpty()) {
                Surface(color = StatusDue.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(formError, color = StatusDue, modifier = Modifier.padding(10.dp).fillMaxWidth(), fontSize = 13.sp)
                }
            }

            OutlinedTextField(value = passengerName,  onValueChange = { passengerName = it },
                label = { Text(s.passengerName) },  modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = passengerPhone, onValueChange = { passengerPhone = it },
                label = { Text(s.passengerMobile) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = pickup,  onValueChange = { pickup = it },
                label = { Text(s.pickup) },  modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = dropoff, onValueChange = { dropoff = it },
                label = { Text(s.dropoff) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = amount,  onValueChange = { amount = it },
                label = { Text(s.amount) },  modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = notes,   onValueChange = { notes = it },
                label = { Text(s.notes) },   modifier = Modifier.fillMaxWidth(), maxLines = 2)

            // Date + Time
            Text(s.startDatetime, style = MaterialTheme.typography.labelMedium, color = InkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(displayDate, fontSize = 13.sp)
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(displayTime, fontSize = 13.sp)
                }
            }

            // Vehicle picker
            ExposedDropdownMenuBox(expanded = vehicleExpanded, onExpandedChange = { vehicleExpanded = it }) {
                OutlinedTextField(
                    value = selectedVehicle?.let { "${it.brand} ${it.model} (${it.registrationNumber})" } ?: "",
                    onValueChange = {}, label = { Text(s.selectVehicle) },
                    readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vehicleExpanded) }
                )
                ExposedDropdownMenu(expanded = vehicleExpanded, onDismissRequest = { vehicleExpanded = false }) {
                    if (vehicles.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("কোনো গাড়ি অ্যাসাইন নেই", color = InkMuted) },
                            onClick = { vehicleExpanded = false }
                        )
                    } else {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.brand} ${v.model} • ${v.registrationNumber}") },
                                onClick = { selectedVehicle = v; vehicleExpanded = false }
                            )
                        }
                    }
                }
            }
            if (vehicles.isEmpty()) {
                Text("কোনো গাড়ি অ্যাসাইন নেই — অ্যাডমিনের সাথে যোগাযোগ করুন",
                    fontSize = 11.sp, color = StatusDue)
            }

            // Trip type
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("one_way" to s.oneWay, "round_trip" to s.roundTrip).forEach { (v, label) ->
                    FilterChip(selected = tripType == v, onClick = { tripType = v }, label = { Text(label) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (!submitting) onDismiss() },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(s.cancel) }

                Button(
                    onClick = {
                        if (passengerName.isBlank() || passengerPhone.isBlank() ||
                            pickup.isBlank() || dropoff.isBlank() ||
                            amount.isBlank() || selectedVehicle == null
                        ) { formError = "সব তথ্য পূরণ করুন"; return@Button }
                        submitting = true; formError = ""
                        scope.launch {
                            try {
                                val res = ApiClient.service.createRental(
                                    CreateRentalRequest(
                                        passengerName   = passengerName,
                                        passengerMobile = passengerPhone,
                                        vehicleId       = selectedVehicle!!.id,
                                        pickupLocation  = pickup,
                                        dropoffLocation = dropoff,
                                        tripType        = tripType,
                                        startDatetime   = apiDatetime,
                                        agreedAmount    = amount.toDoubleOrNull() ?: 0.0,
                                        notes           = notes.ifBlank { null }
                                    )
                                )
                                if (res.success) onCreated(s.tripCreated)
                                else { formError = res.message ?: s.error; submitting = false }
                            } catch (_: Exception) { formError = s.serverError; submitting = false }
                        }
                    },
                    enabled  = !submitting && vehicles.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text(if (submitting) s.loading else s.submit, fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Trip Detail Sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDetailSheet(
    rentalId: Int,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onDismiss: () -> Unit,
    getLocation: suspend () -> Triple<String?, Double?, Double?>,
    requestPerm: (() -> Unit) -> Unit,
    hasLocationPerm: () -> Boolean,
    onStatusChanged: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var rental     by remember { mutableStateOf<Rental?>(null) }
    var expenses   by remember { mutableStateOf<List<TripExpense>>(emptyList()) }
    var loading    by remember { mutableStateOf(true) }
    var actionMsg  by remember { mutableStateOf("") }
    var actionLoading by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            try {
                val r = ApiClient.service.getRental(rentalId)
                if (r.success) rental = r.data
                val e = ApiClient.service.getExpenses(rentalId)
                if (e.success) expenses = e.data ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(rentalId) {
        loading = true
        try {
            val r = ApiClient.service.getRental(rentalId)
            if (r.success) rental = r.data
            val e = ApiClient.service.getExpenses(rentalId)
            if (e.success) expenses = e.data ?: emptyList()
        } catch (_: Exception) {}
        loading = false
    }

    fun updateStatus(newStatus: String) {
        if (actionLoading) return
        actionLoading = true
        actionMsg = s.gettingLocation
        scope.launch {
            try {
                if (!hasLocationPerm()) {
                    // Request permission then retry
                    requestPerm {
                        scope.launch {
                            val (locName, lat, lng) = getLocation()
                            val res = ApiClient.service.updateStatus(
                                rentalId,
                                UpdateStatusRequest(newStatus, locName ?: "", lat ?: 0.0, lng ?: 0.0)
                            )
                            actionMsg = res.message ?: s.success
                            if (res.success) { reload(); onStatusChanged(res.message ?: s.success) }
                            actionLoading = false
                        }
                    }
                    return@launch
                }
                val (locName, lat, lng) = getLocation()
                val res = ApiClient.service.updateStatus(
                    rentalId,
                    UpdateStatusRequest(newStatus, locName ?: "", lat ?: 0.0, lng ?: 0.0)
                )
                actionMsg = res.message ?: if (res.success) s.success else s.error
                if (res.success) { reload(); onStatusChanged(res.message ?: s.success) }
            } catch (_: Exception) { actionMsg = s.error }
            finally { actionLoading = false }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!actionLoading) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center).padding(32.dp), color = Primary)
                rental == null -> Text(s.error, Modifier.padding(32.dp), color = StatusDue)
                else -> {
                    val r = rental!!
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(s.tripDetail, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            val sc = statusColor(r.rentalStatus)
                            val sl = when (r.rentalStatus) {
                                "active" -> s.statusActive; "completed" -> s.statusCompleted
                                "cancelled" -> s.statusCancelled; else -> s.statusPending
                            }
                            Surface(shape = RoundedCornerShape(20.dp), color = sc.copy(alpha = 0.12f)) {
                                Text(sl, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }

                        // Trip info
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TDetailRow(s.passenger, "${r.customerFirstName ?: ""} ${r.customerLastName ?: ""}".trim().ifEmpty { "-" })
                                TDetailRow(s.vehicle, "${r.vehicleBrand ?: ""} ${r.vehicleModel ?: ""}".trim())
                                TDetailRow(s.pickup, r.pickupLocation ?: "-")
                                TDetailRow(s.dropoff, r.dropoffLocation ?: "-")
                                TDetailRow(s.agreedAmount, fmtBDT(r.agreedAmount))
                                r.startDate?.let { TDetailRow(s.plannedStartLabel, it.take(16).replace("T", " ")) }
                                r.actualStartTime?.let { TDetailRow(s.actualStartLabel, it.take(16)) }
                                r.actualEndTime?.let { TDetailRow(s.actualEndLabel, it.take(16)) }
                            }
                        }

                        // Action message
                        if (actionMsg.isNotEmpty()) {
                            Surface(color = PrimaryLight, shape = RoundedCornerShape(8.dp)) {
                                Text(actionMsg, color = Primary, fontSize = 13.sp,
                                    modifier = Modifier.padding(10.dp).fillMaxWidth())
                            }
                        }

                        // Status action buttons
                        when (r.rentalStatus) {
                            "pending" -> Button(
                                onClick = { updateStatus("active") },
                                enabled = !actionLoading,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusActive)
                            ) {
                                if (actionLoading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text(s.startTrip, fontWeight = FontWeight.SemiBold)
                            }
                            "active" -> OutlinedButton(
                                onClick = { updateStatus("completed") },
                                enabled = !actionLoading,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (actionLoading) CircularProgressIndicator(Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                                else Text(s.completeTrip, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        HorizontalDivider()

                        // Expenses section
                        Text(s.expensesLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)

                        if (expenses.isEmpty()) {
                            Text(s.noExpenses, fontSize = 13.sp, color = InkMuted)
                        } else {
                            expenses.forEach { exp ->
                                ExpenseItem(exp, s)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = PrimaryLight) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    Arrangement.SpaceBetween
                                ) {
                                    Text(s.totalExpensesLabel, fontWeight = FontWeight.SemiBold, color = Primary)
                                    Text(fmtBDT(expenses.sumOf { it.amount }), fontWeight = FontWeight.Bold, color = Primary)
                                }
                            }
                        }

                        // Add expense form (only when active)
                        if (r.rentalStatus == "active") {
                            HorizontalDivider()
                            AddExpenseInline(
                                rentalId = rentalId,
                                s = s,
                                context = context,
                                onAdded = { reload() }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ── Expense item ───────────────────────────────────────────────────────────

@Composable
private fun ExpenseItem(exp: TripExpense, s: com.rzzisan.carrental.ui.strings.AppStrings) {
    Card(shape = RoundedCornerShape(8.dp), border = CardDefaults.outlinedCardBorder()) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(expTypeLabel(s, exp.expenseType), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                if (!exp.description.isNullOrBlank())
                    Text(exp.description, fontSize = 12.sp, color = InkMuted)
                if (!exp.locationName.isNullOrBlank())
                    Text(exp.locationName, fontSize = 11.sp, color = InkLight)
                Text(exp.createdAt.take(16), fontSize = 11.sp, color = InkLight)
            }
            Text(fmtBDT(exp.amount), fontWeight = FontWeight.Bold, color = StatusDue, fontSize = 14.sp)
        }
    }
}

// ── Inline Add Expense ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseInline(
    rentalId: Int,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    context: android.content.Context,
    onAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val fusedLocation = remember { LocationServices.getFusedLocationProviderClient(context) }

    val expenseTypes = listOf(
        "toll" to s.expToll, "fuel" to s.expFuel, "parking" to s.expParking,
        "repair" to s.expRepair, "driver_allowance" to s.expAllowance, "other" to s.expOther
    )
    var selectedType by remember { mutableStateOf("fuel") }
    var typeExpanded by remember { mutableStateOf(false) }
    var amount       by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }
    var submitting   by remember { mutableStateOf(false) }
    var statusMsg    by remember { mutableStateOf("") }
    var isSuccess    by remember { mutableStateOf(false) }
    var photoUri     by remember { mutableStateOf<Uri?>(null) }
    var photoFile    by remember { mutableStateOf<File?>(null) }

    val tempFile = remember { File(context.cacheDir, "exp_${System.currentTimeMillis()}.jpg") }
    val tempUri  = remember { FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) { photoUri = tempUri; photoFile = tempFile }
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(tempUri)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(s.addExpenseTitle, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)

        if (statusMsg.isNotEmpty()) {
            Surface(
                color = if (isSuccess) StatusActive.copy(0.1f) else StatusDue.copy(0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(statusMsg, color = if (isSuccess) StatusActive else StatusDue,
                    fontSize = 13.sp, modifier = Modifier.padding(10.dp).fillMaxWidth())
            }
        }

        // Type picker
        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
            OutlinedTextField(
                value = expenseTypes.firstOrNull { it.first == selectedType }?.second ?: selectedType,
                onValueChange = {}, label = { Text(s.expenseType) },
                readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) }
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                expenseTypes.forEach { (v, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { selectedType = v; typeExpanded = false })
                }
            }
        }

        OutlinedTextField(value = amount, onValueChange = { amount = it },
            label = { Text(s.expenseAmount) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it },
            label = { Text(s.description) }, modifier = Modifier.fillMaxWidth(), maxLines = 2)

        // Camera button
        OutlinedButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(tempUri)
                } else {
                    cameraPermLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                if (photoUri != null) Icons.Filled.CheckCircle else Icons.Filled.CameraAlt,
                null, modifier = Modifier.size(16.dp),
                tint = if (photoUri != null) StatusActive else Primary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (photoUri != null) "ছবি নেওয়া হয়েছে ✓" else s.takePhoto,
                color = if (photoUri != null) StatusActive else Primary, fontSize = 13.sp
            )
        }

        Button(
            onClick = {
                if (amount.isBlank()) { statusMsg = "${s.expenseAmount} দিন"; isSuccess = false; return@Button }
                submitting = true; statusMsg = s.gpsCapturing; isSuccess = false
                scope.launch {
                    try {
                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                        var lat: Double? = null; var lng: Double? = null; var locName: String? = null
                        if (hasPerm) {
                            try {
                                @Suppress("MissingPermission")
                                val loc = fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                                if (loc != null) {
                                    lat = loc.latitude; lng = loc.longitude
                                    locName = reverseGeocode(context, lat, lng)
                                }
                            } catch (_: Exception) {}
                        }

                        val typePart   = selectedType.toRequestBody("text/plain".toMediaType())
                        val amountPart = amount.toRequestBody("text/plain".toMediaType())
                        val descPart   = description.ifBlank { null }?.toRequestBody("text/plain".toMediaType())
                        val latPart    = lat?.toString()?.toRequestBody("text/plain".toMediaType())
                        val lngPart    = lng?.toString()?.toRequestBody("text/plain".toMediaType())
                        val locPart    = locName?.toRequestBody("text/plain".toMediaType())
                        val imgPart    = photoFile?.let { f ->
                            MultipartBody.Part.createFormData("receipt_image", f.name, f.asRequestBody("image/jpeg".toMediaType()))
                        }

                        val res = ApiClient.service.addExpense(
                            rentalId = rentalId,
                            expenseType = typePart, amount = amountPart,
                            description = descPart, locationName = locPart,
                            latitude = latPart, longitude = lngPart,
                            receiptImage = imgPart
                        )
                        isSuccess  = res.success
                        statusMsg  = res.message ?: if (res.success) s.expenseAdded else s.error
                        if (res.success) {
                            amount = ""; description = ""; photoUri = null; photoFile = null
                            selectedType = "fuel"
                            onAdded()
                        }
                    } catch (_: Exception) { statusMsg = s.serverError; isSuccess = false }
                    finally { submitting = false }
                }
            },
            enabled  = !submitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Primary)
        ) { Text(if (submitting) s.gpsCapturing else s.addExpense, fontWeight = FontWeight.SemiBold) }
    }
}

// ── Detail row helper ──────────────────────────────────────────────────────

@Composable
private fun TDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = InkMuted, modifier = Modifier.weight(0.45f))
        Text(value.ifBlank { "-" }, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink,
            modifier = Modifier.weight(0.55f))
    }
}
