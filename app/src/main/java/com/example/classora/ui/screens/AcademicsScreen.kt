package com.example.classora.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.*
import com.example.classora.ui.components.ClassoraHeader
import com.example.classora.ui.components.ProfileImage
import com.example.classora.ui.components.SectionHeader
import kotlinx.coroutines.launch

@Composable
fun AcademicsScreen(
    onMenuClick: () -> Unit, 
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAddPreviousClick: () -> Unit,
    onCalculatorClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    
    val userData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", ""))
    val academicData by userPreferences.academicFlow.collectAsState(initial = AcademicData())

    var targetCgpaInput by remember { mutableStateOf("") }
    var showTargetInput by remember { mutableStateOf(false) }
    val currentSemNumber = userData.semester.filter { it.isDigit() }.toIntOrNull() ?: 1
    val isFirstSemester = currentSemNumber == 1

    val previousSemLabel = when (currentSemNumber - 1) {
        1 -> "Semester I"
        2 -> "Semester II"
        3 -> "Semester III"
        4 -> "Semester IV"
        5 -> "Semester V"
        6 -> "Semester VI"
        7 -> "Semester VII"
        else -> ""
    }
    val lastSgpa = academicData.semesterResults.find { it.semester == previousSemLabel }?.gpa ?: "0.0"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item { 
            ClassoraHeader(
                userData = userData,
                title = "Academics",
                subtitle = "CGPA / SGPA Calculator & Analytics",
                onMenuClick = onMenuClick,
                onProfileClick = onProfileClick,
                onNotificationsClick = onNotificationsClick
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        
        // Target CGPA Entry (if not set or showTargetInput is true)
        if (academicData.targetCgpa == "0.0" || academicData.targetCgpa.isEmpty() || showTargetInput) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (showTargetInput) "Edit Target CGPA" else "Set your Target CGPA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (showTargetInput) {
                                IconButton(onClick = { showTargetInput = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = targetCgpaInput.ifEmpty { if (showTargetInput) academicData.targetCgpa else "" },
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() || it == '.' }
                                    if (filtered.count { it == '.' } <= 1) {
                                        val value = filtered.toFloatOrNull()
                                        if (value == null || value <= 10.0) {
                                            targetCgpaInput = filtered
                                        }
                                    }
                                },
                                placeholder = { Text("Enter Target (e.g. 8.5)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (targetCgpaInput.isNotBlank()) {
                                        scope.launch {
                                            userPreferences.saveAcademicData(academicData.copy(targetCgpa = targetCgpaInput))
                                            showTargetInput = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Current CGPA", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Overall ▾", color = Color.Gray, fontSize = 10.sp)
                        }
                        Text(academicData.currentCgpa, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        Text("Overall Academic Progress ", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(40.dp))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GpaStatusCard(
                    title = "Previous SGPA", 
                    subtitle = previousSemLabel, 
                    value = if (isFirstSemester) "__" else lastSgpa, 
                    color = Color(0xFF4CAF50), 
                    modifier = Modifier.weight(1f)
                )
                GpaStatusCard(
                    title = "Target CGPA", 
                    subtitle = "Set Goal", 
                    value = if (academicData.targetCgpa == "0.0") "0.0" else academicData.targetCgpa, 
                    color = Color(0xFF673AB7), 
                    modifier = Modifier.weight(1f),
                    onEditClick = { 
                        targetCgpaInput = if (academicData.targetCgpa == "0.0") "" else academicData.targetCgpa
                        showTargetInput = true 
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { 
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onCalculatorClick() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEDE7F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF673AB7))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CGPA / SGPA Calculator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Calculate required grades to reach your target", fontSize = 10.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Add Previous Semester Button (Prominent)
        if (!isFirstSemester) {
            item {
                Button(
                    onClick = onAddPreviousClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF2196F3))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Previous Semester Data", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
        if (!isFirstSemester) {
            item { 
                SectionHeader(
                    title = "Semester Wise SGPA",
                    actionText = ""
                ) 
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            items(academicData.semesterResults.sortedBy { it.semester }) { result ->
                SemesterListItem(result, onAddPreviousClick)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (academicData.semesterResults.isEmpty()) {
                item {
                    Text(
                        "No records added. Click 'Add Previous Semester Data' to start.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        } else {
            item {
                Text(
                    "You are in your first semester. Previous records are not required.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun GpaStatusCard(
    title: String, 
    subtitle: String, 
    value: String, 
    color: Color, 
    modifier: Modifier,
    onEditClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(subtitle, fontSize = 10.sp, color = Color.Gray)
                }
                if (onEditClick != null) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                Canvas(modifier = Modifier.size(width = 40.dp, height = 20.dp)) {
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(size.width * 0.3f, size.height * 0.4f)
                        lineTo(size.width * 0.6f, size.height * 0.7f)
                        lineTo(size.width, 0f)
                    }
                    drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
fun SemesterListItem(result: SemesterResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(result.semester, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(result.gpa, color = if (result.semester.contains("III")) Color(0xFF2196F3) else Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

