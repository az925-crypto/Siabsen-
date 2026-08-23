package zaaaam.siabsen.com.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets.UTF_8

/**
 * Hash PIN dengan PBKDF2-HMAC-SHA256 + salt acak.
 * Format tersimpan: pbkdf2$<iterations>$<saltB64>$<hashB64>
 */
@Singleton
class PinHasher @Inject constructor() {

    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iterations = 60_000
        val hash = derive(pin, salt, iterations)
        return "pbkdf2\$" + iterations + "\$" + b64(salt) + "\$" + b64(hash)
    }

    fun verify(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        // filterNot(blank): kompatibel dengan entri lama yang mengandung '$$'
        val parts = stored.split("$").filterNot { it.isBlank() }
        if (parts.size != 4 || parts[0] != "pbkdf2") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = unb64(parts[2]) ?: return false
        val expected = unb64(parts[3]) ?: return false
        val actual = derive(pin, salt, iterations)
        return constantTimeEquals(expected, actual)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, iterations, 256))
            .encoded

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun unb64(s: String) = runCatching { Base64.decode(s, Base64.NO_WRAP) }.getOrNull()

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }
}

/** Sesi user yang sedang login (in-memory). */
@Singleton
class SessionManager @Inject constructor() {
    var currentUserId: Long? = null
    var currentUserName: String = ""
    var currentRole: zaaaam.siabsen.com.data.local.entity.Role =
        zaaaam.siabsen.com.data.local.entity.Role.STUDENT
    var linkedStudentId: String? = null
    var linkedTeacherId: Long? = null

    val isLoggedIn: Boolean get() = currentUserId != null

    fun clear() {
        currentUserId = null
        currentUserName = ""
        currentRole = zaaaam.siabsen.com.data.local.entity.Role.STUDENT
        linkedStudentId = null
        linkedTeacherId = null
    }
}
