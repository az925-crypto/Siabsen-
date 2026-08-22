package zaaaam.siabsen.com.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Tahun ajaran + semester aktif */
@Serializable
@Entity(tableName = "academic_years")
data class AcademicYearEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                 // contoh: 2026/2027
    val semester: String,             // GANJIL / GENAP
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val isActive: Boolean = false,
) {
    val label: String get() = "$name — Semester $semester"
}

/** Kalender sekolah: hari libur/ujian/kegiatan agar tidak dihitung alpa */
@Serializable
@Entity(tableName = "school_calendar", indices = [androidx.room.Index(value = ["dateEpochDay"])])
data class SchoolCalendarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val type: CalendarDayType = CalendarDayType.SCHOOL_DAY,
    val note: String? = null,
)

/** Jadwal pelajaran per kelas */
@Serializable
@Entity(tableName = "schedules", indices = [androidx.room.Index("classId"), androidx.room.Index("subjectId")])
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val subjectId: Long,
    val teacherId: Long? = null,
    /** 1 = Senin ... 7 = Minggu (java.time DayOfWeek value) */
    val dayOfWeek: Int,
    /** format HH:mm */
    val startTime: String,
    val endTime: String,
)
