package com.rzzisan.carrental.ui.screens.manager

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rzzisan.carrental.data.network.*
import com.rzzisan.carrental.util.errorMessageOf
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDriversScreen() {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var drivers by remember { mutableStateOf<List<ManagerDriver>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val snackState = remember { SnackbarHostState() }

    fun load() {
        loading = true; error = ""
        scope.launch {
            try {
                val res = ApiClient.service.getManagerDrivers()
                if (res.success) drivers = res.data ?: emptyList() else error = res.message ?: s.error
            } catch (e: Exception) { error = errorMessageOf(e, s.serverError) }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.mgrDriversTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
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
            drivers.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text(s.noMgrDrivers, color = InkMuted) }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(drivers) { d ->
                    MgrDriverCard(d, s)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ── Driver Card ────────────────────────────────────────────────────

@Composable
private fun MgrDriverCard(
    d: ManagerDriver,
    s: com.rzzisan.carrental.ui.strings.AppStrings
) {
    val isActive = d.status == "active"
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
                            Icon(Icons.Filled.Person, null,
                                tint = if (isActive) Primary else InkMuted,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                    Column {
                        Text(d.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink)
                        d.mobile?.let { Text(it, fontSize = 12.sp, color = InkMuted) }
                    }
                }
                Surface(shape = RoundedCornerShape(6.dp),
                    color = if (isActive) StatusActive.copy(alpha = 0.1f) else InkMuted.copy(alpha = 0.1f)) {
                    Text(if (isActive) s.activeLabel else s.inactiveLabel, fontSize = 11.sp,
                        color = if (isActive) StatusActive else InkMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            // Performance stats row
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(0.dp)) {
                MgrPerfStat(Modifier.weight(1f), s.totalTripsLabel, "${d.totalTrips}", Color(0xFF3B82F6))
                MgrPerfStat(Modifier.weight(1f), s.thisMonthLabel, "${d.thisMonthTrips}", Primary)
                MgrPerfStat(Modifier.weight(1f), s.commissionRateLabel, "${d.commissionRate.toInt()}%", Color(0xFF8B5CF6))
                MgrPerfStat(Modifier.weight(1f), s.totalDueLabel, "${s.taka}${String.format("%.0f", d.totalDue)}",
                    if (d.totalDue > 0) StatusDue else StatusPaid)
            }
            // Assigned vehicles chips
            if (d.vehicles.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    d.vehicles.forEach { v ->
                        Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.08f)) {
                            Text("${v.brand} ${v.model}", fontSize = 11.sp, color = Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MgrPerfStat(modifier: Modifier, label: String, value: String, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = InkMuted, maxLines = 1)
    }
}
