package zaaaam.siabsen.com.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import zaaaam.siabsen.com.data.local.dao.AcademicDao
import zaaaam.siabsen.com.data.local.dao.AttendanceDao
import zaaaam.siabsen.com.data.local.dao.AuditDao
import zaaaam.siabsen.com.data.local.dao.BackupDao
import zaaaam.siabsen.com.data.local.dao.LeaveDao
import zaaaam.siabsen.com.data.local.dao.QrDao
import zaaaam.siabsen.com.data.local.dao.RosterDao
import zaaaam.siabsen.com.data.local.entity.AcademicYearEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceCorrectionEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceRecordEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceSessionEntity
import zaaaam.siabsen.com.data.local.entity.AuditLogEntity
import zaaaam.siabsen.com.data.local.entity.ClassEntity
import zaaaam.siabsen.com.data.local.entity.LeaveRequestEntity
import zaaaam.siabsen.com.data.local.entity.QrBroadcastEntity
import zaaaam.siabsen.com.data.local.entity.SchoolCalendarEntity
import zaaaam.siabsen.com.data.local.entity.ScheduleEntity
import zaaaam.siabsen.com.data.local.entity.StudentEntity
import zaaaam.siabsen.com.data.local.entity.SubjectEntity
import zaaaam.siabsen.com.data.local.entity.TeacherEntity
import zaaaam.siabsen.com.data.local.entity.UsedQrTokenEntity
import zaaaam.siabsen.com.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        StudentEntity::class,
        TeacherEntity::class,
        ClassEntity::class,
        SubjectEntity::class,
        AcademicYearEntity::class,
        SchoolCalendarEntity::class,
        ScheduleEntity::class,
        AttendanceSessionEntity::class,
        AttendanceRecordEntity::class,
        AttendanceCorrectionEntity::class,
        LeaveRequestEntity::class,
        AuditLogEntity::class,
        QrBroadcastEntity::class,
        UsedQrTokenEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SiabsenDatabase : RoomDatabase() {
    abstract fun rosterDao(): RosterDao
    abstract fun academicDao(): AcademicDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun leaveDao(): LeaveDao
    abstract fun auditDao(): AuditDao
    abstract fun qrDao(): QrDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "siabsen.db"
    }
}
