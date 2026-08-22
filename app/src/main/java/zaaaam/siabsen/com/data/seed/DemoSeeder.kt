package zaaaam.siabsen.com.data.seed

import zaaaam.siabsen.com.data.local.SiabsenDatabase
import zaaaam.siabsen.com.data.local.entity.AcademicYearEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceRecordEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceSessionEntity
import zaaaam.siabsen.com.data.local.entity.ClassEntity
import zaaaam.siabsen.com.data.local.entity.Role
import zaaaam.siabsen.com.data.local.entity.ScheduleEntity
import zaaaam.siabsen.com.data.local.entity.SessionType
import zaaaam.siabsen.com.data.local.entity.StudentEntity
import zaaaam.siabsen.com.data.local.entity.SubjectEntity
import zaaaam.siabsen.com.data.local.entity.TeacherEntity
import zaaaam.siabsen.com.data.local.entity.UserEntity
import zaaaam.siabsen.com.security.PinHasher
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seed data awal supaya aplikasi langsung bisa dicoba:
 * admin (PIN default 123456), guru, wali kelas, siswa, kelas, mapel, jadwal,
 * dan riwayat absensi 30 hari terakhir untuk demo statistik.
 */
@Singleton
class DemoSeeder @Inject constructor(
    private val db: SiabsenDatabase,
    private val hasher: PinHasher,
) {
    suspend fun seedIfEmpty() {
        val roster = db.rosterDao()
        // cek cepat: jika sudah ada user admin, skip
        if (roster.userByUsername("admin") != null) return

        val academicId = db.academicDao().upsertAcademicYear(
            AcademicYearEntity(
                name = "${LocalDate.now().year}/${LocalDate.now().year + 1}",
                semester = "GANJIL",
                startDateEpochDay = LocalDate.of(LocalDate.now().year, 7, 1).toEpochDay(),
                endDateEpochDay = LocalDate.of(LocalDate.now().year + 1, 6, 30).toEpochDay(),
                isActive = true,
            )
        )

        val teacherGuru = roster.upsertTeacher(TeacherEntity(name = "Budi Santoso", nip = "19800101"))
        val teacherWali = roster.upsertTeacher(
            TeacherEntity(name = "Sari Wulandari", nip = "19850505", homeroomClassId = null)
        )

        val classXiiIp1 = roster.upsertClass(ClassEntity(name = "XII IPA 1", homeroomTeacherId = teacherWali, academicYearId = academicId))
        val classXiIpa2 = roster.upsertClass(ClassEntity(name = "XI IPA 2", homeroomTeacherId = null, academicYearId = academicId))

        // update wali kelas -> kelas
        roster.upsertTeacher(TeacherEntity(id = teacherWali, name = "Sari Wulandari", nip = "19850505", homeroomClassId = classXiiIp1))

        val subjects = listOf("Matematika", "Fisika", "Bahasa Indonesia", "Sejarah")
            .mapIndexed { i, n -> roster.upsertSubject(SubjectEntity(name = n, code = "MP${i + 1}")) }

        // jadwal contoh XII IPA 1 Senin-Kamis
        subjects.forEachIndexed { i, subId ->
            for (d in 1..4) {
                db.academicDao().insertSchedules(
                    listOf(
                        ScheduleEntity(
                            classId = classXiiIp1,
                            subjectId = subId,
                            teacherId = if (subId == subjects[0]) teacherGuru else teacherWali,
                            dayOfWeek = d,
                            startTime = "%02d:00".format(7 + i),
                            endTime = "%02d:30".format(7 + i),
                        )
                    )
                )
            }
        }

        val students = listOf(
            Triple("001", "Azzam Alfarisy", "L"),
            Triple("002", "Budi Pratama", "L"),
            Triple("003", "Citra Maharani", "P"),
            Triple("004", "Dimas Saputra", "L"),
            Triple("005", "Eka Putri", "P"),
        ).map { (nis, nama, g) ->
            StudentEntity(id = nis, name = nama, gender = g, classId = classXiiIp1)
        }
        roster.insertStudents(students)

        val pinHash = hasher.hash("123456")
        roster.insertUsers(
            listOf(
                UserEntity(username = "admin", displayName = "Administrator", role = Role.ADMIN, pinHash = pinHash),
                UserEntity(username = "guru", displayName = "Budi Santoso", role = Role.TEACHER, linkedTeacherId = teacherGuru, pinHash = pinHash),
                UserEntity(username = "wali", displayName = "Sari Wulandari", role = Role.HOMEROOM_TEACHER, linkedTeacherId = teacherWali, pinHash = pinHash),
                UserEntity(username = "azzam", displayName = "Azzam Alfarisy", role = Role.STUDENT, linkedStudentId = "001", pinHash = pinHash),
            )
        )

        seedAttendanceHistory(students.map { it.id }, classXiiIp1)
    }

    private suspend fun seedAttendanceHistory(studentIds: List<String>, classId: Long) {
        val att = db.attendanceDao()
        var rnd = System.currentTimeMillis()
        fun next(): Int {
            rnd = rnd * 6364136223846793005L + 1442695040888963407L
            return ((rnd shr 33).toInt() and Int.MAX_VALUE)
        }
        val today = LocalDate.now()
        for (back in 1..30) {
            val day = today.minusDays(back.toLong())
            if (day.dayOfWeek == DayOfWeek.SUNDAY) continue
            val sesId = UUID.randomUUID().toString()
            att.insertSession(
                AttendanceSessionEntity(
                    id = sesId, classId = classId, dateEpochDay = day.toEpochDay(),
                    type = SessionType.DAILY, closed = true,
                )
            )
            studentIds.forEachIndexed { idx, sid ->
                val r = next() % 100
                val status = when {
                    r < 82 -> zaaaam.siabsen.com.data.local.entity.AttendanceStatus.PRESENT
                    r < 92 -> zaaaam.siabsen.com.data.local.entity.AttendanceStatus.LATE
                    r < 95 -> zaaaam.siabsen.com.data.local.entity.AttendanceStatus.EXCUSED
                    r < 97 -> zaaaam.siabsen.com.data.local.entity.AttendanceStatus.SICK
                    else -> zaaaam.siabsen.com.data.local.entity.AttendanceStatus.ABSENT
                }
                att.insertRecords(
                    listOf(
                        AttendanceRecordEntity(
                            sessionId = sesId,
                            studentId = sid,
                            status = status,
                            checkInTime = when (status) {
                                zaaaam.siabsen.com.data.local.entity.AttendanceStatus.PRESENT -> "06:%02d".format(next() % 45)
                                zaaaam.siabsen.com.data.local.entity.AttendanceStatus.LATE -> "07:%02d".format(next() % 40)
                                else -> null
                            },
                        )
                    )
                )
            }
        }
    }
}
