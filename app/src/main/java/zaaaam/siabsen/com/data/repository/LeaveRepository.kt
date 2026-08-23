package zaaaam.siabsen.com.data.repository

import zaaaam.siabsen.com.data.local.dao.LeaveDao
import zaaaam.siabsen.com.data.local.dao.LeaveRow
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.data.local.entity.LeaveRequestEntity
import zaaaam.siabsen.com.data.local.entity.LeaveStatus
import zaaaam.siabsen.com.data.local.entity.LeaveType
import zaaaam.siabsen.com.security.AuditLogger
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRepository @Inject constructor(
    private val leaveDao: LeaveDao,
    private val audit: AuditLogger,
    private val notifier: zaaaam.siabsen.com.notification.Notifier,
) {
    fun observePending(): Flow<List<LeaveRow>> = leaveDao.observePending()
    fun observeOfStudent(studentId: String): Flow<List<LeaveRow>> = leaveDao.observeOfStudent(studentId)

    suspend fun submit(leave: LeaveRequestEntity): Long {
        val id = leaveDao.insert(leave)
        audit.log("SUBMIT_LEAVE", "LEAVE", id.toString(), "student=${leave.studentId} type=${leave.type} from=${leave.dateFromEpochDay} to=${leave.dateToEpochDay}")
        return id
    }

    suspend fun decide(leave: LeaveRequestEntity, approve: Boolean, byUserId: Long, note: String?): Boolean {
        val updated = leave.copy(
            status = if (approve) LeaveStatus.APPROVED else LeaveStatus.REJECTED,
            decidedByUserId = byUserId,
            decidedAt = System.currentTimeMillis(),
            decisionNote = note,
        )
        leaveDao.update(updated)
        audit.log(
            if (approve) "APPROVE_LEAVE" else "REJECT_LEAVE", "LEAVE",
            leave.id.toString(),
            "student=${leave.studentId} type=${leave.type}"
        )
        // notifikasi ke siswa (lokal)
        notifier.leave(
            if (approve) "Izin disetujui" else "Izin ditolak",
            "Pengajuan ${leave.type.label} kamu mulai ${LocalDate.ofEpochDay(leave.dateFromEpochDay)} " +
                (note?.takeIf { it.isNotBlank() }?.let { "— $it" } ?: "") +
                if (approve) "." else ". Hubungi guru untuk info lebih lanjut."
        )
        return approve
    }

    /** Status absensi yang cocok untuk izin yang disetujui */
    fun statusFor(type: LeaveType): AttendanceStatus =
        if (type == LeaveType.SICK) AttendanceStatus.SICK else AttendanceStatus.EXCUSED

    companion object {
        fun defaultLeave(studentId: String, fromDay: Long, toDay: Long, reason: String, type: LeaveType, attachmentPath: String?) =
            LeaveRequestEntity(
                studentId = studentId,
                type = type,
                dateFromEpochDay = fromDay,
                dateToEpochDay = toDay,
                reason = reason,
                attachmentPath = attachmentPath,
            )
    }
}
