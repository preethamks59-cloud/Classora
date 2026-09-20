package com.example.classora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.UserData
import com.example.classora.data.UserPreferences
import androidx.compose.ui.tooling.preview.Preview
import com.example.classora.ui.components.ProfileImage

@Preview(showBackground = true)
@Composable
fun MoreScreenPreview() {
    MoreScreen(
        onClose = {}, 
        onNavigateToNotifications = {}, 
        onNavigateToReminders = {},
        onNavigateToDnd = {},
        onNavigateToCalendarSync = {},
        onNavigateToDocumentVault = {},
        onNavigateToAbout = {}
    )
}

@Composable
fun MoreScreen(
    onClose: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToDnd: () -> Unit,
    onNavigateToCalendarSync: () -> Unit,
    onNavigateToDocumentVault: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val userData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", ""))
    
    var clickCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                ProfileImage(
                    uriString = userData.profileImageUri,
                    userName = userData.name,
                    fontSize = 24.sp,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(userData.name.ifEmpty { "Enter Name" }, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Student", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        SettingsItem(Icons.Default.Notifications, "Notifications", "Manage your notifications & alerts", onClick = onNavigateToNotifications)
        SettingsItem(Icons.Default.AccessTime, "Reminder Settings", "Set reminders & get timely alerts", onClick = onNavigateToReminders)
        SettingsItem(Icons.Default.DoNotDisturbOn, "Automatic DND / Silent Mode", "Focus without distractions", onClick = onNavigateToDnd)
        SettingsItem(Icons.Default.CalendarToday, "Calendar Synchronization", "Sync your calendar & schedules", onClick = onNavigateToCalendarSync)
        SettingsItem(Icons.Default.Folder, "College Document Vault", "Securely store ID cards, receipts & certificates", onClick = onNavigateToDocumentVault)

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Version 1.143.2",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.Start)
                .clickable {
                    clickCount++
                    if (clickCount >= 7) {
                        clickCount = 0
                        onNavigateToAbout()
                    }
                }
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0F7FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2196F3))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 10.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
