package zaaaam.siabsen.com.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "leave_requests", indices = [Index("studentId"), Index("status")])
data class LeaveRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val type: LeaveType = LeaveType.PERMISSION,
    val dateFromEpochDay: Long,
    val dateToEpochDay: Long,
    val reason: String,
    /** path file lokal (foto/PDF bukti) */
    val attachmentPath: String? = null,
    val status: LeaveStatus = LeaveStatus.PENDING,
    val decidedByUserId: Long? = null,
    val decidedAt: Long? = null,
    val decisionNote: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actorUserId: Long? = null,
    val actorName: String,
    val action: String,
    val targetType: String? = null,
    val targetId: String? = null,
    val details: String,
)

/** QR broadcast untuk satu sesi; token berputar berdasarkan secret + window waktu */
@Serializable
@Entity(tableName = "qr_broadcasts")
data class QrBroadcastEntity(
    @PrimaryKey val sessionId: String,
    val secret: String,                  // hex acak per broadcast
    val rotationSeconds: Int = 30,
    val expiresAt: Long,                 // epoch millis
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/** One-time token yang sudah dipakai siswa (anti foto-ulang / replay) */
@Serializable
@Entity(tableName = "used_qr_tokens", primaryKeys = ["sessionId", "studentId", "token"])
data class UsedQrTokenEntity(
    val sessionId: String,
    val studentId: String,
    val token: String,
    val usedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val authorName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
