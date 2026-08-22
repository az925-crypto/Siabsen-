package zaaaam.siabsen.com.data.backup

import androidx.room.withTransaction
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import zaaaam.siabsen.com.data.local.SiabsenDatabase
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.security.AuditLogger
import java.io.BufferedReader
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: SiabsenDatabase,
    private val settingsRepo: SettingsRepository,
    private val audit: AuditLogger,
) {

    /** Buat file backup JSON dan tulis ke Uri (SAF). Return nama saran file. */
    suspend fun createBackup(target: Uri): String {
        val dao = db.backupDao()
        val file = BackupFile(
            createdAt = System.currentTimeMillis(),
            rowCount = 0, // dihitung setelah semua list terisi
            users = dao.users(),
            students = dao.students(),
            teachers = dao.teachers(),
            classes = dao.classes(),
            subjects = dao.subjects(),
            academicYears = dao.academicYears(),
            schoolCalendar = dao.calendar(),
            schedules = dao.schedules(),
            sessions = dao.sessions(),
            records = dao.records(),
            corrections = dao.corrections(),
            leaves = dao.leaves(),
            auditLogs = dao.auditLogs().take(2000),
        ).copy(rowCount = 0)

        val final = file.copy(rowCount = file.totalRows())
        context.contentResolver.openOutputStream(target)?.use { out ->
            out.write(BackupCodec.encode(final).toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Gagal membuka tujuan penyimpanan")

        audit.log("BACKUP", "DATABASE", null, "rows=${final.totalRows()} integrity=${final.integrityOk()}")
        return suggestedName()
    }

    fun suggestedName(): String =
        "siabsen_backup_${LocalDate.now()}.json"

    sealed class RestoreResult {
        data class Success(val rows: Int) : RestoreResult()
        data class Failure(val reason: String) : RestoreResult()
    }

    /**
     * Restore dari file JSON.
     * mode REPLACE: hapus isi lalu ganti total.
     * mode MERGE: gabung (baris dengan primary key sama ditimpa).
     */
    suspend fun restore(source: Uri, replace: Boolean): RestoreResult {
        val text = context.contentResolver.openInputStream(source)?.use { stream ->
            BufferedReader(stream.reader(Charsets.UTF_8)).readText()
        } ?: return RestoreResult.Failure("Tidak bisa membaca file")

        val parsed = BackupCodec.decode(text)
            ?: return RestoreResult.Failure("Format backup tidak valid")
        if (!parsed.integrityOk()) {
            // tetap lanjut tapi beri peringatan via reason? pilih gagal keras demi keamanan
            return RestoreResult.Failure("Integrity check gagal (${parsed.rowCount} vs ${parsed.totalRows()})")
        }

        db.withTransaction {
            val dao = db.backupDao()
            if (replace) {
                dao.clearUsedTokens()
                dao.clearInactiveBroadcasts()
                clearAll(dao)
            }
            dao.insertUsers(parsed.users)
            dao.insertTeachers(parsed.teachers)
            dao.insertClasses(parsed.classes)
            dao.insertSubjects(parsed.subjects)
            dao.insertYears(parsed.academicYears)
            dao.insertCalendar(parsed.schoolCalendar)
            dao.insertSchedules(parsed.schedules)
            dao.insertStudents(parsed.students)
            dao.insertSessions(parsed.sessions)
            dao.insertRecordsRaw(parsed.records)
            dao.insertCorrections(parsed.corrections)
            dao.insertLeaves(parsed.leaves)
            dao.insertAuditLogs(parsed.auditLogs)
        }
        audit.log(
            "RESTORE", "DATABASE", null,
            "mode=${if (replace) "REPLACE" else "MERGE"} rows=${parsed.totalRows()} createdAt=${Instant.ofEpochMilli(parsed.createdAt)}"
        )
        return RestoreResult.Success(parsed.totalRows())
    }

    private suspend fun clearAll(dao: zaaaam.siabsen.com.data.local.dao.BackupDao) {
        // urutan aman: child dulu
        db.openHelper.writableDatabase.execSQL("DELETE FROM attendance_corrections")
        db.openHelper.writableDatabase.execSQL("DELETE FROM attendance_records")
        db.openHelper.writableDatabase.execSQL("DELETE FROM attendance_sessions")
        db.openHelper.writableDatabase.execSQL("DELETE FROM leave_requests")
        db.openHelper.writableDatabase.execSQL("DELETE FROM used_qr_tokens")
        db.openHelper.writableDatabase.execSQL("DELETE FROM qr_broadcasts")
        db.openHelper.writableDatabase.execSQL("DELETE FROM audit_logs")
        db.openHelper.writableDatabase.execSQL("DELETE FROM schedules")
        db.openHelper.writableDatabase.execSQL("DELETE FROM school_calendar")
        db.openHelper.writableDatabase.execSQL("DELETE FROM academic_years")
        db.openHelper.writableDatabase.execSQL("DELETE FROM subjects")
        db.openHelper.writableDatabase.execSQL("DELETE FROM students")
        db.openHelper.writableDatabase.execSQL("DELETE FROM classes")
        db.openHelper.writableDatabase.execSQL("DELETE FROM teachers")
        db.openHelper.writableDatabase.execSQL("DELETE FROM users")
    }
}
