package zaaaam.siabsen.com.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import zaaaam.siabsen.com.data.local.SiabsenDatabase
import zaaaam.siabsen.com.notification.Notifier
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun scheduleDaily(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "siabsen_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

/**
 * Reminder periodik:
 * - Siswa: belum absen setelah jam reminder.
 * - Guru/wali: kelas yang diampu hari ini belum ada sesi absensi.
 * Hanya aktif pada jam 06:00–17:00 dan hari sekolah.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: SiabsenDatabase,
    private val notifier: Notifier,
    private val settingsRepo: zaaaam.siabsen.com.data.repository.SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val now = LocalTime.now()
        if (now.hour < 6 || now.hour >= 17) return Result.success()

        val settings = settingsRepo.current()
        val today = LocalDate.now()
        if (today.dayOfWeek.value !in settings.schoolDays) return Result.success()

        // Guru/wali: kelas yang diampu (homeroom) belum ada record absensi hari ini
        val staffUsers = db.rosterDao().activeStaffUsersOnce()
        for (u in staffUsers) {
            val tid = u.linkedTeacherId ?: continue
            val teacher = db.rosterDao().teacherById(tid) ?: continue
            val cid = teacher.homeroomClassId ?: continue
            val cls = db.rosterDao().classById(cid) ?: continue
            val ses = db.attendanceDao().dailySessionOf(cid, today.toEpochDay())
            val hasRecords = ses != null && db.attendanceDao().recordsOfSession(ses.id).isNotEmpty()
            val cutoff = LocalTime.of(settings.reminderHour, settings.reminderMinute).plusMinutes(30)
            if (now.isAfter(cutoff) && !hasRecords) {
                val key = "notified_class_${tid}_${today.toEpochDay()}"
                if (settingsRepo.kvGet(key) == null) {
                    notifier.reminder(
                        "Kelas belum diabsen",
                        "Kelas ${cls.name} hari ini belum memiliki data absensi.",
                        ("clsmiss" + tid + today.toEpochDay()).hashCode(),
                    )
                    settingsRepo.kvPut(key, "1")
                }
            }
        }

        // Cek semua akun siswa aktif: belum check-in setelah jam reminder & tidak punya izin
        val studentUsers = db.rosterDao().activeStudentUsersOnce()
        for (u in studentUsers) {
            val sid = u.linkedStudentId ?: continue
            val rec = db.attendanceDao().dailyRecordOf(sid, today.toEpochDay())
            val leave = db.leaveDao().approvedLeaveCovering(sid, today.toEpochDay())
            val afterReminder = now.isAfter(LocalTime.of(settings.reminderHour, settings.reminderMinute))
            if (afterReminder && (rec == null || rec.checkInTime == null) && leave == null) {
                notifier.reminder(
                    "Belum absen",
                    "Halo ${u.displayName}, kamu belum melakukan absensi hari ini.",
                    ("remind" + u.id).hashCode(),
                )
            }
        }
        return Result.success()
    }.getOrElse { Result.success() }
}
