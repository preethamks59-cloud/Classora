package com.example.classora.ui.screens

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.classora.data.SettingsPreferences
import com.example.classora.data.Task
import com.example.classora.data.TaskPreferences
import com.example.classora.ui.components.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSyncScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsPreferences = remember { SettingsPreferences(context) }
    val taskPreferences = remember { TaskPreferences(context) }
    val settings by settingsPreferences.settingsFlow.collectAsState(initial = com.example.classora.data.AppSettings())
    
    var isSyncing by remember { mutableStateOf(false) }
    var detectedAccounts by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    var isAccountSelectorExpanded by remember { mutableStateOf(false) }
    
    var selectedViewDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentMonthView by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var syncedEventsPreview by remember { mutableStateOf<List<Task>>(emptyList()) }
    var eventsForDots by remember { mutableStateOf<List<Task>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            scope.launch { 
                settingsPreferences.updateCalendarSync(true)
                detectedAccounts = getCalendarAccounts(context)
            }
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Please enable Calendar permissions in Settings", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(settings.calendarSyncConnected) {
        if (settings.calendarSyncConnected) {
            detectedAccounts = getCalendarAccounts(context)
            if (detectedAccounts.isNotEmpty() && selectedAccount == null) {
                selectedAccount = detectedAccounts.first()
            }
        }
    }

    // Refresh preview and dots when account, month view, or selected date changes
    LaunchedEffect(selectedAccount, selectedViewDate, currentMonthView, isSyncing, settings.calendarSyncConnected) {
        if (settings.calendarSyncConnected && !isSyncing && selectedAccount != null) {
            // Fetch events for dots based on the visible month AND selected account
            eventsForDots = fetchCalendarEventsForRange(context.contentResolver, selectedAccount!!, currentMonthView)
            
            // Fetch specific preview for the selected account and date
            syncedEventsPreview = fetchCalendarEventsForDate(context.contentResolver, selectedAccount!!, selectedViewDate)
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Header
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
                    text = "Calendar Synchronization",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1724),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Security Banner
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Secure Synchronization", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                            Text("Data is end-to-end encrypted locally. No data is moved to other apps without permission.", 
                                fontSize = 11.sp, color = Color(0xFF388E3C))
                        }
                    }
                }

                if (!settings.calendarSyncConnected) {
                    Text("Sync your calendar & schedules", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                                scope.launch { settingsPreferences.updateCalendarSync(true) }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Connect Device Calendar")
                    }
                } else {
                    // Account Selector (Collapsed/Expanded)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { isAccountSelectorExpanded = !isAccountSelectorExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mail, null, tint = Color(0xFF2196F3), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Synced Account", fontSize = 11.sp, color = Color.Gray)
                                        Text(selectedAccount ?: "Select an account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Icon(
                                    if (isAccountSelectorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    null, tint = Color.Gray
                                )
                            }
                            
                            AnimatedVisibility(visible = isAccountSelectorExpanded) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    detectedAccounts.forEach { account ->
                                        if (account != selectedAccount) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable { 
                                                        selectedAccount = account
                                                        isAccountSelectorExpanded = false
                                                    },
                                                color = Color.White,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(account, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                                            }
                                        }
                                    }
                                    TextButton(
                                        onClick = { scope.launch { settingsPreferences.updateCalendarSync(false) } },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Disconnect", color = Color.Red)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Calendar Widget
                    CalendarWidget(
                        tasks = eventsForDots,
                        selectedDate = selectedViewDate,
                        onDateSelected = { selectedViewDate = it },
                        onMonthChanged = { currentMonthView = it },
                        showAttendanceDots = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Selected Day Preview
                    val dateDisplay = SimpleDateFormat("dd MMMM", Locale.getDefault()).format(selectedViewDate.time)
                    Text("Events on $dateDisplay", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1724))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (syncedEventsPreview.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No events found in your Google Calendar for this day.", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        syncedEventsPreview.forEach { event ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Event, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(event.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(event.time, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Sync Configuration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                    
                    SyncToggleItem("Import device events", settings.syncTimetable) { 
                        scope.launch { settingsPreferences.updateSyncOption("timetable", it) }
                    }
                    
                    SyncToggleItem("Export Classora tasks to device", settings.autoSync) {
                        scope.launch { settingsPreferences.updateSyncOption("auto", it) }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            scope.launch {
                                isSyncing = true
                                if (settings.syncTimetable) syncCalendarEvents(context.contentResolver, taskPreferences, selectedAccount!!)
                                if (settings.autoSync) exportTasksToCalendar(context.contentResolver, taskPreferences, selectedAccount!!)
                                isSyncing = false
                                Toast.makeText(context, "Synchronization complete", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Start Two-Way Sync")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
                    Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Classora respects your privacy. All sync operations happen on-device using Android Content Providers.",
                        fontSize = 11.sp, color = Color.Gray
                    )
                }
            }
        }

        if (isSyncing) {
            Dialog(onDismissRequest = { }) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF2196F3), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Syncing with Google...", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getCalendarAccounts(context: Context): List<String> {
    val accounts = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return accounts
    val cursor = context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, arrayOf(CalendarContract.Calendars.ACCOUNT_NAME), null, null, null)
    cursor?.use {
        val nameIdx = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
        while (it.moveToNext()) {
            val name = it.getString(nameIdx)
            if (name != null && !accounts.contains(name) && name.contains("@")) accounts.add(name)
        }
    }
    return accounts
}

private suspend fun fetchCalendarEventsForDate(contentResolver: ContentResolver, accountName: String, date: Calendar): List<Task> {
    val events = mutableListOf<Task>()
    
    // First, find all calendar IDs associated with this account (including Birthdays, Holidays, etc.)
    val calendarCursor = contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI, 
        arrayOf(CalendarContract.Calendars._ID), 
        "${CalendarContract.Calendars.ACCOUNT_NAME} = ?", 
        arrayOf(accountName), 
        null
    )
    val calendarIds = mutableListOf<Long>()
    calendarCursor?.use { while (it.moveToNext()) calendarIds.add(it.getLong(0)) }
    if (calendarIds.isEmpty()) return events

    val startOfDay = (date.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
    val endOfDay = (date.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
    
    val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"
    val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
    
    val cursor = contentResolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.ALL_DAY), selection, selectionArgs, "${CalendarContract.Events.DTSTART} ASC")
    cursor?.use {
        val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
        val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
        val allDayIdx = it.getColumnIndex(CalendarContract.Events.ALL_DAY)
        while (it.moveToNext()) {
            val title = it.getString(titleIdx)
            val startTime = it.getLong(startIdx)
            val isAllDay = it.getInt(allDayIdx) == 1
            val cal = Calendar.getInstance().apply { timeInMillis = startTime }
            events.add(Task(
                title = title, 
                date = "", 
                time = if (isAllDay) "" else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time), 
                description = ""
            ))
        }
    }
    return events
}

private suspend fun fetchCalendarEventsForRange(contentResolver: ContentResolver, accountName: String, date: Calendar): List<Task> {
    val events = mutableListOf<Task>()
    
    // First, find all calendar IDs associated with this account (including Birthdays, Holidays, etc.)
    val calendarCursor = contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI, 
        arrayOf(CalendarContract.Calendars._ID), 
        "${CalendarContract.Calendars.ACCOUNT_NAME} = ?", 
        arrayOf(accountName), 
        null
    )
    val calendarIds = mutableListOf<Long>()
    calendarCursor?.use { while (it.moveToNext()) calendarIds.add(it.getLong(0)) }
    if (calendarIds.isEmpty()) return events

    // Fetch for the whole current month of the provided date
    val rangeStart = (date.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
    val rangeEnd = (date.clone() as Calendar).apply { 
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) 
    }.timeInMillis
    
    // Query calendars only for the selected account
    val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"
    val selectionArgs = arrayOf(rangeStart.toString(), rangeEnd.toString())
    
    val cursor = contentResolver.query(
        CalendarContract.Events.CONTENT_URI, 
        arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.ALL_DAY), 
        selection, 
        selectionArgs, 
        "${CalendarContract.Events.DTSTART} ASC"
    )
    
    cursor?.use {
        val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
        val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
        while (it.moveToNext()) {
            val startTime = it.getLong(startIdx)
            val cal = Calendar.getInstance().apply { timeInMillis = startTime }
            events.add(Task(
                title = it.getString(titleIdx) ?: "", 
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time), 
                time = "",
                description = "",
                priority = "Medium"
            ))
        }
    }
    return events
}

private suspend fun syncCalendarEvents(contentResolver: ContentResolver, taskPreferences: TaskPreferences, accountName: String) {
    val calendarCursor = contentResolver.query(CalendarContract.Calendars.CONTENT_URI, arrayOf(CalendarContract.Calendars._ID), "${CalendarContract.Calendars.ACCOUNT_NAME} = ?", arrayOf(accountName), null)
    val calendarIds = mutableListOf<Long>()
    calendarCursor?.use { while (it.moveToNext()) calendarIds.add(it.getLong(0)) }
    if (calendarIds.isEmpty()) return

    val startMillis = Calendar.getInstance().timeInMillis
    val endMillis = startMillis + (30L * 24 * 60 * 60 * 1000)
    val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"
    val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
    val cursor = contentResolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.DESCRIPTION), selection, selectionArgs, "${CalendarContract.Events.DTSTART} ASC")
    val existingTasks = taskPreferences.tasksFlow.first()
    cursor?.use {
        val idIdx = it.getColumnIndex(CalendarContract.Events._ID); val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE); val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART); val descIdx = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
        while (it.moveToNext()) {
            val eventId = it.getString(idIdx); val title = it.getString(titleIdx); val startTime = it.getLong(startIdx); val desc = it.getString(descIdx) ?: ""
            val externalId = "calendar_${accountName}_$eventId"
            if (existingTasks.any { it.externalId == externalId }) continue
            val cal = Calendar.getInstance().apply { timeInMillis = startTime }
            taskPreferences.saveTask(Task(title = title, description = desc, date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time), time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time), category = "Imported", priority = "Medium", externalId = externalId))
        }
    }
}

private suspend fun exportTasksToCalendar(contentResolver: ContentResolver, taskPreferences: TaskPreferences, accountName: String) {
    val tasks = taskPreferences.tasksFlow.first().filter { it.category != "Imported" && !it.isCompleted }
    val calendarCursor = contentResolver.query(CalendarContract.Calendars.CONTENT_URI, arrayOf(CalendarContract.Calendars._ID), "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.IS_PRIMARY} = 1", arrayOf(accountName), null)
    val calendarId = calendarCursor?.use { if (it.moveToFirst()) it.getLong(0) else null } ?: return
    val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
    tasks.forEach { task ->
        val selection = "${CalendarContract.Events.DESCRIPTION} LIKE ? AND ${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf("%ClassoraID:${task.id}%", calendarId.toString())
        val existCursor = contentResolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events._ID), selection, selectionArgs, null)
        if (existCursor?.use { it.count > 0 } == true) return@forEach
        try {
            val dateObj = sdf.parse("${task.date} ${task.time}")
            if (dateObj != null) {
                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, dateObj.time)
                    put(CalendarContract.Events.DTEND, dateObj.time + (60 * 60 * 1000))
                    put(CalendarContract.Events.TITLE, task.title)
                    put(CalendarContract.Events.DESCRIPTION, "${task.description}\n\nClassoraID:${task.id}")
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
                contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}

@Composable
fun SyncToggleItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, color = Color(0xFF0D1724))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2196F3)))
    }
}
