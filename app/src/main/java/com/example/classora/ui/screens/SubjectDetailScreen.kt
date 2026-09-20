package com.example.classora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.AttendancePreferences
import com.example.classora.data.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectName: String,
    versionId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val attendancePreferences = remember { AttendancePreferences(context) }
    val attendanceRecords by attendancePreferences.attendanceFlow.collectAsState(initial = emptyList())
    
    val subjectRecords = remember(attendanceRecords, subjectName, versionId) {
        attendanceRecords.filter { it.subject == subjectName && (it.timetableVersionId == versionId || versionId == "null") }
            .sortedByDescending { it.date }
    }

    val presentCount = subjectRecords.count { it.isPresent }
    val totalCount = subjectRecords.size
    val percentage = if (totalCount > 0) (presentCount.toFloat() / totalCount) * 100 else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subjectName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Stats Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Attendance Rate", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            "${String.format("%.1f", percentage)}%",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Classes", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            "$presentCount / $totalCount",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Attendance History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (subjectRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records found for this subject.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(subjectRecords) { record ->
                        HistoryItem(record)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(record: AttendanceRecord) {
    val displayDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.US)
        parser.parse(record.date)?.let { formatter.format(it) } ?: record.date
    } catch (e: Exception) {
        record.date
    }

    val dayName = try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("EEEE", Locale.US)
        parser.parse(record.date)?.let { formatter.format(it) } ?: ""
    } catch (e: Exception) {
        ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(displayDate, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(dayName, fontSize = 12.sp, color = Color.Gray)
            }
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (record.isPresent) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (record.isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (record.isPresent) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (record.isPresent) "Present" else "Absent",
                        color = if (record.isPresent) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
