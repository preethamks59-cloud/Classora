package com.example.classora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.classora.data.SettingsPreferences
import kotlinx.coroutines.launch

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsPreferences = remember { SettingsPreferences(context) }
    val settings by settingsPreferences.settingsFlow.collectAsState(initial = com.example.classora.data.AppSettings())

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    var isSystemNotificationEnabled by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isSystemNotificationEnabled = isGranted
        if (isGranted) {
            scope.launch { settingsPreferences.updateNotifications(true) }
        }
    }

    fun openAppNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                else -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
            }
        }
        context.startActivity(intent)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Custom Header to avoid double inset padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 8.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0D1724))
                }
                Text(
                    text = "Notification Settings",
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
                Text("Manage your notifications & alerts", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                
                NotificationToggleItem(
                    label = "Enable / disable notifications", 
                    checked = settings.notificationsEnabled && isSystemNotificationEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // Always try to request if permission is currently denied
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    showPermissionDeniedDialog = true
                                } else {
                                    scope.launch { settingsPreferences.updateNotifications(true) }
                                }
                            } else {
                                scope.launch { settingsPreferences.updateNotifications(true) }
                            }
                        } else {
                            scope.launch { settingsPreferences.updateNotifications(false) }
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                val contentEnabled = settings.notificationsEnabled && isSystemNotificationEnabled
                
                NotificationToggleItem(
                    label = "Attendance alerts", 
                    checked = contentEnabled && settings.attendanceAlerts, 
                    onCheckedChange = { if (contentEnabled) scope.launch { settingsPreferences.updateNotificationSetting("attendance", it) } }
                )
                NotificationToggleItem(
                    label = "Academic alerts", 
                    checked = contentEnabled && settings.academicAlerts, 
                    onCheckedChange = { if (contentEnabled) scope.launch { settingsPreferences.updateNotificationSetting("academic", it) } }
                )
                NotificationToggleItem(
                    label = "Exam notifications", 
                    checked = contentEnabled && settings.examNotifications, 
                    onCheckedChange = { if (contentEnabled) scope.launch { settingsPreferences.updateNotificationSetting("exam", it) } }
                )
                NotificationToggleItem(
                    label = "Assignment notifications", 
                    checked = contentEnabled && settings.assignmentNotifications, 
                    onCheckedChange = { if (contentEnabled) scope.launch { settingsPreferences.updateNotificationSetting("assignment", it) } }
                )
                NotificationToggleItem(
                    label = "Calendar notifications", 
                    checked = contentEnabled && settings.calendarNotifications, 
                    onCheckedChange = { if (contentEnabled) scope.launch { settingsPreferences.updateNotificationSetting("calendar", it) } }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NotificationToggleItem(
                    label = "Notification sound", 
                    checked = contentEnabled && settings.soundEnabled,
                    onCheckedChange = { enabled -> 
                        if (contentEnabled) {
                            scope.launch { settingsPreferences.updateSound(enabled) }
                            openAppNotificationSettings()
                        }
                    }
                )
                NotificationToggleItem(
                    label = "Vibration", 
                    checked = contentEnabled && settings.vibrationEnabled,
                    onCheckedChange = { enabled -> 
                        if (contentEnabled) {
                            scope.launch { settingsPreferences.updateVibration(enabled) }
                            openAppNotificationSettings()
                        }
                    }
                )
            }
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Permission Denied", fontWeight = FontWeight.Bold) },
            text = { Text("Notifications are disabled at the system level. To enable them, please go to Settings and allow notifications for Classora.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDeniedDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Go to Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun NotificationToggleItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
