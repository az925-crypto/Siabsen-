package zaaaam.siabsen.com.ui.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import zaaaam.siabsen.com.data.local.entity.ClassEntity
import zaaaam.siabsen.com.data.local.entity.SubjectEntity
import zaaaam.siabsen.com.data.local.entity.TeacherEntity
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold

// ============== GURU ==============
@Composable
fun TeachersManage(nav: NavController, vm: RosterVm = hiltViewModel()) {
    val teachers by vm.teachers.collectAsState()
    var editing by remember { mutableStateOf<TeacherEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    SubPageScaffold(title = "Kelola Guru") { mod ->
        Scaffold(floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = null) }
        }) { pad ->
            LazyColumn(mod.padding(pad).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (teachers.isEmpty()) item { EmptyState("Belum ada guru") }
                items(teachers.size) { i ->
                    val t = teachers[i]
                    Card(onClick = { editing = t.teacher }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(t.teacher.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    t.homeroomClassName?.let { "Wali kelas $it" } ?: (t.teacher.nip?.let { "NIP $it" } ?: "-"),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            TextButton(onClick = { vm.deleteTeacher(t.teacher.id) }) { Text("Nonaktifkan") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        TeacherDialog(initial = editing, onDismiss = { showAdd = false; editing = null }, onSave = { vm.saveTeacher(it); showAdd = false; editing = null })
    }
}

@Composable
private fun TeacherDialog(initial: TeacherEntity?, onDismiss: () -> Unit, onSave: (TeacherEntity) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var nip by remember { mutableStateOf(initial?.nip ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Guru" else "Edit Guru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") })
                OutlinedTextField(value = nip, onValueChange = { nip = it }, label = { Text("NIP (opsional)") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telepon (opsional)") })
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                onSave(TeacherEntity(id = initial?.id ?: 0, name = name.trim(), nip = nip.ifBlank { null }, phone = phone.ifBlank { null }))
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

// ============== KELAS ==============
@Composable
fun ClassesManage(nav: NavController, vm: RosterVm = hiltViewModel()) {
    val classes by vm.classes.collectAsState()
    val teachers by vm.teachers.collectAsState()
    var editing by remember { mutableStateOf<ClassEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    SubPageScaffold(title = "Kelola Kelas") { mod ->
        Scaffold(floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = null) }
        }) { pad ->
            LazyColumn(mod.padding(pad).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (classes.isEmpty()) item { EmptyState("Belum ada kelas") }
                items(classes.size) { i ->
                    val c = classes[i]
                    Card(onClick = { editing = c.clazz }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(c.clazz.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    listOfNotNull(
                                        c.homeroomTeacherName?.let { "Wali: $it" },
                                        "${c.studentCount} siswa",
                                    ).joinToString(" • ").ifBlank { "-" },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            TextButton(onClick = { vm.deleteClass(c.clazz.id) }) { Text("Nonaktifkan") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        ClassDialog(
            initial = editing,
            teacherOptions = teachers.map { it.teacher.id to it.teacher.name },
            onDismiss = { showAdd = false; editing = null },
            onSave = { vm.saveClass(it); showAdd = false; editing = null },
        )
    }
}

@Composable
private fun ClassDialog(
    initial: ClassEntity?,
    teacherOptions: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (ClassEntity) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var homeroom by remember { mutableStateOf(initial?.homeroomTeacherId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Kelas" else "Edit Kelas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama kelas, mis. XI IPA 2") })
                Text("Wali kelas:", style = MaterialTheme.typography.labelLarge)
                androidx.compose.material3.OutlinedButton(onClick = { homeroom = null }) {
                    Text(homeroom?.let { id -> teacherOptions.firstOrNull { it.first == id }?.second } ?: "— tanpa wali —")
                }
                teacherOptions.forEach { (id, nm) ->
                    androidx.compose.material3.FilterChip(
                        selected = homeroom == id,
                        onClick = { homeroom = id },
                        label = { Text(nm) },
                    )
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                onSave(ClassEntity(id = initial?.id ?: 0, name = name.trim(), homeroomTeacherId = homeroom))
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

// ============== MAPEL ==============
@Composable
fun SubjectsManage(nav: NavController, vm: RosterVm = hiltViewModel()) {
    val subjects by vm.subjects.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    SubPageScaffold(title = "Mata Pelajaran") { mod ->
        Scaffold(floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = null) }
        }) { pad ->
            LazyColumn(mod.padding(pad).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (subjects.isEmpty()) item { EmptyState("Belum ada mata pelajaran") }
                items(subjects.size) { i ->
                    val s = subjects[i]
                    Card {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.name, Modifier.weight(1f))
                            TextButton(onClick = { vm.deleteSubject(s.id) }) { Text("Hapus") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Tambah Mapel") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nama mapel") }) },
            confirmButton = {
                Button(enabled = newName.isNotBlank(), onClick = {
                    vm.saveSubject(SubjectEntity(name = newName.trim())); newName = ""; showAdd = false
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } },
        )
    }
}
