package com.example.classora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.*
import com.example.classora.ui.components.ProfileImage
import com.example.classora.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPreviousSemesterScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    
    val userData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", ""))
    val academicData by userPreferences.academicFlow.collectAsState(initial = AcademicData())

    var selectedSemester by remember { mutableStateOf("Semester I") }
    var cgpaInput by remember { mutableStateOf("") }
    var expandedSemester by remember { mutableStateOf(false) }

    val semesters = listOf("Semester I", "Semester II", "Semester III", "Semester IV", "Semester V", "Semester VI", "Semester VII", "Semester VIII")

    // Filter semesters to only show previous ones based on user's current semester
    val currentSemNumber = userData.semester.filter { it.isDigit() }.toIntOrNull() ?: 1
    val previousSemesters = semesters.filterIndexed { index, _ -> index < currentSemNumber - 1 }
    
    // Default to the most recent previous semester if none selected
    LaunchedEffect(previousSemesters) {
        if (previousSemesters.isNotEmpty() && selectedSemester !in previousSemesters) {
            selectedSemester = previousSemesters.last()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            IconButton(
                onClick = onBack, 
                modifier = Modifier.size(24.dp).align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            
            ProfileImage(
                uriString = userData.profileImageUri,
                userName = userData.name,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.CenterEnd)
                    .clickable { onProfileClick() }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Previous Semester Records",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D1724)
        )
        Text(
            text = "Manage your academic history",
            fontSize = 14.sp,
            color = Color.Gray
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }
            
            // Input Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add New Record", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Select Semester
                        Box {
                            OutlinedTextField(
                                value = selectedSemester,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Semester") },
                                modifier = Modifier.fillMaxWidth().clickable { expandedSemester = true },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, Modifier.clickable { expandedSemester = true })
                                },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.Black,
                                    disabledBorderColor = Color.LightGray,
                                    disabledLabelColor = Color.Gray,
                                    disabledTrailingIconColor = Color.Gray,
                                    disabledContainerColor = Color.White
                                )
                            )
                            DropdownMenu(
                                expanded = expandedSemester,
                                onDismissRequest = { expandedSemester = false }
                            ) {
                                previousSemesters.forEach { sem ->
                                    DropdownMenuItem(
                                        text = { Text(sem) },
                                        onClick = {
                                            selectedSemester = sem
                                            expandedSemester = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Enter SGPA
                        OutlinedTextField(
                            value = cgpaInput,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    val value = filtered.toFloatOrNull()
                                    if (value == null || value <= 10.0) {
                                        cgpaInput = filtered
                                    }
                                }
                            },
                            label = { Text("Semester SGPA") },
                            placeholder = { Text("e.g. 8.45") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            suffix = { Text("/ 10") },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                if (cgpaInput.isNotBlank()) {
                                    val newResults = academicData.semesterResults.toMutableList()
                                    val index = newResults.indexOfFirst { it.semester == selectedSemester }
                                    if (index != -1) {
                                        newResults[index] = SemesterResult(selectedSemester, cgpaInput, "Completed")
                                    } else {
                                        newResults.add(SemesterResult(selectedSemester, cgpaInput, "Completed"))
                                    }
                                    
                                    val filteredGpas = newResults.mapNotNull { it.gpa.toFloatOrNull() }.filter { it > 0.0f }
                                    val average = if (filteredGpas.isEmpty()) 0f else filteredGpas.average().toFloat()
                                    
                                    scope.launch {
                                        userPreferences.saveAcademicData(
                                            academicData.copy(
                                                semesterResults = newResults,
                                                currentCgpa = String.format("%.2f", average)
                                            )
                                        )
                                    }
                                    cgpaInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1724))
                        ) {
                            Text("Save Record", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
            
            // Previous Records
            item {
                Text("Saved Records", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D1724))
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            items(academicData.semesterResults.sortedBy { it.semester }) { result ->
                AddPreviousSemesterItem(result, onDelete = {
                    val newResults = academicData.semesterResults.filter { it.semester != result.semester }
                    val filteredGpas = newResults.mapNotNull { it.gpa.toFloatOrNull() }.filter { it > 0.0f }
                    val average = if (filteredGpas.isEmpty()) 0f else filteredGpas.average().toFloat()
                    
                    scope.launch {
                        userPreferences.saveAcademicData(
                            academicData.copy(
                                semesterResults = newResults,
                                currentCgpa = String.format("%.2f", average)
                            )
                        )
                    }
                })
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (academicData.semesterResults.isEmpty()) {
                item {
                    Text(
                        "No records saved yet.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun AddPreviousSemesterItem(result: SemesterResult, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(result.semester, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "SGPA: ${result.gpa}", 
                    color = Color(0xFF4CAF50), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline, 
                    contentDescription = "Delete", 
                    tint = Color.Red.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

