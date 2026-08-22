package zaaaam.siabsen.com.ui.feature.student

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
import androidx.compose.material3.LinearProgressIndicator
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import zaaaam.siabsen.com.data.local.dao.StatusCount
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.data.repository.AttendanceRepository
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.components.statusColor
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsVm @Inject constructor(
    attendance: AttendanceRepository,
    session: SessionManager,
) : ViewModel() {

    enum class Range(val label: String, val days: Long) { MINGGU("Mingguan", 7), BULAN("Bulanan", 30), SEMESTER("Semester", 180) }

    val range = MutableStateFlow(Range.BULAN)
    private val sid = session.linkedStudentId

    private val countsFlow = range.flatMapLatest { r ->
        val to = LocalDate.now()
        val from = to.minusDays(r.days - 1)
        if (sid == null) kotlinx.coroutines.flow.flowOf(emptyList<StatusCount>())
        else attendance.observeStudentStatusCounts(sid!!, from, to)
    }

    data class Ui(
        val counts: Map<AttendanceStatus, Int> = emptyMap(),
        val total: Int = 0,
        val percent: Int = 0,
    )

    val ui = countsFlow.map { list ->
        val m = list.associate { it.status to it.cnt }
        val total = m.values.sum()
        val attended = m.filterKeys { !it.countsAsAbsent }.values.sum()
        Ui(m, total, if (total == 0) 0 else ((attended * 100.0) / total).toInt())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Ui())
}

@Composable
fun StudentStats(nav: NavController, vm: StatsVm = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val range by vm.range.collectAsState()

    SubPageScaffold(title = "Statistik Kehadiran") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsVm.Range.entries.forEach { r ->
                        FilterChip(selected = range == r, onClick = { vm.range.value = r }, label = { Text(r.label) })
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("KEHADIRAN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${ui.percent}%", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        LinearProgressIndicator(
                            progress = { ui.percent / 100f },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                        Text("Total ${ui.total} hari absensi", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.EXCUSED,
                            AttendanceStatus.SICK, AttendanceStatus.ABSENT,
                        ).forEach { st ->
                            val c = ui.counts[st] ?: 0
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(st.label, color = statusColor(st), fontWeight = FontWeight.SemiBold)
                                    Text("$c (${if (ui.total == 0) 0 else (c * 100) / ui.total}%)")
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { if (ui.total == 0) 0f else c.toFloat() / ui.total },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = statusColor(st),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
