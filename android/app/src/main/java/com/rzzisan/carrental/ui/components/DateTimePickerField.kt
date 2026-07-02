package com.rzzisan.carrental.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rzzisan.carrental.ui.strings.LocalStrings
import com.rzzisan.carrental.ui.theme.InkMuted
import com.rzzisan.carrental.ui.theme.Primary
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// রেন্টাল/ট্রিপ শুরুর তারিখ ও সময় বাছাই — আগে এই ফিল্ডটা প্লেইন টেক্সট ইনপুট ছিল
// (admin/manager-এর "নতুন রেন্টাল" ফর্মে "YYYY-MM-DD HH:MM" placeholder সহ, কোনো picker ছাড়া),
// যেটা ভুল ফরম্যাটে টাইপ করার ঝুঁকি তৈরি করত। CreateTripScreen.kt-এর বিদ্যমান কাজ-করা
// date/time picker প্যাটার্ন এখানে শেয়ারড কম্পোনেন্ট হিসেবে তোলা হয়েছে।
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    onValueChange: (String) -> Unit, // "yyyy-MM-dd HH:mm:ss" ফরম্যাটে emit করে (backend API-এর প্রত্যাশিত ফরম্যাট)
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val displayDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    val displayTime = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    LaunchedEffect(selectedDate, selectedTime) {
        onValueChange(
            LocalDateTime.of(selectedDate, selectedTime)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        selectedDate = Instant.ofEpochMilli(ms)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(s.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(s.cancel) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour   = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour      = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(s.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(s.cancel) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(s.startDatetime, style = MaterialTheme.typography.labelMedium, color = InkMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = displayDate, onValueChange = {}, readOnly = true,
                label = { Text("তারিখ") },
                trailingIcon = { Icon(Icons.Filled.CalendarMonth, null, tint = Primary) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = displayTime, onValueChange = {}, readOnly = true,
                label = { Text("সময়") },
                trailingIcon = { Icon(Icons.Filled.Schedule, null, tint = Primary) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("তারিখ বেছে নিন")
            }
            OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("সময় বেছে নিন")
            }
        }
    }
}
