package zaaaam.siabsen.com.ui.feature.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.ui.components.ProgressBar
import zaaaam.siabsen.com.ui.components.StatusChip
import zaaaam.siabsen.com.ui.components.statusColor
import zaaaam.siabsen.com.ui.navigation.Routes

@Composable
fun StudentHome(nav: NavController, vm: StudentHomeVm = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    fun toast(msg: String) =
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()

    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "${greeting()}, ${ui.name.split(" ").first()}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    ui.className?.let { "$it • ${ui.schoolName}" } ?: ui.schoolName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // STATUS HARI INI
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("STATUS HARI INI", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    if (ui.todayStatus != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatusChip(ui.todayStatus!!)
                            ui.checkIn?.let { Text("Masuk $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                        ui.checkOut?.let { Text("Pulang $it", style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        Text("Belum absen hari ini", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate(Routes.STUDENT_SCAN) }, enabled = ui.todayStatus == null || ui.checkIn == null) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.padding(start = 4.dp))
                            Text("Scan QR")
                        }
                        OutlinedButton(
                            onClick = { vm.selfCheckIn({ toast("Berhasil: $it") }, { toast(it) }) },
                            enabled = ui.todayStatus == null,
                        ) { Text("Check-in mandiri") }
                        OutlinedButton(
                            onClick = { vm.checkOut({ toast(it) }, { toast(it) }) },
                            enabled = ui.checkIn != null && ui.checkOut == null,
                        ) { Text("Check-out") }
                    }
                }
            }
        }

        // KEHADIRAN bulan ini
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("KEHADIRAN BULAN INI", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("${ui.percent}%", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(
                        progress = { ui.percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = statusColor(AttendanceStatus.PRESENT),
                    )
                    RecapGrid(ui.monthCounts)
                }
            }
        }

        // JADWAL HARI INI
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("JADWAL HARI INI", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    if (ui.schedule.isEmpty()) {
                        Text("Tidak ada jadwal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ui.schedule.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(s.schedule.startTime, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                                Column {
                                    Text(s.subjectName, style = MaterialTheme.typography.titleMedium)
                                    s.teacherName?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PENGUMUMAN
        item {
            if (vm.announcements.collectAsState().value.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PENGUMUMAN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    vm.announcements.collectAsState().value.forEach { a ->
                        Card {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(a.title, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text(a.body, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    a.authorName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pengajuan izin shortcut
        item {
            Card(onClick = { nav.navigate(Routes.STUDENT_LEAVE) }) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Ajukan Izin / Sakit", style = MaterialTheme.typography.titleMedium)
                        Text("Sertakan alasan dan bukti", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("→", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun RecapGrid(counts: Map<AttendanceStatus, Int>) {
    listOf(
        AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.EXCUSED,
        AttendanceStatus.SICK, AttendanceStatus.ABSENT,
    ).forEach { st ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1.2f)
                    .height(8.dp)
                    .padding(end = 8.dp),
            ) {}
            Text(st.label, Modifier.weight(1.5f), color = statusColor(st), style = MaterialTheme.typography.labelLarge)
            Text("${counts[st] ?: 0}", Modifier.weight(0.6f), fontWeight = FontWeight.Bold)
        }
    }
}
