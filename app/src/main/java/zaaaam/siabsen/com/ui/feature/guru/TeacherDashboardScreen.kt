package zaaaam.siabsen.com.ui.feature.guru

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.components.statusColor
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import zaaaam.siabsen.com.ui.feature.student.greeting
import zaaaam.siabsen.com.ui.navigation.Routes
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboard(nav: NavController, vm: TeacherDashboardVm = hiltViewModel()) {
    val sessions by vm.todaySessions.collectAsState()
    val insights by vm.insights.collectAsState()
    val sessionName = hiltViewModel<TeacherSessionVm>().session.currentUserName

    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("${greeting()}, ${sessionName.split(" ").first()}", style = MaterialTheme.typography.headlineMedium)
                    Text(LocalDate.now().toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { nav.navigate(Routes.SEARCH) }) { Icon(Icons.Filled.Search, contentDescription = "Cari") }
            }
        }

        item {
            Text("SESI ABSENSI HARI INI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (sessions.isEmpty()) {
            item { EmptyState("Belum ada sesi hari ini. Buka tab Kelas untuk mengambil absensi.") }
        }
        items(sessions.size) { i ->
            val s = sessions[i]
            Card(onClick = { nav.navigate(Routes.recap(s.session.classId)) }) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(s.className, fontWeight = FontWeight.Bold)
                            Text(
                                s.subjectName ?: "Absensi harian",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (s.total > 0) {
                            val pct = ((s.present + s.late) * 100) / s.total
                            Text("$pct%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        } else Text("-", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (s.total == 0) 0f else (s.present + s.late).toFloat() / s.total },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = statusColor(AttendanceStatus.PRESENT),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Hadir ${s.present} • Terlambat ${s.late} • Total record ${s.total}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item { Text("INSIGHT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(insights.size) { i ->
            Card {
                Text(insights[i], Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@HiltViewModel
class TeacherSessionVm @Inject constructor(val session: SessionManager) : androidx.lifecycle.ViewModel()

@Composable
fun ClassList(nav: NavController, vm: ClassListVm = hiltViewModel()) {
    val classes by vm.classes.collectAsState()
    SubPageScaffold(title = "Kelas") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (classes.isEmpty()) item { EmptyState("Belum ada kelas. Admin perlu menambahkan kelas & siswa.") }
            items(classes.size) { i ->
                val c = classes[i]
                Card(onClick = { nav.navigate(Routes.takeAttendance(c.clazz.id)) }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(c.clazz.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            c.homeroomTeacherName?.let {
                                Text("Wali: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("${c.studentCount} siswa", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
