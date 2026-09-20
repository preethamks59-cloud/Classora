package com.example.classora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.SettingsPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(onBack: () -> Unit, onAddTask: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsPreferences = remember { SettingsPreferences(context) }
    val settings by settingsPreferences.settingsFlow.collectAsState(initial = com.example.classora.data.AppSettings())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Custom Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 8.dp, start = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0D1724))
                }
                Text(
                    text = "Reminder Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1724),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Set reminders & get timely alerts", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("General Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                Spacer(modifier = Modifier.height(8.dp))
                ReminderTypeItem(
                    label = "Enable Reminders", 
                    checked = settings.remindersEnabled,
                    onCheckedChange = { scope.launch { settingsPreferences.updateReminderSetting("enabled", it) } }
                )
                ReminderTypeItem(
                    label = "Recurring reminders", 
                    checked = settings.recurringReminders,
                    onCheckedChange = { scope.launch { settingsPreferences.updateReminderSetting("recurring", it) } }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Academic Reminders", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                Spacer(modifier = Modifier.height(8.dp))
                ReminderTypeItem(
                    label = "Class reminders", 
                    checked = settings.classReminders,
                    onCheckedChange = { scope.launch { settingsPreferences.updateReminderSetting("class", it) } }
                )
                ReminderTypeItem(
                    label = "Study reminders", 
                    checked = settings.studyReminders,
                    onCheckedChange = { scope.launch { settingsPreferences.updateReminderSetting("study", it) } }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Alerts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                Spacer(modifier = Modifier.height(8.dp))
                ReminderTypeItem(
                    label = "Snooze reminders", 
                    checked = settings.snoozeEnabled,
                    onCheckedChange = { scope.launch { settingsPreferences.updateReminderSetting("snooze", it) } }
                )
            }
        }
    }
}

@Composable
fun ReminderTypeItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = Color(0xFF0D1724))
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2196F3)
            )
        )
    }
}
