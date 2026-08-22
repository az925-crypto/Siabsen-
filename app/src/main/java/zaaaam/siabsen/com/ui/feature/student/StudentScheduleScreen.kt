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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import zaaaam.siabsen.com.data.local.dao.ScheduleRow
import zaaaam.siabsen.com.data.local.dao.StudentRow
import zaaaam.siabsen.com.data.repository.AcademicRepository
import zaaaam.siabsen.com.data.repository.RosterRepository
import zaaaam.siabsen.com.security.SessionManager
import zaaaam.siabsen.com.ui.components.EmptyState
import java.time.DayOfWeek
import javax.inject.Inject

data class WeeklyScheduleUi(
    val className: String? = null,
    val days: Map<Int, List<ScheduleRow>> = emptyMap(),
)

@HiltViewModel
class ScheduleVm @Inject constructor(
    roster: RosterRepository,
    academic: AcademicRepository,
    session: SessionManager,
) : ViewModel() {

    private val sid = session.linkedStudentId

    val ui = kotlinx.coroutines.flow.flow {
        val row: StudentRow? = sid?.let { roster.studentById(it) }
        val cid = row?.student?.classId
        if (cid == null) emit(WeeklyScheduleUi())
        else {
            academic.weeklySchedule(cid).collect { rows ->
                emit(
                    WeeklyScheduleUi(
                        className = row.className,
                        days = rows.groupBy { it.schedule.dayOfWeek },
                    )
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyScheduleUi())
}

@Composable
fun StudentSchedule(nav: NavController, vm: ScheduleVm = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val todayDow = DayOfWeek.from(java.time.LocalDate.now()).value

    SubPageScaffold(title = "Jadwal Pelajaran") { mod ->
        if (ui.days.isEmpty()) {
            EmptyState("Belum ada jadwal untuk kelas ${ui.className ?: "-"}", mod)
            return@SubPageScaffold
        }
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..7).forEach { dow ->
                val rows = ui.days[dow] ?: return@forEach
                item {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(dayName(dow) + if (dow == todayDow) " • hari ini" else "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            rows.forEach { r ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Text("${r.schedule.startTime}–${r.schedule.endTime}", Modifier.padding(end = 12.dp), fontWeight = FontWeight.SemiBold)
                                    Column {
                                        Text(r.subjectName, style = MaterialTheme.typography.titleMedium)
                                        r.teacherName?.let {
                                            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun dayName(dow: Int): String = when (dow) {
    1 -> "Senin"; 2 -> "Selasa"; 3 -> "Rabu"; 4 -> "Kamis"; 5 -> "Jumat"; 6 -> "Sabtu"; else -> "Minggu"
}
