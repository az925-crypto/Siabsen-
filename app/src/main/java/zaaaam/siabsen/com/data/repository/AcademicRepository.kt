package zaaaam.siabsen.com.data.repository

import kotlinx.coroutines.flow.Flow
import zaaaam.siabsen.com.data.local.dao.AcademicDao
import zaaaam.siabsen.com.data.local.dao.ScheduleRow
import zaaaam.siabsen.com.data.local.entity.AcademicYearEntity
import zaaaam.siabsen.com.data.local.entity.CalendarDayType
import zaaaam.siabsen.com.data.local.entity.ScheduleEntity
import zaaaam.siabsen.com.data.local.entity.SchoolCalendarEntity
import zaaaam.siabsen.com.security.AuditLogger
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcademicRepository @Inject constructor(
    private val dao: AcademicDao,
    private val audit: AuditLogger,
) {
    // Mapel lookup (untuk sesi per pelajaran)
    suspend fun subjectById(id: Long) = dao.subjectById(id)

    // Jadwal
    fun scheduleForClass(classId: Long, day: DayOfWeek): Flow<List<ScheduleRow>> =
        dao.observeScheduleForClass(classId, day.value)

    fun weeklySchedule(classId: Long): Flow<List<ScheduleRow>> = dao.observeWeeklySchedule(classId)

    suspend fun saveSchedule(s: ScheduleEntity): Long {
        val id = dao.upsertSchedule(s)
        audit.log(if (s.id == 0L) "CREATE_SCHEDULE" else "UPDATE_SCHEDULE", "SCHEDULE", id.toString(), "class=${s.classId} subject=${s.subjectId} day=${s.dayOfWeek}")
        return id
    }

    suspend fun deleteSchedule(s: ScheduleEntity) {
        dao.deleteSchedule(s)
        audit.log("DELETE_SCHEDULE", "SCHEDULE", s.id.toString(), "")
    }

    // Tahun ajaran
    fun observeYears(): Flow<List<AcademicYearEntity>> = dao.observeAcademicYears()
    suspend fun activeYear(): AcademicYearEntity? = dao.activeAcademicYear()

    suspend fun saveYear(y: AcademicYearEntity) {
        val id = dao.upsertAcademicYear(y)
        if (y.isActive) dao.setActiveAcademicYear(id)
        audit.log(if (y.id == 0L) "CREATE_YEAR" else "UPDATE_YEAR", "YEAR", id.toString(), y.label)
    }

    suspend fun activateYear(id: Long) {
        dao.setActiveAcademicYear(id)
        audit.log("ACTIVATE_YEAR", "YEAR", id.toString(), "")
    }

    suspend fun deleteYear(id: Long) = dao.deleteAcademicYear(id)

    // Kalender
    fun observeCalendar(): Flow<List<SchoolCalendarEntity>> = dao.observeCalendar()
    suspend fun calendarDay(day: LocalDate): SchoolCalendarEntity? = dao.calendarDay(day.toEpochDay())

    suspend fun setCalendarDay(date: LocalDate, type: CalendarDayType, note: String?) {
        val existing = dao.calendarDay(date.toEpochDay())
        val entity = SchoolCalendarEntity(
            id = existing?.id ?: 0,
            dateEpochDay = date.toEpochDay(),
            type = type,
            note = note?.takeIf { it.isNotBlank() },
        )
        dao.upsertCalendarDay(entity)
        audit.log("SET_CALENDAR_DAY", "CALENDAR", date.toString(), "$type $note")
    }

    suspend fun deleteCalendarDay(id: Long) = dao.deleteCalendarDay(id)
}
