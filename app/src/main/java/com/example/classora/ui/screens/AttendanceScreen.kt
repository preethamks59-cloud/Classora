package com.example.classora.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.*
import com.example.classora.ui.components.CalendarWidget
import com.example.classora.ui.components.ClassoraHeader
import com.example.classora.ui.components.SectionHeader
import com.example.classora.utils.NotificationScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceScreen(
    onMenuClick: () -> Unit, 
    onProfileClick: () -> Unit, 
    onNotificationsClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onAddTimetableClick: () -> Unit,
    onSubjectClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    val userData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", ""))
    
    val attendancePreferences = remember { AttendancePreferences(context) }
    val timetableVersions by attendancePreferences.timetableVersionsFlow.collectAsState(initial = emptyList())
    val attendanceRecords by attendancePreferences.attendanceFlow.collectAsState(initial = emptyList())
    val overrides by attendancePreferences.overridesFlow.collectAsState(initial = emptyList())

    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var showArchiveInfo by remember { mutableStateOf(false) }
    var showOverrideDialog by remember { mutableStateOf(false) }
    
    val dayOfWeek = remember(selectedDate, overrides) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
        val override = overrides.find { it.date == dateStr }
        override?.sourceDay ?: SimpleDateFormat("EEEE", Locale.US).format(selectedDate.time)
    }
    val selectedDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.time)

    // Find applicable version for selected date
    val activeVersion = remember(timetableVersions, selectedDateStr) {
        if (timetableVersions.isEmpty()) return@remember null

        val applicableVersions = timetableVersions.filter { it.startDate <= selectedDateStr }
        if (applicableVersions.isNotEmpty()) {
            applicableVersions.maxByOrNull { it.startDate }
        } else {
            // If no version exists for the selected date, show the earliest one available
            // instead of the latest one, to avoid confusion on old dates.
            timetableVersions.minByOrNull { it.startDate }
        }
    }

    val selectedDayClasses = activeVersion?.entries?.filter { entry ->
        val dayInput = entry.day.trim().lowercase()
        val todayFull = dayOfWeek.lowercase()
        val todayShort = dayOfWeek.take(3).lowercase()
        
        val effectiveDow = when (todayFull) {
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            "sunday" -> Calendar.SUNDAY
            else -> selectedDate.get(Calendar.DAY_OF_WEEK)
        }
        
        val isWeekday = effectiveDow in Calendar.MONDAY..Calendar.FRIDAY
        val isWeekend = effectiveDow == Calendar.SATURDAY || effectiveDow == Calendar.SUNDAY
        
        when {
            dayInput.contains("everyday") || dayInput.contains("daily") || dayInput.contains("all days") -> true
            
            dayInput.contains("weekday") || 
            dayInput.contains("mon-fri") || 
            dayInput.contains("mon to fri") ||
            dayInput.contains("monday-friday") ||
            dayInput.contains("monday to friday") -> isWeekday
            
            dayInput.contains("weekend") ||
            dayInput.contains("sat-sun") ||
            dayInput.contains("saturday-sunday") ||
            dayInput.contains("sat to sun") -> isWeekend
            
            else -> {
                // More robust matching for specific days and common abbreviations
                val matches = when (effectiveDow) {
                    Calendar.MONDAY -> listOf("monday", "mon", " m ")
                    Calendar.TUESDAY -> listOf("tuesday", "tue", " tu ")
                    Calendar.WEDNESDAY -> listOf("wednesday", "wed", " w ")
                    Calendar.THURSDAY -> listOf("thursday", "thu", " th ")
                    Calendar.FRIDAY -> listOf("friday", "fri", " f ")
                    Calendar.SATURDAY -> listOf("saturday", "sat", " sa ")
                    Calendar.SUNDAY -> listOf("sunday", "sun", " su ")
                    else -> emptyList()
                }
                matches.any { dayInput.contains(it) } || dayInput.contains(todayFull) || dayInput.contains(todayShort)
            }
        }
    } ?: emptyList()
    
    val subjectStats = remember(activeVersion, attendanceRecords) {
        val subjects = activeVersion?.entries?.map { it.subject }?.distinct() ?: emptyList()
        subjects.map { subject ->
            val presentCount = attendanceRecords.count { it.subject == subject && it.isPresent && it.timetableVersionId == activeVersion?.id }
            val totalCount = attendanceRecords.count { it.subject == subject && it.timetableVersionId == activeVersion?.id }
            SubjectAttendance(subject, presentCount, totalCount)
        }.sortedByDescending { it.percentage }
    }
    
    val overallPercentage = if (activeVersion != null) {
        val versionRecords = attendanceRecords.filter { it.timetableVersionId == activeVersion.id }
        if (versionRecords.isNotEmpty()) {
            (versionRecords.count { it.isPresent }.toFloat() / versionRecords.size) * 100
        } else 100f
    } else 100f

    val currentTime = Calendar.getInstance()
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val currentClass = selectedDayClasses.find { entry ->
        try {
            val classTime = Calendar.getInstance().apply {
                time = timeFormat.parse(entry.time)!!
                set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
                set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))
            }
            val classEndTime = (classTime.clone() as Calendar).apply { add(Calendar.HOUR, 1) }
            currentTime.after(classTime) && currentTime.before(classEndTime)
        } catch (e: Exception) { false }
    }

    if (showArchiveInfo && activeVersion != null) {
        AlertDialog(
            onDismissRequest = { showArchiveInfo = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2196F3))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Timetable Info", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Viewing: ${activeVersion.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Active since: ${activeVersion.startDate}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Note: For this $dayOfWeek you changed to a new semester. If you select older dates to view, the app will show your previous semester's data.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showArchiveInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got it")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveInfo = false }) {
                    Text("Close", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showOverrideDialog) {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false },
            title = { Text("Select Schedule for this Day") },
            text = {
                Column {
                    days.forEach { day ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                                    attendancePreferences.saveTimetableOverride(TimetableOverride(dateStr, day))
                                    showOverrideDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(day, textAlign = androidx.compose.ui.text.style.TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOverrideDialog = false }) { Text("Cancel") }
            },
            containerColor = Color.White
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item { 
            ClassoraHeader(
                userData = userData,
                title = "Attendance",
                subtitle = "Track your daily attendance",
                onMenuClick = onMenuClick,
                onProfileClick = onProfileClick,
                onNotificationsClick = onNotificationsClick,
                titleIcon = Icons.Default.Info,
                onTitleIconClick = { showArchiveInfo = true }
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }

        if (timetableVersions.isNotEmpty()) {
            item {
                CalendarWidget(
                    timetable = activeVersion?.entries ?: emptyList(),
                    attendanceRecords = attendanceRecords,
                    overrides = overrides,
                    selectedDate = selectedDate,
                    onDateSelected = { date -> selectedDate = date },
                    allowFutureDates = false,
                    initialMonth = calendarMonth,
                    onMonthChanged = { calendarMonth = it }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        if (timetableVersions.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val promptText = "Convert my timetable into a plain text CSV format with columns: Subject, Day, Time. Use standard Day names (Monday, Tuesday, etc.) and 12-hour time format (e.g. 09:00 AM). My timetable is: "
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB)),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("AI Prompt Assistant", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Classora AI Prompt", promptText))
                                    Toast.makeText(context, "Prompt Copied!", Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp)) }
                            }
                            Text("Copy this prompt and paste along with timetable it in ChatGPT/Gemini to get the CSV data.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        UploadFilePrompt(onSetup = onAddTimetableClick)
                    }
                }
            }
        } else {
            if (currentClass != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = Color(0xFF2196F3))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Now this ${currentClass.subject} class is going on...",
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayDate = if (SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time) == 
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) 
                        "Today's Classes" else "Classes for ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(selectedDate.time)}"
                    
                    Text("$displayDate ($dayOfWeek)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onAddTimetableClick) {
                        Icon(Icons.Default.EditCalendar, contentDescription = "Edit Timetable", tint = Color(0xFF2196F3))
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            if (selectedDayClasses.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB))) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No classes scheduled for $dayOfWeek.", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { showOverrideDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF2196F3))
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("We have classes today...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedButton(
                                onClick = onAddTaskClick, 
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = Color(0xFF2196F3))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Task instead", fontSize = 12.sp, color = Color(0xFF2196F3))
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val records = selectedDayClasses.map { AttendanceRecord(selectedDateStr, it.subject, true, activeVersion?.id) }
                                    attendancePreferences.markAllAttendance(records)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Present All", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val records = selectedDayClasses.map { AttendanceRecord(selectedDateStr, it.subject, false, activeVersion?.id) }
                                    attendancePreferences.markAllAttendance(records)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Absent All", fontSize = 12.sp)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                items(selectedDayClasses) { entry ->
                    val record = attendanceRecords.find { it.date == selectedDateStr && it.subject == entry.subject }
                    AttendanceClassItem(
                        entry = entry,
                        status = record?.isPresent,
                        onPresent = {
                            scope.launch {
                                attendancePreferences.markAttendance(AttendanceRecord(selectedDateStr, entry.subject, true, activeVersion?.id))
                            }
                        },
                        onAbsent = {
                            scope.launch {
                                attendancePreferences.markAttendance(AttendanceRecord(selectedDateStr, entry.subject, false, activeVersion?.id))
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Attendance Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D1724))
                Text("Total Subjects: ${subjectStats.size}", color = Color.Gray, fontSize = 12.sp)
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { 
            AttendanceStatsGrid(
                overallPercentage = overallPercentage,
                attended = attendanceRecords.count { it.isPresent },
                total = attendanceRecords.size
            ) 
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        if (subjectStats.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { SectionHeader(title = "Subject-wise Percentage", actionText = "") }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            items(subjectStats) { stats ->
                SubjectAttendanceItem(stats) {
                    onSubjectClick(stats.subject, activeVersion?.id ?: "null")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun UploadFilePrompt(onSetup: () -> Unit) {
    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFFF0F7FF), RoundedCornerShape(16.dp))
            .clickable { onSetup() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0xFF2196F3),
                style = stroke,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Default.AddCircleOutline,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Setup Your Timetable", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Upload CSV or Paste Text to begin", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AttendanceClassItem(
    entry: TimetableEntry,
    status: Boolean?,
    onPresent: () -> Unit,
    onAbsent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.subject, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (entry.time.isNotEmpty()) {
                    Text(entry.time, fontSize = 12.sp, color = Color.Gray)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPresent,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == true) Color(0xFF4CAF50) else Color.White,
                        contentColor = if (status == true) Color.White else Color(0xFF4CAF50)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Present", fontSize = 11.sp)
                }
                
                Button(
                    onClick = onAbsent,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == false) Color(0xFFF44336) else Color.White,
                        contentColor = if (status == false) Color.White else Color(0xFFF44336)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF44336)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Absent", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun SubjectAttendanceItem(stats: SubjectAttendance, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB)),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stats.subject, 
                    fontWeight = FontWeight.Medium, 
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${String.format("%.1f", stats.percentage)}%", 
                        fontWeight = FontWeight.Bold, 
                        color = if (stats.percentage >= 85) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ChevronRight, 
                        contentDescription = null, 
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { stats.percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (stats.percentage >= 85) Color(0xFF4CAF50) else Color(0xFFF44336),
                trackColor = Color(0xFFEEEEEE)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("${stats.presentCount}/${stats.totalCount} Classes attended", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AttendanceStatsGrid(overallPercentage: Float, attended: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Overall Attendance", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(
                        "${String.format("%.1f", overallPercentage)}%",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            if (overallPercentage >= 85) Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else Color(0xFFF44336).copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { overallPercentage / 100f },
                        modifier = Modifier.size(50.dp),
                        color = if (overallPercentage >= 85) Color(0xFF4CAF50) else Color(0xFFF44336),
                        strokeWidth = 4.dp,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Attended", color = Color.Gray, fontSize = 11.sp)
                    Text(attended.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                VerticalDivider(modifier = Modifier.height(30.dp), color = Color.White.copy(alpha = 0.1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Missed", color = Color.Gray, fontSize = 11.sp)
                    Text((total - attended).toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                VerticalDivider(modifier = Modifier.height(30.dp), color = Color.White.copy(alpha = 0.1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Total Held", color = Color.Gray, fontSize = 11.sp)
                    Text(total.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

