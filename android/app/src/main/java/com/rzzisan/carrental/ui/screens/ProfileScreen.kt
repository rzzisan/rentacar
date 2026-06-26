package com.rzzisan.carrental.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.ProfileData
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<ProfileData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var name    by remember { mutableStateOf("") }
    var mobile  by remember { mutableStateOf("") }
    var curPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var msg     by remember { mutableStateOf("") }
    var saving  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.service.getProfile()
            if (res.success && res.data != null) {
                profile = res.data
                name   = res.data.driver.name
                mobile = res.data.driver.mobile ?: ""
            }
        } catch (_: Exception) {}
        finally { loading = false }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(s.profileTitle, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        if (loading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val p = profile ?: return@Scaffold

        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(p.stats.totalTrips.toString(),     s.totalTrips,     Modifier.weight(1f))
                StatCard(p.stats.completedTrips.toString(), s.completedTrips, Modifier.weight(1f))
                StatCard(p.stats.activeTrips.toString(),    s.activeTrips,    Modifier.weight(1f))
            }

            // Driver info
            Card(shape = RoundedCornerShape(12.dp), border = CardDefaults.outlinedCardBorder()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.driver.email, fontSize = 13.sp, color = InkMuted)
                    Text("${s.commissionRate}: ${p.driver.commissionRate}%", fontSize = 13.sp, color = InkMuted)
                }
            }

            Text(s.profileTitle, fontWeight = FontWeight.SemiBold, color = Ink)
            OutlinedTextField(value = name,   onValueChange = { name = it },   label = { Text(s.name) },   modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text(s.mobile) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Divider()
            Text(s.changePassword, fontWeight = FontWeight.SemiBold, color = Ink)
            OutlinedTextField(value = curPass, onValueChange = { curPass = it }, label = { Text(s.currentPassword) }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
            OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text(s.newPassword) },     modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)

            if (msg.isNotEmpty()) {
                Text(msg, color = if (msg.contains("ব্যর্থ") || msg.contains("fail", true)) StatusDue else StatusActive, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    saving = true; msg = ""
                    scope.launch {
                        try {
                            val res = ApiClient.service.updateProfile(
                                name = name.toRequestBody("text/plain".toMediaType()),
                                mobile = mobile.toRequestBody("text/plain".toMediaType()),
                                currentPassword = curPass.ifBlank { null }?.toRequestBody("text/plain".toMediaType()),
                                newPassword = newPass.ifBlank { null }?.toRequestBody("text/plain".toMediaType())
                            )
                            msg = res.message ?: if (res.success) s.profileUpdated else s.error
                            if (res.success) { curPass = ""; newPass = "" }
                        } catch (_: Exception) { msg = s.serverError }
                        finally { saving = false }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text(if (saving) s.loading else s.save, fontWeight = FontWeight.SemiBold) }

            // Assigned vehicles
            if (p.vehicles.isNotEmpty()) {
                Text(s.assignedVehicles, fontWeight = FontWeight.SemiBold, color = Ink)
                p.vehicles.forEach { v ->
                    Surface(color = SurfaceVariant, shape = RoundedCornerShape(8.dp)) {
                        Text("${v.brand} ${v.model} • ${v.registrationNumber}",
                            modifier = Modifier.padding(12.dp).fillMaxWidth(), fontSize = 13.sp, color = Ink)
                    }
                }
            }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDue)
            ) { Text(s.logout, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = PrimaryLight)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary)
            Text(label, fontSize = 11.sp, color = InkMuted)
        }
    }
}
