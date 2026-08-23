package zaaaam.siabsen.com.notification

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Menulis stacktrace crash terakhir ke file agar mudah dibaca lewat
 * file manager: Android/data/zaaaam.siabsen.com/files/crash-latest.txt
 */
object CrashReporter {

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                File(dir, "crash-latest.txt").writeText(
                    buildString {
                        appendLine("SiAbsen crash report")
                        appendLine("time: ${System.currentTimeMillis()}")
                        appendLine("thread: ${thread.name}")
                        appendLine()
                        val sw = StringWriter()
                        throwable.printStackTrace(PrintWriter(sw))
                        append(sw.toString())
                    }
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
