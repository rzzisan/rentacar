package com.rzzisan.carrental.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.rzzisan.carrental.ui.LocalAdminDrawerState
import com.rzzisan.carrental.ui.strings.AppStrings
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.util.errorMessageOf
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch

// isAdmin=true  → full CRUD (admin)
// isAdmin=false → read-only (manager)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomersScreen(isAdmin: Boolean = true) {
    val s     = LocalStrings.current
    val scope = rememberCoroutineScope()
    val drawerState = LocalAdminDrawerState.current

    var customers   by remember { mutableStateOf<List<CustomerListItem>>(emptyList()) }
    var loading     by remember { mutableStateOf(true) }
    var error       by remember { mutableStateOf("") }
    var search      by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("") }

    // modals
    var showCreate  by remember { mutableStateOf(false) }
    var editTarget  by remember { mutableStateOf<CustomerListItem?>(null) }
    var detailTarget by remember { mutableStateOf<CustomerListItem?>(null) }
    var detail      by remember { mutableStateOf<CustomerDetailData?>(null) }
    var detailLoading by remember { mutableStateOf(false) }

    val snackState = remember { SnackbarHostState() }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val res = if (isAdmin)
                    ApiClient.service.getAdminCustomers(search.ifBlank { null }, filterStatus.ifBlank { null })
                else
                    ApiClient.service.getManagerCustomers(search.ifBlank { null }, filterStatus.ifBlank { null })
                if (res.success) customers = res.data ?: emptyList()
                else error = res.message ?: s.error
            } catch (e: Exception) { error = errorMessageOf(e, s.error) }
            finally { loading = false }
        }
    }

    LaunchedEffect(search, filterStatus) { load() }

    fun openDetail(c: CustomerListItem) {
        detailTarget = c; detail = null; detailLoading = true
        scope.launch {
            try {
                val res = if (isAdmin)
                    ApiClient.service.getAdminCustomerDetail(c.id)
                else
                    ApiClient.service.getManagerCustomerDetail(c.id)
                if (res.success) detail = res.data
            } catch (_: Exception) {}
            finally { detailLoading = false }
        }
    }

    fun toggleStatus(c: CustomerListItem) {
        val next = if (c.status == "active") "inactive" else "active"
        scope.launch {
            try {
                val res = ApiClient.service.updateCustomerStatus(c.id, CustomerStatusRequest(next))
                val msg = if (res.success) {
                    load()
                    if (next == "active") s.customerActivated else s.customerDeactivated
                } else res.message ?: s.error
                snackState.showSnackbar(msg)
            } catch (e: Exception) { snackState.showSnackbar(errorMessageOf(e, s.error)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.customersTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState?.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = Primary
                ) { Icon(Icons.Filled.Add, s.addCustomer, tint = Color.White) }
            }
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        Column(Modifier.padding(padding)) {

            // ── Search + Filter ──────────────────────────────────────────
            Column(
                Modifier
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("নাম, মোবাইল বা NID...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("" to s.allStatus, "active" to s.activeLabel, "inactive" to s.inactiveLabel)
                        .forEach { (k, lbl) ->
                            FilterChip(
                                selected = filterStatus == k,
                                onClick = { filterStatus = k },
                                label = { Text(lbl, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                }
            }

            HorizontalDivider()

            // ── Body ─────────────────────────────────────────────────────
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                error.isNotEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                        Button(onClick = ::load, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text(s.retry)
                        }
                    }
                }
                customers.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.People, null, Modifier.size(48.dp), tint = InkMuted.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text(s.noCustomers, color = InkMuted, fontSize = 14.sp)
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customers) { c ->
                        CustomerCard(
                            customer = c, s = s, isAdmin = isAdmin,
                            onDetail = { openDetail(c) },
                            onEdit = { editTarget = c },
                            onToggleStatus = { toggleStatus(c) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Detail Sheet ───────────────────────────────────────────────────────
    detailTarget?.let { c ->
        ModalBottomSheet(onDismissRequest = { detailTarget = null; detail = null }) {
            if (detailLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (detail != null) {
                CustomerDetailSheet(
                    data = detail!!, s = s, isAdmin = isAdmin,
                    onEdit = {
                        detailTarget = null
                        editTarget = detail!!.customer
                        detail = null
                    },
                    onToggleStatus = {
                        val c2 = detail!!.customer
                        detailTarget = null; detail = null
                        toggleStatus(c2)
                    }
                )
            }
        }
    }

    // ── Create Sheet (admin only) ───────────────────────────────────────────
    if (showCreate) {
        CustomerFormSheet(
            s = s, existing = null,
            onDismiss = { showCreate = false },
            onSaved = { msg -> showCreate = false; load(); scope.launch { snackState.showSnackbar(msg) } }
        )
    }

    // ── Edit Sheet (admin only) ─────────────────────────────────────────────
    editTarget?.let { c ->
        CustomerFormSheet(
            s = s, existing = c,
            onDismiss = { editTarget = null },
            onSaved = { msg -> editTarget = null; load(); scope.launch { snackState.showSnackbar(msg) } }
        )
    }
}

// ── Customer Card ─────────────────────────────────────────────────────────────

@Composable
private fun CustomerCard(
    customer: CustomerListItem,
    s: AppStrings,
    isAdmin: Boolean,
    onDetail: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isActive = customer.status == "active"
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onDetail),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Header: Avatar + Name + Status badge
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isActive) Primary.copy(alpha = 0.1f) else InkMuted.copy(alpha = 0.1f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Icon(Icons.Filled.Person, null,
                                tint = if (isActive) Primary else InkMuted,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                    Column {
                        Text(
                            "${customer.firstName} ${customer.lastName ?: ""}".trim(),
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink
                        )
                        Text(customer.phone, fontSize = 12.sp, color = InkMuted)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isActive) StatusActive.copy(alpha = 0.12f) else InkMuted.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (isActive) s.activeLabel else s.inactiveLabel,
                        fontSize = 11.sp,
                        color = if (isActive) StatusActive else InkMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Stats row
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
                StatChip("${customer.totalTrips} ${s.trips}", Primary)
                if (customer.totalSpent > 0)
                    StatChip("৳${"%,.0f".format(customer.totalSpent)}", Color(0xFF059669))
                customer.lastTripDate?.let {
                    StatChip(it.take(10), InkMuted)
                }
            }

            // NID if available
            customer.nid?.let {
                Text("${s.nidLabel}: $it", fontSize = 11.sp, color = InkMuted)
            }

            // Action buttons
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onDetail,
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Filled.Visibility, null, Modifier.size(13.dp), tint = Primary)
                    Spacer(Modifier.width(4.dp))
                    Text(s.customerDetail, fontSize = 11.sp, color = Primary)
                }
                if (isAdmin) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Edit, null, Modifier.size(13.dp), tint = Primary)
                        Spacer(Modifier.width(4.dp))
                        Text(s.editCustomer.take(6), fontSize = 11.sp, color = Primary)
                    }
                    Button(
                        onClick = onToggleStatus,
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) Color(0xFFEF4444) else StatusActive
                        )
                    ) {
                        Text(
                            if (isActive) s.deactivate else s.activate,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.1f)) {
        Text(text, fontSize = 11.sp, color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Medium)
    }
}

// ── Customer Detail Sheet ─────────────────────────────────────────────────────

@Composable
private fun CustomerDetailSheet(
    data: CustomerDetailData,
    s: AppStrings,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val c = data.customer
    val isActive = c.status == "active"

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(
                        "${c.firstName} ${c.lastName ?: ""}".trim(),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp
                    )
                    Text(c.phone, fontSize = 13.sp, color = InkMuted)
                }
                Surface(shape = RoundedCornerShape(8.dp),
                    color = if (isActive) StatusActive.copy(0.12f) else InkMuted.copy(0.12f)) {
                    Text(
                        if (isActive) s.activeLabel else s.inactiveLabel,
                        fontSize = 12.sp,
                        color = if (isActive) StatusActive else InkMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Stats cards
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                MiniStatCard(s.totalTrips, "${data.stats.totalTrips}", Primary, Modifier.weight(1f))
                MiniStatCard(s.completedTrips, "${data.stats.completedTrips}", StatusActive, Modifier.weight(1f))
                MiniStatCard(s.totalSpent, "৳${"%,.0f".format(data.stats.totalSpent)}", Color(0xFF7C3AED), Modifier.weight(1f))
            }
        }

        // Profile info
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    c.email?.let { InfoRow(s.email, it) }
                    c.nid?.let { InfoRow(s.nidLabel, it) }
                    c.licenseNumber?.let { InfoRow(s.licenseLabel, it) }
                    c.licenseExpiry?.let { InfoRow(s.licenseExpiry, it) }
                    c.city?.let { InfoRow(s.cityLabel, it) }
                    c.address?.let { InfoRow(s.addressLabel, it) }
                    if (data.stats.pendingPaymentTrips > 0) {
                        InfoRow(s.pendingPayments, "${data.stats.pendingPaymentTrips}টি", Color(0xFFDC2626))
                    }
                }
            }
        }

        // Admin actions
        if (isAdmin) {
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(44.dp)) {
                        Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.editCustomer)
                    }
                    Button(
                        onClick = onToggleStatus,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) Color(0xFFEF4444) else StatusActive
                        )
                    ) {
                        Text(if (isActive) s.deactivate else s.activate, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Trip history
        if (data.rentals.isNotEmpty()) {
            item {
                Text(s.customerTrips, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            items(data.rentals) { r ->
                TripHistoryCard(r, s)
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = Ink) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = InkMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun TripHistoryCard(r: CustomerRentalItem, s: AppStrings) {
    val statusColor = when (r.rentalStatus) {
        "active"    -> StatusActive
        "completed" -> Primary
        "cancelled" -> Color(0xFFEF4444)
        else        -> StatusPending
    }
    val statusLabel = when (r.rentalStatus) {
        "pending"   -> s.statusPending
        "active"    -> s.statusActive
        "completed" -> s.statusCompleted
        "cancelled" -> s.statusCancelled
        else        -> r.rentalStatus
    }
    Card(shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                if (!r.pickupLocation.isNullOrBlank() || !r.dropoffLocation.isNullOrBlank()) {
                    Text(
                        "${r.pickupLocation ?: "?"} → ${r.dropoffLocation ?: "?"}",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink,
                        maxLines = 2
                    )
                }
                Text(
                    buildString {
                        r.startDate?.take(10)?.let { append(it) }
                        r.vehicleBrand?.let { append(" • $it ${r.vehicleModel ?: ""}") }
                    },
                    fontSize = 11.sp, color = InkMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("৳${"%,.0f".format(r.agreedAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(statusLabel, fontSize = 10.sp, color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ── Customer Form Sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerFormSheet(
    s: AppStrings,
    existing: CustomerListItem?,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var firstName   by remember { mutableStateOf(existing?.firstName ?: "") }
    var lastName    by remember { mutableStateOf(existing?.lastName ?: "") }
    var phone       by remember { mutableStateOf(existing?.phone ?: "") }
    var email       by remember { mutableStateOf(existing?.email ?: "") }
    var nid         by remember { mutableStateOf(existing?.nid ?: "") }
    var address     by remember { mutableStateOf(existing?.address ?: "") }
    var city        by remember { mutableStateOf(existing?.city ?: "") }
    var license     by remember { mutableStateOf(existing?.licenseNumber ?: "") }
    var licExpiry   by remember { mutableStateOf(existing?.licenseExpiry ?: "") }
    var submitting  by remember { mutableStateOf(false) }
    var formError   by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = { if (!submitting) onDismiss() }) {
        LazyColumn(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    if (existing == null) s.addCustomer else s.editCustomer,
                    fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
            }

            if (formError.isNotEmpty()) {
                item { Text(formError, color = Color(0xFFDC2626), fontSize = 13.sp) }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        firstName, { firstName = it },
                        label = { Text("${s.firstName} *") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        lastName, { lastName = it },
                        label = { Text(s.lastName) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            item {
                OutlinedTextField(
                    phone, { phone = it },
                    label = { Text("${s.mobile} *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp)
                )
            }
            item {
                OutlinedTextField(
                    email, { email = it },
                    label = { Text(s.email) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp)
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        nid, { nid = it },
                        label = { Text(s.nidLabel) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        city, { city = it },
                        label = { Text(s.cityLabel) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            item {
                OutlinedTextField(
                    address, { address = it },
                    label = { Text(s.addressLabel) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        license, { license = it },
                        label = { Text(s.licenseLabel) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        licExpiry, { licExpiry = it },
                        label = { Text(s.licenseExpiry) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (firstName.isBlank() || phone.isBlank()) {
                            formError = "প্রথম নাম ও মোবাইল আবশ্যক"
                            return@Button
                        }
                        submitting = true; formError = ""
                        scope.launch {
                            try {
                                if (existing == null) {
                                    val res = ApiClient.service.createCustomer(
                                        CreateCustomerRequest(
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            phone = phone.trim(),
                                            email = email.ifBlank { null },
                                            nid = nid.ifBlank { null },
                                            address = address.ifBlank { null },
                                            city = city.ifBlank { null },
                                            licenseNumber = license.ifBlank { null },
                                            licenseExpiry = licExpiry.ifBlank { null }
                                        )
                                    )
                                    onSaved(if (res.success) s.customerAdded else res.message ?: s.error)
                                } else {
                                    val res = ApiClient.service.updateCustomer(
                                        existing.id,
                                        UpdateCustomerRequest(
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            phone = phone.trim(),
                                            email = email.ifBlank { null },
                                            nid = nid.ifBlank { null },
                                            address = address.ifBlank { null },
                                            city = city.ifBlank { null },
                                            licenseNumber = license.ifBlank { null },
                                            licenseExpiry = licExpiry.ifBlank { null }
                                        )
                                    )
                                    onSaved(if (res.success) s.customerUpdated else res.message ?: s.error)
                                }
                            } catch (e: Exception) {
                                formError = e.message ?: s.error
                            } finally { submitting = false }
                        }
                    },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        if (submitting) s.loading else s.save,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                    )
                }
            }
        }
    }
}
