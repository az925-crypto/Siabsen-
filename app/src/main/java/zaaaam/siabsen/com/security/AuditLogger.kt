package zaaaam.siabsen.com.security

import zaaaam.siabsen.com.data.local.dao.AuditDao
import zaaaam.siabsen.com.data.local.entity.AuditLogEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogger @Inject constructor(
    private val auditDao: AuditDao,
    private val session: SessionManager,
) {
    suspend fun log(action: String, targetType: String? = null, targetId: String? = null, details: String = "") {
        auditDao.insert(
            AuditLogEntity(
                actorUserId = session.currentUserId,
                actorName = session.currentUserName.ifBlank { "SYSTEM" },
                action = action,
                targetType = targetType,
                targetId = targetId,
                details = details,
            )
        )
    }
}
