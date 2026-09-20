package com.example.classora.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.classora.data.AttendancePreferences
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import com.example.classora.data.InfoNotification
import com.example.classora.data.InfoPreferences
import com.example.classora.data.NotificationPreferences
import com.example.classora.data.Task
import com.example.classora.data.TaskPreferences
import com.example.classora.data.UserPreferences
import androidx.activity.compose.BackHandler
import com.example.classora.ui.components.CustomCalendarDialog
import com.example.classora.utils.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

enum class NotificationFilter(val label: String) {
    ALL("All"),
    CALENDAR("Calendar"),
    INFO("Info"),
    NOTIFIED("Notified"),
    UPCOMING("Upcoming"),
    LATER("Later"),
    COMPLETED("Completed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, rescheduleTaskId: String? = null) {
    BackHandler {
        onBack()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val userPreferences = remember { UserPreferences(context) }
    val notificationPreferences = remember { NotificationPreferences(context) }
    val taskPreferences = remember { TaskPreferences(context) }
    val attendancePreferences = remember { AttendancePreferences(context) }
    val infoPreferences = remember { InfoPreferences(context) }
    
    val allTasks by taskPreferences.tasksFlow.collectAsState(initial = emptyList())
    val attendanceRecords by attendancePreferences.attendanceFlow.collectAsState(initial = emptyList())
    val allInfo by infoPreferences.infoFlow.collectAsState(initial = emptyList())

    var isFetchingInfo by remember { mutableStateOf(false) }
    var networkError by remember { mutableStateOf<String?>(null) }

    val overallPercentage = if (attendanceRecords.isNotEmpty()) {
        (attendanceRecords.count { it.isPresent }.toFloat() / attendanceRecords.size) * 100
    } else 100f

    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedTaskToReschedule by remember { mutableStateOf<Task?>(null) }
    var newDate by remember { mutableStateOf("") }
    var rawNewDate by remember { mutableStateOf("") }
    
    val timePickerState = rememberTimePickerState()

    // Handle deep link reschedule
    LaunchedEffect(rescheduleTaskId, allTasks) {
        if (rescheduleTaskId != null && allTasks.isNotEmpty()) {
            val task = allTasks.find { it.id == rescheduleTaskId }
            if (task != null && !task.isCompleted && selectedTaskToReschedule == null) {
                selectedTaskToReschedule = task
                showDatePicker = true
            }
        }
    }

    // Fetch Info from Sheet trigger (polling moved to MainScreen for background support)
    LaunchedEffect(selectedFilter) {
        if (selectedFilter == NotificationFilter.INFO || selectedFilter == NotificationFilter.ALL) {
            if (allInfo.isEmpty()) {
                isFetchingInfo = true
                delay(2000) // Give background process some time
                isFetchingInfo = false
            }
        }
    }

    // Clear unread status when entering the screen
    LaunchedEffect(Unit) {
        userPreferences.setHasUnreadNotifications(false)
        notificationPreferences.clearAll() 
    }

    if (showDatePicker) {
        CustomCalendarDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { cal ->
                val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val rawFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                newDate = displayFormat.format(cal.time)
                rawNewDate = rawFormat.format(cal.time)
                showDatePicker = false
                showTimePicker = true
            },
            allowFutureDates = true,
            tasks = allTasks
        )
    }

    if (showTimePicker) {
        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f).padding(8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select New Time", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(20.dp))
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = Color(0xFFF5F7F9),
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = Color(0xFF0D1724),
                            selectorColor = Color(0xFF2196F3),
                            periodSelectorSelectedContainerColor = Color(0xFF2196F3),
                            periodSelectorSelectedContentColor = Color.White,
                            periodSelectorUnselectedContainerColor = Color(0xFFF5F7F9),
                            periodSelectorUnselectedContentColor = Color(0xFF0D1724),
                            periodSelectorBorderColor = Color(0xFFE0E0E0),
                            timeSelectorSelectedContainerColor = Color(0xFFE3F2FD),
                            timeSelectorSelectedContentColor = Color(0xFF2196F3),
                            timeSelectorUnselectedContainerColor = Color(0xFFF5F7F9),
                            timeSelectorUnselectedContentColor = Color(0xFF0D1724)
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        Button(onClick = {
                            val amPm = if (timePickerState.hour < 12) "AM" else "PM"
                            val hour = if (timePickerState.hour % 12 == 0) 12 else timePickerState.hour % 12
                            val newTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour, timePickerState.minute, amPm)
                            
                            selectedTaskToReschedule?.let { task ->
                                scope.launch {
                                    val updatedTask = task.copy(date = rawNewDate, time = newTime, isCompleted = false)
                                    taskPreferences.saveTask(updatedTask)
                                    NotificationScheduler.scheduleNotification(
                                        context, updatedTask.id, updatedTask.title, updatedTask.description,
                                        updatedTask.date, updatedTask.time, updatedTask.repeatType
                                    )
                                    showTimePicker = false
                                    selectedTaskToReschedule = null
                                }
                            }
                        }) { Text("Confirm") }
                    }
                }
            }
        }
    }

    var showSlogan by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    action = {
                        TextButton(
                            onClick = { data.performAction() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6259A8))
                        ) {
                            Text(data.visuals.actionLabel ?: "", fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFFF5F7F9),
                    contentColor = Color(0xFF0D1724),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(data.visuals.message)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Custom Header instead of TopAppBar to avoid double inset padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 4.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0D1724))
                }
                Text(
                    text = "Notifications",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1724),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (showSlogan) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color(0xFFFFF9C4), // Light yellow/gold background
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFF6D00), // Vibrant orange
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Real-time updates are enabled. You'll receive important info instantly.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE65100), // Deep orange
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showSlogan = false },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFE65100).copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Horizontal Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6259A8),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF5F7F9),
                            labelColor = Color.Gray
                        ),
                        border = null,
                        shape = RoundedCornerShape(100.dp)
                    )
                }
            }

            val now = Calendar.getInstance().time
            val sdfFull = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
            
            val upcomingThreshold = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 4) }.time
            
            val filteredTasks = allTasks.filter { task ->
                val taskDate = try { sdfFull.parse("${task.date} ${task.time}") } catch (e: Exception) { null }
                val isImported = task.category == "Imported"
                
                when (selectedFilter) {
                    NotificationFilter.ALL -> !task.isCompleted && !isImported 
                    NotificationFilter.CALENDAR -> isImported
                    NotificationFilter.NOTIFIED -> !task.isCompleted && taskDate != null && taskDate.before(now)
                    NotificationFilter.UPCOMING -> !task.isCompleted && taskDate != null && taskDate.after(now) && taskDate.before(upcomingThreshold)
                    NotificationFilter.LATER -> !task.isCompleted && taskDate != null && taskDate.after(upcomingThreshold)
                    NotificationFilter.COMPLETED -> task.isCompleted
                    NotificationFilter.INFO -> false
                }
            }

            if (selectedFilter == NotificationFilter.INFO) {
                if (allInfo.isEmpty()) {
                    EmptyState(
                        message = if (isFetchingInfo) "Fetching updates..." else networkError ?: "No updates found",
                        subMessage = if (isFetchingInfo) "Please wait while we sync the latest information." else "Check back later for new announcements and updates."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(allInfo, key = { "info_${it.id}" }) { info ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                        scope.launch {
                                            val deletedItem = info
                                            infoPreferences.deleteInfo(info.id)
                                            val result = snackbarHostState.showSnackbar(
                                                message = "info deleted",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                infoPreferences.restoreInfo(deletedItem.id)
                                            }
                                        }
                                        true
                                    } else false
                                }
                            )
                            
                            LaunchedEffect(info.id) {
                                if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                    dismissState.reset()
                                }
                            }

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color.Red).padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color.White)
                                    }
                                }
                            ) {
                                InfoItemCard(info)
                            }
                        }
                    }
                }
            } else if (filteredTasks.isEmpty()) {
                EmptyState(
                    message = when (selectedFilter) {
                        NotificationFilter.NOTIFIED -> "No recent notifications"
                        NotificationFilter.UPCOMING -> "No upcoming tasks"
                        NotificationFilter.LATER -> "Nothing scheduled for later"
                        NotificationFilter.COMPLETED -> "No completed tasks"
                        NotificationFilter.CALENDAR -> "No imported events"
                        else -> "No notifications yet"
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        // Add a side-effect to reset the state if the task is re-inserted (e.g. after UNDO)
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        // Delete Task
                                        scope.launch {
                                            try {
                                                val deletedTask = task
                                                taskPreferences.deleteTask(task.id)
                                                NotificationScheduler.cancelNotification(context, task.id)
                                                
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "message is deleted",
                                                    actionLabel = "UNDO",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    taskPreferences.saveTask(deletedTask)
                                                    NotificationScheduler.scheduleNotification(
                                                        context, deletedTask.id, deletedTask.title, deletedTask.description,
                                                        deletedTask.date, deletedTask.time, deletedTask.repeatType
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        true
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        // Complete Task
                                        if (!task.isCompleted) {
                                            scope.launch {
                                                try {
                                                    val originalTask = task
                                                    taskPreferences.saveTask(task.copy(isCompleted = true))
                                                    NotificationScheduler.cancelNotification(context, task.id)
                                                    
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "message is marked as completed",
                                                        actionLabel = "UNDO",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        taskPreferences.saveTask(originalTask)
                                                        NotificationScheduler.scheduleNotification(
                                                            context, originalTask.id, originalTask.title, originalTask.description,
                                                            originalTask.date, originalTask.time, originalTask.repeatType
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                            true
                                        } else false
                                    }
                                    else -> false
                                }
                            }
                        )

                        // Reset dismiss state if it's NOT settled
                        // This fixes the "green/red box staying" bug on re-insertion (UNDO) or property update (Mark Complete)
                        LaunchedEffect(task.id, task.isCompleted) {
                            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                dismissState.reset()
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val color by animateColorAsState(
                                    when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Color.Red
                                        SwipeToDismissBoxValue.EndToStart -> Color(0xFF4CAF50)
                                        else -> Color.Transparent
                                    }, label = "bg_color"
                                )
                                val alignment = when (direction) {
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                }
                                val icon = when (direction) {
                                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.CheckCircle
                                    else -> null
                                }
                                val scale by animateFloatAsState(
                                    if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) 1.2f else 1f, label = "icon_scale"
                                )

                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(color)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = alignment
                                ) {
                                    icon?.let {
                                        Icon(
                                            it, 
                                            contentDescription = null, 
                                            tint = Color.White, 
                                            modifier = Modifier.scale(scale).size(28.dp)
                                        )
                                    }
                                }
                            }
                        ) {
                            TaskItemCard(
                                task = task,
                                onComplete = {
                                    if (!task.isCompleted) {
                                        scope.launch {
                                            try {
                                                val originalTask = task
                                                taskPreferences.saveTask(task.copy(isCompleted = true))
                                                NotificationScheduler.cancelNotification(context, task.id)
                                                
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "message is marked as completed",
                                                    actionLabel = "UNDO",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    taskPreferences.saveTask(originalTask)
                                                    NotificationScheduler.scheduleNotification(
                                                        context, originalTask.id, originalTask.title, originalTask.description,
                                                        originalTask.date, originalTask.time, originalTask.repeatType
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                },
                                onCalendar = {
                                    if (!task.isCompleted) {
                                        selectedTaskToReschedule = task
                                        showDatePicker = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TaskItemCard(
    task: Task,
    onComplete: () -> Unit,
    onCalendar: () -> Unit
) {
    val (icon, bgColor, iconTint) = getTaskVisuals(task)
    var isExpanded by remember { mutableStateOf(false) }
    val showReadMore = task.description.length > 100
    
    // Formatting date and time in a single string: "20 May 2025 • 11:59 PM"
    val displayDateTime = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(task.date)
        val formattedDate = if (date != null) outputFormat.format(date) else task.date
        "$formattedDate • ${task.time}"
    } catch (e: Exception) {
        "${task.date} • ${task.time}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0D1724)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isExpanded || !showReadMore) task.description else task.description.take(100) + "...",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )
                        
                        if (showReadMore) {
                            TextButton(
                                onClick = { isExpanded = !isExpanded },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "Read Less" else "Read More",
                                    color = Color(0xFF2196F3),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF6259A8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                displayDateTime,
                                fontSize = 12.sp,
                                color = Color(0xFF6259A8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                if (task.marks.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = task.marks,
                            color = Color(0xFFE57373),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            if (!task.isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .clickable { onComplete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Complete", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE3F2FD))
                            .clickable { onCalendar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

fun getTaskVisuals(task: Task): Triple<ImageVector, Color, Color> {
    val title = task.title.lowercase()
    return when {
        title.contains("lab") || title.contains("chemistry") || title.contains("physics") -> 
            Triple(Icons.Default.Science, Color(0xFFFFF8E1), Color(0xFFFFC107))
        title.contains("quiz") || title.contains("operating system") -> 
            Triple(Icons.Default.MenuBook, Color(0xFFFFEBEE), Color(0xFFF06292))
        title.contains("code") || title.contains("program") || title.contains("java") -> 
            Triple(Icons.Default.Code, Color(0xFFE3F2FD), Color(0xFF2196F3))
        title.contains("presentation") || title.contains("dbms") -> 
            Triple(Icons.Default.CoPresent, Color(0xFFE8F5E9), Color(0xFF66BB6A))
        else -> 
            Triple(Icons.Default.Description, Color(0xFFF3E5F5), Color(0xFF9C27B0))
    }
}

@Composable
fun InfoItemCard(info: InfoNotification) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val showReadMore = info.message.length > 150

    val dateTimeStr = remember(info.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(info.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        info.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0D1724)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // The description is now more prominent
                    Text(
                        text = if (isExpanded || !showReadMore) info.message else info.message.take(150) + "...",
                        fontSize = 13.sp,
                        color = Color(0xFF424242), // Slightly darker gray for better readability
                        lineHeight = 20.sp
                    )
                    
                    if (showReadMore) {
                        TextButton(
                            onClick = { isExpanded = !isExpanded },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "Read Less" else "Read More",
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Instead of a simple timestamp, we display it as the "place" where metadata lives
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        dateTimeStr,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (info.link.isNotEmpty() && !info.link.contains("about:blank", ignoreCase = true)) {
                    TextButton(
                        onClick = {
                            var url = info.link.trim()
                            if (url.isNotEmpty()) {
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    url = "https://$url"
                                }
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                        setPackage("com.android.chrome")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        Toast.makeText(context, "Invalid link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Open Link", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(14.dp), tint = Color(0xFF2196F3))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String = "No notifications yet", subMessage: String = "Stay tuned! We'll notify you about your upcoming tasks and schedule.") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F7F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.LightGray
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            message,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D1724)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            subMessage,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
