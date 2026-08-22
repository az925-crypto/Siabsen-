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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.local.SiabsenDatabase
import zaaaam.siabsen.com.data.repository.AttendanceRepository
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.ui.components.StatCard
import zaaaam.siabsen.com.ui.feature.student.greeting
import zaaaam.siabsen.com.ui.navigation.Routes
import javax.inject.Inject

data class AdminOverview(
    val schoolName: String = "",
    val students: Int = 0,
    val teachers: Int = 0,
    val classes: Int = 0,
    val insights: List<String> = emptyList(),
)

@HiltViewModel
class AdminDashboardVm @Inject constructor(
    db: SiabsenDatabase,
    attendance: AttendanceRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {

    private val counts = combine(
        db.rosterDao().observeStudents(),
        db.rosterDao().observeTeachers(),
        db.rosterDao().observeClasses(),
        settingsRepo.settings,
    ) { s, t, c, set ->
        Triple(s.size to t.size, c.size, set.schoolName)
    }

    private val insightList = kotlinx.coroutines.flow.flow {
        emit(attendance.insightForClass(null))
    }

    val overview: StateFlow<AdminOverview> = combine(counts, insightList) { (st, cls, name), ins ->
        AdminOverview(
            schoolName = name,
            students = st.first,
            teachers = st.second,
            classes = cls,
            insights = ins,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminOverview())
}

@Composable
fun Dashboard(nav: NavController, vm: AdminDashboardVm = hiltViewModel()) {
    val ov by vm.overview.collectAsState()

    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text("Dashboard Admin", style = MaterialTheme.typography.headlineMedium)
                Text(ov.schoolName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Siswa", "${ov.students}", Modifier.weight(1f))
                StatCard("Guru", "${ov.teachers}", Modifier.weight(1f))
                StatCard("Kelas", "${ov.classes}", Modifier.weight(1f))
            }
        }
        item { Text("Master Data", style = MaterialTheme.typography.titleMedium) }
        items(ADMIN_MENU.size) { i ->
            val (label, route) = ADMIN_MENU[i]
            Card(onClick = { nav.navigate(route) }) {
                Text(label, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (ov.insights.isNotEmpty()) {
            item { Text("Insight", style = MaterialTheme.typography.titleMedium) }
            items(ov.insights.size) { i ->
                Card { Text(ov.insights[i], Modifier.padding(12.dp)) }
            }
        }
    }
}

private val ADMIN_MENU = listOf(
    "Kelola Siswa & Import CSV" to Routes.ADMIN_STUDENTS,
    "Kelola Guru" to Routes.ADMIN_TEACHERS,
    "Kelola Kelas" to Routes.ADMIN_CLASSES,
    "Mata Pelajaran" to Routes.ADMIN_SUBJECTS,
    "Tahun Ajaran" to Routes.ADMIN_YEARS,
    "Kalender Sekolah" to Routes.ADMIN_CALENDAR,
    "Pengaturan Sekolah" to Routes.ADMIN_SETTINGS,
    "Backup & Restore" to Routes.ADMIN_BACKUP,
)
