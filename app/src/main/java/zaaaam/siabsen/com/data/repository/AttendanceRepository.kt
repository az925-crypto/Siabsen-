package zaaaam.siabsen.com.data.repository

import zaaaam.siabsen.com.data.local.dao.AttendanceDao
import zaaaam.siabsen.com.data.local.dao.StatusCount
import zaaaam.siabsen.com.data.local.dao.StudentRateRow
import zaaaam.siabsen.com.data.local.entity.AttendanceCorrectionEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceMethod
import zaaaam.siabsen.com.data.local.entity.AttendanceRecordEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceSessionEntity
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.data.local.entity.CalendarDayType
import zaaaam.siabsen.com.data.local.entity.SessionType
import zaaaam.siabsen.com.qr.QrCodec
import zaaaam.siabsen.com.security.AuditLogger
import zaaaam.siabsen.com.security.SessionManager
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class CheckResult {
    data class Success(
        val status: AttendanceStatus,
        val time: String,
        val message: String,
    ) : CheckResult()
    data class Failure(val reason: String) : CheckResult()
}

data class EarlyWarningItem(
    val studentId: String,
    val studentName: String,
    val className: String?,
    val ratePercent: Int,
    val lateCnt: Int,
    val absentCnt: Int,
) {
    fun level(warnThreshold: Int, criticalThreshold: Int): Int =
        when {
            ratePercent < criticalThreshold -> 2   // merah
            ratePercent < warnThreshold -> 1       // kuning
            else -> 0                              // hijau
        }
}

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val academicDao: zaaaam.siabsen.com.data.local.dao.AcademicDao,
    private val rosterDao: zaaaam.siabsen.com.data.local.dao.RosterDao,
    private val leaveDao: zaaaam.siabsen.com.data.local.dao.LeaveDao,
    private val qrDao: zaaaam.siabsen.com.data.local.dao.QrDao,
    private val qrCodec: QrCodec,
    private val settingsRepo: SettingsRepository,
    private val session: SessionManager,
    private val audit: AuditLogger,
) {

    // ================= Sesi =================

    suspend fun ensureDailySession(classId: Long, day: LocalDate = LocalDate.now()): AttendanceSessionEntity? {
        val existing = attendanceDao.dailySessionOf(classId, day.toEpochDay())
        if (existing != null) return existing
        val s = AttendanceSessionEntity(
            id = UUID.randomUUID().toString(),
            classId = classId,
            subjectId = null,
            dateEpochDay = day.toEpochDay(),
            type = SessionType.DAILY,
            createdByUserId = session.currentUserId,
        )
        attendanceDao.insertSession(s)
        return s
    }

    suspend fun createSubjectSession(classId: Long, subjectId: Long, type: SessionType = SessionType.SUBJECT): AttendanceSessionEntity? {
        val s = AttendanceSessionEntity(
            id = UUID.randomUUID().toString(),
            classId = classId,
            subjectId = subjectId,
            teacherId = session.linkedTeacherId,
            dateEpochDay = LocalDate.now().toEpochDay(),
            type = type,
            createdByUserId = session.currentUserId,
        )
        val ok = attendanceDao.insertSession(s) != -1L
        audit.log("CREATE_SESSION", "SESSION", s.id, "class=$classId subject=$subjectId type=$type")
        return if (ok || attendanceDao.sessionById(s.id) != null) s else null
    }

    suspend fun closeSession(sessionId: String) {
        val s = attendanceDao.sessionById(sessionId) ?: return
        attendanceDao.updateSession(s.copy(closed = true))
        audit.log("CLOSE_SESSION", "SESSION", sessionId, "")
    }

    suspend fun recordOf(sessionId: String, studentId: String) =
        attendanceDao.recordOf(sessionId, studentId)

    suspend fun sessionById(id: String) = attendanceDao.sessionById(id)
    fun observeSession(id: String) = attendanceDao.observeSession(id)

    /** Mulai broadcast QR untuk sesi (guru menampilkan QR di layar). */
    suspend fun startBroadcast(sessionId: String, secret: String, rotationSeconds: Int, expiresAt: Long) {
        // matikan broadcast lama sesi ini bila ada
        qrDao.bySession(sessionId)?.let { qrDao.deactivate(sessionId) }
        qrDao.insert(
            zaaaam.siabsen.com.data.local.entity.QrBroadcastEntity(
                sessionId = sessionId,
                secret = secret,
                rotationSeconds = rotationSeconds,
                expiresAt = expiresAt,
                active = true,
            )
        )
        audit.log("START_QR_BROADCAST", "SESSION", sessionId, "rotation=${rotationSeconds}s")
    }

    fun observeSessionsBetween(from: Long, to: Long) = attendanceDao.observeSessionsBetween(from, to)
    fun observeRecordsOfSession(sessionId: String) = attendanceDao.observeRecordsOfSession(sessionId)
    suspend fun recordsOfSession(sessionId: String) = attendanceDao.recordsOfSession(sessionId)
    suspend fun unrecordedStudents(classId: Long, sessionId: String) =
        attendanceDao.unrecordedStudents(classId, sessionId)

    // ================= Penandaan =================

    /**
     * Guru menandai status siswa pada sesi.
     * Jika record sudah ada → dianggap koreksi (wajib alasan), masuk tabel koreksi + audit.
     */
    suspend fun mark(
        sessionId: String,
        studentId: String,
        newStatus: AttendanceStatus,
        method: AttendanceMethod = AttendanceMethod.MANUAL,
        note: String? = null,
        correctionReason: String? = null,
    ): CheckResult {
        val ses = attendanceDao.sessionById(sessionId)
            ?: return CheckResult.Failure("Sesi tidak ditemukan")
        if (ses.closed) return CheckResult.Failure("Sesi sudah ditutup")

        val existing = attendanceDao.recordOf(sessionId, studentId)
        if (existing != null && existing.status == newStatus && note == null) {
            return CheckResult.Success(newStatus, existing.checkInTime ?: "", "Tidak berubah")
        }

        val now = System.currentTimeMillis()
        val nowTime = LocalTime.now().format(FMT)

        if (existing != null) {
            val reason = correctionReason?.takeIf { it.isNotBlank() }
                ?: return CheckResult.Failure("Perubahan status wajib menyertakan alasan koreksi")
            attendanceDao.upsertRecord(
                existing.copy(status = newStatus, note = note ?: existing.note, updatedAt = now, recordedByUserId = session.currentUserId)
            )
            attendanceDao.insertCorrection(
                AttendanceCorrectionEntity(
                    recordId = existing.id,
                    sessionId = sessionId,
                    studentId = studentId,
                    oldStatus = existing.status,
                    newStatus = newStatus,
                    reason = reason,
                    changedByUserId = session.currentUserId,
                )
            )
            audit.log(
                "CHANGE_ATTENDANCE", "RECORD", existing.id.toString(),
                "student=$studentId OLD=${existing.status} NEW=$newStatus reason=$reason"
            )
            return CheckResult.Success(newStatus, nowTime, "Diperbarui: ${existing.status} → ${newStatus.label}")
        }

        attendanceDao.upsertRecord(
            AttendanceRecordEntity(
                sessionId = sessionId,
                studentId = studentId,
                status = newStatus,
                checkInTime = if (newStatus == AttendanceStatus.PRESENT || newStatus == AttendanceStatus.LATE) nowTime else null,
                note = note,
                method = method,
                deviceId = deviceId(),
                recordedByUserId = session.currentUserId,
                updatedAt = now,
            )
        )
        return CheckResult.Success(newStatus, nowTime, "Tersimpan: ${newStatus.label}")
    }

    /** Check-in mandiri siswa (QR atau tombol). Status ditentukan jam vs pengaturan. */
    suspend fun selfCheckIn(studentId: String, method: AttendanceMethod, deviceId: String?): CheckResult {
        val settings = settingsRepo.current()
        val today = LocalDate.now()
        val dayCheck = validateSchoolDay(today, settings)
        if (dayCheck != null) return CheckResult.Failure(dayCheck)

        val leave = leaveDao.approvedLeaveCovering(studentId, today.toEpochDay())
        if (leave != null) return CheckResult.Failure("Kamu punya izin ${leave.type.label} yang disetujui hari ini")

        val student = rosterDao.studentRaw(studentId)
            ?: return CheckResult.Failure("Data siswa tidak ditemukan")
        val classId = student.classId ?: return CheckResult.Failure("Kamu belum ditempatkan di kelas mana pun")

        val ses = ensureDailySession(classId, today) ?: return CheckResult.Failure("Gagal membuat sesi")
        if (attendanceDao.recordOf(ses.id, studentId)?.let { it.checkInTime != null } == true) {
            return CheckResult.Failure("Kamu sudah check-in hari ini")
        }

        val now = LocalTime.now()
        val onTimeUntil = parseHm(settings.onTimeUntil)
        val lateUntil = parseHm(settings.lateUntil)
        val status = when {
            now.isAfter(lateUntil) -> null
            now.isAfter(onTimeUntil) -> AttendanceStatus.LATE
            else -> AttendanceStatus.PRESENT
        } ?: return CheckResult.Failure("Window absensi sudah ditutup (${settings.lateUntil})")

        val rec = attendanceDao.recordOf(ses.id, studentId)
        val entity = AttendanceRecordEntity(
            id = rec?.id ?: 0,
            sessionId = ses.id,
            studentId = studentId,
            status = status,
            checkInTime = now.format(FMT),
            method = method,
            deviceId = deviceId,
            recordedByUserId = session.currentUserId,
            updatedAt = System.currentTimeMillis(),
        )
        attendanceDao.upsertRecord(entity)
        audit.log("SELF_CHECKIN", "STUDENT", studentId, "status=$status method=$method time=${entity.checkInTime}")
        return CheckResult.Success(status, entity.checkInTime!!, "Berhasil: ${status.label} pukul ${entity.checkInTime}")
    }

    /** Check-out (jam pulang) */
    suspend fun selfCheckOut(studentId: String): CheckResult {
        val today = LocalDate.now().toEpochDay()
        val rec = attendanceDao.dailyRecordOf(studentId, today)
            ?: return CheckResult.Failure("Belum ada data absensi hari ini")
        if (rec.checkInTime == null) return CheckResult.Failure("Kamu belum check-in")
        if (rec.checkOutTime != null) return CheckResult.Failure("Sudah check-out pukul ${rec.checkOutTime}")
        val nowStr = LocalTime.now().format(FMT)
        attendanceDao.setCheckOut(rec.id, nowStr, System.currentTimeMillis())
        audit.log("SELFCHECKOUT", "STUDENT", studentId, "time=$nowStr")
        return CheckResult.Success(rec.status, nowStr, "Check-out pukul $nowStr")
    }

    /**
     * Validasi payload QR hasil scan:
     * format → broadcast aktif → HMAC valid & belum kedaluwarsa → token belum dipakai → catat.
     */
    suspend fun qrCheckIn(studentId: String, payload: String, deviceId: String?): CheckResult {
        val parsed = qrCodec.parse(payload)
            ?: return CheckResult.Failure("QR tidak dikenali (bukan QR SiAbsen)")

        val broadcast = qrDao.bySession(parsed.sessionId)
            ?: return CheckResult.Failure("Sesi QR tidak aktif")
        if (!broadcast.active) return CheckResult.Failure("Sesi QR sudah ditutup")
        if (System.currentTimeMillis() > broadcast.expiresAt) {
            qrDao.deactivate(parsed.sessionId)
            return CheckResult.Failure("QR sudah kedaluwarsa")
        }
        val settings = settingsRepo.current()
        if (!qrCodec.isValid(parsed, broadcast.secret, System.currentTimeMillis(), broadcast.rotationSeconds)) {
            return CheckResult.Failure("Token QR tidak valid/kadaluarsa, scan ulang")
        }
        if (qrDao.tokenUsedCount(parsed.sessionId, studentId, parsed.token) > 0) {
            return CheckResult.Failure("QR ini sudah pernah kamu pakai")
        }
        if (attendanceDao.recordOf(parsed.sessionId, studentId)?.checkInTime != null) {
            return CheckResult.Failure("Kamu sudah tercatat hadir pada sesi ini")
        }

        // lokasi opsional
        settingsRepo.current().let { s ->
            if (s.locationCheckEnabled && (s.schoolLatitude != 0.0 || s.schoolLongitude != 0.0)) {
                val ok = LocationChecker.isWithinRadius(
                    s.schoolLatitude, s.schoolLongitude, s.radiusMeters.toDouble()
                )
                if (!ok) return CheckResult.Failure("Di luar radius sekolah (${s.radiusMeters} m)")
            }
        }

        qrDao.insertUsedToken(
            zaaaam.siabsen.com.data.local.entity.UsedQrTokenEntity(
                sessionId = parsed.sessionId, studentId = studentId, token = parsed.token
            )
        )

        val result = selfCheckIn(studentId, AttendanceMethod.QR, deviceId)
        if (result is CheckResult.Failure &&
            result.reason.contains("sudah check-in", true).not()
        ) {
            // token tetap terpakai walau window tutup — mencegah replay
        }
        return result
    }

    /** Koreksi eksplisit oleh guru/wali (dipanggil dari sheet edit). */
    suspend fun correct(record: AttendanceRecordEntity, newStatus: AttendanceStatus, reason: String): CheckResult {
        val ses = attendanceDao.sessionById(record.sessionId) ?: return CheckResult.Failure("Sesi hilang")
        if (ses.closed) return CheckResult.Failure("Sesi sudah ditutup")
        val now = System.currentTimeMillis()
        attendanceDao.upsertRecord(
            record.copy(status = newStatus, updatedAt = now, recordedByUserId = session.currentUserId)
        )
        attendanceDao.insertCorrection(
            AttendanceCorrectionEntity(
                recordId = record.id,
                sessionId = record.sessionId,
                studentId = record.studentId,
                oldStatus = record.status,
                newStatus = newStatus,
                reason = reason,
                changedByUserId = session.currentUserId,
            )
        )
        audit.log(
            "CORRECT_ATTENDANCE", "RECORD", record.id.toString(),
            "student=${record.studentId} OLD=${record.status} NEW=$newStatus reason=$reason"
        )
        return CheckResult.Success(newStatus, "", "Koreksi tersimpan")
    }

    // ================= Statistik =================

    fun observeStudentStatusCounts(studentId: String, from: LocalDate, to: LocalDate): Flow<List<StatusCount>> =
        attendanceDao.observeStatusCounts(studentId, from.toEpochDay(), to.toEpochDay())

    fun observeStudentRecords(studentId: String, from: LocalDate, to: LocalDate) =
        attendanceDao.observeStudentRecords(studentId, from.toEpochDay(), to.toEpochDay())

    suspend fun dailyRecordOfToday(studentId: String): AttendanceRecordEntity? =
        attendanceDao.dailyRecordOf(studentId, LocalDate.now().toEpochDay())

    suspend fun recapOfSession(sessionId: String): Map<AttendanceStatus, Int> =
        attendanceDao.sessionStatusCounts(sessionId).associate { it.status to it.cnt }

    /** Early warning: daftar siswa + persentase kehadiran dalam rentang tanggal */
    suspend fun earlyWarning(classId: Long?, daysBack: Long = 60, warn: Int, critical: Int): List<EarlyWarningItem> {
        val from = LocalDate.now().minusDays(daysBack).toEpochDay()
        val to = LocalDate.now().toEpochDay()
        return attendanceDao.studentRates(from, to, classId)
            .filter { it.total > 0 }
            .map {
                EarlyWarningItem(
                    it.studentId, it.studentName, it.className,
                    ratePercent = ((it.attended * 100.0) / it.total).toInt(),
                    lateCnt = it.lateCnt, absentCnt = it.absentCnt,
                )
            }
            .sortedWith(compareBy({ -it.ratePercent }, { it.studentName }))
    }

    suspend fun insightForClass(classId: Long?): List<String> {
        val settings = settingsRepo.current()
        val items = earlyWarning(classId, 30, settings.warnThresholdPercent, settings.criticalThresholdPercent)
        val insights = mutableListOf<String>()
        val belowCritical = items.filter { it.level(settings.warnThresholdPercent, settings.criticalThresholdPercent) == 2 }
        if (belowCritical.isNotEmpty()) {
            insights.add("${belowCritical.size} siswa memiliki kehadiran di bawah ${settings.criticalThresholdPercent}%.")
        }
        val oftenLate = items.filter { it.lateCnt >= 3 }.size
        if (oftenLate > 0) insights.add("$oftenLate siswa terlambat >= 3 kali dalam 30 hari.")
        return insights
    }

    // ================= Helper =================

    private suspend fun validateSchoolDay(today: LocalDate, settings: SchoolSettings): String? {
        val cal = academicDao.calendarDay(today.toEpochDay())
        if (cal != null && cal.type != CalendarDayType.SCHOOL_DAY && cal.type != CalendarDayType.EXAM) {
            return "Hari ini ${cal.type.label.lowercase()}${cal.note?.let { " ($it)" } ?: ""}"
        }
        if (today.dayOfWeek.value !in settings.schoolDays) {
            return "Hari ini bukan hari sekolah"
        }
        return null
    }

    private fun deviceId(): String? = session.currentUserId?.toString()

    companion object {
        val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun parseHm(hm: String): LocalTime =
            runCatching { LocalTime.parse(hm.trim()) }.getOrElse { LocalTime.of(7, 0) }
    }
}

/** Cek jarak GPS sederhana memakai LocationManager last known location. */
object LocationChecker {
    fun isWithinRadius(lat: Double, lng: Double, radiusMeters: Double): Boolean {
        val ctx: android.content.Context? = AppContextHolder.get() ?: return false
        val lm = ctx!!.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val providers = listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER)
        var best: android.location.Location? = null
        for (p in providers) {
            try {
                val l = lm.getLastKnownLocation(p) ?: continue
                if (best == null || l.accuracy < best!!.accuracy) best = l
            } catch (_: SecurityException) {
                return false
            }
        }
        val loc = best ?: return false
        val target = android.location.Location("school").apply { latitude = lat; longitude = lng }
        return loc.distanceTo(target) <= radiusMeters
    }
}

/** Context holder untuk util non-injectable */
object AppContextHolder {
    @Volatile private var ref: android.content.Context? = null
    fun set(c: android.content.Context) { ref = c.applicationContext }
    fun get(): android.content.Context? = ref
}
