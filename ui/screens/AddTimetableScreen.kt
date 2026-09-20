package com.example.classora.ui.screens

import android.net.Uri
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
    var existingVersionId by remember { mutableStateOf<String?>(null) }

    fun detectDuplicate(newEntries: List<TimetableEntry>, versions: List<TimetableVersion>): TimetableVersion? {
        return versions.find { version ->
            version.entries.size == newEntries.size && 
            version.entries.all { old -> newEntries.any { it.subject == old.subject && it.day == old.day && it.time == old.time } }
        }
    }

    fun processNewTimetable(entries: List<TimetableEntry>) {
        scope.launch {
            val versions = attendancePreferences.timetableVersionsFlow.first()
            val duplicate = detectDuplicate(entries, versions)
            
            pendingEntries = entries
            isDuplicate = duplicate != null
            existingVersionId = duplicate?.id
            showOptionsDialog = true
        }
    }

    if (showOptionsDialog) {
        Dialog(onDismissRequest = { showOptionsDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = if (isDuplicate) "Same Timetable Detected" else "Update Timetable",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isDuplicate) 
                            "This timetable matches a previous version. Would you like to reset attendance and start fresh, or continue using existing records?"
                            else "How would you like to apply this new timetable?",
                        fontSize = 14.sp, color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (isDuplicate) {
                        OptionButton("Start New (Reset)", "Start new attendance from today", Icons.Default.Refresh) {
                            scope.launch {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                attendancePreferences.saveTimetableVersion(TimetableVersion(entries = pendingEntries, startDate = today))
                                showOptionsDialog = false
                                onBack()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OptionButton("Continue Existing", "Keep using current attendance data", Icons.Default.PlayArrow) {
                            showOptionsDialog = false
                            onBack()
                        }
                    } else {
                        OptionButton("Start Fresh Today", "Archive old data and start new from today", Icons.Default.FiberNew) {
                            scope.launch {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                attendancePreferences.saveTimetableVersion(TimetableVersion(entries = pendingEntries, startDate = today))
                                showOptionsDialog = false
                                onBack()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OptionButton("Attach with Old Data", "Mix these classes with your history", Icons.Default.Link) {
                             scope.launch {
                                 // Simple logic: update latest version entries or add as new with very old start date
                                 val versions = attendancePreferences.timetableVersionsFlow.first().toMutableList()
                                 if (versions.isNotEmpty()) {
                                     val last = versions.last()
                                     versions[versions.size - 1] = last.copy(entries = pendingEntries)
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
                        Text("Cancel")
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
            else Toast.makeText(context, "Invalid CSV", Toast.LENGTH_SHORT).show()
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
            // UI elements from before...
            Card(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
                border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.2f))
            ) {
                Box(modifier = Modifier.fillMaxSize().clickable { launcher.launch("text/*") }, contentAlignment = Alignment.Center) {
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Icon(Icons.Default.CloudUpload, null, tint = Color(0xFF2196F3), modifier = Modifier.size(40.dp))
                         Text("Upload CSV File", fontWeight = FontWeight.Bold)
                         Text("Format: Subject, Day, Time", fontSize = 12.sp, color = Color.Gray)
                     }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // AI Prompt Assistant Card
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
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp)) }
                    }
                    Text("Copy this prompt for ChatGPT/Gemini to get the correct CSV text.", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = csvText,
                onValueChange = { csvText = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                placeholder = { Text("Paste CSV text here...") },
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
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Verify & Save", fontWeight = FontWeight.Bold)
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
