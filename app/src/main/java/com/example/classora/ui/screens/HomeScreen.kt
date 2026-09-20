package com.example.classora.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.classora.data.*
import com.example.classora.utils.NotificationScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.classora.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit, 
    onProfileClick: () -> Unit, 
    onNotificationsClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onViewAllTasksClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    val userData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", ""))
    val taskPreferences = remember { TaskPreferences(context) }
    val allTasks by taskPreferences.tasksFlow.collectAsState(initial = emptyList())
    
    val attendancePreferences = remember { AttendancePreferences(context) }
    val timetableVersions by attendancePreferences.timetableVersionsFlow.collectAsState(initial = emptyList())
    val attendanceRecords by attendancePreferences.attendanceFlow.collectAsState(initial = emptyList())
    val overrides by attendancePreferences.overridesFlow.collectAsState(initial = emptyList())
    
    var selectedViewDate by remember { mutableStateOf(Calendar.getInstance()) }
    
    val activeVersion = remember(timetableVersions, selectedViewDate) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedViewDate.time)
        val applicable = timetableVersions.filter { it.startDate <= dateStr }
        if (applicable.isNotEmpty()) applicable.maxByOrNull { it.startDate }
        else timetableVersions.minByOrNull { it.startDate }
    }
    
    // Dynamic Greeting State
    var greeting by remember { mutableStateOf("") }
    var displayedGreeting by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val initialCalendar = Calendar.getInstance()
        val initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY)
        greeting = when {
            initialHour in 0..11 -> "Good Morning"
            initialHour in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
        displayedGreeting = greeting // Set immediately on first load

        while(true) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val newGreeting = when {
                hour in 0..11 -> "Good Morning"
                hour in 12..16 -> "Good Afternoon"
                else -> "Good Evening"
            }
            
            if (newGreeting != greeting) {
                // Erase effect
                while (displayedGreeting.isNotEmpty()) {
                    displayedGreeting = displayedGreeting.dropLast(1)
                    delay(50)
                }
                
                greeting = newGreeting
                
                // Typewriter effect
                greeting.forEach { char ->
                    displayedGreeting += char
                    delay(100)
                }
            }
            delay(60000) // Check every minute
        }
    }

    val filteredTasks = allTasks.filter { isTaskOnDate(it, selectedViewDate) && !it.isCompleted }

    var selectedTaskForDetails by remember { mutableStateOf<Task?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet && selectedTaskForDetails != null) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Dialog(
            onDismissRequest = { showSheet = false },
            properties = DialogProperties(
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
                        isUpcoming = selectedTaskForDetails!!.date > todayStr,
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
                title = "$displayedGreeting,",
                subtitle = "${userData.name.ifEmpty { "Student" }}! 👋",
                onMenuClick = onMenuClick,
                onProfileClick = onProfileClick,
                onNotificationsClick = onNotificationsClick
            )
        }
        item {
            Text(
                text = "Stay organized. Stay ahead.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
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
                    "Today's Schedule" else "Schedule for ${displayFormat.format(selectedViewDate.time)}", 
                actionText = "View All",
                onActionClick = onViewAllTasksClick
            ) 
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        
        if (filteredTasks.isEmpty()) {
            item {
                val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(selectedViewDate.time)
                val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(selectedViewDate.time)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .clickable { onAddTaskClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "there is no task on $dateStr $dayStr you can add now...",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(items = filteredTasks) { task ->
                ScheduleTaskItem(
                    task = task,
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
fun ScheduleTaskItem(task: Task, onClick: () -> Unit) {
    val priority = task.priority ?: "Medium"
    val statusColor = when(priority) {
        "High" -> Color(0xFFFF9800)
        "Medium" -> Color(0xFF2196F3)
        "Low" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(task.time, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Reminder", fontSize = 8.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            VerticalDivider(modifier = Modifier.height(40.dp), color = Color.LightGray)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.title ?: "Untitled Task", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (!task.attachmentName.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.AttachFile, 
                            contentDescription = "Has Attachment", 
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                    }
                }
                Text((task.description ?: "").ifEmpty { "No description" }, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
            val priority = task.priority ?: "Medium"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(priority, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        onMenuClick = {}, 
        onProfileClick = {}, 
        onNotificationsClick = {},
        onAddTaskClick = {},
        onViewAllTasksClick = {}
    )
}
