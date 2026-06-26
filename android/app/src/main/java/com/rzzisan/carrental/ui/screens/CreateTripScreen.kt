package com.rzzisan.carrental.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rzzisan.carrental.data.network.*
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(navController: NavController) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var passengerName  by remember { mutableStateOf("") }
    var passengerPhone by remember { mutableStateOf("") }
    var pickup         by remember { mutableStateOf("") }
    var dropoff        by remember { mutableStateOf("") }
    var amount         by remember { mutableStateOf("") }
    var notes          by remember { mutableStateOf("") }
    var tripType       by remember { mutableStateOf("one_way") }
    var selectedVehicle by remember { mutableStateOf<AssignedVehicle?>(null) }
    var vehicles       by remember { mutableStateOf<List<AssignedVehicle>>(emptyList()) }
    var vehicleExpanded by remember { mutableStateOf(false) }
    var loading        by remember { mutableStateOf(false) }
    var error          by remember { mutableStateOf("") }
    var successMsg     by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.service.getVehicles()
            if (res.success) vehicles = res.data ?: emptyList()
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.createTripTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, s.back) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (successMsg.isNotEmpty()) {
                Surface(color = StatusActive.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(successMsg, color = StatusActive, modifier = Modifier.padding(12.dp).fillMaxWidth())
                }
            }
            if (error.isNotEmpty()) {
                Surface(color = StatusDue.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(error, color = StatusDue, modifier = Modifier.padding(12.dp).fillMaxWidth())
                }
            }

            OutlinedTextField(value = passengerName,  onValueChange = { passengerName = it },  label = { Text(s.passengerName) },  modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = passengerPhone, onValueChange = { passengerPhone = it }, label = { Text(s.passengerMobile) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = pickup,          onValueChange = { pickup = it },          label = { Text(s.pickup) },          modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = dropoff,         onValueChange = { dropoff = it },         label = { Text(s.dropoff) },         modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = amount,          onValueChange = { amount = it },          label = { Text(s.amount) },          modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = notes,           onValueChange = { notes = it },           label = { Text(s.notes) },           modifier = Modifier.fillMaxWidth(), maxLines = 2)

            // Vehicle picker
            ExposedDropdownMenuBox(expanded = vehicleExpanded, onExpandedChange = { vehicleExpanded = it }) {
                OutlinedTextField(
                    value = selectedVehicle?.let { "${it.brand} ${it.model} (${it.registrationNumber})" } ?: "",
                    onValueChange = {},
                    label = { Text(s.selectVehicle) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vehicleExpanded) }
                )
                ExposedDropdownMenu(expanded = vehicleExpanded, onDismissRequest = { vehicleExpanded = false }) {
                    vehicles.forEach { v ->
                        DropdownMenuItem(
                            text = { Text("${v.brand} ${v.model} • ${v.registrationNumber}") },
                            onClick = { selectedVehicle = v; vehicleExpanded = false }
                        )
                    }
                }
            }

            // Trip type
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("one_way" to s.oneWay, "round_trip" to s.roundTrip).forEach { (value, label) ->
                    FilterChip(selected = tripType == value, onClick = { tripType = value }, label = { Text(label) })
                }
            }

            Button(
                onClick = {
                    if (passengerName.isBlank() || passengerPhone.isBlank() || pickup.isBlank() || dropoff.isBlank() || amount.isBlank() || selectedVehicle == null) {
                        error = "সব তথ্য পূরণ করুন"; return@Button
                    }
                    loading = true; error = ""; successMsg = ""
                    scope.launch {
                        try {
                            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            val res = ApiClient.service.createRental(
                                CreateRentalRequest(
                                    passengerName  = passengerName,
                                    passengerMobile = passengerPhone,
                                    vehicleId      = selectedVehicle!!.id,
                                    pickupLocation = pickup,
                                    dropoffLocation = dropoff,
                                    tripType       = tripType,
                                    startDatetime  = now,
                                    agreedAmount   = amount.toDoubleOrNull() ?: 0.0,
                                    notes          = notes.ifBlank { null }
                                )
                            )
                            if (res.success) {
                                successMsg = s.tripCreated
                                passengerName = ""; passengerPhone = ""; pickup = ""; dropoff = ""; amount = ""; notes = ""
                                selectedVehicle = null
                            } else { error = res.message ?: s.error }
                        } catch (_: Exception) { error = s.serverError }
                        finally { loading = false }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text(if (loading) s.loading else s.submit, fontWeight = FontWeight.SemiBold) }
        }
    }
}
