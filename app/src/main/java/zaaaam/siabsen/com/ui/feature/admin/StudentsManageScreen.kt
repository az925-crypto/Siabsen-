package zaaaam.siabsen.com.ui.feature.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import zaaaam.siabsen.com.data.local.entity.StudentEntity
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold

@Composable
fun StudentsManage(nav: NavController, vm: RosterVm = hiltViewModel()) {
    val students by vm.students.collectAsState()
    var editing by remember { mutableStateOf<StudentEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val context = LocalContext.current
    fun toast(m: String) = android.widget.Toast.makeText(context, m, android.widget.Toast.LENGTH_LONG).show()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            }.getOrDefault("")
            vm.importCsvAsync(text)
        }
    }

    SubPageScaffold(title = "Kelola Siswa") { mod ->
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = "Tambah") }
            }
        ) { pad ->
            LazyColumn(mod.padding(pad).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedButton(onClick = {
                        importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv", "text/comma-separated-values"))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Import CSV (NIS,Nama,Kelas,NISN)") }
                    vm.importMsg.collectAsState().value?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (students.isEmpty()) item { EmptyState("Belum ada siswa") }
                items(students.size) { i ->
                    val s = students[i]
                    Card(onClick = { editing = s.student }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(s.student.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "NIS ${s.student.id}" + (s.className?.let { " • $it" } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { vm.deleteStudent(s.student.id) }) { Text("Nonaktifkan") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        StudentFormDialog(
            initial = editing,
            onDismiss = { showAdd = false; editing = null },
            onSave = { vm.saveStudent(it); showAdd = false; editing = null },
        )
    }
}

@Composable
private fun StudentFormDialog(initial: StudentEntity?, onDismiss: () -> Unit, onSave: (StudentEntity) -> Unit) {
    var nis by remember { mutableStateOf(initial?.id ?: "") }
    var nisn by remember { mutableStateOf(initial?.nisn ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var gender by remember { mutableStateOf(initial?.gender ?: "L") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Siswa" else "Edit Siswa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nis, onValueChange = { nis = it }, label = { Text("NIS") }, enabled = initial == null)
                OutlinedTextField(value = nisn, onValueChange = { nisn = it }, label = { Text("NISN (opsional)") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama lengkap") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(selected = gender == "L", onClick = { gender = "L" }, label = { Text("Laki-laki") })
                    androidx.compose.material3.FilterChip(selected = gender == "P", onClick = { gender = "P" }, label = { Text("Perempuan") })
                }
                Text("Kelas diatur lewat menu Kelas atau import CSV.", style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            Button(enabled = nis.isNotBlank() && name.isNotBlank(), onClick = {
                onSave(StudentEntity(id = nis.trim(), nisn = nisn.ifBlank { null }, name = name.trim(), gender = gender))
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
