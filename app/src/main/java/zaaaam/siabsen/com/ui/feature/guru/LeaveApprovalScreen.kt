package zaaaam.siabsen.com.ui.feature.guru

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.local.dao.LeaveRow
import zaaaam.siabsen.com.data.repository.LeaveRepository
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LeaveApprovalVm @Inject constructor(
    private val leaveRepo: LeaveRepository,
    private val session: SessionManager,
) : ViewModel() {

    val pending: StateFlow<List<LeaveRow>> = leaveRepo.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun decide(leave: zaaaam.siabsen.com.data.local.entity.LeaveRequestEntity, approve: Boolean, note: String, onDone: () -> Unit) {
        viewModelScope.launch {
            leaveRepo.decide(leave, approve, session.currentUserId ?: 0, note.ifBlank { null })
            onDone()
        }
    }
}

@Composable
fun LeaveApproval(nav: NavController, vm: LeaveApprovalVm = hiltViewModel()) {
    val pending by vm.pending.collectAsState()
    var deciding by remember { mutableStateOf<zaaaam.siabsen.com.data.local.entity.LeaveRequestEntity?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    fun toast(m: String) = android.widget.Toast.makeText(context, m, android.widget.Toast.LENGTH_SHORT).show()

    SubPageScaffold(title = "Verifikasi Izin") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (pending.isEmpty()) item { EmptyState("Tidak ada pengajuan menunggu verifikasi.") }
            items(pending.size) { i ->
                val l = pending[i]
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(l.studentName + (l.className?.let { " • $it" } ?: ""), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${l.leave.type.label}: ${LocalDate.ofEpochDay(l.leave.dateFromEpochDay)} s/d ${LocalDate.ofEpochDay(l.leave.dateToEpochDay)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(l.leave.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        l.leave.attachmentPath?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("Lampiran tersedia", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { deciding = l.leave }) { Text("Proses") }
                        }
                    }
                }
            }
        }
    }

    deciding?.let { target ->
        DecideDialog(
            studentName = pending.firstOrNull { it.leave.id == target.id }?.studentName ?: "Siswa",
            onDismiss = { deciding = null },
            onDecide = { approve, note ->
                vm.decide(target, approve, note) {
                    toast(if (approve) "Disetujui" else "Ditolak")
                    deciding = null
                }
            },
        )
    }
}

@Composable
private fun DecideDialog(
    studentName: String,
    onDismiss: () -> Unit,
    onDecide: (Boolean, String) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keputusan izin — $studentName") },
        text = {
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Catatan (opsional)") })
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onDecide(true, note) }) { Text("Setujui") }
                OutlinedButton(onClick = { onDecide(false, note) }) { Text("Tolak") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
