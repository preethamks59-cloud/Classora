package com.example.classora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.Task
import com.example.classora.data.TaskPreferences
import com.example.classora.ui.components.TaskDetailsContent
import com.example.classora.utils.NotificationScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAllTasksScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taskPreferences = remember { TaskPreferences(context) }
    val allTasks by taskPreferences.tasksFlow.collectAsState(initial = emptyList())
    
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showTaskDetails by remember { mutableStateOf(false) }

    val pendingTasks = allTasks.filter { !it.isCompleted }
    val completedTasks = allTasks.filter { it.isCompleted }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending (${pendingTasks.size})", "Completed (${completedTasks.size})")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                    text = "All Tasks",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1724),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF2196F3),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF2196F3)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color(0xFF2196F3) else Color.Gray
                            ) 
                        }
                    )
                }
            }

            val tasksToShow = if (selectedTab == 0) pendingTasks else completedTasks

            if (tasksToShow.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks found", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(tasksToShow) { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable { 
                                    selectedTask = task
                                    showTaskDetails = true
                                },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null, 
                                    tint = if (task.isCompleted) Color(0xFF4CAF50) else Color.Gray
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(task.date, fontSize = 12.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(task.time, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskDetails && selectedTask != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showTaskDetails = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                TaskDetailsContent(
                    task = selectedTask!!,
                    isUpcoming = !selectedTask!!.isCompleted,
                    onClose = { showTaskDetails = false },
                    onComplete = {
                        scope.launch {
                            try {
                                val task = selectedTask
                                if (task != null) {
                                    taskPreferences.saveTask(task.copy(isCompleted = true))
                                    NotificationScheduler.cancelNotification(context, task.id)
                                    showTaskDetails = false
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    onDelete = {
                        scope.launch {
                            taskPreferences.deleteTask(selectedTask!!.id)
                            showTaskDetails = false
                        }
                    },
                    onReschedule = {
                        // Reschedule logic could be added here
                    }
                )
            }
        }
    }
}
