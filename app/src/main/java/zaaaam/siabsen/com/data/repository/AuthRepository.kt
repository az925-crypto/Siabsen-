package zaaaam.siabsen.com.data.repository

import zaaaam.siabsen.com.data.local.dao.RosterDao
import zaaaam.siabsen.com.data.local.entity.Role
import zaaaam.siabsen.com.data.local.entity.UserEntity
import zaaaam.siabsen.com.security.AuditLogger
import zaaaam.siabsen.com.security.PinHasher
import zaaaam.siabsen.com.security.SessionManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed class LoginResult {
    data class Success(val user: UserEntity) : LoginResult()
    object InvalidPin : LoginResult()
    object UserNotFound : LoginResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val rosterDao: RosterDao,
    private val hasher: PinHasher,
    private val session: SessionManager,
    private val audit: AuditLogger,
) {
    fun observeActiveUsers() = rosterDao.observeActiveUsers()

    suspend fun login(userId: Long, pin: String): LoginResult {
        val user = rosterDao.userById(userId) ?: return LoginResult.UserNotFound
        if (user.pinHash.isNullOrBlank()) {
            // user tanpa PIN (mis. akun demo) langsung masuk
            return startSession(user)
        }
        return if (hasher.verify(pin, user.pinHash)) startSession(user)
        else LoginResult.InvalidPin
    }

    /** Login tanpa PIN (dipakai setelah biometrik sukses) */
    suspend fun loginTrusted(userId: Long): LoginResult {
        val user = rosterDao.userById(userId) ?: return LoginResult.UserNotFound
        return startSession(user)
    }

    private suspend fun startSession(user: UserEntity): LoginResult.Success {
        session.currentUserId = user.id
        session.currentUserName = user.displayName
        session.currentRole = user.role
        session.linkedStudentId = user.linkedStudentId
        session.linkedTeacherId = user.linkedTeacherId
        audit.log("LOGIN", targetType = "USER", targetId = user.id.toString(), details = "${user.role.name} ${user.username}")
        return LoginResult.Success(user)
    }

    suspend fun logout() {
        audit.log("LOGOUT")
        session.clear()
    }

    suspend fun createOrUpdateUser(user: UserEntity, pin: String?): Long {
        val withHash = if (!pin.isNullOrBlank()) user.copy(pinHash = hasher.hash(pin)) else user
        return rosterDao.upsertUser(withHash)
    }

    suspend fun setPin(userId: Long, pin: String?) =
        rosterDao.updatePin(userId, pin?.let { hasher.hash(it) })

    suspend fun ensureAdminExists(): Boolean {
        val existing = rosterDao.userByUsername("admin")
        if (existing != null) return false
        rosterDao.upsertUser(
            UserEntity(
                username = "admin",
                displayName = "Administrator",
                role = Role.ADMIN,
                active = true,
            )
        )
        return true
    }

    suspend fun allUsersCount(): Int = rosterDao.observeActiveUsers().first().size

    /** Nama user terakhir (untuk layar lock) tanpa membuka sesi */
    suspend fun lastUserDisplayName(): String? {
        val id = settingsRepo.lastUserId() ?: return null
        return rosterDao.userById(id)?.displayName
    }

    companion object {
        const val DEFAULT_ADMIN_PIN = "123456"
    }
}
