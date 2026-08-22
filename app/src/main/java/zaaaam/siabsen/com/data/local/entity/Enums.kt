package zaaaam.siabsen.com.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    STUDENT, TEACHER, HOMEROOM_TEACHER, ADMIN;

    val isTeacherLike: Boolean get() = this == TEACHER || this == HOMEROOM_TEACHER
    val isStaff: Boolean get() = this != STUDENT

    fun label(): String = when (this) {
        STUDENT -> "Siswa"
        TEACHER -> "Guru"
        HOMEROOM_TEACHER -> "Wali Kelas"
        ADMIN -> "Admin"
    }
}

@Serializable
enum class AttendanceStatus {
    PRESENT, LATE, EXCUSED, SICK, ABSENT, DISPENSATION, EARLY_LEAVE, DUTY;

    val label: String
        get() = when (this) {
            PRESENT -> "Hadir"
            LATE -> "Terlambat"
            EXCUSED -> "Izin"
            SICK -> "Sakit"
            ABSENT -> "Alpa"
            DISPENSATION -> "Dispensasi"
            EARLY_LEAVE -> "Pulang Cepat"
            DUTY -> "Dinas"
        }

    /** Status yang dianggap mengurangi kehadiran */
    val countsAsAbsent: Boolean get() = this == ABSENT

    companion object {
        fun colorHex(s: AttendanceStatus): Long = when (s) {
            PRESENT -> 0xFF2E7D32
            LATE -> 0xFFF9A825
            EXCUSED -> 0xFF1565C0
            SICK -> 0xFF6A1B9A
            ABSENT -> 0xFFC62828
            DISPENSATION -> 0xFF00838F
            EARLY_LEAVE -> 0xFFEF6C00
            DUTY -> 0xFF37474F
        }
    }
}

@Serializable
enum class LeaveType { SICK, PERMISSION;

    val label: String get() = if (this == SICK) "Sakit" else "Izin" }

@Serializable
enum class LeaveStatus { PENDING, APPROVED, REJECTED;

    val label: String get() = when (this) {
        PENDING -> "Menunggu"
        APPROVED -> "Disetujui"
        REJECTED -> "Ditolak"
    } }

@Serializable
enum class SessionType { DAILY, SUBJECT;

    val label: String get() = if (this == DAILY) "Harian" else "Mapel" }

@Serializable
enum class AttendanceMethod { MANUAL, QR, SELF_CHECKIN;

    val label: String get() = when (this) {
        MANUAL -> "Manual Guru"
        QR -> "QR Code"
        SELF_CHECKIN -> "Check-in Mandiri"
    } }

@Serializable
enum class CalendarDayType { SCHOOL_DAY, HOLIDAY, EXAM, EVENT;

    val label: String get() = when (this) {
        SCHOOL_DAY -> "Hari Sekolah"
        HOLIDAY -> "Libur"
        EXAM -> "Ujian"
        EVENT -> "Kegiatan"
    } }
