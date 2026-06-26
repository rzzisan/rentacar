package com.rzzisan.carrental.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rzzisan.carrental.data.network.ApiClient
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(rentalId: Int, navController: NavController) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocation = remember { LocationServices.getFusedLocationProviderClient(context) }

    val expenseTypes = listOf(
        "toll" to s.expToll, "fuel" to s.expFuel, "parking" to s.expParking,
        "repair" to s.expRepair, "driver_allowance" to s.expAllowance, "other" to s.expOther
    )
    var selectedType  by remember { mutableStateOf("fuel") }
    var typeExpanded  by remember { mutableStateOf(false) }
    var amount        by remember { mutableStateOf("") }
    var description   by remember { mutableStateOf("") }
    var photoUri      by remember { mutableStateOf<Uri?>(null) }
    var photoFile     by remember { mutableStateOf<File?>(null) }
    var loading       by remember { mutableStateOf(false) }
    var statusMsg     by remember { mutableStateOf("") }
    var isSuccess     by remember { mutableStateOf(false) }

    // Camera setup
    val tempFile = remember { File(context.cacheDir, "expense_${System.currentTimeMillis()}.jpg") }
    val tempUri  = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) { photoUri = tempUri; photoFile = tempFile }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(tempUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.expenseTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, s.back) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (statusMsg.isNotEmpty()) {
                Surface(
                    color = if (isSuccess) StatusActive.copy(0.1f) else StatusDue.copy(0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(statusMsg, color = if (isSuccess) StatusActive else StatusDue,
                        modifier = Modifier.padding(12.dp).fillMaxWidth())
                }
            }

            // Expense type picker
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = expenseTypes.firstOrNull { it.first == selectedType }?.second ?: selectedType,
                    onValueChange = {},
                    label = { Text(s.expenseType) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) }
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    expenseTypes.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { selectedType = value; typeExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = amount,      onValueChange = { amount = it },      label = { Text(s.expenseAmount) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(s.description) },   modifier = Modifier.fillMaxWidth(), maxLines = 2)

            // Camera button
            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(tempUri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(if (photoUri != null) Icons.Filled.CheckCircle else Icons.Filled.CameraAlt,
                    contentDescription = null, tint = if (photoUri != null) StatusActive else Primary)
                Spacer(Modifier.width(8.dp))
                Text(if (photoUri != null) "ছবি নেওয়া হয়েছে ✓" else s.takePhoto,
                    color = if (photoUri != null) StatusActive else Primary)
            }

            Button(
                onClick = {
                    if (amount.isBlank()) { statusMsg = "${s.expenseAmount} দিন"; isSuccess = false; return@Button }
                    loading = true; statusMsg = s.gettingLocation; isSuccess = false
                    scope.launch {
                        try {
                            @Suppress("MissingPermission")
                            val loc = fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                            val locName = if (loc != null) reverseGeocode(context, loc.latitude, loc.longitude) else null

                            val typePart   = selectedType.toRequestBody("text/plain".toMediaType())
                            val amountPart = amount.toRequestBody("text/plain".toMediaType())
                            val descPart   = description.ifBlank { null }?.toRequestBody("text/plain".toMediaType())
                            val latPart    = loc?.latitude?.toString()?.toRequestBody("text/plain".toMediaType())
                            val lngPart    = loc?.longitude?.toString()?.toRequestBody("text/plain".toMediaType())
                            val locPart    = locName?.toRequestBody("text/plain".toMediaType())

                            val imagePart = photoFile?.let { f ->
                                MultipartBody.Part.createFormData("receipt_image", f.name, f.asRequestBody("image/jpeg".toMediaType()))
                            }

                            val res = ApiClient.service.addExpense(
                                rentalId = rentalId,
                                expenseType = typePart, amount = amountPart,
                                description = descPart, locationName = locPart,
                                latitude = latPart, longitude = lngPart,
                                receiptImage = imagePart
                            )
                            isSuccess = res.success
                            statusMsg = res.message ?: if (res.success) s.expenseAdded else s.error
                            if (res.success) { amount = ""; description = ""; photoUri = null; photoFile = null }
                        } catch (_: Exception) { statusMsg = s.serverError; isSuccess = false }
                        finally { loading = false }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text(if (loading) s.gpsCapturing else s.submit, fontWeight = FontWeight.SemiBold) }
        }
    }
}
