package com.rzzisan.carrental.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.*
import com.rzzisan.carrental.ui.LocalAdminDrawerState
import com.rzzisan.carrental.util.errorMessageOf
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagersScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val drawerState = LocalAdminDrawerState.current
    var managers by remember { mutableStateOf<List<AdminManager>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<AdminManager?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminManager?>(null) }
    val snackState = remember { SnackbarHostState() }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getAdminManagers()
                if (res.success) managers = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = errorMessageOf(e, s.serverError) }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.managersTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState?.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editTarget = null; showForm = true }, containerColor = Primary) {
                Icon(Icons.Filled.Add, s.addManager, tint = Color.White)
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
            managers.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text(s.noManagers, color = InkMuted) }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(managers) { manager ->
                    ManagerCard(manager, s,
                        onEdit = { editTarget = manager; showForm = true },
                        onDelete = { deleteTarget = manager })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    // Add/Edit form
    if (showForm) {
        ManagerFormSheet(s = s, existing = editTarget, onDismiss = { showForm = false },
            onSaved = { msg ->
                showForm = false; load()
                scope.launch { snackState.showSnackbar(msg) }
            })
    }

    // Delete confirm
    deleteTarget?.let { m ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(s.deleteManager, fontWeight = FontWeight.SemiBold) },
            text = { Text("${m.name}\n${s.deleteQuestion}") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val res = ApiClient.service.deleteManager(m.id)
                            deleteTarget = null; load()
                            snackState.showSnackbar(if (res.success) s.managerDeleted else res.message ?: s.error)
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
}

// ── Manager Form Sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerFormSheet(
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    existing: AdminManager?,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var mobile by remember { mutableStateOf(existing?.mobile ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var managerStatus by remember { mutableStateOf(existing?.status ?: "active") }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleExpanded by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }

    // Pre-select existing assigned vehicle
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val res = ApiClient.service.getAdminVehicles()
                if (res.success) {
                    vehicles = res.data ?: emptyList()
                    // Pre-select if editing
                    existing?.vehicles?.firstOrNull()?.let { mv ->
                        selectedVehicle = vehicles.find { it.id == mv.id }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        LazyColumn(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(if (existing == null) s.addManager else s.editManager, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
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
                    label = { Text(if (existing == null) s.managerPassword else "${s.managerPassword} (খালি রাখলে পরিবর্তন হবে না)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(10.dp))
            }
            // Status
            item {
                Text(s.driverStatus, fontSize = 13.sp, color = InkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("active" to s.activeLabel, "inactive" to s.inactiveLabel).forEach { (k, lbl) ->
                        FilterChip(managerStatus == k, { managerStatus = k }, label = { Text(lbl, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = Color.White))
                    }
                }
            }
            // Vehicle assignment (single)
            item {
                ExposedDropdownMenuBox(expanded = vehicleExpanded, onExpandedChange = { vehicleExpanded = it }) {
                    OutlinedTextField(
                        value = selectedVehicle?.let { "${it.brand} ${it.model} (${it.registrationNumber})" } ?: s.noVehicleAssigned,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.assignedVehicleLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vehicleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = vehicleExpanded, onDismissRequest = { vehicleExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(s.noVehicleAssigned, fontSize = 13.sp) },
                            onClick = { selectedVehicle = null; vehicleExpanded = false }
                        )
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.brand} ${v.model} — ${v.registrationNumber}", fontSize = 13.sp) },
                                onClick = { selectedVehicle = v; vehicleExpanded = false }
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (name.isBlank() || mobile.isBlank() || email.isBlank()) { formError = "নাম, মোবাইল ও ইমেইল আবশ্যক"; return@Button }
                        if (existing == null && password.length < 6) { formError = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষর"; return@Button }
                        val vehicleIdStr = selectedVehicle?.id?.toString() ?: "0"
                        submitting = true; formError = ""
                        scope.launch {
                            try {
                                if (existing == null) {
                                    val res = ApiClient.service.createManager(
                                        name.trim(), mobile.trim(), email.trim(), password, managerStatus, vehicleIdStr
                                    )
                                    onSaved(if (res.success) s.managerAdded else res.message ?: s.error)
                                } else {
                                    val res = ApiClient.service.updateManager(
                                        existing.id, name.trim(), mobile.trim(), email.trim(), managerStatus, vehicleIdStr
                                    )
                                    onSaved(if (res.success) s.managerUpdated else res.message ?: s.error)
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

// ── Manager Card ───────────────────────────────────────────────────

@Composable
private fun ManagerCard(
    manager: AdminManager,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = manager.status == "active"
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
                            Icon(Icons.Filled.SupervisorAccount, null,
                                tint = if (isActive) Primary else InkMuted,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                    Column {
                        Text(manager.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                        manager.mobile?.let { Text(it, fontSize = 12.sp, color = InkMuted) }
                    }
                }
                Surface(shape = RoundedCornerShape(6.dp),
                    color = if (isActive) StatusActive.copy(alpha = 0.1f) else InkMuted.copy(alpha = 0.1f)) {
                    Text(if (isActive) s.activeLabel else s.inactiveLabel, fontSize = 11.sp,
                        color = if (isActive) StatusActive else InkMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            manager.email?.let { Text(it, fontSize = 12.sp, color = InkMuted) }
            // Assigned vehicle
            val assignedV = manager.vehicles.firstOrNull()
            Surface(shape = RoundedCornerShape(6.dp),
                color = if (assignedV != null) Primary.copy(alpha = 0.08f) else Color(0xFFF3F4F6)) {
                Text(
                    if (assignedV != null) "🚗 ${assignedV.brand} ${assignedV.model} (${assignedV.registrationNumber})"
                    else s.noVehicleAssigned,
                    fontSize = 12.sp,
                    color = if (assignedV != null) Primary else InkMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp), tint = Primary)
                    Spacer(Modifier.width(4.dp))
                    Text(s.editManager.take(6), fontSize = 12.sp, color = Primary)
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                    Spacer(Modifier.width(4.dp))
                    Text(s.deleteManager.take(4), fontSize = 12.sp, color = Color(0xFFEF4444))
                }
            }
        }
    }
}
