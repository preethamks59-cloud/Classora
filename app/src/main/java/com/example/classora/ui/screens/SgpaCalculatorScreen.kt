package com.example.classora.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.*
import com.example.classora.ui.components.ClassoraHeader
import com.example.classora.ui.components.SectionHeader
import kotlinx.coroutines.launch
import java.util.*

data class SubjectEntry(
    val name: String = "",
    val credits: String = "",
    val gradePoint: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgpaCalculatorScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    
    val userData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", ""))
    val academicData by userPreferences.academicFlow.collectAsState(initial = AcademicData())
    
    var subjects by remember { mutableStateOf(listOf(SubjectEntry())) }
    var calculatedSgpa by remember { mutableStateOf(0.0) }
    
    var showPromoteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var subjectIndexToDelete by remember { mutableStateOf(-1) }
    
    // Auto-calculate SGPA
    LaunchedEffect(subjects) {
        var totalCredits = 0.0
        var totalPoints = 0.0
        subjects.forEach { sub ->
            val c = sub.credits.toDoubleOrNull() ?: 0.0
            val g = sub.gradePoint.toDoubleOrNull() ?: 0.0
            totalCredits += c
            totalPoints += (c * g)
        }
        calculatedSgpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp)
        ) {
            item { 
                ClassoraHeader(
                    userData = userData,
                    title = "SGPA Calculator",
                    subtitle = "Manage grades for ${getRomanSemester(userData.semester)}",
                    onMenuClick = {}, // Not needed here
                    onProfileClick = onProfileClick,
                    onNotificationsClick = onNotificationsClick,
                    showMenu = false,
                    onBackClick = onBack
                )
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
            
            // Result Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1724))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Estimated Semester SGPA",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format("%.2f", calculatedSgpa),
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = getRomanSemester(userData.semester),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Course Subjects", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D1724))
                    Button(
                        onClick = { 
                            if (subjects.size < 25) {
                                subjects = subjects + SubjectEntry() 
                            } else {
                                Toast.makeText(context, "Maximum 25 subjects allowed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.1f), contentColor = Color(0xFF2196F3))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Subject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            itemsIndexed(subjects) { index, subject ->
                SubjectInputItem(
                    index = index + 1,
                    entry = subject,
                    onUpdate = { updated ->
                        val newItems = subjects.toMutableList()
                        newItems[index] = updated
                        subjects = newItems
                    },
                    onRemove = {
                        if (subjects.size > 1) {
                            subjectIndexToDelete = index
                            showDeleteDialog = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { subjects = listOf(SubjectEntry()) },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                val currentSem = getRomanSemester(userData.semester)
                                val sgpaString = String.format("%.2f", calculatedSgpa)
                                
                                val newResults = academicData.semesterResults.toMutableList()
                                val existingIndex = newResults.indexOfFirst { it.semester == currentSem }
                                
                                if (existingIndex != -1) {
                                    newResults[existingIndex] = SemesterResult(currentSem, sgpaString, "Completed")
                                } else {
                                    newResults.add(SemesterResult(currentSem, sgpaString, "Completed"))
                                }
                                
                                val validGpas = newResults.mapNotNull { it.gpa.toDoubleOrNull() }.filter { it > 0.0 }
                                val average = if (validGpas.isEmpty()) 0.0 else validGpas.average()
                                
                                userPreferences.saveAcademicData(
                                    academicData.copy(
                                        semesterResults = newResults,
                                        currentCgpa = String.format("%.2f", average)
                                    )
                                )
                            }
                        },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Result", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Next Step", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Once you have finished this semester, promote your profile to the next one to archive these results.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showPromoteDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Promote to Next Semester", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showPromoteDialog) {
        AlertDialog(
            onDismissRequest = { showPromoteDialog = false },
            title = { Text("Confirm Promotion") },
            text = { Text("Are you sure you want to move to the next semester? This will clear your current calculator inputs.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val currentNum = userData.semester.filter { it.isDigit() }.toIntOrNull() ?: 1
                            if (currentNum < 8) {
                                val nextSem = "${currentNum + 1} Semester"
                                userPreferences.saveUserData(userData.copy(semester = nextSem))
                                subjects = listOf(SubjectEntry())
                            }
                            showPromoteDialog = false
                        }
                    }
                ) {
                    Text("Promote", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoteDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Subject") },
            text = { Text("Are you sure you want to remove this subject from the list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newItems = subjects.toMutableList()
                        if (subjectIndexToDelete != -1 && subjectIndexToDelete < newItems.size) {
                            newItems.removeAt(subjectIndexToDelete)
                            subjects = newItems
                        }
                        showDeleteDialog = false
                        subjectIndexToDelete = -1
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun SubjectInputItem(
    index: Int,
    entry: SubjectEntry,
    onUpdate: (SubjectEntry) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(index.toString(), color = Color(0xFF2196F3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subject Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
                OutlinedTextField(
                    value = entry.name,
                    onValueChange = { onUpdate(entry.copy(name = it)) },
                    label = { Text("Subject Name (Optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = entry.credits,
                    onValueChange = { input ->
                        if (input.isEmpty()) {
                            onUpdate(entry.copy(credits = ""))
                        } else if (input.all { it.isDigit() }) {
                            val v = input.toIntOrNull()
                            if (v != null && v < 20) {
                                onUpdate(entry.copy(credits = input))
                            }
                        }
                    },
                    label = { Text("Credits", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
                
                OutlinedTextField(
                    value = entry.gradePoint,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            if (filtered.isEmpty()) {
                                onUpdate(entry.copy(gradePoint = ""))
                            } else {
                                val v = filtered.toDoubleOrNull()
                                if (v != null && v < 20.0) {
                                    onUpdate(entry.copy(gradePoint = filtered))
                                } else if (filtered == ".") {
                                    onUpdate(entry.copy(gradePoint = filtered))
                                }
                            }
                        }
                    },
                    label = { Text("Grade Point", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

fun getRomanSemester(sem: String): String {
    val num = sem.filter { it.isDigit() }.toIntOrNull() ?: 1
    return when (num) {
        1 -> "Semester I"
        2 -> "Semester II"
        3 -> "Semester III"
        4 -> "Semester IV"
        5 -> "Semester V"
        6 -> "Semester VI"
        7 -> "Semester VII"
        8 -> "Semester VIII"
        else -> sem
    }
}
