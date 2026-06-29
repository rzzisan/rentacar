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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.*
import com.rzzisan.carrental.ui.LocalAdminDrawerState
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVehiclesScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val drawerState = LocalAdminDrawerState.current
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Vehicle?>(null) }
    var deleteTarget by remember { mutableStateOf<Vehicle?>(null) }
    val snackState = remember { SnackbarHostState() }

    fun load(status: String? = selectedStatus) {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getAdminVehicles(status = status)
                if (res.success) vehicles = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.vehiclesTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState?.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editTarget = null; showForm = true },
                containerColor = Primary
            ) { Icon(Icons.Filled.Add, s.addVehicle, tint = Color.White) }
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(null to s.allStatus, "available" to s.statusAvailable,
                    "rented" to s.statusRented, "maintenance" to s.statusMaintenance,
                    "inactive" to s.statusInactive
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
                vehicles.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(s.noVehicles, color = InkMuted) }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vehicles) { v ->
                        VehicleCard(v, s,
                            onEdit = { editTarget = v; showForm = true },
                            onDelete = { deleteTarget = v })
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Add / Edit form
    if (showForm) {
        VehicleFormSheet(
            s = s,
            existing = editTarget,
            onDismiss = { showForm = false },
            onSaved = { msg ->
                showForm = false
                load()
                scope.launch { snackState.showSnackbar(msg) }
            }
        )
    }

    // Delete confirmation
    deleteTarget?.let { v ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(s.deleteVehicle, fontWeight = FontWeight.SemiBold) },
            text = { Text("${v.brand} ${v.model} (${v.registrationNumber})\n${s.deleteQuestion}") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val res = ApiClient.service.deleteVehicle(v.id)
                            deleteTarget = null
                            load()
                            snackState.showSnackbar(if (res.success) s.vehicleDeleted else res.message ?: s.error)
                        } catch (e: HttpException) {
                            deleteTarget = null
                            val msg = e.response()?.errorBody()?.string() ?: s.error
                            snackState.showSnackbar("HTTP ${e.code()}: $msg")
                        } catch (e: Exception) {
                            deleteTarget = null
                            snackState.showSnackbar("${e.javaClass.simpleName}: ${e.message}")
                        }
                    }
                }) { Text(s.yes, color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(s.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleFormSheet(
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    existing: Vehicle?,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var reg by remember { mutableStateOf(existing?.registrationNumber ?: "") }
    var brand by remember { mutableStateOf(existing?.brand ?: "") }
    var model by remember { mutableStateOf(existing?.model ?: "") }
    var year by remember { mutableStateOf(existing?.year?.toString() ?: "") }
    var vehicleType by remember { mutableStateOf(existing?.vehicleType ?: "sedan") }
    var color by remember { mutableStateOf(existing?.color ?: "") }
    var fuelType by remember { mutableStateOf("petrol") }
    var seats by remember { mutableStateOf(existing?.seatingCapacity?.toString() ?: "5") }
    var price by remember { mutableStateOf(existing?.dailyRentPrice?.let { "%.0f".format(it) } ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: "available") }
    var submitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }

    val vehicleTypes = listOf("sedan", "suv", "van", "truck", "premium")
    val fuelTypes = listOf("petrol" to s.fuelPetrol, "diesel" to s.fuelDiesel, "hybrid" to s.fuelHybrid, "electric" to s.fuelElectric)
    val statuses = listOf("available" to s.statusAvailable, "maintenance" to s.statusMaintenance, "inactive" to s.statusInactive)

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        LazyColumn(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(if (existing == null) s.addVehicle else s.editVehicle,
                    fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            if (formError.isNotEmpty()) {
                item { Text(formError, color = Color(0xFFDC2626), fontSize = 13.sp) }
            }
            if (existing == null) {
                item {
                    OutlinedTextField(reg, { reg = it }, label = { Text(s.registrationNumberLabel) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(brand, { brand = it }, label = { Text("ব্র্যান্ড") },
                        modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(model, { model = it }, label = { Text("মডেল") },
                        modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(year, { year = it }, label = { Text(s.yearLabel) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(color, { color = it }, label = { Text(s.colorLabel) },
                        modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
                }
            }
            item {
                Text(s.vehicleTypeLabel, fontSize = 13.sp, color = InkMuted)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    vehicleTypes.forEach { t ->
                        FilterChip(t == vehicleType, { vehicleType = t }, label = { Text(t, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White))
                    }
                }
            }
            item {
                Text(s.fuelTypeLabel, fontSize = 13.sp, color = InkMuted)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fuelTypes.forEach { (key, label) ->
                        FilterChip(fuelType == key, { fuelType = key }, label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(seats, { seats = it }, label = { Text(s.seatsLabel) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(price, { price = it }, label = { Text(s.priceLabel) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp))
                }
            }
            item {
                Text(s.driverStatus, fontSize = 13.sp, color = InkMuted)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { (key, label) ->
                        FilterChip(status == key, { status = key }, label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White))
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        val p = price.toDoubleOrNull()
                        val y = year.toIntOrNull()
                        val sc = seats.toIntOrNull() ?: 5
                        if (brand.isBlank() || model.isBlank() || p == null) { formError = "ব্র্যান্ড, মডেল ও ভাড়া আবশ্যক"; return@Button }
                        if (existing == null && reg.isBlank()) { formError = s.registrationNumberLabel + " আবশ্যক"; return@Button }
                        submitting = true; formError = ""
                        scope.launch {
                            try {
                                if (existing == null) {
                                    val res = ApiClient.service.createVehicle(
                                        CreateVehicleRequest(reg.trim(), brand.trim(), model.trim(),
                                            y ?: 2020, vehicleType, color.ifBlank { null }, fuelType, sc, p, status)
                                    )
                                    onSaved(if (res.success) s.vehicleAdded else res.message ?: s.error)
                                } else {
                                    val res = ApiClient.service.updateVehicle(
                                        existing.id,
                                        UpdateVehicleRequest(brand.trim(), model.trim(), y, vehicleType,
                                            color.ifBlank { null }, fuelType, sc, p, status)
                                    )
                                    onSaved(if (res.success) s.vehicleUpdated else res.message ?: s.error)
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

@Composable
private fun VehicleCard(
    v: Vehicle,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusColor, statusLabel) = when (v.status) {
        "available"   -> Color(0xFF10B981) to s.statusAvailable
        "rented"      -> StatusActive      to s.statusRented
        "maintenance" -> Color(0xFFF59E0B) to s.statusMaintenance
        else          -> InkMuted          to s.statusInactive
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${v.brand} ${v.model}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                    Text(v.registrationNumber, fontSize = 12.sp, color = InkMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.15f)) {
                        Text(statusLabel, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                v.color?.let { Text("🎨 $it", fontSize = 12.sp, color = InkMuted) }
                v.seatingCapacity?.let { Text("💺 $it ${s.seatsLabel}", fontSize = 12.sp, color = InkMuted) }
                v.year?.let { Text("📅 $it", fontSize = 12.sp, color = InkMuted) }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("${s.taka}${String.format("%.0f", v.dailyRentPrice)}/${s.dailyRate}",
                    fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp), tint = Primary)
                        Spacer(Modifier.width(4.dp))
                        Text(s.editVehicle.take(4), fontSize = 12.sp, color = Primary)
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy()) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                        Spacer(Modifier.width(4.dp))
                        Text(s.deleteVehicle.take(3), fontSize = 12.sp, color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}
