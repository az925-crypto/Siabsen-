package zaaaam.siabsen.com.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import zaaaam.siabsen.com.data.local.entity.AcademicYearEntity
import zaaaam.siabsen.com.data.local.entity.SchoolCalendarEntity
import zaaaam.siabsen.com.data.local.entity.ScheduleEntity

data class ScheduleRow(
    @Embedded val schedule: ScheduleEntity,
    val subjectName: String,
    val teacherName: String?,
)

@Dao
interface AcademicDao {

    // ---------- Jadwal ----------
    @Transaction
    @Query(
        """SELECT sch.*, sub.name AS subjectName, t.name AS teacherName
           FROM schedules sch
           JOIN subjects sub ON sub.id = sch.subjectId
           LEFT JOIN teachers t ON t.id = sch.teacherId
           WHERE sch.classId = :classId AND sch.dayOfWeek = :dayOfWeek
           ORDER BY sch.startTime"""
    )
    fun observeScheduleForClass(classId: Long, dayOfWeek: Int): Flow<List<ScheduleRow>>

    @Transaction
    @Query(
        """SELECT sch.*, sub.name AS subjectName, t.name AS teacherName
           FROM schedules sch
           JOIN subjects sub ON sub.id = sch.subjectId
           LEFT JOIN teachers t ON t.id = sch.teacherId
           WHERE sch.classId = :classId
           ORDER BY CASE sch.dayOfWeek WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 3 WHEN 4 THEN 4 WHEN 5 THEN 5 WHEN 6 THEN 6 ELSE 7 END, sch.startTime"""
    )
    fun observeWeeklySchedule(classId: Long): Flow<List<ScheduleRow>>

    @Query("SELECT * FROM schedules WHERE classId = :classId")
    suspend fun schedulesOfClass(classId: Long): List<ScheduleEntity>

    @Upsert suspend fun upsertSchedule(s: ScheduleEntity): Long

    @Delete suspend fun deleteSchedule(s: ScheduleEntity)

    @Query("DELETE FROM schedules")
    suspend fun clearSchedules()

    @Insert suspend fun insertSchedules(list: List<ScheduleEntity>)

    // ---------- Tahun ajaran ----------
    @Query("SELECT * FROM academic_years ORDER BY startDateEpochDay DESC")
    fun observeAcademicYears(): Flow<List<AcademicYearEntity>>

    @Query("SELECT * FROM academic_years WHERE isActive = 1 LIMIT 1")
    suspend fun activeAcademicYear(): AcademicYearEntity?

    @Upsert suspend fun upsertAcademicYear(y: AcademicYearEntity): Long

    @Query("UPDATE academic_years SET isActive = (id = :id)")
    suspend fun setActiveAcademicYear(id: Long)

    @Query("DELETE FROM academic_years WHERE id = :id")
    suspend fun deleteAcademicYear(id: Long)

    @Query("DELETE FROM academic_years")
    suspend fun clearAcademicYears()

    // ---------- Kalender sekolah ----------
    @Query("SELECT * FROM school_calendar WHERE dateEpochDay = :epochDay LIMIT 1")
    suspend fun calendarDay(epochDay: Long): SchoolCalendarEntity?

    @Query("SELECT * FROM school_calendar ORDER BY dateEpochDay")
    fun observeCalendar(): Flow<List<SchoolCalendarEntity>>

    @Upsert suspend fun upsertCalendarDay(c: SchoolCalendarEntity): Long

    @Query("DELETE FROM school_calendar WHERE id = :id")
    suspend fun deleteCalendarDay(id: Long)

    @Query("DELETE FROM school_calendar")
    suspend fun clearCalendar()
}
