package zaaaam.siabsen.com.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import zaaaam.siabsen.com.data.local.dao.AttendanceDao
import zaaaam.siabsen.com.data.local.dao.RosterDao
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.security.AuditLogger
import java.io.BufferedReader
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rosterDao: RosterDao,
    private val attendanceDao: AttendanceDao,
    private val audit: AuditLogger,
) {

    // ================= CSV =================

    /** Rekap per siswa dalam rentang tanggal (semua kelas atau satu kelas). */
    suspend fun buildStudentRecapCsv(classId: Long?, from: LocalDate, to: LocalDate): String {
        val rates = attendanceDao.studentRates(from.toEpochDay(), to.toEpochDay(), classId)
        val sb = StringBuilder()
        sb.appendLine("NIS,Nama,Kelas,Total,Hadir,Alpa,Terlambat,Persentase")
        for (r in rates) {
            val present = r.attended - r.lateCnt
            sb.appendLine(
                listOf(
                    r.studentId, csv(r.studentName), csv(r.className ?: "-"),
                    r.total.toString(), present.toString(), r.absentCnt.toString(),
                    r.lateCnt.toString(),
                    if (r.total == 0) "0" else ((r.attended * 100) / r.total).toString() + "%",
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    suspend fun writeCsv(target: Uri, content: String): Boolean =
        runCatching {
            context.contentResolver.openOutputStream(target)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            true
        }.getOrDefault(false)

    fun csvFileName(prefix: String): String = "${prefix}_${LocalDate.now()}.csv"

    private fun csv(v: String): String = "\"" + v.replace("\"", "\"\"") + "\""

    // ================= PDF =================

    /**
     * PDF rekap sederhana: judul + tabel baris teks.
     * rows = daftar kolom per baris.
     */
    suspend fun writePdf(
        target: Uri,
        title: String,
        subtitle: String,
        header: List<String>,
        rows: List<List<String>>,
    ): Boolean = runCatching {
        val doc = PdfDocument()
        val pageWidth = 595  // A4 72dpi
        val pageHeight = 842
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
        var canvas = page.canvas
        var y = 40f

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
        val subPaint = Paint().apply { textSize = 10f; isAntiAlias = true }
        val headPaint = Paint().apply { textSize = 11f; isFakeBoldText = true; isAntiAlias = true }
        val cellPaint = Paint().apply { textSize = 10f; isAntiAlias = true }

        fun drawHeader() {
            y = 40f
            canvas.drawText(title, 36f, y, titlePaint); y += 16f
            canvas.drawText(subtitle, 36f, y, subPaint); y += 20f
            var x = 36f
            for (h in header) {
                canvas.drawText(h, x, y, headPaint); x += colW(header.size)
            }
            y += 8f
        }

        drawHeader()

        for (row in rows) {
            if (y > pageHeight - 50) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
                canvas = page.canvas
                drawHeader()
            }
            var x = 36f
            for ((i, c) in row.withIndex()) {
                val maxChars = if (header.size <= 3) 30 else 14
                canvas.drawText(c.take(maxChars), x, y, cellPaint)
                x += colW(header.size)
            }
            y += 15f
        }
        doc.finishPage(page)

        context.contentResolver.openOutputStream(target)?.use { out -> doc.writeTo(out) }
        doc.close()
        audit.log("EXPORT_PDF", null, null, "$title rows=${rows.size}")
        true
    }.getOrDefault(false)

    fun pdfFileName(prefix: String): String = "${prefix}_${LocalDate.now()}.pdf"

    private fun colW(cols: Int): Float = (523f / cols.coerceAtLeast(1)).coerceAtMost(120f)
}
