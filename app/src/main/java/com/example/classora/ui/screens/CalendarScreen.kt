package com.example.classora.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.classora.data.*
import com.example.classora.ui.components.*
import com.example.classora.utils.NotificationScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onMenuClick: () -> Unit, 
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAddTaskClick: (Calendar) -> Unit,
    onViewAllTasksClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    val taskPreferences = remember { TaskPreferences(context) }
    val attendancePreferences = remember { AttendancePreferences(context) }
    
    val userData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", ""))
    val allTasks by taskPreferences.tasksFlow.collectAsState(initial = emptyList())
    val timetableVersions by attendancePreferences.timetableVersionsFlow.collectAsState(initial = emptyList())
    val attendanceRecords by attendancePreferences.attendanceFlow.collectAsState(initial = emptyList())
    val overrides by attendancePreferences.overridesFlow.collectAsState(initial = emptyList())
    
    var selectedViewDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showOverrideDialog by remember { mutableStateOf(false) }

    val activeVersion = remember(timetableVersions, selectedViewDate) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedViewDate.time)
        val applicable = timetableVersions.filter { it.startDate <= dateStr }
        if (applicable.isNotEmpty()) applicable.maxByOrNull { it.startDate }
        else timetableVersions.minByOrNull { it.startDate }
    }

    val selectedDateOverride = overrides.find { 
        it.date == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedViewDate.time) 
    }
    
    val dayToQuery = selectedDateOverride?.sourceDay ?: SimpleDateFormat("EEEE", Locale.US).format(selectedViewDate.time)

    val selectedDayClasses = activeVersion?.entries?.filter { entry ->
        val dayInput = entry.day.lowercase()
        val query = dayToQuery.lowercase()
        dayInput.contains(query) || (query.length >= 3 && dayInput.contains(query.take(3)))
    } ?: emptyList()
    
    val filteredTasks = allTasks.filter { isTaskOnDate(it, selectedViewDate) && !it.isCompleted }
    val upcomingTasks = allTasks.filter { 
        val dateStr = it.date ?: ""
        val taskCal = Calendar.getInstance().apply {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } else {
                set(2000, 0, 1) // Old invalid date
            }
        }
        taskCal.after(Calendar.getInstance()) && !it.isCompleted && it.category != "Imported"
    }

    var selectedTaskForDetails by remember { mutableStateOf<Task?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet && selectedTaskForDetails != null) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Dialog(
            onDismissRequest = { showSheet = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Custom Header for Full Screen Dialog
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                        Text(
                            "Task Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    TaskDetailsContent(
                        task = selectedTaskForDetails!!,
                        isUpcoming = (selectedTaskForDetails!!.date ?: "") > todayStr,
                        onClose = { showSheet = false },
                        onComplete = {
                            val task = selectedTaskForDetails
                            if (task != null && !task.isCompleted) {
                                scope.launch {
                                    try {
                                        taskPreferences.saveTask(task.copy(isCompleted = true))
                                        NotificationScheduler.cancelNotification(context, task.id)
                                        showSheet = false
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        onDelete = {
                            val task = selectedTaskForDetails
                            if (task != null) {
                                scope.launch {
                                    taskPreferences.deleteTask(task.id)
                                    NotificationScheduler.cancelNotification(context, task.id)
                                    showSheet = false
                                }
                            }
                        },
                        onReschedule = {
                            showSheet = false
                        }
                    )
                }
            }
        }
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
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedViewDate.time)
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
                title = "Calendar",
                subtitle = "Plan tasks & set reminders",
                onMenuClick = onMenuClick,
                onProfileClick = onProfileClick,
                onNotificationsClick = onNotificationsClick
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { 
            CalendarWidget(
                tasks = allTasks,
                timetable = activeVersion?.entries ?: emptyList(),
                attendanceRecords = attendanceRecords,
                overrides = overrides,
                selectedDate = selectedViewDate,
                onDateSelected = { date -> selectedViewDate = date },
                showAttendanceDots = false
            ) 
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { 
            val displayFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
            SectionHeader(
                title = if (SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedViewDate.time) == 
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) 
                    "Today's Tasks" else "Tasks for ${displayFormat.format(selectedViewDate.time)}", 
                actionText = "View All", 
                onActionClick = onViewAllTasksClick
            ) 
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }

        if (selectedDayClasses.isEmpty()) {
            item {
                Button(
                    onClick = { showOverrideDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F7F9), contentColor = Color(0xFF2196F3))
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("We have classes...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            item {
                Text(
                    "Classes scheduled: ${selectedDayClasses.size}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        
        if (filteredTasks.isEmpty()) {
            item {
                Text(
                    "No tasks for this date. Start planning!",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(filteredTasks) { task ->
                TaskItem(
                    task = task,
                    onDelete = { 
                        scope.launch { 
                            taskPreferences.deleteTask(task.id)
                            NotificationScheduler.cancelNotification(context, task.id)
                        } 
                    },
                    onComplete = { 
                        if (!task.isCompleted) {
                            scope.launch { 
                                taskPreferences.saveTask(task.copy(isCompleted = true))
                                NotificationScheduler.cancelNotification(context, task.id)
                            } 
                        }
                    },
                    onClick = {
                        selectedTaskForDetails = task
                        showSheet = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        item { 
            Box(modifier = Modifier.clickable { onAddTaskClick(selectedViewDate) }) {
                AddTaskCard() 
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { SectionHeader(title = "Upcoming Reminders", actionText = "") }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        
        if (upcomingTasks.isEmpty()) {
            item {
                ReminderItem("No upcoming tasks", "Plan ahead to stay organized", "", onClick = {})
            }
        } else {
            items(upcomingTasks) { task ->
                val dateDisplay = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                    outputFormat.format(inputFormat.parse(task.date ?: "")!!)
                } catch (e: Exception) { task.date ?: "" }
                
                ReminderItem(
                    time = task.time ?: "", 
                    title = task.title ?: "Untitled Task",
                    sub = dateDisplay,
                    onClick = {
                        selectedTaskForDetails = task
                        showSheet = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun AddTaskCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFFF0F7FF), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2196F3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Add Task", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2196F3))
                Text("Plan your study schedule", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
