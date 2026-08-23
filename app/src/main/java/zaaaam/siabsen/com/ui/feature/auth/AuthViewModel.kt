package zaaaam.siabsen.com.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import zaaaam.siabsen.com.data.local.entity.Role
import zaaaam.siabsen.com.data.local.entity.UserEntity
import zaaaam.siabsen.com.data.repository.AuthRepository
import zaaaam.siabsen.com.data.repository.LoginResult
import zaaaam.siabsen.com.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val settingsRepo: SettingsRepository,
    private val seeder: zaaaam.siabsen.com.data.seed.DemoSeeder,
) : ViewModel() {

    val users: StateFlow<List<UserEntity>> = authRepo.observeActiveUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schoolSettings = settingsRepo.settings
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, SettingsRepository.SchoolSettings())

    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            runCatching { seeder.seedIfEmpty() }
        }
    }

    fun login(user: UserEntity, pin: String, onSuccess: (Role) -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            when (val res = authRepo.login(user.id, pin)) {
                is LoginResult.Success -> {
                    settingsRepo.saveLastUser(res.user.id)
                    busy.value = false
                    onSuccess(res.user.role)
                }
                LoginResult.InvalidPin -> { busy.value = false; error.value = "PIN salah" }
                LoginResult.UserNotFound -> { busy.value = false; error.value = "User tidak ditemukan" }
            }
        }
    }

    suspend fun shouldShowLock(): Boolean =
        settingsRepo.current().appLockEnabled && settingsRepo.lastUserId() != null
}
