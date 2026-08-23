package zaaaam.siabsen.com.ui.navigation

object Routes {
    const val LOCK = "lock"
    const val LOGIN = "login"

    // Siswa
    const val STUDENT_HOME = "student_home"
    const val STUDENT_SCAN = "student_scan"
    const val STUDENT_HISTORY = "student_history"
    const val STUDENT_SCHEDULE = "student_schedule"
    const val STUDENT_STATS = "student_stats"
    const val STUDENT_LEAVE = "student_leave"
    const val PROFILE = "profile"

    // Guru / Wali kelas
    const val GURU_DASHBOARD = "guru_dashboard"
    const val GURU_CLASSES = "guru_classes"
    const val GURU_LEAVES = "guru_leaves"
    const val GURU_REPORTS = "guru_reports"
    const val EARLY_WARNING = "early_warning"

    fun takeAttendance(classId: Long, subjectId: Long? = null) =
        if (subjectId == null) "take_attendance/$classId"
        else "take_attendance/$classId?subjectId=$subjectId"
    const val TAKE_ATTENDANCE_ROUTE = "take_attendance/{classId}?subjectId={subjectId}"

    fun qrBroadcast(sessionId: String) = "qr_broadcast/$sessionId"
    const val QR_BROADCAST_ROUTE = "qr_broadcast/{sessionId}"

    fun recap(classId: Long) = "recap/$classId"
    const val RECAP_ROUTE = "recap/{classId}"

    // Admin
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_STUDENTS = "admin_students"
    const val ADMIN_TEACHERS = "admin_teachers"
    const val ADMIN_CLASSES = "admin_classes"
    const val ADMIN_SUBJECTS = "admin_subjects"
    const val ADMIN_YEARS = "admin_years"
    const val ADMIN_CALENDAR = "admin_calendar"
    const val ADMIN_SETTINGS = "admin_settings"
    const val ADMIN_BACKUP = "admin_backup"
    const val ADMIN_AUDIT = "admin_audit"
    const val ADMIN_ANNOUNCEMENTS = "admin_announcements"

    // Shared
    const val SEARCH = "search"
    fun studentDetail(id: String) = "student_detail/$id"
    const val STUDENT_DETAIL_ROUTE = "student_detail/{studentId}"
}
