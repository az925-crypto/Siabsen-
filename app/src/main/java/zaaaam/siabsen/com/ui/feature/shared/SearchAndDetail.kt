package zaaaam.siabsen.com.ui.feature.shared

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.local.dao.LeaveRow
import zaaaam.siabsen.com.data.local.dao.StudentRow
import zaaaam.siabsen.com.data.local.entity.LeaveStatus
import zaaaam.siabsen.com.data.repository.AttendanceRepository
import zaaaam.siabsen.com.data.repository.LeaveRepository
import zaaaam.siabsen.com.data.repository.RosterRepository
import zaaaam.siabsen.com.ui.components.Avatar
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.components.StatusChip
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import java.time.LocalDate
import javax.inject.Inject

data class SearchResults(
    val students: List<StudentRow> = emptyList(),
    val teachers: List<zaaaam.siabsen.com.data.local.entity.TeacherEntity> = emptyList(),
    val classes: List<zaaaam.siabsen.com.data.local.entity.ClassEntity> = emptyList(),
    val subjects: List<zaaaam.siabsen.com.data.local.entity.SubjectEntity> = emptyList(),
) {
    val isEmpty: Boolean get() = students.isEmpty() && teachers.isEmpty() && classes.isEmpty() && subjects.isEmpty()
}

@HiltViewModel
class SearchVm @Inject constructor(
    private val roster: RosterRepository,
) : ViewModel() {

    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results

    fun search(q: String) {
        viewModelScope.launch {
            if (q.isBlank()) { _results.value = SearchResults(); return@launch }
            _results.value = SearchResults(
                students = roster.searchStudents(q),
                teachers = roster.searchTeachers(q),
                classes = roster.searchClasses(q),
                subjects = roster.searchSubjects(q),
            )
        }
    }
}

@Composable
fun GlobalSearchScreen(nav: NavController, vm: SearchVm = hiltViewModel()) {
    var q by remember { mutableStateOf("") }
    val results by vm.results.collectAsState()

    SubPageScaffold(title = "Pencarian") { mod ->
        Column(mod) {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it; vm.search(it) },
                label = { Text("Cari nama / NIS / NISN siswa") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (results.isEmpty && q.isNotBlank()) item { EmptyState("Tidak ditemukan") }

                if (results.students.isNotEmpty()) {
                    item { Text("Siswa", style = MaterialTheme.typography.titleMedium) }
                    items(results.students.size) { i ->
                        val s = results.students[i]
                        Card(onClick = { nav.navigate(zaaaam.siabsen.com.ui.navigation.Routes.studentDetail(s.student.id)) }) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Avatar(s.student.name)
                                Spacer(Modifier.padding(start = 10.dp))
                                Column {
                                    Text(s.student.name, fontWeight = FontWeight.SemiBold)
                                    Text("NIS ${s.student.id}" + (s.className?.let { " • $it" } ?: ""), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                if (results.teachers.isNotEmpty()) {
                    item { Text("Guru", style = MaterialTheme.typography.titleMedium) }
                    items(results.teachers.size) { i ->
                        Card { Text(results.teachers[i].name, Modifier.fillMaxWidth().padding(14.dp), fontWeight = FontWeight.SemiBold) }
                    }
                }

                if (results.classes.isNotEmpty()) {
                    item { Text("Kelas", style = MaterialTheme.typography.titleMedium) }
                    items(results.classes.size) { i ->
                        Card { Text(results.classes[i].name, Modifier.fillMaxWidth().padding(14.dp), fontWeight = FontWeight.SemiBold) }
                    }
                }

                if (results.subjects.isNotEmpty()) {
                    item { Text("Mata Pelajaran", style = MaterialTheme.typography.titleMedium) }
                    items(results.subjects.size) { i ->
                        Card { Text(results.subjects[i].name, Modifier.fillMaxWidth().padding(14.dp), fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

// ================= STUDENT DETAIL (guru/admin) =================

@HiltViewModel
class StudentDetailVm @Inject constructor(
    private val roster: RosterRepository,
    private val attendance: AttendanceRepository,
    private val leaveRepo: LeaveRepository,
) : ViewModel() {

    data class Ui(
        val student: StudentRow? = null,
        val leaves: List<LeaveRow> = emptyList(),
        val counts: Map<zaaaam.siabsen.com.data.local.entity.AttendanceStatus, Int> = emptyMap(),
        val percent: Int = 0,
        val recent: List<zaaaam.siabsen.com.data.local.dao.RecordRow> = emptyList(),
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui

    fun load(id: String) {
        viewModelScope.launch {
            val row = roster.studentById(id)
            val from = LocalDate.now().minusDays(90)
            var total = 0
            var attended = 0
            val countsMap = attendance.observeStudentStatusCounts(id, from, LocalDate.now())
                .firstOrNull()?.associate { it.status to it.cnt } ?: emptyMap()
            countsMap.forEach { (st, c) -> total += c; if (!st.countsAsAbsent) attended += c }
            val recent = attendance.observeStudentRecords(id, from, LocalDate.now()).firstOrNull() ?: emptyList()
            val leaves = leaveRepo.observeOfStudent(id).firstOrNull() ?: emptyList()
            _ui.value = Ui(
                student = row,
                leaves = leaves.take(10),
                counts = countsMap,
                percent = if (total == 0) 0 else ((attended * 100.0) / total).toInt(),
                recent = recent.take(15),
            )
        }
    }
}

@Composable
fun StudentDetailScreen(nav: NavController, studentId: String, vm: StudentDetailVm = hiltViewModel()) {
    androidx.compose.runtime.LaunchedEffect(studentId) { vm.load(studentId) }
    val ui by vm.ui.collectAsState()

    SubPageScaffold(title = "Profil Siswa") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Avatar(ui.student?.student?.name ?: "?", size = 56)
                        Spacer(Modifier.padding(start = 12.dp))
                        Column {
                            Text(ui.student?.student?.name ?: "-", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "NIS ${ui.student?.student?.id}" + (ui.student?.className?.let { " • $it" } ?: ""),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("KEHADIRAN 90 HARI", style = MaterialTheme.typography.labelLarge)
                        Text("${ui.percent}%", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        listOf(
                            zaaaam.siabsen.com.data.local.entity.AttendanceStatus.PRESENT,
                            zaaaam.siabsen.com.data.local.entity.AttendanceStatus.LATE,
                            zaaaam.siabsen.com.data.local.entity.AttendanceStatus.EXCUSED,
                            zaaaam.siabsen.com.data.local.entity.AttendanceStatus.SICK,
                            zaaaam.siabsen.com.data.local.entity.AttendanceStatus.ABSENT,
                        ).forEach { st ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(st.label, color = zaaaam.siabsen.com.ui.components.statusColor(st))
                                Text("${ui.counts[st] ?: 0}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (ui.leaves.isNotEmpty()) {
                item { Text("Riwayat izin/sakit", style = MaterialTheme.typography.titleMedium) }
                items(ui.leaves.size) { i ->
                    val l = ui.leaves[i]
                    Card {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(
                                    "${l.leave.type.label} • ${LocalDate.ofEpochDay(l.leave.dateFromEpochDay)} s/d ${LocalDate.ofEpochDay(l.leave.dateToEpochDay)}",
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(l.leave.reason, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                l.leave.status.label,
                                color = when (l.leave.status) {
                                    LeaveStatus.APPROVED -> MaterialTheme.colorScheme.primary
                                    LeaveStatus.REJECTED -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
            item { Text("Riwayat terakhir", style = MaterialTheme.typography.titleMedium) }
            if (ui.recent.isEmpty()) item { EmptyState("Belum ada riwayat") }
            items(ui.recent.size) { i ->
                val r = ui.recent[i]
                Card {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column {
                            Text(LocalDate.ofEpochDay(r.sessionEpochDay).toString(), fontWeight = FontWeight.SemiBold)
                            r.record.checkInTime?.let { Text("Masuk $it", style = MaterialTheme.typography.labelMedium) }
                        }
                        StatusChip(r.record.status)
                    }
                }
            }
        }
    }
}
