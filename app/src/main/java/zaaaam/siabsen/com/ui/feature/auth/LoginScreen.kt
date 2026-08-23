package zaaaam.siabsen.com.ui.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavController
import zaaaam.siabsen.com.data.local.entity.Role
import zaaaam.siabsen.com.data.local.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.components.Avatar
import zaaaam.siabsen.com.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(nav: NavController, vm: AuthViewModel = hiltViewModel()) {
    val users by vm.users.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val session: SessionManager = hiltViewModel<LoginSessionVm>().session

    var selected by remember { mutableStateOf<UserEntity?>(null) }

    // App-lock gate: jika aktif & ada user terakhir → kunci dulu
    LaunchedEffect(Unit) {
        if (vm.shouldShowLock() && !session.isLoggedIn) nav.navigate(Routes.LOCK)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val school = vm.schoolSettings.collectAsState().value
                        if (school.logoPath.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = java.io.File(school.logoPath),
                                contentDescription = "Logo",
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        Column {
                            Text("SiAbsen", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Absensi sekolah offline-first", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text("Pilih akun untuk masuk", style = MaterialTheme.typography.titleMedium)
            }
            if (users.isEmpty() && !busy) {
                item {
                    Text("Menyiapkan data awal…")
                }
            }
            items(users) { u ->
                Card(
                    onClick = { selected = u },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.displayName)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.displayName, style = MaterialTheme.typography.titleMedium)
                            Text("@${u.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(u.role.label(), color = roleColor(u.role), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (busy) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(28.dp)) } }
            error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }
            item { Spacer(Modifier.height(24.dp)) }
            item {
                Text(
                    "Akun demo: admin / guru / wali / siswa — PIN 123456",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    selected?.let { sel ->
        PinDialog(
            name = sel.displayName,
            hasPin = !sel.pinHash.isNullOrBlank(),
            onDismiss = { selected = null; vm.error.value = null },
            onConfirm = { pin ->
                vm.login(sel, pin) { role ->
                    selected = null
                    val dest = when (role) {
                        Role.STUDENT -> Routes.STUDENT_HOME
                        Role.ADMIN -> Routes.ADMIN_DASHBOARD
                        else -> Routes.GURU_DASHBOARD
                    }
                    nav.navigate(dest) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
            },
            error = error,
        )
    }
}

@Composable
private fun roleColor(r: Role) = when (r) {
    Role.ADMIN -> MaterialTheme.colorScheme.error
    Role.HOMEROOM_TEACHER -> MaterialTheme.colorScheme.tertiary
    Role.TEACHER -> MaterialTheme.colorScheme.primary
    Role.STUDENT -> MaterialTheme.colorScheme.secondary
}

@Composable
fun PinDialog(
    name: String,
    hasPin: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    error: String? = null,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Halo, $name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasPin) {
                    Text("Akun ini belum punya PIN — langsung masuk.")
                } else {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                        label = { Text("PIN") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = error != null,
                        supportingText = { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = !hasPin || pin.length >= 4) { Text(if (hasPin) "Masuk" else "Lanjut") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@HiltViewModel
class LoginSessionVm @Inject constructor(val session: SessionManager) : ViewModel()
