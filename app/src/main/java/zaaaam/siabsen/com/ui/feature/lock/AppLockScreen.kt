package zaaaam.siabsen.com.ui.feature.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.local.entity.Role
import zaaaam.siabsen.com.data.repository.AuthRepository
import zaaaam.siabsen.com.data.repository.LoginResult
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.components.Avatar
import zaaaam.siabsen.com.ui.navigation.Routes
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val settingsRepo: SettingsRepository,
    val session: SessionManager,
) : ViewModel() {

    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val userName = MutableStateFlow("")
    val biometricAvailable = MutableStateFlow(false)
    val biometricEnabled = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            userName.value = authRepo.lastUserDisplayName() ?: ""
        }
        viewModelScope.launch {
            biometricEnabled.value = settingsRepo.current().biometricEnabled
            val bm = BiometricManager.from(zaaaam.siabsen.com.data.repository.AppContextHolder.get() ?: return@launch)
            biometricAvailable.value =
                bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        }
    }

    @Volatile var stopCollect: Boolean = false

    fun unlockWithPin(pin: String, onSuccess: (Role) -> Unit) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            val lastId = settingsRepo.lastUserId()
            if (lastId == null) { busy.value = false; return@launch }
            when (val res = authRepo.login(lastId, pin)) {
                is LoginResult.Success -> { busy.value = false; onSuccess(res.user.role) }
                else -> { busy.value = false; error.value = "PIN salah" }
            }
        }
    }

    fun showBiometric(activity: FragmentActivity, onSuccess: (Role) -> Unit) {
        val bm = BiometricManager.from(activity)
        if (!biometricEnabled.value || bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) return
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModelScope.launch {
                        val lastId = settingsRepo.lastUserId() ?: return@launch
                        when (val res = authRepo.loginTrusted(lastId)) {
                            is LoginResult.Success -> onSuccess(res.user.role)
                            else -> error.value = "Sesi tidak valid"
                        }
                    }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    error.value = errString.toString()
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Buka SiAbsen")
                .setSubtitle("Gunakan biometrik perangkat")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        )
    }
}

@Composable
fun AppLockScreen(nav: NavController, vm: LockViewModel = hiltViewModel()) {
    val name by vm.userName.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val activity = LocalContext.current as? FragmentActivity

    androidx.compose.material3.Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Avatar(name.ifBlank { "?" })
            Spacer(Modifier.height(8.dp))
            Text(name.ifBlank { "Aplikasi terkunci" }, style = MaterialTheme.typography.titleLarge)
            Text("Masukkan PIN atau gunakan biometrik", style = MaterialTheme.typography.bodyMedium)

            var pin by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                label = { Text("PIN") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                singleLine = true,
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    vm.unlockWithPin(pin) { role -> navToRole(nav, role) }
                },
                enabled = pin.length >= 4 && !busy,
            ) { Text("Buka") }

            OutlinedButton(onClick = {
                activity?.let { vm.showBiometric(it) { role -> navToRole(nav, role) } }
            }) { Text("Gunakan biometrik") }

            if (busy) CircularProgressIndicator(Modifier.size(24.dp).padding(top = 8.dp))
        }
    }
}

internal fun navToRole(nav: NavController, role: Role) {
    val dest = when (role) {
        Role.STUDENT -> Routes.STUDENT_HOME
        Role.ADMIN -> Routes.ADMIN_DASHBOARD
        else -> Routes.GURU_DASHBOARD
    }
    nav.navigate(dest) { popUpTo(Routes.LOCK) { inclusive = true } }
}
