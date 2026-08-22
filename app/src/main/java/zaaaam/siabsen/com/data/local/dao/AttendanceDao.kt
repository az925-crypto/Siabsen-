package zaaaam.siabsen.com.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import zaaaam.siabsen.com.data.local.entity.AttendanceCorrectionEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceRecordEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceSessionEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.data.local.entity.StudentEntity

data class StatusCount(val status: AttendanceStatus, val cnt: Int)

data class RecordRow(
    @Embedded val record: AttendanceRecordEntity,
    val studentName: String,
    val className: String?,
    val sessionEpochDay: Long = 0,
)

data class SessionRow(
    @Embedded val session: AttendanceSessionEntity,
    val className: String,
    val subjectName: String?,
    val present: Int,
    val late: Int,
    val total: Int,
)

data class StudentRateRow(
    val studentId: String,
    val studentName: String,
    val className: String?,
    val total: Int,
    val attended: Int,
    val lateCnt: Int,
    val absentCnt: Int,
)

@Dao
interface AttendanceDao {

    // ---------- Sessions ----------
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(s: AttendanceSessionEntity): Long

    @Update suspend fun updateSession(s: AttendanceSessionEntity)

    @Query("SELECT * FROM attendance_sessions WHERE id = :id LIMIT 1")
    suspend fun sessionById(id: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE id = :id LIMIT 1")
    fun observeSession(id: String): Flow<AttendanceSessionEntity?>

    @Transaction
    @Query(
        """SELECT ses.*, c.name AS className, sub.name AS subjectName,
                  (SELECT COUNT(*) FROM attendance_records r WHERE r.sessionId = ses.id AND r.status = 'PRESENT') AS present,
                  (SELECT COUNT(*) FROM attendance_records r WHERE r.sessionId = ses.id AND r.status = 'LATE') AS late,
                  (SELECT COUNT(*) FROM attendance_records r WHERE r.sessionId = ses.id) AS total
           FROM attendance_sessions ses
           JOIN classes c ON c.id = ses.classId
           LEFT JOIN subjects sub ON sub.id = ses.subjectId
           WHERE ses.dateEpochDay BETWEEN :fromDay AND :toDay
           ORDER BY ses.dateEpochDay DESC, ses.createdAt DESC"""
    )
    fun observeSessionsBetween(fromDay: Long, toDay: Long): Flow<List<SessionRow>>

    @Query(
        """SELECT * FROM attendance_sessions
           WHERE classId = :classId AND dateEpochDay = :day AND type = 'DAILY' LIMIT 1"""
    )
    suspend fun dailySessionOf(classId: Long, day: Long): AttendanceSessionEntity?

    @Transaction
    @Query(
        """SELECT r.*, s.name AS studentName, c.name AS className
           FROM attendance_records r
           JOIN students s ON s.id = r.studentId
           LEFT JOIN classes c ON c.id = s.classId
           WHERE r.sessionId = :sessionId ORDER BY s.name"""
    )
    fun observeRecordsOfSession(sessionId: String): Flow<List<RecordRow>>

    @Transaction
    @Query(
        """SELECT r.*, s.name AS studentName, c.name AS className
           FROM attendance_records r
           JOIN students s ON s.id = r.studentId
           LEFT JOIN classes c ON c.id = s.classId
           WHERE r.sessionId = :sessionId ORDER BY s.name"""
    )
    suspend fun recordsOfSession(sessionId: String): List<RecordRow>

    @Query(
        """SELECT r.* FROM attendance_records r
           JOIN attendance_sessions s ON s.id = r.sessionId
           WHERE r.studentId = :studentId AND s.dateEpochDay = :day AND s.type = 'DAILY'
           LIMIT 1"""
    )
    suspend fun dailyRecordOf(studentId: String, day: Long): AttendanceRecordEntity?

    @Transaction
    @Query(
        """SELECT r.*, s.name AS studentName, c.name AS className, ses.dateEpochDay AS sessionEpochDay
           FROM attendance_records r
           JOIN students s ON s.id = r.studentId
           LEFT JOIN classes c ON c.id = s.classId
           JOIN attendance_sessions ses ON ses.id = r.sessionId
           WHERE r.studentId = :studentId AND ses.dateEpochDay BETWEEN :fromDay AND :toDay
           ORDER BY ses.dateEpochDay DESC"""
    )
    fun observeStudentRecords(studentId: String, fromDay: Long, toDay: Long): Flow<List<RecordRow>>

    @Upsert suspend fun upsertRecord(r: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId AND studentId = :studentId LIMIT 1")
    suspend fun recordOf(sessionId: String, studentId: String): AttendanceRecordEntity?

    @Query("UPDATE attendance_records SET checkOutTime = :time, updatedAt = :now WHERE id = :recordId")
    suspend fun setCheckOut(recordId: Long, time: String, now: Long)

    // ---------- Statistik ----------
    @Query(
        """SELECT r.status AS status, COUNT(*) AS cnt FROM attendance_records r
           JOIN attendance_sessions s ON s.id = r.sessionId
           WHERE r.studentId = :studentId AND s.dateEpochDay BETWEEN :fromDay AND :toDay AND s.type = 'DAILY'
           GROUP BY r.status"""
    )
    fun observeStatusCounts(studentId: String, fromDay: Long, toDay: Long): Flow<List<StatusCount>>

    @Query(
        """SELECT r.status AS status, COUNT(*) AS cnt FROM attendance_records r
           JOIN attendance_sessions s ON s.id = r.sessionId
           WHERE r.studentId = :studentId AND s.dateEpochDay BETWEEN :fromDay AND :toDay AND s.type = 'DAILY'
           GROUP BY r.status"""
    )
    suspend fun statusCounts(studentId: String, fromDay: Long, toDay: Long): List<StatusCount>

    /** Tingkat kehadiran per siswa (untuk early warning) */
    @Transaction
    @Query(
        """SELECT r.studentId AS studentId, st.name AS studentName, c.name AS className,
                  COUNT(*) AS total,
                  SUM(CASE WHEN r.status != 'ABSENT' THEN 1 ELSE 0 END) AS attended,
                  SUM(CASE WHEN r.status = 'LATE' THEN 1 ELSE 0 END) AS lateCnt,
                  SUM(CASE WHEN r.status = 'ABSENT' THEN 1 ELSE 0 END) AS absentCnt
           FROM attendance_records r
           JOIN attendance_sessions ses ON ses.id = r.sessionId AND ses.type = 'DAILY'
           JOIN students st ON st.id = r.studentId
           LEFT JOIN classes c ON c.id = st.classId
           WHERE ses.dateEpochDay BETWEEN :fromDay AND :toDay
             AND (:classId IS NULL OR st.classId = :classId)
           GROUP BY r.studentId"""
    )
    suspend fun studentRates(fromDay: Long, toDay: Long, classId: Long?): List<StudentRateRow>

    /** Rekap kelas per hari: hitungan status */
    @Query(
        """SELECT r.status AS status, COUNT(*) AS cnt FROM attendance_records r
           WHERE r.sessionId = :sessionId GROUP BY r.status"""
    )
    suspend fun sessionStatusCounts(sessionId: String): List<StatusCount>

    /** Siswa yang belum punya record pada sesi */
    @Query(
        """SELECT * FROM students WHERE active = 1 AND classId = :classId
           AND id NOT IN (SELECT studentId FROM attendance_records WHERE sessionId = :sessionId)
           ORDER BY name"""
    )
    suspend fun unrecordedStudents(classId: Long, sessionId: String): List<StudentEntity>

    // ---------- Koreksi ----------
    @Insert suspend fun insertCorrection(c: AttendanceCorrectionEntity)

    @Transaction
    @Query(
        """SELECT cor.*, s.name AS studentName FROM attendance_corrections cor
           JOIN students s ON s.id = cor.studentId
           ORDER BY cor.timestamp DESC LIMIT 200"""
    )
    fun observeCorrections(): Flow<List<CorrectionWithStudent>>
}

data class CorrectionWithStudent(
    @Embedded val correction: AttendanceCorrectionEntity,
    val studentName: String,
)
