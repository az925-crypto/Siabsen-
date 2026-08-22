package zaaaam.siabsen.com.ui.feature.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import zaaaam.siabsen.com.data.local.dao.RecordRow
import zaaaam.siabsen.com.data.repository.AttendanceRepository
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.components.StatusChip
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryVm @Inject constructor(
    attendance: AttendanceRepository,
    session: SessionManager,
) : ViewModel() {

    val month = MutableStateFlow(YearMonth.now())
    val selected = MutableStateFlow<LocalDate?>(null)

    private val sid = session.linkedStudentId

    private val recordsFlow = month.flatMapLatest { m ->
        val from = m.atDay(1)
        val to = m.atEndOfMonth()
        if (sid == null) kotlinx.coroutines.flow.flowOf(emptyList<RecordRow>())
        else attendance.observeStudentRecords(sid!!, from, to)
    }

    data class Ui(
        val records: List<RecordRow> = emptyList(),
        val selectedDay: List<RecordRow> = emptyList(),
    )

    val ui = combine(recordsFlow, selected) { recs, sel ->
        Ui(
            records = recs,
            selectedDay = sel?.let { d ->
                recs.filter { LocalDate.ofEpochDay(it.sessionEpochDay) == d }
            } ?: emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Ui())

    fun dotFor(date: LocalDate): zaaaam.siabsen.com.data.local.entity.AttendanceStatus? =
        ui.value.records.firstOrNull {
            LocalDate.ofEpochDay(it.sessionEpochDay) == date
        }?.record?.status
}

@Composable
fun StudentHistory(nav: NavController, vm: HistoryVm = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val month by vm.month.collectAsState()
    val selected by vm.selected.collectAsState()

    SubPageScaffold(title = "Riwayat Absensi") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(onClick = { vm.month.value = month.minusMonths(1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Bulan lalu")
                    }
                    MonthCalendar(month, { vm.dotFor(it) }, selected, { vm.selected.value = it })
                    androidx.compose.material3.IconButton(onClick = { vm.month.value = month.plusMonths(1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Bulan depan")
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Detail ${selected?.toString() ?: "-"}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        if (ui.selectedDay.isEmpty()) {
                            Text("Tidak ada data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            ui.selectedDay.forEach { r ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Absensi harian", style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        r.record.checkInTime?.let { Text("$it ", style = MaterialTheme.typography.labelMedium) }
                                        StatusChip(r.record.status)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Semua catatan bulan ini", style = MaterialTheme.typography.titleMedium)
                        if (ui.records.isEmpty()) EmptyState("Belum ada riwayat")
                        ui.records.take(31).forEach { r ->
                            val d = LocalDate.ofEpochDay(r.sessionEpochDay)
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(d.dayOfMonth.toString().padStart(2, '0') + " " + d.month.name.lowercase().take(3))
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    r.record.checkInTime?.let { Text("$it ", style = MaterialTheme.typography.labelMedium) }
                                    StatusChip(r.record.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
