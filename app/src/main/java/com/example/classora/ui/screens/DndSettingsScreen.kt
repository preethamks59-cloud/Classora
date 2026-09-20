package com.example.classora.ui.screens

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.AppSettings
import com.example.classora.data.SettingsPreferences
import com.example.classora.utils.DndScheduler
import com.example.classora.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsPreferences = remember { SettingsPreferences(context) }
    val settings by settingsPreferences.settingsFlow.collectAsState(initial = AppSettings())
    
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    var hasDndPermission by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }
    var showDndPermissionDialog by remember { mutableStateOf(false) }
    var hasExactAlarmPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
        ) 
    }
    var showAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showExamCalendar by remember { mutableStateOf(false) }
    var tempSelectedExamDates by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(showExamCalendar) {
        if (showExamCalendar) {
            tempSelectedExamDates = settings.examDates.split(",").filter { it.isNotEmpty() }.toSet()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Check permissions whenever returning to this screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasDndPermission = notificationManager.isNotificationPolicyAccessGranted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    hasExactAlarmPermission = alarmManager.canScheduleExactAlarms()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        // Initial check
        hasDndPermission = notificationManager.isNotificationPolicyAccessGranted
        if (!hasDndPermission && !settings.hasSkippedSpecialPermissions) {
            showDndPermissionDialog = true
        }
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showDndPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showDndPermissionDialog = false },
            title = { Text("Silent Mode Access", fontWeight = FontWeight.Bold) },
            text = { Text("To automatically silence your phone during college hours, Classora needs 'Do Not Disturb' access.") },
            confirmButton = {
                Button(onClick = {
                    showDndPermissionDialog = false
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }) { Text("Grant Access") }
            },
            dismissButton = {
                TextButton(onClick = { showDndPermissionDialog = false }) { Text("Later") }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Custom Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 8.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0D1724))
                }
                Text(
                    text = "Automatic Silent Mode",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1724),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!hasDndPermission) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF57C00))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Permission Required", fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "To switch your phone to Silent mode, Classora needs access to your system's notification policy.",
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                    } else {
                                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    }
                                    
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant Access in Settings", color = Color.White)
                            }
                        }
                    }
                }

                if (!hasExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2196F3))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Precise Timing Permission", fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "To trigger silent mode at exactly the right time, Classora needs the 'Alarms & Reminders' permission.",
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant Permission", color = Color.White)
                            }
                        }
                    }
                }

                Text("Focus without distractions", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                
                ToggleSettingItem(
                    label = "Enable Automatic Silent Mode", 
                    checked = settings.autoDndEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                            showAlarmPermissionDialog = true
                            return@ToggleSettingItem
                        }
                        scope.launch {
                            settingsPreferences.updateAutoDnd(enabled)
                            if (enabled && hasDndPermission) {
                                DndScheduler.scheduleDnd(
                                    context = context, 
                                    startTime = settings.collegeStartTime, 
                                    endTime = settings.collegeEndTime, 
                                    activateImmediately = true,
                                    repeatType = settings.dndRepeatType
                                )
                            } else {
                                DndScheduler.cancelDnd(context)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Repeat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                
                val repeatOptions = listOf("Today only", "Weekdays", "Weekends", "Everyday")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeatOptions.forEach { option ->
                        val isSelected = settings.dndRepeatType == option
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                scope.launch { 
                                    settingsPreferences.updateDndRepeatType(option)
                                    // Reschedule DND with the new repeat type
                                    if (settings.autoDndEnabled && hasDndPermission) {
                                        DndScheduler.scheduleDnd(
                                            context = context, 
                                            startTime = settings.collegeStartTime, 
                                            endTime = settings.collegeEndTime, 
                                            activateImmediately = true,
                                            repeatType = option
                                        )
                                    }
                                } 
                            },
                            label = { Text(option, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2196F3),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("College Schedule", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TimeSelectionItem(
                        label = "Morning Time",
                        time = settings.collegeStartTime,
                        modifier = Modifier.weight(1f),
                        onTimeSelected = { newTime ->
                            scope.launch {
                                settingsPreferences.updateCollegeStartTime(newTime)
                                if (settings.autoDndEnabled && hasDndPermission) {
                                    DndScheduler.scheduleDnd(
                                        context = context, 
                                        startTime = newTime, 
                                        endTime = settings.collegeEndTime, 
                                        activateImmediately = true,
                                        repeatType = settings.dndRepeatType
                                    )
                                }
                            }
                        }
                    )
                    TimeSelectionItem(
                        label = "Evening Time",
                        time = settings.collegeEndTime,
                        modifier = Modifier.weight(1f),
                        onTimeSelected = { newTime ->
                            scope.launch {
                                settingsPreferences.updateCollegeEndTime(newTime)
                                if (settings.autoDndEnabled && hasDndPermission) {
                                    DndScheduler.scheduleDnd(
                                        context = context, 
                                        startTime = settings.collegeStartTime, 
                                        endTime = newTime, 
                                        activateImmediately = true,
                                        repeatType = settings.dndRepeatType
                                    )
                                }
                            }
                        }
                    )
                }
                
                Text(
                    text = "* Phone will go silent 10 mins before morning time and return to normal 10 mins after evening time.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Additional Options", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DND during exams", fontSize = 15.sp, color = Color(0xFF0D1724))
                        if (settings.dndDuringExams) {
                            IconButton(onClick = { showExamCalendar = true }) {
                                Icon(
                                    Icons.Default.CalendarToday, 
                                    contentDescription = "Select Exam Dates",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF2196F3)
                                )
                            }
                        }
                    }
                    Switch(
                        checked = settings.dndDuringExams, 
                        onCheckedChange = { enabled -> 
                            scope.launch { 
                                settingsPreferences.updateDndDuringExams(enabled)
                                if (settings.autoDndEnabled && hasDndPermission) {
                                    DndScheduler.scheduleDnd(
                                        context = context,
                                        startTime = settings.collegeStartTime,
                                        endTime = settings.collegeEndTime,
                                        activateImmediately = true,
                                        repeatType = settings.dndRepeatType
                                    )
                                }
                            } 
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2196F3)
                        )
                    )
                }

                ToggleSettingItem(
                    label = "Allow important notifications", 
                    checked = settings.allowImportantNotifs, 
                    onCheckedChange = { scope.launch { settingsPreferences.updateAllowImportantNotifs(it) } }
                )
            }
        }
    }

    if (showExamCalendar) {
        com.example.classora.ui.components.CustomCalendarDialog(
            onDismiss = { showExamCalendar = false },
            onDateSelected = { /* Not used in multi-select */ },
            allowFutureDates = true,
            multiSelectEnabled = true,
            selectedDates = tempSelectedExamDates,
            onDateLongClicked = { dateStr: String ->
                tempSelectedExamDates = if (tempSelectedExamDates.contains(dateStr)) {
                    tempSelectedExamDates - dateStr
                } else {
                    tempSelectedExamDates + dateStr
                }
            },
            onConfirm = {
                scope.launch {
                    val datesStr = tempSelectedExamDates.joinToString(",")
                    settingsPreferences.updateExamDates(datesStr)
                    
                    // Automatically toggle off if no dates are selected
                    if (tempSelectedExamDates.isEmpty()) {
                        settingsPreferences.updateDndDuringExams(false)
                    }

                    if (settings.autoDndEnabled && hasDndPermission) {
                        DndScheduler.scheduleDnd(
                            context = context,
                            startTime = settings.collegeStartTime,
                            endTime = settings.collegeEndTime,
                            activateImmediately = true,
                            repeatType = settings.dndRepeatType
                        )
                    }
                    showExamCalendar = false
                }
            }
        )
    }

    if (showAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmPermissionDialog = false },
            title = { Text("Precise Timing Permission", fontWeight = FontWeight.Bold) },
            text = { Text("To trigger silent mode at exactly the right time, Classora needs 'Alarms & Reminders' permission.") },
            confirmButton = {
                Button(onClick = {
                    showAlarmPermissionDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                }) { Text("Grant Permission") }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmPermissionDialog = false }) { Text("Later") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectionItem(label: String, time: String, modifier: Modifier = Modifier, onTimeSelected: (String) -> Unit) {
    var showTimePicker by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(time, color = Color(0xFF0D1724), fontWeight = FontWeight.Bold)
        }
    }

    if (showTimePicker) {
        val timeParts = time.split(":")
        val timePickerState = rememberTimePickerState(
            initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9,
            initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val formattedTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    onTimeSelected(formattedTime)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Composable
fun ToggleSettingItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = Color(0xFF0D1724))
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2196F3)
            )
        )
    }
}
