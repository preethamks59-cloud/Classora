package com.example.classora.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.classora.data.Task
import com.example.classora.data.TaskPreferences
import com.example.classora.data.UserData
import com.example.classora.data.UserPreferences
import com.example.classora.ui.components.ClassoraHeader
import com.example.classora.ui.components.CustomCalendarDialog
import com.example.classora.ui.components.ProfileImage
import com.example.classora.utils.NotificationScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onBack: () -> Unit,
    onTaskAdded: () -> Unit,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    initialDate: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taskPreferences = remember { TaskPreferences(context) }
    val userPreferences = remember { UserPreferences(context) }
    val userData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", ""))
    val allTasks by taskPreferences.tasksFlow.collectAsState(initial = emptyList())
    
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    var showAlarmPermissionDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var marks by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("Select time") }
    
    val today = Calendar.getInstance()
    val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val rawFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    var date by remember { 
        mutableStateOf(
            if (!initialDate.isNullOrEmpty()) {
                try {
                    displayFormat.format(rawFormat.parse(initialDate)!!)
                } catch (e: Exception) { displayFormat.format(today.time) }
            } else displayFormat.format(today.time)
        )
    }
    var rawDate by remember { mutableStateOf(initialDate ?: rawFormat.format(today.time)) } // YYYY-MM-DD
    var repeat by remember { mutableStateOf("Does not repeat") }
    var priority by remember { mutableStateOf("Medium") }
    var reminder by remember { mutableStateOf("None") }
    var attachmentName by remember { mutableStateOf("") }
    var attachmentSize by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf("") }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            attachmentUri = it.toString()
            
            // Request persistable permission so we can access the file after app restarts
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, 
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Not a document provider URI or permission already granted
            }

            // Get file name and size
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                attachmentName = cursor.getString(nameIndex)
                val sizeInBytes = cursor.getLong(sizeIndex)
                attachmentSize = if (sizeInBytes < 1024 * 1024) "${sizeInBytes / 1024} KB" else "${String.format("%.1f", sizeInBytes / (1024f * 1024f))} MB"
            }
        }
    }
    
    val timePickerState = rememberTimePickerState()
    var showTimePicker by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showRepeatMenu by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }

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
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Select Reminder Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start)
                    )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel", color = Color(0xFF2196F3))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amPm = if (timePickerState.hour < 12) "AM" else "PM"
                                val hour = if (timePickerState.hour % 12 == 0) 12 else timePickerState.hour % 12
                                time = String.format("%02d:%02d %s", hour, timePickerState.minute, amPm)
                                showTimePicker = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("OK", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        CustomCalendarDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val rawFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                date = displayFormat.format(selectedDate.time)
                rawDate = rawFormat.format(selectedDate.time)
                showDatePicker = false
            },
            allowFutureDates = true,
            tasks = allTasks
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ClassoraHeader(
            userData = userData,
            title = "Add New Task",
            subtitle = "Create a task and set reminder",
            onMenuClick = onMenuClick,
            onProfileClick = onProfileClick,
            onNotificationsClick = onNotificationsClick,
            showMenu = false,
            onBackClick = onBack
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        AddTaskField(
            icon = Icons.Default.InsertDriveFile,
            iconBg = Color(0xFFE3F2FD),
            iconTint = Color(0xFF2196F3),
            label = "Task Title",
            value = title,
            onValueChange = { if (it.length <= 100) title = it },
            placeholder = "Enter task title",
            charCount = "${title.length}/100",
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        AddTaskField(
            icon = Icons.Default.Label,
            iconBg = Color(0xFFE8F5E9),
            iconTint = Color(0xFF4CAF50),
            label = "Category / Course Code",
            value = category,
            onValueChange = { if (it.length <= 15) category = it },
            placeholder = "e.g. CSE - 201",
            charCount = "${category.length}/15",
            isOptional = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AddTaskField(
            icon = Icons.Default.Notes,
            iconBg = Color(0xFFF3E5F5),
            iconTint = Color(0xFF9C27B0),
            label = "Description",
            value = description,
            onValueChange = { if (it.length <= 500) description = it },
            placeholder = "Enter task description (optional)",
            charCount = "${description.length}/500",
            isMultiline = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        AddTaskField(
            icon = Icons.Default.Star,
            iconBg = Color(0xFFFFF3E0),
            iconTint = Color(0xFFFF9800),
            label = "Marks / Weightage",
            value = marks,
            onValueChange = { if (it.length <= 10) marks = it },
            placeholder = "e.g. 5 Marks",
            charCount = "${marks.length}/10",
            isOptional = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        ClickableTaskField(
            icon = Icons.Default.AccessTime,
            iconBg = Color(0xFFE8F5E9),
            iconTint = Color(0xFF4CAF50),
            label = "Reminder Time",
            value = time,
            onClick = { showTimePicker = true },
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            ClickableTaskField(
                icon = Icons.Outlined.Notifications,
                iconBg = Color(0xFFFFF3E0),
                iconTint = Color(0xFFFF9800),
                label = "Reminder",
                value = reminder,
                onClick = { showReminderMenu = true }
            )
            DropdownMenu(
                expanded = showReminderMenu,
                onDismissRequest = { showReminderMenu = false },
                modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
            ) {
                listOf("None", "5 mins before", "15 mins before", "30 mins before", "1 hour before").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            reminder = option
                            showReminderMenu = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ClickableTaskField(
            icon = Icons.Default.CalendarToday,
            iconBg = Color(0xFFE3F2FD),
            iconTint = Color(0xFF2196F3),
            label = "Date",
            value = date,
            onClick = { showDatePicker = true },
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3E5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Repeat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            val repeatOptions = listOf("Does not repeat", "Everyday", "Weekdays", "Weekends")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeatOptions.forEach { option ->
                    FilterChip(
                        selected = repeat == option,
                        onClick = { repeat = option },
                        label = { Text(option, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF9C27B0),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        var priorityExpanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Priority", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Box {
                Row(
                    modifier = Modifier.clickable { priorityExpanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(priority, fontSize = 14.sp, color = Color.Gray)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                }
                DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        DropdownMenuItem(text = { Text(p) }, onClick = { priority = p; priorityExpanded = false })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ClickableTaskField(
            icon = Icons.Default.AttachFile,
            iconBg = Color(0xFFE3F2FD),
            iconTint = Color(0xFF2196F3),
            label = "Attachments",
            value = if (attachmentName.isEmpty()) "Add attachment (Optional)" else attachmentName,
            onClick = {
                filePickerLauncher.launch(arrayOf("*/*"))
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (title.isNotBlank() && rawDate.isNotBlank()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        showAlarmPermissionDialog = true
                        return@Button
                    }
                    
                    scope.launch {
                        val newTask = Task(
                            title = title,
                            description = description,
                            time = time,
                            date = rawDate,
                            repeatType = repeat,
                            marks = marks,
                            priority = priority,
                            category = category,
                            reminder = reminder,
                            attachmentName = attachmentName,
                            attachmentSize = attachmentSize,
                            attachmentUri = attachmentUri
                        )
                        taskPreferences.saveTask(newTask)
                        
                        // Schedule notification
                        if (time != "Select time") {
                            NotificationScheduler.scheduleNotification(
                                context = context,
                                taskId = newTask.id,
                                title = newTask.title,
                                message = newTask.description.ifBlank { "You have a task scheduled now." },
                                date = rawDate,
                                time = time,
                                repeatType = repeat
                            )
                        }

                        onTaskAdded()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text("Create Task", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmPermissionDialog = false },
            title = { Text("Precise Alarms Required", fontWeight = FontWeight.Bold) },
            text = { Text("To send reminders at exactly the right time, Classora needs 'Alarms & Reminders' permission.") },
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

@Composable
fun AddTaskField(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    charCount: String,
    isRequired: Boolean = false,
    isOptional: Boolean = false,
    isMultiline: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Next
    )
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = buildString {
                        append(label)
                        if (isRequired) append(" *")
                        if (isOptional) append(" (Optional)")
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                minLines = if (isMultiline) 3 else 1,
                keyboardOptions = keyboardOptions,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedBorderColor = Color(0xFF2196F3)
                )
            )
            Text(
                text = charCount,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ClickableTaskField(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    onClick: () -> Unit,
    isRequired: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(label)
                        if (isRequired) append(" *")
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(value, fontSize = 14.sp, color = if (value.contains("Select")) Color.Gray else Color.Black)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
