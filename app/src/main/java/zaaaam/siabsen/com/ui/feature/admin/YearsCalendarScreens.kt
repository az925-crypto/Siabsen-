package zaaaam.siabsen.com.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import zaaaam.siabsen.com.data.local.entity.AcademicYearEntity
import zaaaam.siabsen.com.data.local.entity.CalendarDayType
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import java.time.LocalDate
import java.time.YearMonth

// ============== TAHUN AJARAN ==============
@Composable
fun YearsManage(nav: NavController, vm: AcademicVm = hiltViewModel()) {
    val years by vm.years.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    SubPageScaffold(title = "Tahun Ajaran") { mod ->
        Scaffold(floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = null) }
        }) { pad ->
            LazyColumn(mod.padding(pad).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (years.isEmpty()) item { EmptyState("Belum ada tahun ajaran") }
                items(years.size) { i ->
                    val y = years[i]
                    Card {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(y.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${LocalDate.ofEpochDay(y.startDateEpochDay)} s/d ${LocalDate.ofEpochDay(y.endDateEpochDay)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (y.isActive) Text("AKTIF", color = MaterialTheme.colorScheme.primary)
                            else {
                                Row {
                                    TextButton(onClick = { vm.activate(y.id) }) { Text("Aktifkan") }
                                    TextButton(onClick = { vm.deleteYear(y.id) }) { Text("Hapus") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        YearDialog(onDismiss = { showAdd = false }, onSave = { vm.saveYear(it); showAdd = false })
    }
}

@Composable
private fun YearDialog(onDismiss: () -> Unit, onSave: (AcademicYearEntity) -> Unit) {
    val now = YearMonth.now()
    var name by remember { mutableStateOf("${now.year}/${now.year + 1}") }
    var semester by remember { mutableStateOf("GANJIL") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tahun Ajaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Contoh: 2026/2027") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = semester == "GANJIL", onClick = { semester = "GANJIL" }, label = { Text("Ganjil") })
                    FilterChip(selected = semester == "GENAP", onClick = { semester = "GENAP" }, label = { Text("Genap") })
                }
                Text(
                    if (semester == "GANJIL")
                        "Periode: Juli–Des ${now.year}"
                    else "Periode: Jan–Jun ${now.year + 1}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                val start: LocalDate
                val end: LocalDate
                if (semester == "GANJIL") {
                    start = LocalDate.of(now.year, 7, 1); end = LocalDate.of(now.year, 12, 31)
                } else {
                    start = LocalDate.of(now.year + 1, 1, 1); end = LocalDate.of(now.year + 1, 6, 30)
                }
                onSave(
                    AcademicYearEntity(
                        name = name.trim(), semester = semester,
                        startDateEpochDay = start.toEpochDay(), endDateEpochDay = end.toEpochDay(),
                        isActive = true,
                    )
                )
            }) { Text("Simpan & Aktifkan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

// ============== KALENDER SEKOLAH ==============
@Composable
fun CalendarManage(nav: NavController, vm: AcademicVm = hiltViewModel()) {
    val calendar by vm.calendar.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    SubPageScaffold(title = "Kalender Sekolah") { mod ->
        Scaffold(floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = null) }
        }) { pad ->
            LazyColumn(mod.padding(pad).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        "Tandai hari libur/ujian/kegiatan agar siswa tidak dianggap alpa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (calendar.isEmpty()) item { EmptyState("Belum ada entri kalender") }
                items(calendar.sortedByDescending { it.dateEpochDay }.size) { i ->
                    val c = calendar.sortedByDescending { it.dateEpochDay }[i]
                    Card {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column {
                                Text(LocalDate.ofEpochDay(c.dateEpochDay).toString(), style = MaterialTheme.typography.titleMedium)
                                c.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            }
                            Text(c.type.label, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        CalendarDayDialog(onDismiss = { showAdd = false }, onSave = { d, t, n -> vm.setDay(d, t, n); showAdd = false })
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDayDialog(onDismiss: () -> Unit, onSave: (LocalDate, CalendarDayType, String?) -> Unit) {
    var type by remember { mutableStateOf(CalendarDayType.HOLIDAY) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var datePickerOpenFlag by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Hari Kalender") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    // pilih tanggal via datepicker dialog sederhana
                    datePickerOpenFlag = true
                }) { Text(date.toString()) }
                listOf(
                    CalendarDayType.HOLIDAY to "Libur",
                    CalendarDayType.EXAM to "Ujian",
                    CalendarDayType.EVENT to "Kegiatan",
                    CalendarDayType.SCHOOL_DAY to "Hari sekolah",
                ).forEach { (t, label) ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(label) })
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Catatan (opsional)") })
            }
        },
        confirmButton = { Button(onClick = { onSave(date, type, note.ifBlank { null }) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (datePickerOpenFlag) {
        val state = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86400000L)
        AlertDialog(
            onDismissRequest = { datePickerOpenFlag = false },
            text = { androidx.compose.material3.DatePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    datePickerOpenFlag = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { datePickerOpenFlag = false }) { Text("Tutup") } },
        )
    }
}
