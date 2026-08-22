package zaaaam.siabsen.com.ui.feature.guru

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavController
import zaaaam.siabsen.com.data.local.dao.StudentRow
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.ui.components.Avatar
import zaaaam.siabsen.com.ui.components.StatusChip
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import zaaaam.siabsen.com.ui.navigation.Routes

private val QUICK_STATUSES = listOf(
    AttendanceStatus.PRESENT,
    AttendanceStatus.LATE,
    AttendanceStatus.EXCUSED,
    AttendanceStatus.SICK,
    AttendanceStatus.ABSENT,
)

@Composable
fun TakeAttendance(nav: NavController, classId: Long, vm: TakeAttendanceVm = hiltViewModel()) {
    LaunchedEffect(classId) { vm.initFor(classId) }
    val ui by vm.ui.collectAsState()
    var correctionTarget by remember { mutableStateOf<StudentRow?>(null) }
    val context = LocalContext.current
    fun toast(msg: String) = android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()

    SubPageScaffold(title = "Ambil Absensi") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(ui.className ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Tap status untuk menandai. Tap lama baris → koreksi.", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = {
                        vm.startQr(
                            onReady = {
                                val sid = vm.sessionId.value ?: return@startQr
                                nav.navigate(Routes.qrBroadcast(sid))
                            },
                            onError = { toast(it) },
                        )
                    }) {
                        androidx.compose.material3.Icon(Icons.Filled.QrCode2, contentDescription = null)
                        Spacer(Modifier.padding(start = 4.dp))
                        Text("Mode QR")
                    }
                }
            }

            items(ui.students.size) { i ->
                val s = ui.students[i]
                val rec = ui.records.firstOrNull { it.record.studentId == s.student.id }
                StudentMarkRow(
                    student = s,
                    current = rec?.record?.status,
                    checkIn = rec?.record?.checkInTime,
                    onTap = { st -> vm.quickMark(s, st) { toast(it) } },
                    onLongPress = { correctionTarget = s },
                )
            }
        }
    }

    correctionTarget?.let { target ->
        CorrectionDialog(
            studentName = target.student.name,
            currentStatus = ui.records.firstOrNull { it.record.studentId == target.student.id }?.record?.status,
            onDismiss = { correctionTarget = null },
            onSubmit = { newStatus, reason ->
                vm.correct(target, newStatus, reason, { toast(it) }, { toast(it) })
                correctionTarget = null
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun StudentMarkRow(
    student: StudentRow,
    current: AttendanceStatus?,
    checkIn: String?,
    onTap: (AttendanceStatus) -> Unit,
    onLongPress: () -> Unit,
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(student.student.name)
                Spacer(Modifier.padding(start = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text(student.student.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            append(student.student.id)
                            checkIn?.let { append(" • masuk $it") }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                current?.let { StatusChip(it) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QUICK_STATUSES.forEach { st ->
                    FilterChip(
                        selected = current == st,
                        onClick = { onTap(st) },
                        label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }
}

@Composable
fun CorrectionDialog(
    studentName: String,
    currentStatus: AttendanceStatus?,
    onDismiss: () -> Unit,
    onSubmit: (AttendanceStatus, String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(currentStatus ?: AttendanceStatus.PRESENT) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Koreksi: $studentName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentStatus != null) {
                    Text(
                        "Status saat ini: ${currentStatus.label}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text("Status baru:")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QUICK_STATUSES.forEach { st ->
                        FilterChip(selected = selected == st, onClick = { selected = st }, label = { Text(st.label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan koreksi (wajib)") },
                    supportingText = { Text("Tercatat di audit log") },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = reason.isNotBlank(), onClick = { onSubmit(selected, reason.trim()) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
