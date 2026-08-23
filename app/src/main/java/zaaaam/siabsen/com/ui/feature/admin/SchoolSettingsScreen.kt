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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold

// ============== PENGATURAN SEKOLAH ==============
@Composable
fun SchoolSettings(nav: NavController, vm: SettingsVm = hiltViewModel()) {
    val s by vm.settings.collectAsState()
    val saved by vm.saved.collectAsState()
    var form by remember(s) { mutableStateOf(s) }

    @Composable fun field(label: String, value: String, onChange: (String) -> Unit) {
        OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
    }

    SubPageScaffold(title = "Pengaturan Sekolah") { mod ->
        Column(
            mod.padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profil Sekolah", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (form.logoPath.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = java.io.File(form.logoPath),
                                contentDescription = "Logo",
                                modifier = Modifier.size(56.dp),
                            )
                        } else {
                            Box(
                                Modifier.size(56.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("Logo") }
                        }
                        val ctx = LocalContext.current
                        val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                            if (uri != null) {
                                runCatching {
                                    val dir = java.io.File(ctx.filesDir, "logo").apply { mkdirs() }
                                    val dst = java.io.File(dir, "logo.jpg")
                                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                                        dst.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    form = form.copy(logoPath = dst.absolutePath)
                                }
                            }
                        }
                        OutlinedButton(onClick = {
                            logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text(if (form.logoPath.isBlank()) "Pilih Logo" else "Ganti Logo") }
                    }
                    field("Nama sekolah", form.schoolName) { form = form.copy(schoolName = it) }
                    field("Alamat", form.schoolAddress) { form = form.copy(schoolAddress = it) }
                }
            }
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Jam Absensi", style = MaterialTheme.typography.titleMedium)
                    Text("Format HH:mm", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = form.checkInStart, onValueChange = { form = form.copy(checkInStart = it) }, label = { Text("Buka") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = form.onTimeUntil, onValueChange = { form = form.copy(onTimeUntil = it) }, label = { Text("Tepat waktu") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = form.lateUntil, onValueChange = { form = form.copy(lateUntil = it) }, label = { Text("Batas terlambat") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = form.checkOutFrom, onValueChange = { form = form.copy(checkOutFrom = it) }, label = { Text("Pulang dari") }, modifier = Modifier.weight(1f))
                    }
                    Text("Hari sekolah:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "Sen", 2 to "Sel", 3 to "Rab", 4 to "Kam", 5 to "Jum", 6 to "Sab").forEach { (d, l) ->
                            FilterChipMini(
                                selected = d in form.schoolDays,
                                label = l,
                                onClick = {
                                    val cur = form.schoolDays.toMutableSet()
                                    if (d in cur) cur.remove(d) else cur.add(d)
                                    form = form.copy(schoolDays = cur)
                                },
                            )
                        }
                    }
                }
            }
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("QR & Validasi", style = MaterialTheme.typography.titleMedium)
                    SwitchRow("Aktifkan QR absensi", form.qrEnabled) { form = form.copy(qrEnabled = it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = form.qrRotationSeconds.toString(),
                            onValueChange = { form = form.copy(qrRotationSeconds = it.toIntOrNull() ?: 30) },
                            label = { Text("Rotasi QR (detik)") }, modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.qrValidityMinutes.toString(),
                            onValueChange = { form = form.copy(qrValidityMinutes = it.toIntOrNull() ?: 15) },
                            label = { Text("Masa berlaku (menit)") }, modifier = Modifier.weight(1f),
                        )
                    }
                    SwitchRow("Validasi lokasi (GPS)", form.locationCheckEnabled) { form = form.copy(locationCheckEnabled = it) }
                    if (form.locationCheckEnabled) {
                        val ctx = LocalContext.current
                        var locMsg by remember { mutableStateOf<String?>(null) }
                        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                            if (granted) {
                                val coord = zaaaam.siabsen.com.data.repository.LocationChecker.lastKnown()
                                if (coord != null) {
                                    form = form.copy(schoolLatitude = coord.first, schoolLongitude = coord.second)
                                    locMsg = "Koordinat tersimpan: %.6f, %.6f".format(coord.first, coord.second)
                                } else locMsg = "Lokasi belum tersedia — buka Maps dulu lalu coba lagi"
                            } else locMsg = "Izin lokasi ditolak"
                        }
                        OutlinedButton(onClick = {
                            permLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        }) { Text("Ambil koordinat dari GPS") }
                        locMsg?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = form.schoolLatitude.toString(), onValueChange = { form = form.copy(schoolLatitude = it.toDoubleOrNull() ?: 0.0) }, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = form.schoolLongitude.toString(), onValueChange = { form = form.copy(schoolLongitude = it.toDoubleOrNull() ?: 0.0) }, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                        }
                        field("Radius (meter)", form.radiusMeters.toString()) { form = form.copy(radiusMeters = it.toIntOrNull() ?: 150) }
                    }
                }
            }
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Wi-Fi Sekolah (opsional)", style = MaterialTheme.typography.titleMedium)
                    SwitchRow("Hanya boleh absen via Wi-Fi sekolah", form.wifiCheckEnabled) { form = form.copy(wifiCheckEnabled = it) }
                    if (form.wifiCheckEnabled) {
                        field("SSID Wi-Fi sekolah", form.wifiSsid) { form = form.copy(wifiSsid = it) }
                        Text(
                            "Nama Wi-Fi harus sama persis saat siswa check-in.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Early Warning & Keamanan", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = form.warnThresholdPercent.toString(),
                            onValueChange = { form = form.copy(warnThresholdPercent = it.toIntOrNull() ?: 90) },
                            label = { Text("Perlu perhatian %") }, modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.criticalThresholdPercent.toString(),
                            onValueChange = { form = form.copy(criticalThresholdPercent = it.toIntOrNull() ?: 80) },
                            label = { Text("Risiko tinggi %") }, modifier = Modifier.weight(1f),
                        )
                    }
                    SwitchRow("App lock (PIN saat buka)", form.appLockEnabled) { form = form.copy(appLockEnabled = it) }
                    SwitchRow("Izinkan biometrik", form.biometricEnabled) { form = form.copy(biometricEnabled = it) }
                    SwitchRow("Device binding (1 akun siswa = 1 HP)", form.deviceBindingEnabled) { form = form.copy(deviceBindingEnabled = it) }
                }
            }

            Button(onClick = { vm.save(form) }, modifier = Modifier.fillMaxWidth()) { Text("Simpan Pengaturan") }
            if (saved) Text("Tersimpan ✓", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun FilterChipMini(selected: Boolean, label: String, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
