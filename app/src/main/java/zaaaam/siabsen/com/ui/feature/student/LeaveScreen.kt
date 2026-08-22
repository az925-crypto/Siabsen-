package zaaaam.siabsen.com.ui.feature.student

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zaaaam.siabsen.com.data.local.entity.LeaveStatus
import zaaaam.siabsen.com.data.local.entity.LeaveType
import zaaaam.siabsen.com.data.repository.LeaveRepository
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.security.SessionManager
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LeaveVm @Inject constructor(
    private val leaveRepo: LeaveRepository,
    private val settingsRepo: SettingsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    val session: SessionManager,
) : ViewModel() {

    val leaves = session.linkedStudentId?.let { sid ->
        leaveRepo.observeOfStudent(sid).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } ?: MutableStateFlow(emptyList<zaaaam.siabsen.com.data.local.dao.LeaveRow>())

    fun submit(type: LeaveType, from: LocalDate, to: LocalDate, reason: String, attachment: Uri?, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val sid = session.linkedStudentId ?: return@launch onDone(false, "Akun tidak terhubung siswa")
            if (reason.isBlank()) return@launch onDone(false, "Alasan wajib diisi")
            if (to.isBefore(from)) return@launch onDone(false, "Tanggal tidak valid")
            val path = attachment?.let { copyToInternal(it) }
            leaveRepo.submit(
                LeaveRepository.defaultLeave(
                    sid, from.toEpochDay(), to.toEpochDay(), reason.trim(), type, path,
                )
            )
            onDone(true, "Pengajuan terkirim, menunggu persetujuan guru")
        }
    }

    private suspend fun copyToInternal(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(appContext.filesDir, "attachments").apply { mkdirs() }
            val file = File(dir, "leave_${System.currentTimeMillis()}.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file.absolutePath
        }.getOrNull()
    }

    suspend fun schoolName(): String = settingsRepo.current().schoolName
}

@Composable
fun LeaveRequest(nav: NavController, vm: LeaveVm = hiltViewModel()) {
    var type by remember { mutableStateOf(LeaveType.SICK) }
    var from by remember { mutableStateOf(LocalDate.now()) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var reason by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val leaves by vm.leaves.collectAsState()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        photo = uri
    }

    SubPageScaffold(title = "Ajukan Izin / Sakit") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = type == LeaveType.SICK, onClick = { type = LeaveType.SICK }, label = { Text("Sakit") })
                            FilterChip(selected = type == LeaveType.PERMISSION, onClick = { type = LeaveType.PERMISSION }, label = { Text("Izin") })
                        }
                        DateRow("Dari tanggal", from) { from = it; if (to.isBefore(from)) to = from }
                        DateRow("Sampai", to) { to = it }
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Alasan") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                        OutlinedButton(onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text(photo?.let { "Foto terlampir ✓" } ?: "Tambah foto bukti")
                        }
                        Button(onClick = {
                            vm.submit(type, from, to, reason, photo) { ok, msg ->
                                message = msg
                                if (ok) { reason = ""; photo = null }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Kirim Pengajuan") }
                        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            item { Text("Riwayat pengajuan", style = MaterialTheme.typography.titleMedium) }
            if (leaves.isEmpty()) item { Text("Belum ada pengajuan", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(leaves.size) { i ->
                val l = leaves[i]
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(l.leave.type.label + " • ${LocalDate.ofEpochDay(l.leave.dateFromEpochDay)}", fontWeight = FontWeight.SemiBold)
                            Text(l.leave.status.label, color = when (l.leave.status) {
                                LeaveStatus.APPROVED -> MaterialTheme.colorScheme.primary
                                LeaveStatus.REJECTED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            })
                        }
                        Text(l.leave.reason, style = MaterialTheme.typography.bodyMedium)
                        l.leave.decisionNote?.let { Text("Catatan guru: $it", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(label: String, value: LocalDate, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        OutlinedButton(onClick = { open = true }) { Text(value.toString()) }
    }
    if (open) {
        val state = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = value.toEpochDay() * 86400000L)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        onChange(java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                    }
                    open = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Batal") } },
            text = { androidx.compose.material3.DatePicker(state = state) },
        )
    }
}
