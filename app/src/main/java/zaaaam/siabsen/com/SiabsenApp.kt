package zaaaam.siabsen.com

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import zaaaam.siabsen.com.notification.CrashReporter
import zaaaam.siabsen.com.work.ReminderScheduler
import javax.inject.Inject

@HiltAndroidApp
class SiabsenApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        zaaaam.siabsen.com.data.repository.AppContextHolder.set(this)
        CrashReporter.install(this)
        createNotificationChannels()
        ReminderScheduler.scheduleDaily(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val attendance = NotificationChannel(
                Notifier.CHANNEL_ATTENDANCE,
                "Absensi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifikasi terkait absensi harian" }
            val leave = NotificationChannel(
                Notifier.CHANNEL_LEAVE,
                "Pengajuan Izin",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Status pengajuan izin/sakit" }
            val reminder = NotificationChannel(
                Notifier.CHANNEL_REMINDER,
                "Pengingat",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminder belum absen / kelas belum diabsen" }
            nm.createNotificationChannel(attendance)
            nm.createNotificationChannel(leave)
            nm.createNotificationChannel(reminder)
        }
    }
}
