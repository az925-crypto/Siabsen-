package zaaaam.siabsen.com.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Sesi absensi. Attendance jangan sekadar student.status;
 * record selalu terikat pada sesi (harian kelas / per mata pelajaran).
 */
@Serializable
@Entity(tableName = "attendance_sessions", indices = [Index("classId"), Index("dateEpochDay")])
data class AttendanceSessionEntity(
    @PrimaryKey val id: String,          // UUID
    val classId: Long,
    /** null => sesi harian (masuk/pulang sekolah) */
    val subjectId: Long? = null,
    val teacherId: Long? = null,
    val dateEpochDay: Long,
    val type: SessionType = SessionType.DAILY,
    val createdAt: Long = System.currentTimeMillis(),
    val createdByUserId: Long? = null,
    val closed: Boolean = false,
)

/** Satu baris per (sesi, siswa) */
@Serializable
@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = AttendanceSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["sessionId", "studentId"], unique = true), Index("studentId")]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val studentId: String,
    val status: AttendanceStatus,
    /** HH:mm check-in; null jika belum/absen */
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val note: String? = null,
    val method: AttendanceMethod = AttendanceMethod.MANUAL,
    val deviceId: String? = null,
    val recordedByUserId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Jejak koreksi status (audit trail) */
@Serializable
@Entity(tableName = "attendance_corrections", indices = [Index("recordId"), Index("studentId")])
data class AttendanceCorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val sessionId: String,
    val studentId: String,
    val oldStatus: AttendanceStatus,
    val newStatus: AttendanceStatus,
    val reason: String,
    val changedByUserId: Long?,
    val timestamp: Long = System.currentTimeMillis(),
)
