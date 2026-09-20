package com.example.classora.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.UserData
import com.example.classora.data.UserPreferences
import androidx.compose.ui.tooling.preview.Preview
import com.example.classora.ui.components.ProfileImage
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(onBack = {}, onEdit = {})
}

@Composable
fun ProfileScreen(onBack: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    val userData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", "", ""))

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistable permission for the URI so it works after reboot
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if it fails (might not be a document URI)
            }
            scope.launch {
                userPreferences.saveUserData(userData.copy(profileImageUri = it.toString()))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(userData, onBack, onEdit, onAddPhoto = { launcher.launch("image/*") })
        
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            ProfileInfoCard(userData)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProfileHeader(userData: UserData, onBack: () -> Unit, onEdit: () -> Unit, onAddPhoto: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Color(0xFF0D1724))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                ) {
                    ProfileImage(
                        uriString = userData.profileImageUri,
                        userName = userData.name,
                        fontSize = 40.sp,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                        .clickable { onAddPhoto() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF0D1724)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(userData.name.ifEmpty { "Enter Name" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF2196F3).copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Student", color = Color(0xFF2196F3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Navigation Icons Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            // Requirement 8: Edit icon at corner
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ProfileInfoCard(userData: UserData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileInfoItem(Icons.Default.Badge, "Student ID / USN", userData.studentId.ifEmpty { "Not set" })
            Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            ProfileInfoItem(Icons.Default.CalendarToday, "Date of Birth", userData.dob.ifEmpty { "Not set" })
            Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            ProfileInfoItem(Icons.Default.Book, "Course / Branch", userData.course.ifEmpty { "Not set" })
            Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            ProfileInfoItem(Icons.Default.School, "Semester", userData.semester.ifEmpty { "Not set" })
            Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            ProfileInfoItem(Icons.Default.AccountBalance, "College Name", userData.college.ifEmpty { "Not set" })
        }
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF0F7FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1724), modifier = Modifier.widthIn(max = 200.dp))
    }
}
