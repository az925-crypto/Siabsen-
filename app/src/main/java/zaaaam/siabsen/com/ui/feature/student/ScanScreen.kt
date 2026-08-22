package zaaaam.siabsen.com.ui.feature.student

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import zaaaam.siabsen.com.qr.QrImage

@Composable
fun ScanQr(nav: NavController, vm: ScanVm = hiltViewModel()) {
    val result by vm.result.collectAsState()
    val success by vm.success.collectAsState()

    val scanner = androidx.activity.compose.rememberLauncherForActivityResult(ScanContract()) { res ->
        res.contents?.let { payload -> vm.onPayload(payload) {} }
    }

    val launchScan = {
        scanner.launch(
            ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scan QR absensi")
                setBeepEnabled(true)
                setOrientationLocked(true)
            }
        )
    }

    LaunchedEffect(Unit) {
        if (vm.qrEnabled()) launchScan()
        else vm.manualCheckIn {}
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Absensi QR", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Arahkan kamera ke QR yang ditampilkan guru. QR berubah otomatis setiap beberapa detik.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { vm.reset(); launchScan() }) { Text("Buka Kamera Lagi") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.manualCheckIn {} }) { Text("Check-in tanpa QR") }
        OutlinedButton(onClick = { vm.checkOut {}; nav.popBackStack() }) { Text("Check-out") }
        OutlinedButton(onClick = { nav.popBackStack() }) { Text("Tutup") }
    }

    if (result != null) {
        AlertDialog(
            onDismissRequest = { vm.reset() },
            title = { Text(if (success == true) "Berhasil" else "Gagal") },
            text = { Text(result ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    vm.reset()
                    if (success == true) nav.popBackStack()
                }) { Text("OK") }
            },
        )
    }
}
