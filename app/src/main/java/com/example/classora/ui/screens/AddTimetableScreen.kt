package com.example.classora.ui.screens

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.view.DragEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.classora.data.AttendancePreferences
import com.example.classora.data.TimetableEntry
import com.example.classora.data.TimetableVersion
import com.example.classora.utils.CSVParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimetableScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attendancePreferences = remember { AttendancePreferences(context) }
    
    var csvText by remember { mutableStateOf("") }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var pendingEntries by remember { mutableStateOf<List<TimetableEntry>>(emptyList()) }
    var isDuplicate by remember { mutableStateOf(false) }
    var shouldClearAttendanceData by remember { mutableStateOf(false) }

    fun detectDuplicate(newEntries: List<TimetableEntry>, versions: List<TimetableVersion>): Boolean {
        return versions.any { version ->
            version.entries.size == newEntries.size && 
            version.entries.all { old -> newEntries.any { it.subject == old.subject && it.day == old.day && it.time == old.time } }
        }
    }

    fun processNewTimetable(entries: List<TimetableEntry>) {
        scope.launch {
            val versions = attendancePreferences.timetableVersionsFlow.first()
            if (versions.isEmpty()) {
                // First time user, skip dialog and save directly
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                attendancePreferences.saveTimetableVersion(TimetableVersion(entries = entries, startDate = today))
                Toast.makeText(context, "Timetable saved successfully!", Toast.LENGTH_SHORT).show()
                onBack()
            } else {
                isDuplicate = detectDuplicate(entries, versions)
                pendingEntries = entries
                shouldClearAttendanceData = false // Reset checkbox state
                showOptionsDialog = true
            }
        }
    }

    if (showOptionsDialog) {
        Dialog(onDismissRequest = { showOptionsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 500.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDuplicate) Icons.Default.History else Icons.Default.EditCalendar,
                            contentDescription = null,
                            tint = if (isDuplicate) Color(0xFF4CAF50) else Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isDuplicate) "Existing Timetable Found" else "Timetable Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isDuplicate) 
                            "This timetable has been used before. Would you like to reset it to start fresh from today, or just continue with your current records?"
                            else "How would you like to apply these changes to your schedule?",
                        fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    // The "Clear All" Checkbox requested by user
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (shouldClearAttendanceData) Color(0xFFFFEBEE) else Color(0xFFF5F7F9))
                            .clickable { shouldClearAttendanceData = !shouldClearAttendanceData }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = shouldClearAttendanceData,
                            onCheckedChange = { shouldClearAttendanceData = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF44336))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Clear All Attendance Data", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (shouldClearAttendanceData) Color(0xFFD32F2F) else Color(0xFF0D1724))
                            Text("Wipe all historical subject-wise stats", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isDuplicate) {
                        OptionButton("Reset from Today", "Start a new tracking period today", Icons.Default.Refresh) {
                            scope.launch {
                                if (shouldClearAttendanceData) attendancePreferences.clearAttendance()
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                attendancePreferences.saveTimetableVersion(TimetableVersion(entries = pendingEntries, startDate = today, name = "Timetable Reset"))
                                Toast.makeText(context, "Attendance reset for today", Toast.LENGTH_SHORT).show()
                                showOptionsDialog = false
                                onBack()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OptionButton("Continue with Existing", "Just keep things as they are", Icons.Default.PlayArrow) {
                            scope.launch {
                                if (shouldClearAttendanceData) {
                                    attendancePreferences.clearAttendance()
                                    Toast.makeText(context, "Attendance data cleared", Toast.LENGTH_SHORT).show()
                                }
                                showOptionsDialog = false
                                onBack()
                            }
                        }
                    } else {
                        OptionButton("It is a new sem...", "Start fresh for a new semester", Icons.Default.School) {
                            scope.launch {
                                attendancePreferences.clearAttendance()
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                attendancePreferences.saveTimetableVersion(TimetableVersion(entries = pendingEntries, startDate = today, name = "New Semester"))
                                Toast.makeText(context, "New semester started!", Toast.LENGTH_SHORT).show()
                                showOptionsDialog = false
                                onBack()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OptionButton("Continue New Attendance", "Start tracking these classes from today", Icons.Default.FiberNew) {
                            scope.launch {
                                if (shouldClearAttendanceData) attendancePreferences.clearAttendance()
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                attendancePreferences.saveTimetableVersion(TimetableVersion(entries = pendingEntries, startDate = today))
                                showOptionsDialog = false
                                onBack()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OptionButton("Edit Old Timetable", "Update current version without archiving", Icons.Default.Edit) {
                             scope.launch {
                                 if (shouldClearAttendanceData) attendancePreferences.clearAttendance()
                                 val versions = attendancePreferences.timetableVersionsFlow.first().toMutableList()
                                 if (versions.isNotEmpty()) {
                                     val lastIndex = versions.size - 1
                                     versions[lastIndex] = versions[lastIndex].copy(entries = pendingEntries)
                                     attendancePreferences.updateTimetableVersions(versions)
                                 } else {
                                     attendancePreferences.saveTimetableVersion(TimetableVersion(entries = pendingEntries, startDate = "2000-01-01"))
                                 }
                                 showOptionsDialog = false
                                 onBack()
                             }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OptionButton("Attach with Old Data", "Merge history with new schedule", Icons.Default.Link) {
                             scope.launch {
                                 if (shouldClearAttendanceData) attendancePreferences.clearAttendance()
                                 val versions = attendancePreferences.timetableVersionsFlow.first().toMutableList()
                                 if (versions.isNotEmpty()) {
                                     val lastIndex = versions.size - 1
                                     versions[lastIndex] = versions[lastIndex].copy(entries = pendingEntries)
                                     attendancePreferences.updateTimetableVersions(versions)
                                 } else {
                                     attendancePreferences.saveTimetableVersion(TimetableVersion(entries = pendingEntries, startDate = "2000-01-01"))
                                 }
                                 showOptionsDialog = false
                                 onBack()
                             }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showOptionsDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val parsed = CSVParser.parseTimetableCsv(context, it)
            if (parsed.isNotEmpty()) processNewTimetable(parsed)
            else Toast.makeText(context, "Invalid CSV format.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Timetable", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
                border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // This AndroidView acts as a transparent overlay to handle standard Android drag and drop
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                setOnDragListener { _, event ->
                                    when (event.action) {
                                        DragEvent.ACTION_DRAG_STARTED -> true
                                        DragEvent.ACTION_DRAG_ENTERED -> {
                                            setBackgroundColor(android.graphics.Color.parseColor("#1A2196F3"))
                                            true
                                        }
                                        DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                            true
                                        }
                                        DragEvent.ACTION_DROP -> {
                                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                            
                                            val dropPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                (context as? Activity)?.requestDragAndDropPermissions(event)
                                            } else null

                                            try {
                                                val clipData = event.clipData
                                                if (clipData != null && clipData.itemCount > 0) {
                                                    val uri = clipData.getItemAt(0).uri
                                                    if (uri != null) {
                                                        val parsed = CSVParser.parseTimetableCsv(context, uri)
                                                        if (parsed.isNotEmpty()) {
                                                            processNewTimetable(parsed)
                                                        } else {
                                                            Toast.makeText(context, "Could not parse file. Check format: Subject, Day, Time", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    dropPermissions?.release()
                                                }
                                            }
                                            true
                                        }
                                        else -> true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // The actual UI content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { launcher.launch("text/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                            drawRoundRect(color = Color(0xFF2196F3), style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudUpload, null, tint = Color(0xFF2196F3), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Upload or Drag CSV File", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Format: Subject, Day, Time", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB)),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                val promptText = "Convert my timetable into a plain text CSV format with columns: Subject, Day, Time. Use standard Day names (Monday, Tuesday, etc.) and 12-hour time format (e.g. 09:00 AM). My timetable is: "
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Prompt Assistant", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Classora AI Prompt", promptText))
                            Toast.makeText(context, "Prompt Copied!", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp)) }
                    }
                    Text("Copy this prompt and paste it in ChatGPT/Gemini to get the correct format.", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = csvText,
                onValueChange = { csvText = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                placeholder = { Text("Subject, Day, Time\nMaths, Monday, 09:00 AM...") },
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val parsed = CSVParser.parseTimetableText(csvText)
                    if (parsed.isNotEmpty()) processNewTimetable(parsed)
                    else Toast.makeText(context, "Invalid format", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Verify & Save Timetable", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OptionButton(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
