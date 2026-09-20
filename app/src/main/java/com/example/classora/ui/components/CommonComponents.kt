package com.example.classora.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.classora.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SectionHeader(title: String, actionText: String, onActionClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D1724))
        if (actionText.isNotEmpty()) {
            Text(
                text = actionText, 
                color = Color(0xFF2196F3), 
                fontSize = 12.sp,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun ClassoraHeader(
    userData: UserData,
    title: String,
    subtitle: String,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    showMenu: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    titleIcon: ImageVector? = null,
    onTitleIconClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Menu/Back on Left
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                if (showMenu) {
                    IconButton(onClick = onMenuClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                } else if (onBackClick != null) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            }

            // App Name in Middle
            Text(
                text = "Classora",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D1724)
            )

            // Icons on Right
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.clickable { onNotificationsClick() }) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        modifier = Modifier.size(26.dp),
                        tint = Color(0xFF0D1724)
                    )
                    if (userData.hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE57373))
                                .align(Alignment.TopEnd)
                                .offset(x = 1.dp, y = (-1).dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                ProfileImage(
                    uriString = userData.profileImageUri,
                    userName = userData.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onProfileClick() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D1724)
            )
            if (titleIcon != null) {
                IconButton(onClick = { onTitleIconClick?.invoke() }, modifier = Modifier.size(28.dp)) {
                    Icon(titleIcon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ProfileImage(
    userData: UserData? = null, // Kept for consistency if needed, but unused now
    uriString: String? = null,
    userName: String = "",
    modifier: Modifier = Modifier,
    placeholderColor: Color = Color(0xFFE3F2FD),
    fontSize: TextUnit = 16.sp
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        if (!uriString.isNullOrEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(uriString),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (userName.isNotEmpty()) {
            Text(
                text = userName.take(1).uppercase(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = "Default Profile",
                modifier = Modifier.fillMaxSize().padding(8.dp),
                tint = Color.DarkGray
            )
        }
    }
}

@Composable
fun CalendarWidget(
    tasks: List<Task> = emptyList(),
    timetable: List<TimetableEntry> = emptyList(),
    attendanceRecords: List<AttendanceRecord> = emptyList(),
    overrides: List<TimetableOverride> = emptyList(),
    selectedDate: Calendar = Calendar.getInstance(),
    onDateSelected: (Calendar) -> Unit = {},
    allowFutureDates: Boolean = true,
    initialMonth: Calendar? = null,
    onMonthChanged: (Calendar) -> Unit = {},
    showAttendanceDots: Boolean = true
) {
    var currentMonth by remember { mutableStateOf(initialMonth ?: (selectedDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    
    // Sync internal month state if selectedDate changes significantly or initialMonth is provided
    LaunchedEffect(initialMonth, selectedDate) {
        if (initialMonth != null) {
            currentMonth = initialMonth
        }
    }
    var showYearPicker by remember { mutableStateOf(false) }

    val monthFormatter = SimpleDateFormat("MMMM", Locale.getDefault())
    val yearFormatter = SimpleDateFormat("yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showYearPicker = !showYearPicker }
                ) {
                    Text(
                        text = "${monthFormatter.format(currentMonth.time)} ${yearFormatter.format(currentMonth.time)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Year",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    DropdownMenu(
                        expanded = showYearPicker,
                        onDismissRequest = { showYearPicker = false },
                        modifier = Modifier.heightIn(max = 200.dp).background(Color(0xFF1A2637))
                    ) {
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        (currentYear + 5 downTo currentYear - 90).forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString(), color = Color.White) },
                                onClick = {
                                    val newMonth = currentMonth.clone() as Calendar
                                    newMonth.set(Calendar.YEAR, year)
                                    currentMonth = newMonth
                                    showYearPicker = false
                                    onMonthChanged(newMonth)
                                }
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = {
                        val newMonth = currentMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, -1)
                        currentMonth = newMonth
                        onMonthChanged(newMonth)
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val newMonth = currentMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, 1)
                        currentMonth = newMonth
                        onMonthChanged(newMonth)
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val daysOfWeek = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.width(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val calendar = currentMonth.clone() as Calendar
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val prevMonthCalendar = currentMonth.clone() as Calendar
            prevMonthCalendar.add(Calendar.MONTH, -1)
            val daysInPrevMonth = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val days = mutableListOf<CalendarDay>()
            
            // Prev month padding
            for (i in 0 until firstDayOfWeek) {
                days.add(CalendarDay((daysInPrevMonth - firstDayOfWeek + i + 1).toString(), false))
            }
            
            // Current month days
            for (i in 1..daysInMonth) {
                val isSelected = selectedDate.get(Calendar.DAY_OF_MONTH) == i &&
                                 selectedDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                                 selectedDate.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)
                days.add(CalendarDay(i.toString(), true, isSelected))
            }
            
            // Next month padding
            var nextMonthDay = 1
            while (days.size % 7 != 0) {
                days.add(CalendarDay(nextMonthDay.toString(), false))
                nextMonthDay++
            }
            
            val rows = days.chunked(7)
            
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { day ->
                        val isToday = day.isCurrentMonth && 
                                      day.text == Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString() &&
                                      currentMonth.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) &&
                                      currentMonth.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)

                        val dayCal = currentMonth.clone() as Calendar
                        if (day.isCurrentMonth) {
                            dayCal.set(Calendar.DAY_OF_MONTH, day.text.toInt())
                        }
                        
                        val dayTasks = if (day.isCurrentMonth) {
                            tasks.filter { isTaskOnDate(it, dayCal) }
                        } else emptyList()

                        // Attendance Dot Logic
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayCal.time)
                        val override = overrides.find { it.date == dateStr }
                        val dayOfWeekStr = override?.sourceDay ?: SimpleDateFormat("EEEE", Locale.getDefault()).format(dayCal.time)
                        val dayClasses = timetable.filter { entry ->
                            val dayInput = entry.day.lowercase()
                            val query = dayOfWeekStr.lowercase()
                            dayInput.contains(query) || (query.length >= 3 && dayInput.contains(query.take(3)))
                        }
                        val dayRecords = attendanceRecords.filter { it.date == dateStr }
                        
                        val attendanceDotColor = if (showAttendanceDots && day.isCurrentMonth && dayClasses.isNotEmpty()) {
                            val totalClasses = dayClasses.size
                            val markedRecords = dayRecords.filter { record -> 
                                dayClasses.any { it.subject == record.subject }
                            }
                            
                            if (markedRecords.size == totalClasses) {
                                val presentCount = markedRecords.count { it.isPresent }
                                val absentCount = totalClasses - presentCount
                                
                                when {
                                    presentCount == totalClasses -> Color(0xFF4CAF50) // Green: All Present
                                    absentCount == totalClasses -> Color(0xFFF44336) // Red: All Absent
                                    absentCount.toFloat() / totalClasses > 0.5f -> Color(0xFFFFEB3B) // Yellow: > 50% Absent
                                    absentCount > 0 -> Color(0xFF2196F3) // Blue: 1 or 2 (<= 50%) Absent
                                    else -> Color.Transparent
                                }
                            } else Color.Transparent
                        } else Color.Transparent

                        val taskDotColor = if (dayTasks.isNotEmpty()) {
                            val isMulti = dayTasks.size >= 2
                            if (isMulti) Color.Red
                            else when (dayTasks.first().priority) {
                                "High" -> Color(0xFFFFEB3B) // Yellow
                                "Medium" -> Color(0xFF2196F3) // Blue
                                "Low" -> Color(0xFF4CAF50) // Green
                                else -> Color.Transparent
                            }
                        } else Color.Transparent

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day.isSelected -> Color(0xFF2196F3)
                                        isToday -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(enabled = day.isCurrentMonth) {
                                    val newSelected = currentMonth.clone() as Calendar
                                    newSelected.set(Calendar.DAY_OF_MONTH, day.text.toInt())
                                    
                                    if (allowFutureDates || !newSelected.after(Calendar.getInstance())) {
                                        onDateSelected(newSelected)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val isFuture = !allowFutureDates && day.isCurrentMonth && currentMonth.clone().let {
                                (it as Calendar).set(Calendar.DAY_OF_MONTH, day.text.toInt())
                                it.after(Calendar.getInstance())
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.text,
                                    color = when {
                                        day.isSelected -> Color.White
                                        day.isCurrentMonth && !isFuture -> Color.White
                                        isFuture -> Color.White.copy(alpha = 0.2f)
                                        else -> Color.Gray.copy(alpha = 0.5f)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (day.isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                                
                                Row(horizontalArrangement = Arrangement.Center) {
                                    if (attendanceDotColor != Color.Transparent) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(attendanceDotColor)
                                        )
                                    }
                                    if (taskDotColor != Color.Transparent) {
                                        if (attendanceDotColor != Color.Transparent) Spacer(modifier = Modifier.width(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(taskDotColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CalendarDay(val text: String, val isCurrentMonth: Boolean, val isSelected: Boolean = false)

fun isTaskOnDate(task: Task, date: Calendar): Boolean {
    val taskDate = Calendar.getInstance().apply {
        val dateStr = task.date
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    
    val checkDate = date.clone() as Calendar
    checkDate.set(Calendar.HOUR_OF_DAY, 0)
    checkDate.set(Calendar.MINUTE, 0)
    checkDate.set(Calendar.SECOND, 0)
    checkDate.set(Calendar.MILLISECOND, 0)
    
    if (checkDate.before(taskDate)) return false
    
    return when (task.repeatType) {
        "Does not repeat" -> {
            checkDate.get(Calendar.YEAR) == taskDate.get(Calendar.YEAR) &&
            checkDate.get(Calendar.MONTH) == taskDate.get(Calendar.MONTH) &&
            checkDate.get(Calendar.DAY_OF_MONTH) == taskDate.get(Calendar.DAY_OF_MONTH)
        }
        "Everyday" -> true
        "Weekdays" -> {
            val dow = checkDate.get(Calendar.DAY_OF_WEEK)
            dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
        }
        "Weekends" -> {
            val dow = checkDate.get(Calendar.DAY_OF_WEEK)
            dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
        }
        else -> false
    }
}

@Composable
fun TaskDetailsContent(
    task: Task,
    isUpcoming: Boolean,
    onClose: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit = {},
    onReschedule: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF7E57C2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.ifEmpty { "Untitled Task" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1724),
                    lineHeight = 24.sp
                )
                Text(
                    text = task.category.ifEmpty { "General Task" },
                    fontSize = 14.sp,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isUpcoming) Color(0xFFE3F2FD) else Color(0xFFE8F5E9)
            ) {
                Text(
                    text = if (isUpcoming) "Upcoming" else "Completed",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = if (isUpcoming) Color(0xFF2196F3) else Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Info Row (Date, Time, Reminder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoRowItem(Icons.Default.CalendarToday, formatDateForDisplay(task.date), Modifier.weight(1f))
            VerticalDivider(modifier = Modifier.padding(vertical = 4.dp).width(1.dp), color = Color.LightGray.copy(alpha = 0.4f))
            InfoRowItem(Icons.Default.AccessTime, task.time, Modifier.weight(1f))
            VerticalDivider(modifier = Modifier.padding(vertical = 4.dp).width(1.dp), color = Color.LightGray.copy(alpha = 0.4f))
            InfoRowItem(Icons.Outlined.Notifications, task.reminder.ifEmpty { "No reminder" }, Modifier.weight(1.2f))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Description
        Text("Description", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0D1724))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = task.description.ifEmpty { "No description provided for this task." },
            fontSize = 13.sp,
            color = Color.Gray,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Priority & Status Row
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Priority", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0D1724))
                Spacer(modifier = Modifier.height(10.dp))
                val priority = task.priority
                StatusPill(
                    label = priority,
                    dotColor = when (priority) {
                        "High" -> Color(0xFF7E57C2)
                        "Medium" -> Color(0xFF2196F3)
                        else -> Color(0xFF4CAF50)
                    },
                    backgroundColor = when (priority) {
                        "High" -> Color(0xFFF3E5F5)
                        "Medium" -> Color(0xFFE3F2FD)
                        else -> Color(0xFFE8F5E9)
                    }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Status", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0D1724))
                Spacer(modifier = Modifier.height(10.dp))
                StatusPill(
                    label = if (task.isCompleted) "Completed" else "Not Completed",
                    dotColor = if (task.isCompleted) Color(0xFF4CAF50) else Color(0xFF2196F3),
                    backgroundColor = if (task.isCompleted) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Attachments
        if (task.attachmentName.isNotEmpty()) {
            Text("Attachments", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0D1724))
            Spacer(modifier = Modifier.height(10.dp))
            AttachmentCard(task.attachmentName, task.attachmentSize, task.attachmentUri)
            Spacer(modifier = Modifier.height(28.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom Button
        Button(
            onClick = {
                if (!task.isCompleted) onComplete() else onClose()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text(
                text = if (task.isCompleted) "Done" else "Mark as Completed",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun InfoRowItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
    }
}

@Composable
fun StatusPill(label: String, dotColor: Color, backgroundColor: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = dotColor)
        }
    }
}

@Composable
fun AttachmentCard(name: String, size: String, uriString: String = "") {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (uriString.isNotEmpty()) {
                try {
                    val uri = Uri.parse(uriString)
                    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Cannot open file. Please ensure you have a suitable app (like Google Drive) installed.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFE57373))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(size, fontSize = 11.sp, color = Color.Gray)
            }
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

fun formatDateForDisplay(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        parser.parse(dateStr)?.let { formatter.format(it) } ?: dateStr
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
fun TaskItem(task: Task, onDelete: () -> Unit, onComplete: () -> Unit, onClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2196F3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Book, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.time, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (task.attachmentUri.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    }
                }
                Text(task.description, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
            }
            IconButton(onClick = onComplete) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Complete", tint = Color.LightGray)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = { 
                            onDelete()
                            showMenu = false 
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderItem(time: String, title: String, sub: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF2196F3))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(time, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(sub, fontSize = 10.sp, color = Color(0xFF2196F3))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE8F5E9))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Today", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomCalendarDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Calendar) -> Unit,
    allowFutureDates: Boolean = false,
    tasks: List<Task> = emptyList(),
    initialMonth: Calendar? = null,
    onMonthChanged: (Calendar) -> Unit = {},
    multiSelectEnabled: Boolean = false,
    selectedDates: Set<String> = emptySet(),
    onDateLongClicked: (String) -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            CustomCalendarWidget(
                onDateSelected = onDateSelected,
                onClose = onDismiss,
                allowFutureDates = allowFutureDates,
                tasks = tasks,
                initialMonth = initialMonth,
                onMonthChanged = onMonthChanged,
                multiSelectEnabled = multiSelectEnabled,
                selectedDates = selectedDates,
                onDateLongClicked = onDateLongClicked,
                onConfirm = onConfirm
            )
        }
    }
}

@Composable
fun CustomCalendarWidget(
    onDateSelected: (Calendar) -> Unit,
    onClose: () -> Unit,
    allowFutureDates: Boolean = false,
    tasks: List<Task> = emptyList(),
    initialMonth: Calendar? = null,
    onMonthChanged: (Calendar) -> Unit = {},
    multiSelectEnabled: Boolean = false,
    selectedDates: Set<String> = emptySet(),
    onDateLongClicked: (String) -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentMonth by remember { mutableStateOf(initialMonth ?: Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }

    // Sync internal state with external initialMonth if it changes
    LaunchedEffect(initialMonth) {
        if (initialMonth != null) {
            currentMonth = initialMonth
        }
    }
    var showYearPicker by remember { mutableStateOf(false) }

    val monthFormatter = SimpleDateFormat("MMMM", Locale.getDefault())
    val yearFormatter = SimpleDateFormat("yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showYearPicker = !showYearPicker }
                ) {
                    Text(
                        text = "${monthFormatter.format(currentMonth.time)} ${yearFormatter.format(currentMonth.time)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Year",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    DropdownMenu(
                        expanded = showYearPicker,
                        onDismissRequest = { showYearPicker = false },
                        modifier = Modifier.heightIn(max = 200.dp).background(Color(0xFF1A2637))
                    ) {
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val yearRange = if (allowFutureDates) {
                            (currentYear + 10 downTo currentYear - 90)
                        } else {
                            (currentYear downTo currentYear - 100)
                        }
                        
                        yearRange.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString(), color = Color.White) },
                                onClick = {
                                    val newMonth = currentMonth.clone() as Calendar
                                    newMonth.set(Calendar.YEAR, year)
                                    currentMonth = newMonth
                                    showYearPicker = false
                                    onMonthChanged(newMonth)
                                }
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = {
                        val newMonth = currentMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, -1)
                        currentMonth = newMonth
                        onMonthChanged(newMonth)
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val newMonth = currentMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, 1)
                        currentMonth = newMonth
                        onMonthChanged(newMonth)
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val daysOfWeek = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.width(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val calendar = currentMonth.clone() as Calendar
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val prevMonthCalendar = currentMonth.clone() as Calendar
            prevMonthCalendar.add(Calendar.MONTH, -1)
            val daysInPrevMonth = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val days = mutableListOf<CalendarDay>()
            for (i in 0 until firstDayOfWeek) {
                days.add(CalendarDay((daysInPrevMonth - firstDayOfWeek + i + 1).toString(), false))
            }
            for (i in 1..daysInMonth) {
                val dayCal = currentMonth.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, i)
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayCal.time)
                
                val isSelected = if (multiSelectEnabled) {
                    selectedDates.contains(dateStr)
                } else {
                    selectedDate.get(Calendar.DAY_OF_MONTH) == i &&
                    selectedDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                    selectedDate.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR)
                }
                days.add(CalendarDay(i.toString(), true, isSelected))
            }
            var nextMonthDay = 1
            while (days.size % 7 != 0) {
                days.add(CalendarDay(nextMonthDay.toString(), false))
                nextMonthDay++
            }
            
            val rows = days.chunked(7)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { day ->
                        val isToday = day.isCurrentMonth && 
                                      day.text == Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString() &&
                                      currentMonth.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) &&
                                      currentMonth.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)

                        val dayCal = currentMonth.clone() as Calendar
                        if (day.isCurrentMonth) {
                            dayCal.set(Calendar.DAY_OF_MONTH, day.text.toInt())
                        }
                        
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayCal.time)
                        
                        val dayTasks = if (day.isCurrentMonth) {
                            tasks.filter { isTaskOnDate(it, dayCal) }
                        } else emptyList()

                        val isMulti = dayTasks.size >= 2
                        val dotColor = if (dayTasks.isNotEmpty()) {
                            if (isMulti) Color.Red
                            else when (dayTasks.first().priority) {
                                "High" -> Color(0xFFFFEB3B) // Yellow
                                "Medium" -> Color(0xFF2196F3) // Blue
                                "Low" -> Color(0xFF4CAF50) // Green
                                else -> Color.Transparent
                            }
                        } else Color.Transparent

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day.isSelected -> Color(0xFF2196F3)
                                        isToday -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            if (day.isCurrentMonth && !multiSelectEnabled) {
                                                val newSelected = currentMonth.clone() as Calendar
                                                newSelected.set(Calendar.DAY_OF_MONTH, day.text.toInt())
                                                
                                                if (allowFutureDates || !newSelected.after(Calendar.getInstance())) {
                                                    onDateSelected(newSelected)
                                                }
                                            }
                                        },
                                        onLongPress = {
                                            if (day.isCurrentMonth && multiSelectEnabled) {
                                                onDateLongClicked(dateStr)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val isFuture = !allowFutureDates && day.isCurrentMonth && currentMonth.clone().let {
                                (it as Calendar).set(Calendar.DAY_OF_MONTH, day.text.toInt())
                                it.after(Calendar.getInstance())
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.text,
                                    color = when {
                                        day.isSelected -> Color.White
                                        day.isCurrentMonth && !isFuture -> Color.White
                                        isFuture -> Color.White.copy(alpha = 0.2f)
                                        else -> Color.Gray.copy(alpha = 0.5f)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (day.isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                                if (dotColor != Color.Transparent && !multiSelectEnabled) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (isMulti) {
                                        Box(
                                            modifier = Modifier.size(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Red)
                                            )
                                            Icon(
                                                Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(6.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClose) {
                    Text("CANCEL", color = Color(0xFF2196F3))
                }
                if (multiSelectEnabled) {
                    TextButton(onClick = onConfirm) {
                        Text("CONFIRM", color = Color(0xFF2196F3))
                    }
                }
            }
        }
    }
}
