package zaaaam.siabsen.com.ui.feature.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.local.entity.Role
import zaaaam.siabsen.com.data.repository.AuthRepository
import zaaaam.siabsen.com.data.repository.RosterRepository
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.components.Avatar
import zaaaam.siabsen.com.ui.navigation.Routes
import javax.inject.Inject

@HiltViewModel
class AccountVm @Inject constructor(
    private val authRepo: AuthRepository,
    private val settingsRepo: SettingsRepository,
    val session: SessionManager,
) : ViewModel() {

    val appLock = MutableStateFlow(false)

    init {
        viewModelScope.launch { appLock.value = settingsRepo.current().appLockEnabled }
    }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.save { it.copy(appLockEnabled = enabled) }; appLock.value = enabled }
    }

    fun changePin(newPin: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val uid = session.currentUserId ?: return@launch onDone(false, "Tidak ada sesi")
            if (newPin.length !in 4..8) return@launch onDone(false, "PIN harus 4–8 digit")
            authRepo.setPin(uid, newPin)
            onDone(true, "PIN berhasil diganti")
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch { authRepo.logout(); onDone() }
    }
}

@Composable
fun AccountScreen(nav: NavController, vm: AccountVm = hiltViewModel()) {
    val name = vm.session.currentUserName.ifBlank { "Belum login" }
    val role = vm.session.currentRole
    var showPinDialog by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }
    val lock by vm.appLock.collectAsState()

    LazyColumn(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(name)
                    Spacer(Modifier.padding(start = 12.dp))
                    Column {
                        Text(name, style = MaterialTheme.typography.titleLarge)
                        Text(role.label(), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Keamanan", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Kunci aplikasi dengan PIN")
                        androidx.compose.material3.Switch(checked = lock, onCheckedChange = { vm.setAppLock(it) })
                    }
                    Button(onClick = { showPinDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Ganti PIN") }
                }
            }
        }
        item {
            OutlinedButton(onClick = { showLogout = true }, modifier = Modifier.fillMaxWidth()) { Text("Keluar") }
        }
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        var msg by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Ganti PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                        label = { Text("PIN baru (4–8 digit)") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    )
                    msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.changePin(pin) { ok, m -> msg = m; if (ok) pin = "" }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Tutup") } },
        )
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Keluar?") },
            text = { Text("Sesi kamu akan diakhiri.") },
            confirmButton = {
                Button(onClick = {
                    vm.logout {
                        nav.navigate(Routes.LOGIN) { popUpTo(0) }
                    }
                }) { Text("Ya, keluar") }
            },
            dismissButton = { OutlinedButton(onClick = { showLogout = false }) { Text("Batal") } },
        )
    }
}
