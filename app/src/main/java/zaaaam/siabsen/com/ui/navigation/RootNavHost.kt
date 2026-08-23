package zaaaam.siabsen.com.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import zaaaam.siabsen.com.data.local.entity.Role
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.feature.admin.AuditLog
import zaaaam.siabsen.com.ui.feature.admin.BackupRestore
import zaaaam.siabsen.com.ui.feature.admin.CalendarManage
import zaaaam.siabsen.com.ui.feature.admin.ClassesManage
import zaaaam.siabsen.com.ui.feature.admin.Dashboard as AdminDashboard
import zaaaam.siabsen.com.ui.feature.admin.SchoolSettings
import zaaaam.siabsen.com.ui.feature.admin.StudentsManage
import zaaaam.siabsen.com.ui.feature.admin.SubjectsManage
import zaaaam.siabsen.com.ui.feature.admin.TeachersManage
import zaaaam.siabsen.com.ui.feature.admin.YearsManage
import zaaaam.siabsen.com.ui.feature.auth.LoginScreen
import zaaaam.siabsen.com.ui.feature.admin.AnnouncementsManage
import zaaaam.siabsen.com.ui.feature.guru.ClassList
import zaaaam.siabsen.com.ui.feature.guru.EarlyWarningScreen
import zaaaam.siabsen.com.ui.feature.guru.TeacherDashboard as TeacherDashboardScreen
import zaaaam.siabsen.com.ui.feature.guru.LeaveApproval
import zaaaam.siabsen.com.ui.feature.guru.QrBroadcast
import zaaaam.siabsen.com.ui.feature.guru.Recap
import zaaaam.siabsen.com.ui.feature.guru.Reports
import zaaaam.siabsen.com.ui.feature.guru.TakeAttendance
import zaaaam.siabsen.com.ui.feature.lock.AppLockScreen
import zaaaam.siabsen.com.ui.feature.shared.AccountScreen
import zaaaam.siabsen.com.ui.feature.shared.GlobalSearchScreen
import zaaaam.siabsen.com.ui.feature.shared.StudentDetailScreen
import zaaaam.siabsen.com.ui.feature.student.LeaveRequest
import zaaaam.siabsen.com.ui.feature.student.ScanQr
import zaaaam.siabsen.com.ui.feature.student.StudentHistory
import zaaaam.siabsen.com.ui.feature.student.StudentHome
import zaaaam.siabsen.com.ui.feature.student.StudentSchedule
import zaaaam.siabsen.com.ui.feature.student.StudentStats
import zaaaam.siabsen.com.ui.theme.SiabsenTheme

data class TabItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun RootNavHost() {
    val nav = rememberNavController()
    val rootVm: RootViewModel = hiltViewModel()
    val session = rootVm.session

    // Auto-lock: kunci aplikasi bila di background melebihi batas waktu
    val lifecycleOwner = LocalLifecycleOwner.current
    val lockScope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> rootVm.onAppBackground()
                Lifecycle.Event.ON_RESUME -> {
                    lockScope.launch {
                        if (rootVm.shouldLockOnResume()) {
                            nav.navigate(Routes.LOCK) { popUpTo(0) }
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val role = session.currentRole

    val studentTabs = listOf(
        TabItem(Routes.STUDENT_HOME, "Beranda", Icons.Filled.Home),
        TabItem(Routes.STUDENT_HISTORY, "Absensi", Icons.Filled.History),
        TabItem(Routes.STUDENT_SCHEDULE, "Jadwal", Icons.Filled.CalendarMonth),
        TabItem(Routes.STUDENT_STATS, "Statistik", Icons.Filled.BarChart),
        TabItem(Routes.PROFILE, "Akun", Icons.Filled.Person),
    )
    val teacherTabs = listOf(
        TabItem(Routes.GURU_DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
        TabItem(Routes.GURU_CLASSES, "Kelas", Icons.Filled.Groups),
        TabItem(Routes.GURU_LEAVES, "Izin", Icons.Filled.History),
        TabItem(Routes.GURU_REPORTS, "Laporan", Icons.Filled.BarChart),
        TabItem(Routes.PROFILE, "Akun", Icons.Filled.Person),
    )
    val adminTabs = listOf(
        TabItem(Routes.ADMIN_DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
        TabItem(Routes.ADMIN_STUDENTS, "Siswa", Icons.Filled.Groups),
        TabItem(Routes.ADMIN_SETTINGS, "Pengaturan", Icons.Filled.Settings),
        TabItem(Routes.ADMIN_AUDIT, "Audit", Icons.Filled.History),
        TabItem(Routes.PROFILE, "Akun", Icons.Filled.Person),
    )

    val r = currentRoute ?: ""
    val tabs: List<TabItem> = when {
        r.startsWith("student_") || ((r == Routes.PROFILE || r == Routes.SEARCH) && role == Role.STUDENT) -> studentTabs
        (r.startsWith("guru_") || r.startsWith("take_attendance") || r.startsWith("qr_broadcast") ||
            r.startsWith("recap") || r.startsWith("early_warning") ||
            r == Routes.PROFILE || r == Routes.SEARCH) &&
            (role == Role.TEACHER || role == Role.HOMEROOM_TEACHER) -> teacherTabs
        (r.startsWith("admin_") || r == Routes.PROFILE || r == Routes.SEARCH) && role == Role.ADMIN -> adminTabs
        else -> emptyList()
    }

    Scaffold(
        bottomBar = {
            if (tabs.isNotEmpty()) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(pad),
        ) {
            composable(Routes.LOGIN) { LoginScreen(nav) }
            composable(Routes.LOCK) { AppLockScreen(nav) }

            // ===== Siswa =====
            composable(Routes.STUDENT_HOME) { StudentHome(nav) }
            composable(Routes.STUDENT_SCAN) { ScanQr(nav) }
            composable(Routes.STUDENT_HISTORY) { StudentHistory(nav) }
            composable(Routes.STUDENT_SCHEDULE) { StudentSchedule(nav) }
            composable(Routes.STUDENT_STATS) { StudentStats(nav) }
            composable(Routes.STUDENT_LEAVE) { LeaveRequest(nav) }

            // ===== Guru / Wali kelas =====
            composable(Routes.GURU_DASHBOARD) { TeacherDashboardScreen(nav) }
            composable(Routes.GURU_CLASSES) { ClassList(nav) }
            composable(
                Routes.TAKE_ATTENDANCE_ROUTE,
                arguments = listOf(
                    navArgument("subjectId") {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    }
                ),
            ) { entry ->
                TakeAttendance(
                    nav,
                    classId = entry.arguments?.getString("classId")?.toLongOrNull() ?: 0L,
                    subjectId = entry.arguments?.getString("subjectId")?.toLongOrNull(),
                )
            }

            composable(Routes.EARLY_WARNING) { EarlyWarningScreen(nav) }
            composable(Routes.ADMIN_ANNOUNCEMENTS) { AnnouncementsManage(nav) }
            composable(Routes.QR_BROADCAST_ROUTE) { entry ->
                QrBroadcast(nav, entry.arguments?.getString("sessionId") ?: "")
            }
            composable(Routes.GURU_LEAVES) { LeaveApproval(nav) }
            composable(Routes.RECAP_ROUTE) { entry ->
                Recap(nav, entry.arguments?.getString("classId")?.toLongOrNull() ?: 0L)
            }
            composable(Routes.GURU_REPORTS) { Reports(nav) }

            // ===== Admin =====
            composable(Routes.ADMIN_DASHBOARD) { AdminDashboard(nav) }
            composable(Routes.ADMIN_STUDENTS) { StudentsManage(nav) }
            composable(Routes.ADMIN_TEACHERS) { TeachersManage(nav) }
            composable(Routes.ADMIN_CLASSES) { ClassesManage(nav) }
            composable(Routes.ADMIN_SUBJECTS) { SubjectsManage(nav) }
            composable(Routes.ADMIN_YEARS) { YearsManage(nav) }
            composable(Routes.ADMIN_CALENDAR) { CalendarManage(nav) }
            composable(Routes.ADMIN_SETTINGS) { SchoolSettings(nav) }
            composable(Routes.ADMIN_BACKUP) { BackupRestore(nav) }
            composable(Routes.ADMIN_AUDIT) { AuditLog(nav) }

            // ===== Shared =====
            composable(Routes.SEARCH) { GlobalSearchScreen(nav) }
            composable(Routes.STUDENT_DETAIL_ROUTE) { entry ->
                StudentDetailScreen(nav, entry.arguments?.getString("studentId") ?: "")
            }
            composable(Routes.PROFILE) { AccountScreen(nav) }
        }
    }
}
