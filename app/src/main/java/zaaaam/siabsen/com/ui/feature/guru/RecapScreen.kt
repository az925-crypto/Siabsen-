package zaaaam.siabsen.com.ui.feature.guru

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.local.dao.RecordRow
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.data.repository.AttendanceRepository
import zaaaam.siabsen.com.data.repository.RosterRepository
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.components.StatusChip
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import zaaaam.siabsen.com.ui.navigation.Routes
import java.time.LocalDate
import javax.inject.Inject

data class RecapUi(
    val className: String? = null,
    val date: LocalDate = LocalDate.now(),
    val sessionId: String? = null,
    val recap: Map<AttendanceStatus, Int> = emptyMap(),
    val total: Int = 0,
    val records: List<RecordRow> = emptyList(),
)

@HiltViewModel
class RecapVm @Inject constructor(
    private val attendance: AttendanceRepository,
    roster: RosterRepository,
) : ViewModel() {

    val classId = MutableStateFlow(0L)
    private val state = MutableStateFlow(RecapUi())

    val ui: StateFlow<RecapUi> = state

    fun initFor(cid: Long) {
        if (classId.value == cid) return
        classId.value = cid
        viewModelScope.launch {
            state.value = state.value.copy(className = roster.classById(cid)?.name)
            load(LocalDate.now())
        }
    }

    fun setDate(d: LocalDate) = viewModelScope.launch { load(d) }

    private suspend fun load(date: LocalDate) {
        val ses = classId.value.takeIf { it > 0 }?.let { attendance.ensureDailySession(it, date) }
        if (ses == null) {
            state.value = state.value.copy(date = date, sessionId = null, recap = emptyMap(), records = emptyList(), total = 0)
            return
        }
        val recap = attendance.recapOfSession(ses.id)
        val records = attendance.recordsOfSession(ses.id)
        state.value = state.value.copy(
            date = date, sessionId = ses.id, recap = recap,
            total = recap.values.sum(), records = records,
        )
    }
}

@Composable
fun Recap(nav: NavController, classId: Long, vm: RecapVm = hiltViewModel()) {
    LaunchedEffect(classId) { vm.initFor(classId) }
    val ui by vm.ui.collectAsState()

    SubPageScaffold(title = "Rekap Kelas") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(ui.className ?: "", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    (-2..0).forEach { off ->
                        val d = LocalDate.now().plusDays(off.toLong())
                        FilterChip(
                            selected = ui.date == d,
                            onClick = { vm.setDate(d) },
                            label = { Text(if (off == 0) "Hari ini" else d.dayOfWeek.name.take(3)) },
                        )
                    }
                }
                Text(ui.date.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("${ui.date}", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        listOf(
                            AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.EXCUSED,
                            AttendanceStatus.SICK, AttendanceStatus.ABSENT,
                        ).forEach { st ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(st.label, color = zaaaam.siabsen.com.ui.components.statusColor(st))
                                Text("${ui.recap[st] ?: 0}", fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold)
                            Text("${ui.total}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Text("Per siswa", style = MaterialTheme.typography.titleMedium) }
            if (ui.records.isEmpty()) item { EmptyState("Belum ada record pada sesi ini") }
            items(ui.records.size) { i ->
                val r = ui.records[i]
                Card(onClick = { nav.navigate(Routes.studentDetail(r.record.studentId)) }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(r.studentName, fontWeight = FontWeight.SemiBold)
                            r.record.checkInTime?.let {
                                Text("Masuk $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        StatusChip(r.record.status)
                    }
                }
            }
        }
    }
}
