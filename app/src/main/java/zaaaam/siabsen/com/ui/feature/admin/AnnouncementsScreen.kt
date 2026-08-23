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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.local.SiabsenDatabase
import zaaaam.siabsen.com.data.local.entity.AnnouncementEntity
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import zaaaam.siabsen.com.security.SessionManager
import javax.inject.Inject

@HiltViewModel
class AnnouncementsVm @Inject constructor(
    private val db: SiabsenDatabase,
    val session: SessionManager,
) : ViewModel() {

    val announcements: StateFlow<List<AnnouncementEntity>> =
        db.announcementDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, body: String) {
        viewModelScope.launch {
            db.announcementDao().insert(
                AnnouncementEntity(
                    title = title.trim(),
                    body = body.trim(),
                    authorName = session.currentUserName.ifBlank { "Admin" },
                )
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { db.announcementDao().delete(id) }
    }
}

@Composable
fun AnnouncementsManage(nav: NavController, vm: AnnouncementsVm = hiltViewModel()) {
    val list by vm.announcements.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    SubPageScaffold(title = "Pengumuman") { mod ->
        Scaffold(floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = null) }
        }) { pad ->
            LazyColumn(mod.padding(pad).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (list.isEmpty()) item { EmptyState("Belum ada pengumuman") }
                items(list.size) { i ->
                    val a = list[i]
                    Card {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(a.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(a.body, style = MaterialTheme.typography.bodyMedium)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "${a.authorName} • ${java.time.Instant.ofEpochMilli(a.createdAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm"))}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = { vm.delete(a.id) }) { Text("Hapus") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Buat Pengumuman") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul") }, singleLine = true)
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Isi pengumuman") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank() && body.isNotBlank(), onClick = {
                    vm.add(title, body); showAdd = false
                }) { Text("Terbitkan") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } },
        )
    }
}
