package zaaaam.siabsen.com.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun attendance(title: String, text: String, id: Int = System.currentTimeMillis().toInt()) =
        notify(CHANNEL_ATTENDANCE, title, text, id)

    fun leave(title: String, text: String) = notify(CHANNEL_LEAVE, title, text, System.currentTimeMillis().toInt())

    fun reminder(title: String, text: String, id: Int) = notify(CHANNEL_REMINDER, title, text, id)

    private fun notify(channel: String, title: String, text: String, id: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) return

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi = PendingIntent.getActivity(
            context, 0, intent ?: Intent(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id.coerceIn(1, Int.MAX_VALUE), n)
    }

    companion object {
        const val CHANNEL_ATTENDANCE = "attendance"
        const val CHANNEL_LEAVE = "leave"
        const val CHANNEL_REMINDER = "reminder"
    }
}
