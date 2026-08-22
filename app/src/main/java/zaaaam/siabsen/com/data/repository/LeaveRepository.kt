package zaaaam.siabsen.com.data.repository

import zaaaam.siabsen.com.data.local.dao.LeaveDao
import zaaaam.siabsen.com.data.local.dao.LeaveRow
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.data.local.entity.LeaveRequestEntity
import zaaaam.siabsen.com.data.local.entity.LeaveStatus
import zaaaam.siabsen.com.data.local.entity.LeaveType
import zaaaam.siabsen.com.security.AuditLogger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRepository @Inject constructor(
    private val leaveDao: LeaveDao,
    private val audit: AuditLogger,
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
