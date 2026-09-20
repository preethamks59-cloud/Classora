package com.example.classora.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classora.data.UserData
import com.example.classora.data.UserPreferences
import com.example.classora.ui.components.CustomCalendarDialog
import com.example.classora.ui.components.ProfileImage
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.*

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen(onContinue = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onContinue: () -> Unit, isEditing: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    
    val initialUserData by userPreferences.userFlow.collectAsState(initial = UserData("", "", "", "", "", ""))

    var currentStep by remember { mutableIntStateOf(1) }
    
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profileImageUri = it.toString() }
    }

    val engineeringCourses = listOf(
        "Computer Science & Engineering", "Information Science & Engineering", "Information Technology",
        "Artificial Intelligence & Data Science", "Artificial Intelligence & Machine Learning",
        "Cyber Security", "Software Engineering", "Computer Science & Business Systems",
        "Electronics & Communication Engineering", "Electrical & Electronics Engineering",
        "Electronics & Instrumentation Engineering", "Telecommunication Engineering",
        "Medical Electronics Engineering", "Microelectronics & VLSI Design",
        "Mechanical Engineering", "Civil Engineering", "Industrial & Production Engineering",
        "Automobile Engineering", "Mechatronics Engineering", "Robotics & Automation",
        "Manufacturing Engineering", "Construction Engineering", "Aeronautical Engineering",
        "Aerospace Engineering", "Marine Engineering", "Mining Engineering",
        "Chemical Engineering", "Petrochemical Engineering", "Metallurgical Engineering",
        "Polymer Technology", "Nanotechnology", "Energy Engineering", "Biotechnology",
        "Biomedical Engineering", "Bio-Informatics", "Agricultural Engineering",
        "Food Technology", "Environmental Engineering", "Textile Technology",
        "Fire & Safety Engineering"
    ).sorted()

    var showCourseDropdown by remember { mutableStateOf(false) }
    val filteredCourses = if (course.isEmpty()) engineeringCourses 
                          else engineeringCourses.filter { it.contains(course, ignoreCase = true) }

    LaunchedEffect(initialUserData) {
        if (initialUserData.name.isNotEmpty()) {
            name = initialUserData.name
            studentId = initialUserData.studentId
            course = initialUserData.course
            semester = initialUserData.semester
            className = initialUserData.className
            dob = initialUserData.dob
            college = initialUserData.college
            profileImageUri = initialUserData.profileImageUri
        }
    }

    var showCalendarDialog by remember { mutableStateOf(false) }

    if (showCalendarDialog) {
        CustomCalendarDialog(
            onDismiss = { showCalendarDialog = false },
            onDateSelected = { calendar ->
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dob = format.format(calendar.time)
                showCalendarDialog = false
            },
            allowFutureDates = false
        )
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentStep > 1) {
                    TextButton(
                        onClick = {
                            if (isEditing) {
                                // Save everything immediately and finish if editing
                                scope.launch {
                                    userPreferences.saveUserData(
                                        UserData(name, studentId, dob, course, semester, college, className, profileImageUri)
                                    )
                                    onContinue()
                                }
                            } else {
                                // First time onboarding - clear current page fields before skipping
                                if (currentStep == 2) {
                                    dob = ""
                                    college = ""
                                    className = ""
                                    currentStep++
                                } else if (currentStep == 3) {
                                    profileImageUri = ""
                                    scope.launch {
                                        userPreferences.saveUserData(
                                            UserData(name, studentId, dob, course, semester, college, className, profileImageUri)
                                        )
                                        onContinue()
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Skip", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val isStep1Valid = name.isNotBlank() && studentId.isNotBlank() && course.isNotBlank() && semester.isNotBlank()
                val isStep2Valid = dob.isNotBlank() && college.isNotBlank()
                val isStepValid = when(currentStep) {
                    1 -> isStep1Valid
                    2 -> isStep2Valid
                    else -> true
                }

                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            scope.launch {
                                userPreferences.saveUserData(
                                    UserData(name, studentId, dob, course, semester, college, className, profileImageUri)
                                )
                                onContinue()
                            }
                        }
                    },
                    enabled = isStepValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1557C0),
                        disabledContainerColor = Color(0xFF1557C0).copy(alpha = 0.5f)
                    )
                ) {
                    Text(if (currentStep == 3) "Complete" else "Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Color(0xFF0D0D0D))
            ) {
                if (currentStep > 1) {
                    IconButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { currentStep / 3f },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Step $currentStep of 3",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when(currentStep) {
                            1 -> "Personal Info"
                            2 -> "Additional Details"
                            else -> "Profile Picture"
                        },
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("Your information is ")
                            withStyle(style = SpanStyle(color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)) {
                                append("securely encrypted")
                            }
                            append("")
                        },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                if (currentStep == 1) {
                    Text("Set your Profile", color = Color(0xFF0D0D0D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PersonalTextField(
                        label = "Student Name",
                        value = name,
                        onValueChange = { if (it.length <= 50) name = it },
                        placeholder = "Enter your name",
                        icon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )
                    
                    PersonalTextField(
                        label = "USN",
                        value = studentId,
                        onValueChange = { if (it.length <= 30) studentId = it.uppercase() },
                        placeholder = "Enter your USN",
                        icon = Icons.Default.AssignmentInd,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Characters
                        )
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        PersonalTextField(
                            label = "Course/Branch",
                            value = course,
                            onValueChange = { 
                                if (it.length <= 400) {
                                    course = it
                                    showCourseDropdown = true
                                }
                            },
                            placeholder = "Select or type your course",
                            icon = Icons.Default.Book,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                        
                        DropdownMenu(
                            expanded = showCourseDropdown && filteredCourses.isNotEmpty(),
                            onDismissRequest = { showCourseDropdown = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .heightIn(max = 240.dp)
                                .background(Color.White)
                        ) {
                            filteredCourses.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection, fontSize = 14.sp) },
                                    onClick = {
                                        course = selection
                                        showCourseDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    var semesterExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        PersonalTextField(
                            label = "Semester",
                            value = semester,
                            onValueChange = { },
                            placeholder = "Select Semester",
                            icon = Icons.Default.Language,
                            readOnly = true,
                            onClick = { semesterExpanded = true }
                        )
                        DropdownMenu(
                            expanded = semesterExpanded,
                            onDismissRequest = { semesterExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f).background(Color.White)
                        ) {
                            (1..8).forEach { num ->
                                val semLabel = "$num Semester"
                                DropdownMenuItem(
                                    text = { Text(semLabel) },
                                    onClick = {
                                        semester = semLabel
                                        semesterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (currentStep == 2) {
                    Text("Additional Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    PersonalTextField(
                        label = "Date of Birth",
                        value = dob,
                        onValueChange = { },
                        placeholder = "Select Date of Birth",
                        icon = Icons.Default.CalendarToday,
                        trailingIcon = Icons.Default.CalendarMonth,
                        readOnly = true,
                        onClick = { showCalendarDialog = true }
                    )
                    
                    PersonalTextField(
                        label = "College Name",
                        value = college,
                        onValueChange = { if (it.length <= 1000) college = it },
                        placeholder = "Enter your college name",
                        icon = Icons.Default.AccountBalance,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        )
                    )
                    
                    PersonalTextField(
                        label = "Class",
                        value = className,
                        onValueChange = { className = it },
                        placeholder = "Section/Class",
                        icon = Icons.Default.Class,
                        isOptional = true
                    )
                } else {
                    Text("       Set your Profile Picture", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F7FF))
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUri.isEmpty()) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color(0xFF1557C0))
                            } else {
                                ProfileImage(
                                    uriString = profileImageUri,
                                    userName = name,
                                    fontSize = 60.sp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    
                    if (profileImageUri.isNotEmpty()) {
                        TextButton(
                            onClick = { profileImageUri = "" },
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
                        ) {
                            Text("Remove Photo", color = Color.Red)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
fun PersonalTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    isOptional: Boolean = false,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                placeholder = { Text(placeholder, color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = icon?.let {
                    {
                        Icon(it, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                    }
                },
                trailingIcon = trailingIcon?.let {
                    {
                        Icon(it, contentDescription = null, tint = Color(0xFF1557C0), modifier = Modifier.size(24.dp))
                    }
                },
                readOnly = readOnly,
                keyboardOptions = keyboardOptions,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Color(0xFF1557C0),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedLabelColor = Color(0xFF1557C0),
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledLabelColor = Color.Gray,
                    disabledTextColor = Color.Black
                )
            )
            
            if (onClick != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onClick() }
                )
            }
        }

        if (isOptional) {
            Text(
                text = "(Optional)",
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
