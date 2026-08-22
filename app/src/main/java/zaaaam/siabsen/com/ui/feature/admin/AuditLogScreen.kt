package zaaaam.siabsen.com.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import zaaaam.siabsen.com.data.local.dao.AttendanceDao
import zaaaam.siabsen.com.data.local.SiabsenDatabase
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AuditVm @Inject constructor(db: SiabsenDatabase) : ViewModel() {
    val logs = db.auditDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val corrections = db.attendanceDao().observeCorrections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun AuditLog(nav: NavController, vm: AuditVm = hiltViewModel()) {
    val logs by vm.logs.collectAsState()
    val corrections by vm.corrections.collectAsState()

    SubPageScaffold(title = "Audit Log") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Koreksi Absensi Terakhir", style = MaterialTheme.typography.titleMedium) }
            if (corrections.isEmpty()) item { EmptyState("Belum ada koreksi") }
            items(corrections.size) { i ->
                val c = corrections[i]
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            "${c.studentName}: ${c.correction.oldStatus.label} → ${c.correction.newStatus.label}",
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Alasan: ${c.correction.reason}", style = MaterialTheme.typography.bodyMedium)
                        Text(fmtTime(c.correction.timestamp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Text("Aktivitas Sistem", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(logs.size) { i ->
                val l = logs[i]
                Card {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(l.action, fontWeight = FontWeight.SemiBold)
                            Text(l.details, style = MaterialTheme.typography.bodyMedium)
                            Text("${l.actorName} • ${fmtTime(l.timestamp)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

internal fun fmtTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm"))
