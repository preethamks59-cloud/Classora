package com.example.classora.ui.screens

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.classora.data.InfoNotification
import com.example.classora.data.InfoPreferences
import com.example.classora.data.SettingsPreferences
import com.example.classora.data.UserPreferences
import com.example.classora.ui.navigation.Screen
import com.example.classora.ui.screens.DocumentVaultScreen
import com.example.classora.utils.CSVParser
import com.example.classora.utils.NotificationScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val infoPreferences = remember { InfoPreferences(context) }
    val settingsPreferences = remember { SettingsPreferences(context) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val settings by settingsPreferences.settingsFlow.collectAsState(initial = com.example.classora.data.AppSettings())
    var startDestination by remember { mutableStateOf<String?>(null) }
    
    var showAboutSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle result if needed
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Background polling for Info when app is in foreground
    DisposableEffect(lifecycleOwner) {
        var pollingJob: Job? = null
        
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    pollingJob = scope.launch {
                        while (isActive) {
                            try {
                                val url = "https://docs.google.com/spreadsheets/d/1KRl35wo7H-obeWxOq7z5S-SaVnxSN8jQ31usPw4-UjI/export?format=csv"
                                val content = withContext(Dispatchers.IO) { URL(url).readText() }
                                val csvData = CSVParser.parseCsv(content)
                                val parsedInfo = mutableListOf<InfoNotification>()

                                csvData.forEachIndexed { index, parts ->
                                    if (index > 0 && parts.size >= 3) {
                                        val timestampStr = parts[0]
                                        var title = ""
                                        var message = ""
                                        var linkStr = ""

                                        val linkIndex = parts.indexOfFirst { it.contains("http", ignoreCase = true) }
                                        if (linkIndex != -1) linkStr = parts[linkIndex]

                                        val possibleTextParts = parts.filterIndexed { i, s -> 
                                            i != 0 && i != linkIndex && !s.contains("@") && s.isNotBlank() 
                                        }
                                        
                                        if (possibleTextParts.isNotEmpty()) {
                                            message = possibleTextParts.maxByOrNull { it.length } ?: ""
                                            title = possibleTextParts.firstOrNull { it != message } ?: message
                                        }

                                        if (linkStr.contains("about:blank", ignoreCase = true)) linkStr = ""

                                        val timestamp: Long = try {
                                            val sdf = SimpleDateFormat("M/d/yyyy H:mm:ss", Locale.getDefault())
                                            sdf.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                        } catch (e: Exception) {
                                            System.currentTimeMillis()
                                        }

                                        if (title.isNotBlank() || message.isNotBlank()) {
                                            parsedInfo.add(InfoNotification(timestampStr + title.hashCode(), title, message, linkStr, timestamp))
                                        }
                                    }
                                }

                                if (parsedInfo.isNotEmpty()) {
                                    val newlyAdded = infoPreferences.saveInfoNotifications(parsedInfo)
                                    if (newlyAdded.isNotEmpty()) {
                                        val notifiedIds = mutableListOf<String>()
                                        newlyAdded.forEach { item ->
                                            NotificationScheduler.showInfoNotification(context, item.title, item.message)
                                            notifiedIds.add(item.id)
                                        }
                                        infoPreferences.markAsNotified(notifiedIds)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            delay(1000) // Poll every second while app is resumed for real-time feel
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    pollingJob?.cancel()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pollingJob?.cancel()
        }
    }

    LaunchedEffect(Unit) {
        val userData = userPreferences.userFlow.first()
        startDestination = if (userData.name.isEmpty()) {
            Screen.Onboarding.route
        } else {
            // Ask for notification permission after login process (if returning to Home)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            Screen.Home.route
        }

        // Handle Intent for Rescheduling safely
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) break
            currentContext = currentContext.baseContext
        }
        
        val activity = currentContext as? Activity
        val intent = activity?.intent
        val action = intent?.getStringExtra("action")
        val taskId = intent?.getStringExtra("taskId")
        
        if (action == "RESCHEDULE_TASK" && taskId != null) {
            navController.navigate("${Screen.Notifications.route}?rescheduleId=$taskId")
            intent.removeExtra("action")
            intent.removeExtra("taskId")
        } else if (action == "VIEW_TASK" || action == "VIEW_NOTIFICATIONS") {
            navController.navigate(Screen.Notifications.route)
            intent?.removeExtra("action")
        } else if (intent?.getStringExtra("navigate_to") == "notifications") {
            navController.navigate(Screen.Notifications.route)
            intent.removeExtra("navigate_to")
        }
    }

    val bottomBarScreens = listOf(
        Screen.Home,
        Screen.Attendance,
        Screen.Academics,
        Screen.Calendar
    )

    if (startDestination == null) return

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.8f),
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                MoreScreen(
                    onClose = { scope.launch { drawerState.close() } },
                    onNavigateToNotifications = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.NotificationSettings.route)
                    },
                    onNavigateToReminders = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.ReminderSettings.route)
                    },
                    onNavigateToDnd = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.DndSettings.route)
                    },
                    onNavigateToCalendarSync = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.CalendarSync.route)
                    },
                    onNavigateToDocumentVault = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.DocumentVault.route)
                    },
                    onNavigateToAbout = {
                        scope.launch { 
                            drawerState.close() 
                            showAboutSheet = true
                        }
                    }
                )
            }
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            bottomBar = {
                if (currentDestination?.route in bottomBarScreens.map { it.route }) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = Color(0xFF0D1724),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            tonalElevation = 0.dp
                        ) {
                            bottomBarScreens.forEach { screen ->
                                val selected = currentDestination?.route == screen.route
                                NavigationBarItem(
                                    icon = { 
                                        Box(
                                            modifier = Modifier
                                                .size(width = 48.dp, height = 32.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) Color.White.copy(alpha = 0.1f) else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(screen.icon, contentDescription = screen.title) 
                                        }
                                    },
                                    label = { Text(screen.title) },
                                    selected = selected,
                                    onClick = {
                                        if (currentDestination?.route != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination!!,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onContinue = {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            } else {
                                // Request notification permission immediately after completing onboarding
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }

                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        },
                        isEditing = navController.previousBackStackEntry != null
                    )
                }
                composable(Screen.Home.route) { 
                    HomeScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                        onAddTaskClick = { navController.navigate(Screen.AddTask.route) },
                        onViewAllTasksClick = { navController.navigate(Screen.ViewAllTasks.route) }
                    ) 
                }
                composable(Screen.Attendance.route) { 
                    AttendanceScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                        onAddTaskClick = { navController.navigate(Screen.AddTask.route) },
                        onAddTimetableClick = { navController.navigate(Screen.AddTimetable.route) },
                        onSubjectClick = { subject, versionId ->
                            navController.navigate("subject_details/$subject/$versionId")
                        }
                    ) 
                }
                composable(Screen.Academics.route) { 
                    AcademicsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                        onAddPreviousClick = { navController.navigate(Screen.AddPreviousSemester.route) },
                        onCalculatorClick = { navController.navigate(Screen.SgpaCalculator.route) }
                    ) 
                }
                composable(Screen.Calendar.route) { 
                    CalendarScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                        onAddTaskClick = { date -> 
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
                            navController.navigate("${Screen.AddTask.route}?date=$dateStr")
                        },
                        onViewAllTasksClick = { navController.navigate(Screen.ViewAllTasks.route) }
                    ) 
                }
                composable(Screen.Profile.route) { 
                    ProfileScreen(
                        onBack = { navController.popBackStack() }, 
                        onEdit = { navController.navigate(Screen.Onboarding.route) }
                    )
                }
                composable(Screen.NotificationSettings.route) {
                    NotificationSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.ReminderSettings.route) {
                    ReminderSettingsScreen(
                        onBack = { navController.popBackStack() },
                        onAddTask = { navController.navigate(Screen.AddTask.route) }
                    )
                }
                composable(Screen.DndSettings.route) {
                    DndSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.CalendarSync.route) {
                    CalendarSyncScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.DocumentVault.route) {
                    DocumentVaultScreen(onBack = { navController.popBackStack() })
                }
                composable("${Screen.AddTask.route}?date={date}") { backStackEntry ->
                    val initialDate = backStackEntry.arguments?.getString("date")
                    AddTaskScreen(
                        onBack = { navController.popBackStack() },
                        onTaskAdded = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                        initialDate = initialDate
                    )
                }
                composable(Screen.AddTask.route) {
                    AddTaskScreen(
                        onBack = { navController.popBackStack() },
                        onTaskAdded = { navController.popBackStack() },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) }
                    )
                }
                composable(Screen.ViewAllTasks.route) {
                    ViewAllTasksScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.AddPreviousSemester.route) {
                    AddPreviousSemesterScreen(
                        onBack = { navController.popBackStack() },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) }
                    )
                }
                composable(Screen.SgpaCalculator.route) {
                    SgpaCalculatorScreen(
                        onBack = { navController.popBackStack() },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) }
                    )
                }
                composable(Screen.AddTimetable.route) {
                    AddTimetableScreen(onBack = { navController.popBackStack() })
                }
                composable("${Screen.Notifications.route}?rescheduleId={rescheduleId}") { backStackEntry ->
                    val rescheduleId = backStackEntry.arguments?.getString("rescheduleId")
                    NotificationsScreen(
                        onBack = {
                            if (!navController.popBackStack(Screen.Home.route, false)) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                        },
                        rescheduleTaskId = rescheduleId
                    )
                }
                composable("subject_details/{subjectName}/{versionId}") { backStackEntry ->
                    val subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
                    val versionId = backStackEntry.arguments?.getString("versionId") ?: ""
                    SubjectDetailScreen(
                        subjectName = subjectName,
                        versionId = versionId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showAboutSheet) {
        Dialog(
            onDismissRequest = { showAboutSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "About Classora",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1724)
                        )
                        IconButton(onClick = { showAboutSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Classora is your personal academic companion designed to help students manage their schedules, track attendance, and stay on top of assignments. We believe in simplicity and productivity, helping you focus on what matters most—your education.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Features",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1724)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FeatureListItem("Offline first, data privacy guaranteed")
                    FeatureListItem("Smart attendance tracking & analytics")
                    FeatureListItem("Automated Silent Mode during class")
                    FeatureListItem("Flexible task management with reminders")
                    FeatureListItem("Calendar sync with device events")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    AboutDetailItem("Version", "1.143.2")
                    AboutDetailItem("Developer", "Preetham")
                    AboutDetailItem("Build Date", "Sep 2026")
                    AboutDetailItem("License", "Standard License")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "© 2026 Classora Inc. All rights reserved.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureListItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Check, 
            contentDescription = null, 
            tint = Color(0xFF4CAF50), 
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
fun AboutDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1724))
    }
}
