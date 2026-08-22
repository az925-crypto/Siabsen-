package zaaaam.siabsen.com.ui.feature.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold

@Composable
fun BackupRestore(nav: NavController, vm: BackupVm = hiltViewModel()) {
    val state by vm.state.collectAsState()

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        vm.createBackup(uri)
    }
    var restoreReplace by rememberTrue()
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        vm.restore(uri, restoreReplace)
    }

    SubPageScaffold(title = "Backup & Restore") { mod ->
        Column(
            mod.padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Semua data (siswa, guru, kelas, jadwal, absensi, izin, audit log) diekspor sebagai JSON dengan versi & integrity check.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = {
                        backupLauncher.launch(vm.suggestedName())
                    }, modifier = Modifier.fillMaxWidth()) { Text("Buat Backup") }
                }
            }
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Restore", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "REPLACE: hapus semua data lalu ganti isi backup.\nMERGE: gabungkan (baris sama ditimpa).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = {
                        restoreReplace = false
                        restoreLauncher.launch(arrayOf("application/json", "text/*"))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Restore — Merge") }
                    OutlinedButton(onClick = {
                        restoreReplace = true
                        restoreLauncher.launch(arrayOf("application/json", "text/*"))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Restore — Replace") }
                }
            }
            when (val st = state) {
                is BackupVm.UiState.Done -> Text(st.message, color = MaterialTheme.colorScheme.primary)
                else -> Unit
            }
        }
    }
}

@Composable
private fun rememberTrue() = androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(false)
}
