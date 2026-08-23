package zaaaam.siabsen.com.qr

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets.UTF_8

/**
 * QR absensi dengan token berputar (anti foto-ulang):
 *
 * Format payload QR:
 *   SIABSEN1|<sessionId>|<window>|<hexToken16>
 *
 * token = HMAC_SHA256(secret, "<sessionId>|<window>") diambil 16 byte pertama.
 * Window = epochSeconds / rotationSeconds → QR otomatis berubah tiap N detik.
 */
@Singleton
class QrCodec @Inject constructor() {

    fun currentWindow(nowMillis: Long, rotationSeconds: Int): Long =
        nowMillis / 1000L / rotationSeconds.coerceAtLeast(5)

    fun buildPayload(sessionId: String, secret: String, window: Long): String {
        val token = hmacToken(sessionId, secret, window)
        return "$PREFIX|$sessionId|$window|$token"
    }

    /** Hasil parse payload; null jika format tidak valid */
    data class Parsed(val sessionId: String, val window: Long, val token: String)

    fun parse(payload: String): Parsed? {
        val parts = payload.trim().split("|")
        if (parts.size != 4 || parts[0] != PREFIX) return null
        val sessionId = parts[1]
        val window = parts[2].toLongOrNull() ?: return null
        val token = parts[3]
        if (sessionId.isBlank() || token.length != 32) return null
        return Parsed(sessionId, window, token)
    }

    fun hmacToken(sessionId: String, secret: String, window: Long): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hexToBytes(secret), "HmacSHA256"))
        val digest = mac.doFinal("$sessionId|$window".toByteArray(UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    /** Token valid untuk window saat ini atau window sebelumnya (toleransi scan). */
    fun isValid(parsed: Parsed, secret: String, nowMillis: Long, rotationSeconds: Int): Boolean {
        val expectedCurrent = hmacToken(parsed.sessionId, secret, currentWindow(nowMillis, rotationSeconds))
        if (constantEquals(expectedCurrent, parsed.token)) return true
        val prevWindow = currentWindow(nowMillis, rotationSeconds) - 1
        val expectedPrev = hmacToken(parsed.sessionId, secret, prevWindow)
        return constantEquals(expectedPrev, parsed.token) && parsed.window == prevWindow
    }

    private fun constantEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].code xor b[i].code)
        return r == 0
    }

    companion object {
        const val PREFIX = "SIABSEN1"

        fun randomSecret(): String {
            val bytes = ByteArray(24)
            java.security.SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

private fun hexToBytes(hex: String): ByteArray {
    val out = ByteArray(hex.length / 2)
    for (i in out.indices) out[i] = ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
    return out
}

/** Helper kecil untuk membuat bitmap QR dari payload. */
object QrImage {

    fun bitmap(content: String, size: Int = 640): android.graphics.Bitmap {
        val s = size.coerceAtLeast(1)
        if (content.isBlank()) {
            // QRCodeWriter.encode melempar IllegalArgumentException untuk konten kosong;
            // kembalikan bitmap putih kecil sebagai fallback aman.
            return android.graphics.Bitmap.createBitmap(s, s, android.graphics.Bitmap.Config.RGB_565).apply {
                eraseColor(android.graphics.Color.WHITE)
            }
        }
        val hints = mapOf(
            com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M,
            com.google.zxing.EncodeHintType.MARGIN to 1,
        )
        val matrix = com.google.zxing.qrcode.QRCodeWriter()
            .encode(content, com.google.zxing.BarcodeFormat.QR_CODE, s, s, hints)
        val bmp = android.graphics.Bitmap.createBitmap(s, s, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until s) {
            for (y in 0 until s) {
                bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bmp
    }
}
