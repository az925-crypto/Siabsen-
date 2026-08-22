package zaaaam.siabsen.com.ui.feature.guru

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus
import zaaaam.siabsen.com.qr.QrImage
import zaaaam.siabsen.com.ui.components.StatusChip
import zaaaam.siabsen.com.ui.components.statusColor
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import zaaaam.siabsen.com.ui.navigation.Routes

@Composable
fun QrBroadcast(nav: NavController, sessionId: String, vm: QrBroadcastVm = hiltViewModel()) {
    LaunchedEffect(sessionId) { vm.initFor(sessionId) }
    val render by vm.render.collectAsState()
    val broadcast by vm.broadcast.collectAsState()
    val present by vm.presentCount.collectAsState()
    val context = LocalContext.current

    SubPageScaffold(title = "QR Absensi") { mod ->
        Column(
            mod.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!broadcast.active) {
                Text(
                    "Sesi QR sudah berakhir atau ditutup.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { nav.popBackStack() }) { Text("Kembali") }
                return@Column
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    render?.let { r ->
                        Image(
                            bitmap = remember(r.payload) { QrImage.bitmap(r.payload, 560) }.asImageBitmap(),
                            contentDescription = "QR absensi",
                            modifier = Modifier.size(280.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "QR berganti dalam ${r.secondsLeftInWindow}s",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Sesi berakhir dalam ${formatDuration(r.totalLeftSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } ?: Text("Menyiapkan QR…")
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatMini("Hadir", "$present")
                StatMini("Rotasi", "${broadcast.rotationSeconds}s")
            }

            Spacer(Modifier.height(20.dp))
            Button(onClick = {
                vm.stop()
                nav.popBackStack()
            }) { Text("Akhiri Sesi QR") }
        }
    }
}

@Composable
private fun StatMini(label: String, value: String) {
    Card {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

internal fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m.coerceAtLeast(0), s)
}
