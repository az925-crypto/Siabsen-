package zaaaam.siabsen.com.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.security.SessionManager
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    val session: SessionManager,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private var backgroundedAt: Long = 0

    fun onAppBackground() {
        if (backgroundedAt == 0L) backgroundedAt = System.currentTimeMillis()
    }

    /** true bila app lock aktif & durasi di background melebihi auto-lock */
    suspend fun shouldLockOnResume(): Boolean {
        val s = settingsRepo.current()
        val elapsed = System.currentTimeMillis() - backgroundedAt
        return if (s.appLockEnabled && session.isLoggedIn && elapsed >= s.autoLockMinutes * 60_000L) {
            backgroundedAt = 0
            true
        } else false
    }
}
