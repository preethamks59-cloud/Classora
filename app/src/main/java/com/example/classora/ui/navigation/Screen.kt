package com.example.classora.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Attendance : Screen("attendance", "Attendance", Icons.Default.CheckCircle)
    data object Academics : Screen("academics", "Academics", Icons.Default.School)
    data object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
    data object More : Screen("more", "More", Icons.Default.Settings)
    data object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Person)
    data object NotificationSettings : Screen("notification_settings", "Notifications", Icons.Default.Settings)
    data object ReminderSettings : Screen("reminder_settings", "Reminders", Icons.Default.Settings)
    data object DndSettings : Screen("dnd_settings", "DND Settings", Icons.Default.Settings)
    data object CalendarSync : Screen("calendar_sync", "Calendar Sync", Icons.Default.Settings)
    data object DocumentVault : Screen("document_vault", "Document Vault", Icons.Default.Settings)
    data object AddTask : Screen("add_task", "Add Task", Icons.Default.Settings)
    data object ViewAllTasks : Screen("view_all_tasks", "All Tasks", Icons.Default.Settings)
    data object AddPreviousSemester : Screen("add_previous_semester", "Add CGPA", Icons.Default.School)
    data object SgpaCalculator : Screen("sgpa_calculator", "SGPA Calculator", Icons.Default.School)
    data object Notifications : Screen("notifications_screen", "Notifications", Icons.Default.Notifications)
    data object AddTimetable : Screen("add_timetable", "Add Timetable", Icons.Default.CalendarMonth)
    data object SubjectDetails : Screen("subject_details/{subjectName}/{versionId}", "Subject Details", Icons.Default.CheckCircle)
}
