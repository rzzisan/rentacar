package com.rzzisan.carrental.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMaintenanceScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var snackMsg by remember { mutableStateOf("") }
    val snackHost = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        if (snackMsg.isNotEmpty()) { snackHost.showSnackbar(snackMsg); snackMsg = "" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.maintenanceTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackHost) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text(s.tabServiceRecords, fontSize = 13.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text(s.tabDocuments, fontSize = 13.sp) })
            }
            when (selectedTab) {
                0 -> ServiceRecordsTab(s, scope) { snackMsg = it }
                1 -> DocumentsTab(s, scope) { snackMsg = it }
            }
        }
    }
}

// ── সার্ভিস রেকর্ড ট্যাব ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceRecordsTab(
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    scope: kotlinx.coroutines.CoroutineScope,
    onMsg: (String) -> Unit
) {
    var records by remember { mutableStateOf<List<MaintenanceRecord>>(emptyList()) }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<MaintenanceRecord?>(null) }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val mRes = ApiClient.service.getAdminMaintenance(status = filterStatus.ifEmpty { null })
                val vRes = ApiClient.service.getAdminVehicles()
                if (mRes.success) records = mRes.data ?: emptyList() else error = mRes.message ?: s.error
                if (vRes.success) vehicles = vRes.data ?: emptyList()
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }
    LaunchedEffect(filterStatus) { load() }

    if (showForm || editTarget != null) {
        MaintenanceFormSheet(
            s = s, vehicles = vehicles, existing = editTarget,
            onDismiss = { showForm = false; editTarget = null },
            onSave = { req, id ->
                scope.launch {
                    try {
                        val res = if (id == null)
                            ApiClient.service.createMaintenance(req).also { if (it.success) onMsg(s.maintAdded) }
                        else
                            ApiClient.service.updateMaintenance(id, req.toUpdate()).also { if (it.success) onMsg(s.maintUpdated) }
                        if (res.success) { showForm = false; editTarget = null; load() }
                        else onMsg(res.message ?: s.error)
                    } catch (e: Exception) { onMsg(e.message ?: s.error) }
                }
            }
        )
    }

    deleteTarget?.let { rec ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(s.deleteQuestion) },
            text = { Text("${rec.vehicle} — ${maintTypeLabel(rec.maintenanceType, s)}") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            val res = ApiClient.service.deleteMaintenance(rec.id)
                            if (res.success) { onMsg(s.maintDeleted); load() } else onMsg(res.message ?: s.error)
                        } catch (e: Exception) { onMsg(e.message ?: s.error) }
                        deleteTarget = null
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = StatusDue)) { Text(s.confirm) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(s.cancel) } }
        )
    }

    Column {
        // Filter chips + add button
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                val statuses = listOf("" to s.allStatus, "pending" to s.msPending,
                    "in_progress" to s.msInProgress, "completed" to s.msCompleted)
                items(statuses) { (key, label) ->
                    FilterChip(filterStatus == key, { filterStatus = key }, label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary, selectedLabelColor = Color.White))
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { showForm = true },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary),
                modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp)) }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(error, color = StatusDue, fontSize = 13.sp)
            }
            records.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(s.noMaintenance, color = InkMuted, fontSize = 13.sp)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records, key = { it.id }) { rec ->
                    MaintenanceCard(rec, s,
                        onEdit = { editTarget = rec },
                        onDelete = { deleteTarget = rec })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun MaintenanceCard(
    rec: MaintenanceRecord,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (rec.status) {
        "completed" -> StatusPaid
        "in_progress" -> Primary
        "cancelled" -> InkMuted
        else -> StatusPending
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("${rec.vehicle}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(rec.registrationNumber, fontSize = 11.sp, color = InkMuted)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(maintStatusLabel(rec.status, s), fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = statusColor, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(maintTypeLabel(rec.maintenanceType, s), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            rec.description?.let { Text(it, fontSize = 12.sp, color = InkMuted) }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MInfoChip("📅 ${rec.startDate.take(10)}")
                rec.cost?.let { MInfoChip("৳${String.format("%.0f", it)}") }
                rec.vendor?.let { if (it.isNotEmpty()) MInfoChip("🔧 $it") }
            }
            if (rec.nextDueDate != null || rec.nextDueKm != null) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rec.nextDueDate?.let { Text("${s.nextDueDateLabel}: $it", fontSize = 11.sp, color = StatusPending) }
                    rec.nextDueKm?.let { Text("${s.nextDueKmLabel}: $it km", fontSize = 11.sp, color = StatusPending) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Filled.Edit, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.save, fontSize = 12.sp)
                }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(14.dp), tint = StatusDue)
                    Spacer(Modifier.width(4.dp))
                    Text(s.deleteQuestion.take(4), fontSize = 12.sp, color = StatusDue)
                }
            }
        }
    }
}

// ── ডকুমেন্ট ট্যাব ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentsTab(
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    scope: kotlinx.coroutines.CoroutineScope,
    onMsg: (String) -> Unit
) {
    var docs by remember { mutableStateOf<List<VehicleDocument>>(emptyList()) }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<VehicleDocument?>(null) }
    var deleteTarget by remember { mutableStateOf<VehicleDocument?>(null) }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val dRes = ApiClient.service.getAdminDocuments()
                val vRes = ApiClient.service.getAdminVehicles()
                if (dRes.success) docs = dRes.data ?: emptyList() else error = dRes.message ?: s.error
                if (vRes.success) vehicles = vRes.data ?: emptyList()
            } catch (e: Exception) { error = "${e.javaClass.simpleName}: ${e.message}" }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    if (showForm || editTarget != null) {
        DocumentFormSheet(
            s = s, vehicles = vehicles, existing = editTarget,
            onDismiss = { showForm = false; editTarget = null },
            onSave = { req ->
                scope.launch {
                    try {
                        val res = ApiClient.service.saveDocument(req)
                        if (res.success) { onMsg(s.docSaved); showForm = false; editTarget = null; load() }
                        else onMsg(res.message ?: s.error)
                    } catch (e: Exception) { onMsg(e.message ?: s.error) }
                }
            }
        )
    }

    deleteTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(s.deleteQuestion) },
            text = { Text("${doc.vehicle} — ${docTypeLabel(doc.docType, s)}") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            val res = ApiClient.service.deleteDocument(doc.id)
                            if (res.success) { onMsg(s.docDeleted); load() } else onMsg(res.message ?: s.error)
                        } catch (e: Exception) { onMsg(e.message ?: s.error) }
                        deleteTarget = null
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = StatusDue)) { Text(s.confirm) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(s.cancel) } }
        )
    }

    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            Arrangement.End
        ) {
            FilledIconButton(onClick = { showForm = true },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary),
                modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp)) }
        }

        // Expiry alert banner
        val expiring = docs.filter { it.daysRemaining in 0..30 }
        if (expiring.isNotEmpty()) {
            Surface(
                color = Color(0xFFFFF3E0),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text("⚠️ ${s.expiringAlert} (${expiring.size})", fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100))
                    expiring.take(3).forEach { d ->
                        Text("• ${d.vehicle} — ${docTypeLabel(d.docType, s)}: " +
                            if (d.daysRemaining < 0) s.expiredLabel else "${d.daysRemaining}d",
                            fontSize = 11.sp, color = Color(0xFFBF360C))
                    }
                }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(error, color = StatusDue, fontSize = 13.sp)
            }
            docs.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(s.noDocuments, color = InkMuted, fontSize = 13.sp)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(docs, key = { it.id }) { doc ->
                    DocumentCard(doc, s, onEdit = { editTarget = doc }, onDelete = { deleteTarget = doc })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DocumentCard(
    doc: VehicleDocument,
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val expiryColor = when {
        doc.daysRemaining < 0   -> StatusDue
        doc.daysRemaining <= 15 -> Color(0xFFEA580C)
        doc.daysRemaining <= 30 -> StatusPending
        doc.daysRemaining <= 60 -> Color(0xFFCA8A04)
        else                    -> StatusPaid
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(4.dp).height(IntrinsicSize.Max)
                .background(expiryColor, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)))
            Column(Modifier.weight(1f).padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(doc.vehicle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(doc.registrationNumber, fontSize = 11.sp, color = InkMuted)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = expiryColor.copy(alpha = 0.12f)) {
                        Text(
                            if (doc.daysRemaining < 0) s.expiredLabel else "${doc.daysRemaining}d",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = expiryColor, fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(docTypeLabel(doc.docType, s), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                doc.docNumber?.let { if (it.isNotEmpty()) Text(it, fontSize = 12.sp, color = InkMuted) }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MInfoChip("📋 ${doc.issueDate.take(10)}")
                    MInfoChip("⏰ ${doc.expiryDate.take(10)}")
                }
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Filled.Edit, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.save, fontSize = 12.sp)
                    }
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(14.dp), tint = StatusDue)
                        Spacer(Modifier.width(4.dp))
                        Text(s.deleteQuestion.take(4), fontSize = 12.sp, color = StatusDue)
                    }
                }
            }
        }
    }
}

// ── Maintenance Form Sheet ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceFormSheet(
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    vehicles: List<Vehicle>,
    existing: MaintenanceRecord?,
    onDismiss: () -> Unit,
    onSave: (CreateMaintenanceRequest, Int?) -> Unit
) {
    val maintTypes = listOf("oil_change","tire","brake","engine","battery","ac","body_repair","electrical","inspection","other")
    val statuses   = listOf("pending","in_progress","completed","cancelled")

    var vehicleId by remember { mutableIntStateOf(existing?.vehicleId ?: (vehicles.firstOrNull()?.id ?: 0)) }
    var type      by remember { mutableStateOf(existing?.maintenanceType ?: "oil_change") }
    var desc      by remember { mutableStateOf(existing?.description ?: "") }
    var vendor    by remember { mutableStateOf(existing?.vendor ?: "") }
    var startDate by remember { mutableStateOf(existing?.startDate ?: "") }
    var endDate   by remember { mutableStateOf(existing?.endDate ?: "") }
    var km        by remember { mutableStateOf(existing?.kmReading?.toString() ?: "") }
    var nextDate  by remember { mutableStateOf(existing?.nextDueDate ?: "") }
    var nextKm    by remember { mutableStateOf(existing?.nextDueKm?.toString() ?: "") }
    var cost      by remember { mutableStateOf(existing?.cost?.toString() ?: "") }
    var status    by remember { mutableStateOf(existing?.status ?: "pending") }
    var submitting by remember { mutableStateOf(false) }
    var vMenuOpen  by remember { mutableStateOf(false) }
    var tMenuOpen  by remember { mutableStateOf(false) }
    var sMenuOpen  by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(if (existing == null) s.addMaintenance else s.editMaintenance,
                    fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }

            // গাড়ি নির্বাচন
            item {
                val selectedVehicle = vehicles.find { it.id == vehicleId }
                ExposedDropdownMenuBox(expanded = vMenuOpen, onExpandedChange = { vMenuOpen = it }) {
                    OutlinedTextField(
                        value = selectedVehicle?.let { "${it.brand} ${it.model} (${it.registrationNumber})" } ?: s.selectVehicle,
                        onValueChange = {},
                        label = { Text(s.vehicle) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vMenuOpen) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = vMenuOpen, onDismissRequest = { vMenuOpen = false }) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.brand} ${v.model} (${v.registrationNumber})", fontSize = 13.sp) },
                                onClick = { vehicleId = v.id; vMenuOpen = false }
                            )
                        }
                    }
                }
            }

            // ধরন
            item {
                ExposedDropdownMenuBox(expanded = tMenuOpen, onExpandedChange = { tMenuOpen = it }) {
                    OutlinedTextField(
                        value = maintTypeLabel(type, s), onValueChange = {},
                        label = { Text(s.maintTypeLabel) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tMenuOpen) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = tMenuOpen, onDismissRequest = { tMenuOpen = false }) {
                        maintTypes.forEach { t ->
                            DropdownMenuItem(text = { Text(maintTypeLabel(t, s)) },
                                onClick = { type = t; tMenuOpen = false })
                        }
                    }
                }
            }

            item {
                OutlinedTextField(desc, { desc = it }, label = { Text(s.description) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), maxLines = 2)
            }
            item {
                OutlinedTextField(vendor, { vendor = it }, label = { Text(s.vendorLabel) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(startDate, { startDate = it }, label = { Text(s.startTime) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true)
                    OutlinedTextField(endDate, { endDate = it }, label = { Text(s.endTime) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(km, { km = it }, label = { Text(s.kmReadingLabel) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(cost, { cost = it }, label = { Text("${s.summaryLabel} (৳)") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(nextDate, { nextDate = it }, label = { Text(s.nextDueDateLabel) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true)
                    OutlinedTextField(nextKm, { nextKm = it }, label = { Text(s.nextDueKmLabel) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }

            // স্ট্যাটাস
            item {
                ExposedDropdownMenuBox(expanded = sMenuOpen, onExpandedChange = { sMenuOpen = it }) {
                    OutlinedTextField(
                        value = maintStatusLabel(status, s), onValueChange = {},
                        label = { Text(s.maintStatusLabel) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sMenuOpen) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = sMenuOpen, onDismissRequest = { sMenuOpen = false }) {
                        statuses.forEach { st ->
                            DropdownMenuItem(text = { Text(maintStatusLabel(st, s)) },
                                onClick = { status = st; sMenuOpen = false })
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (vehicleId == 0 || startDate.isEmpty()) return@Button
                        submitting = true
                        val req = CreateMaintenanceRequest(
                            vehicleId = vehicleId, maintenanceType = type,
                            description = desc.ifEmpty { null }, vendor = vendor.ifEmpty { null },
                            startDate = startDate, endDate = endDate.ifEmpty { null },
                            kmReading = km.toIntOrNull(), nextDueDate = nextDate.ifEmpty { null },
                            nextDueKm = nextKm.toIntOrNull(), cost = cost.toDoubleOrNull(),
                            status = status
                        )
                        onSave(req, existing?.id)
                    },
                    enabled = !submitting && vehicleId != 0 && startDate.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text(if (submitting) s.loading else s.save, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ── Document Form Sheet ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentFormSheet(
    s: com.rzzisan.carrental.ui.strings.AppStrings,
    vehicles: List<Vehicle>,
    existing: VehicleDocument?,
    onDismiss: () -> Unit,
    onSave: (CreateDocumentRequest) -> Unit
) {
    val docTypes = listOf("fitness","insurance","tax_token","route_permit","registration","other")

    var vehicleId  by remember { mutableIntStateOf(existing?.vehicleId ?: (vehicles.firstOrNull()?.id ?: 0)) }
    var docType    by remember { mutableStateOf(existing?.docType ?: "fitness") }
    var docNumber  by remember { mutableStateOf(existing?.docNumber ?: "") }
    var issueDate  by remember { mutableStateOf(existing?.issueDate ?: "") }
    var expiryDate by remember { mutableStateOf(existing?.expiryDate ?: "") }
    var note       by remember { mutableStateOf(existing?.note ?: "") }
    var submitting  by remember { mutableStateOf(false) }
    var vMenuOpen   by remember { mutableStateOf(false) }
    var dtMenuOpen  by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (existing == null) s.addDocument else s.editDocument,
                fontWeight = FontWeight.Bold, fontSize = 17.sp)

            // গাড়ি নির্বাচন
            ExposedDropdownMenuBox(expanded = vMenuOpen, onExpandedChange = { vMenuOpen = it }) {
                val selV = vehicles.find { it.id == vehicleId }
                OutlinedTextField(
                    value = selV?.let { "${it.brand} ${it.model} (${it.registrationNumber})" } ?: s.selectVehicle,
                    onValueChange = {}, label = { Text(s.vehicle) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vMenuOpen) },
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(expanded = vMenuOpen, onDismissRequest = { vMenuOpen = false }) {
                    vehicles.forEach { v ->
                        DropdownMenuItem(
                            text = { Text("${v.brand} ${v.model} (${v.registrationNumber})", fontSize = 13.sp) },
                            onClick = { vehicleId = v.id; vMenuOpen = false }
                        )
                    }
                }
            }

            // ডকুমেন্ট ধরন
            ExposedDropdownMenuBox(expanded = dtMenuOpen, onExpandedChange = { dtMenuOpen = it }) {
                OutlinedTextField(
                    value = docTypeLabel(docType, s), onValueChange = {},
                    label = { Text(s.docTypeLabel) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dtMenuOpen) },
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(expanded = dtMenuOpen, onDismissRequest = { dtMenuOpen = false }) {
                    docTypes.forEach { dt ->
                        DropdownMenuItem(text = { Text(docTypeLabel(dt, s)) },
                            onClick = { docType = dt; dtMenuOpen = false })
                    }
                }
            }

            OutlinedTextField(docNumber, { docNumber = it }, label = { Text(s.docNumberLabel) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(issueDate, { issueDate = it }, label = { Text(s.issueDateLabel) },
                    placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true)
                OutlinedTextField(expiryDate, { expiryDate = it }, label = { Text(s.expiryDateLabel) },
                    placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true)
            }

            OutlinedTextField(note, { note = it }, label = { Text(s.paymentNotes) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), maxLines = 2)

            Button(
                onClick = {
                    if (vehicleId == 0 || issueDate.isEmpty() || expiryDate.isEmpty()) return@Button
                    submitting = true
                    onSave(CreateDocumentRequest(
                        vehicleId = vehicleId, docType = docType,
                        docNumber = docNumber.ifEmpty { null }, issueDate = issueDate,
                        expiryDate = expiryDate, note = note.ifEmpty { null }
                    ))
                },
                enabled = !submitting && vehicleId != 0 && issueDate.isNotEmpty() && expiryDate.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text(if (submitting) s.loading else s.save, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// ── Extension helpers ─────────────────────────────────────────────

@Composable
private fun MInfoChip(text: String) {
    Surface(shape = RoundedCornerShape(4.dp), color = SurfaceVariant) {
        Text(text, fontSize = 11.sp, color = InkMuted,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

private fun maintTypeLabel(type: String, s: com.rzzisan.carrental.ui.strings.AppStrings) = when (type) {
    "oil_change"  -> s.mtOilChange
    "tire"        -> s.mtTire
    "brake"       -> s.mtBrake
    "engine"      -> s.mtEngine
    "battery"     -> s.mtBattery
    "ac"          -> s.mtAc
    "body_repair" -> s.mtBodyRepair
    "electrical"  -> s.mtElectrical
    "inspection"  -> s.mtInspection
    else          -> s.mtOther
}

private fun maintStatusLabel(status: String, s: com.rzzisan.carrental.ui.strings.AppStrings) = when (status) {
    "completed"   -> s.msCompleted
    "in_progress" -> s.msInProgress
    "cancelled"   -> s.msCancelled
    else          -> s.msPending
}

private fun docTypeLabel(type: String, s: com.rzzisan.carrental.ui.strings.AppStrings) = when (type) {
    "fitness"      -> s.dtFitness
    "insurance"    -> s.dtInsurance
    "tax_token"    -> s.dtTaxToken
    "route_permit" -> s.dtRoutePermit
    "registration" -> s.dtRegistration
    else           -> s.dtOther
}

private fun CreateMaintenanceRequest.toUpdate() = UpdateMaintenanceRequest(
    maintenanceType = maintenanceType, description = description, vendor = vendor,
    startDate = startDate, endDate = endDate, kmReading = kmReading,
    nextDueDate = nextDueDate, nextDueKm = nextDueKm, cost = cost, status = status
)
