package zaaaam.siabsen.com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

/** DAO khusus backup/restore seluruh isi database */
@Dao
interface BackupDao {
    // Read semua tabel
    @Query("SELECT * FROM users") suspend fun users(): List<UserEntity>
    @Query("SELECT * FROM students") suspend fun students(): List<StudentEntity>
    @Query("SELECT * FROM teachers") suspend fun teachers(): List<TeacherEntity>
    @Query("SELECT * FROM classes") suspend fun classes(): List<ClassEntity>
    @Query("SELECT * FROM subjects") suspend fun subjects(): List<SubjectEntity>
    @Query("SELECT * FROM academic_years") suspend fun academicYears(): List<AcademicYearEntity>
    @Query("SELECT * FROM school_calendar") suspend fun calendar(): List<SchoolCalendarEntity>
    @Query("SELECT * FROM schedules") suspend fun schedules(): List<ScheduleEntity>
    @Query("SELECT * FROM attendance_sessions") suspend fun sessions(): List<AttendanceSessionEntity>
    @Query("SELECT * FROM attendance_records") suspend fun records(): List<AttendanceRecordEntity>
    @Query("SELECT * FROM attendance_corrections") suspend fun corrections(): List<AttendanceCorrectionEntity>
    @Query("SELECT * FROM leave_requests") suspend fun leaves(): List<LeaveRequestEntity>
    @Query("SELECT * FROM audit_logs") suspend fun auditLogs(): List<AuditLogEntity>

    // Insert dengan strategi tertentu
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertUsers(v: List<UserEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStudents(v: List<StudentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTeachers(v: List<TeacherEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertClasses(v: List<ClassEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSubjects(v: List<SubjectEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertYears(v: List<AcademicYearEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCalendar(v: List<SchoolCalendarEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSchedules(v: List<ScheduleEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSessions(v: List<AttendanceSessionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRecordsRaw(v: List<AttendanceRecordEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCorrections(v: List<AttendanceCorrectionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLeaves(v: List<LeaveRequestEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAuditLogs(v: List<AuditLogEntity>)

    // Clear (mode replace)
    @Query("DELETE FROM used_qr_tokens") suspend fun clearUsedTokens()
    @Query("DELETE FROM qr_broadcasts WHERE active = 0") suspend fun clearInactiveBroadcasts()
}
