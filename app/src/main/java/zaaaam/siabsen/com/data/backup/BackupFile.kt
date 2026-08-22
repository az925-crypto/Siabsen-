package zaaaam.siabsen.com.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import zaaaam.siabsen.com.data.local.entity.AcademicYearEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceCorrectionEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceRecordEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceSessionEntity
import zaaaam.siabsen.com.data.local.entity.AuditLogEntity
import zaaaam.siabsen.com.data.local.entity.ClassEntity
import zaaaam.siabsen.com.data.local.entity.LeaveRequestEntity
import zaaaam.siabsen.com.data.local.entity.ScheduleEntity
import zaaaam.siabsen.com.data.local.entity.SchoolCalendarEntity
import zaaaam.siabsen.com.data.local.entity.StudentEntity
import zaaaam.siabsen.com.data.local.entity.SubjectEntity
import zaaaam.siabsen.com.data.local.entity.TeacherEntity
import zaaaam.siabsen.com.data.local.entity.UserEntity

@Serializable
data class BackupFile(
    val backupVersion: Int = 1,
    val appName: String = "SiAbsen",
    val createdAt: Long,
    /** checksum sederhana: jumlah seluruh baris */
    val rowCount: Int,
    val users: List<UserEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val teachers: List<TeacherEntity> = emptyList(),
    val classes: List<ClassEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val academicYears: List<AcademicYearEntity> = emptyList(),
    val schoolCalendar: List<SchoolCalendarEntity> = emptyList(),
    val schedules: List<ScheduleEntity> = emptyList(),
    val sessions: List<AttendanceSessionEntity> = emptyList(),
    val records: List<AttendanceRecordEntity> = emptyList(),
    val corrections: List<AttendanceCorrectionEntity> = emptyList(),
    val leaves: List<LeaveRequestEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
) {
    fun totalRows(): Int =
        users.size + students.size + teachers.size + classes.size + subjects.size +
            academicYears.size + schoolCalendar.size + schedules.size + sessions.size +
            records.size + corrections.size + leaves.size + auditLogs.size

    fun integrityOk(): Boolean = totalRows() == rowCount
}

object BackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): BackupFile? = runCatching {
        json.decodeFromString(BackupFile.serializer(), text)
    }.getOrNull()
}
