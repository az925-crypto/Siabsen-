package zaaaam.siabsen.com.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import zaaaam.siabsen.com.data.local.entity.LeaveRequestEntity
import zaaaam.siabsen.com.data.local.entity.AuditLogEntity
import zaaaam.siabsen.com.data.local.entity.QrBroadcastEntity
import zaaaam.siabsen.com.data.local.entity.UsedQrTokenEntity

data class LeaveRow(
    @Embedded val leave: LeaveRequestEntity,
    val studentName: String,
    val className: String?,
)

@Dao
interface LeaveDao {

    @Transaction
    @Query(
        """SELECT l.*, s.name AS studentName, c.name AS className
           FROM leave_requests l
           JOIN students s ON s.id = l.studentId
           LEFT JOIN classes c ON c.id = s.classId
           WHERE l.status = 'PENDING'
           ORDER BY l.createdAt DESC"""
    )
    fun observePending(): Flow<List<LeaveRow>>

    @Transaction
    @Query(
        """SELECT l.*, s.name AS studentName, NULL AS className
           FROM leave_requests l
           JOIN students s ON s.id = l.studentId
           WHERE l.studentId = :studentId
           ORDER BY l.createdAt DESC"""
    )
    fun observeOfStudent(studentId: String): Flow<List<LeaveRow>>

    @Insert suspend fun insert(l: LeaveRequestEntity): Long

    @Update suspend fun update(l: LeaveRequestEntity)

    /** Izin disetujui yang mencakup tanggal tertentu */
    @Query(
        """SELECT * FROM leave_requests
           WHERE studentId = :studentId AND status = 'APPROVED'
             AND dateFromEpochDay <= :day AND dateToEpochDay >= :day LIMIT 1"""
    )
    suspend fun approvedLeaveCovering(studentId: String, day: Long): LeaveRequestEntity?

    @Query("DELETE FROM leave_requests")
    suspend fun clearLeaves()
}

@Dao
interface AuditDao {

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 500")
    fun observeAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY timestamp DESC LIMIT 200")
    fun observeByAction(action: String): Flow<List<AuditLogEntity>>

    @Insert suspend fun insert(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}

@Dao
interface QrDao {

    @Insert suspend fun insert(b: QrBroadcastEntity)

    @Update suspend fun update(b: QrBroadcastEntity)

    @Query("SELECT * FROM qr_broadcasts WHERE sessionId = :sessionId LIMIT 1")
    suspend fun bySession(sessionId: String): QrBroadcastEntity?

    @Query("SELECT * FROM qr_broadcasts WHERE sessionId = :sessionId LIMIT 1")
    fun observeBySession(sessionId: String): Flow<QrBroadcastEntity?>

    @Query("UPDATE qr_broadcasts SET active = 0 WHERE sessionId = :sessionId")
    suspend fun deactivate(sessionId: String)

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertUsedToken(t: UsedQrTokenEntity): Long

    @Query(
        """SELECT COUNT(*) FROM used_qr_tokens
           WHERE sessionId = :sessionId AND studentId = :studentId AND token = :token"""
    )
    suspend fun tokenUsedCount(sessionId: String, studentId: String, token: String): Int

    @Query("DELETE FROM used_qr_tokens")
    suspend fun clearTokens()
}
