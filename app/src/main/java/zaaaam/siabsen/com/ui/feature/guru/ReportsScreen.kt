package zaaaam.siabsen.com.ui.feature.guru

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.export.ExportRepository
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReportsVm @Inject constructor(
    private val export: ExportRepository,
) : ViewModel() {

    enum class Period(val label: String, val daysBack: Long) {
        MINGGU("7 hari", 7), BULAN("30 hari", 30), SEMESTER("180 hari", 180)
    }

    private val _msg = MutableStateFlow<String?>(null)
    val msg: StateFlow<String?> = _msg

    fun exportCsv(target: android.net.Uri?, period: Period) {
        if (target == null) return
        viewModelScope.launch {
            val from = LocalDate.now().minusDays(period.daysBack - 1)
            val csv = export.buildStudentRecapCsv(null, from, LocalDate.now())
            _msg.value = if (export.writeCsv(target, csv)) "CSV tersimpan" else "Gagal menyimpan CSV"
        }
    }

    fun exportPdf(target: android.net.Uri?, period: Period) {
        if (target == null) return
        viewModelScope.launch {
            val ok = export.writePdf(
                target,
                title = "Rekap Kehadiran Siswa",
                subtitle = "Periode ${LocalDate.now().minusDays(period.daysBack - 1)} s/d ${LocalDate.now()}",
                header = listOf("Nama", "Kehadiran", "Alpa/TL"),
                rows = rateRows(period),
            )
            _msg.value = if (ok) "PDF tersimpan" else "Gagal menyimpan PDF"
        }
    }

    private suspend fun rateRows(period: Period): List<List<String>> =
        export.buildStudentRecapCsv(null, LocalDate.now().minusDays(period.daysBack - 1), LocalDate.now())
            .lineSequence().drop(1).filter { it.isNotBlank() }.map { line ->
                // kolom CSV di-quote; parse sederhana
                val cols = line.split("\",\"").map { it.trim('"') }
                listOf(
                    cols.getOrElse(1) { "-" },
                    cols.getOrElse(7) { "-" },
                    "${cols.getOrElse(5) { "0" }}/${cols.getOrElse(6) { "0" }}",
                )
            }.toList()
}

@Composable
fun Reports(nav: NavController, vm: ReportsVm = hiltViewModel()) {
    var period by remember { mutableStateOf(ReportsVm.Period.BULAN) }
    val msg by vm.msg.collectAsState()
    val context = LocalContext.current

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        vm.exportCsv(uri, period)
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        vm.exportPdf(uri, period)
    }

    SubPageScaffold(title = "Laporan & Export") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Periode", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportsVm.Period.entries.forEach { p ->
                        FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Rekap per siswa (CSV)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "NIS, nama, kelas, total, hadir, alpa, terlambat, persentase.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(onClick = { csvLauncher.launch("siabsen_rekap_${LocalDate.now()}.csv") }) {
                            Text("Export CSV")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Laporan kehadiran (PDF)", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { pdfLauncher.launch("siabsen_laporan_${LocalDate.now()}.pdf") }) {
                            Text("Export PDF")
                        }
                    }
                }
            }
            item {
                Text(
                    "Export ditulis lewat penyimpanan dokumen Android (SAF). Pilih folder tujuan saat menyimpan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            msg?.let { m ->
                item {
                    Text(m, color = MaterialTheme.colorScheme.primary)
                    LaunchedEffect(m) { android.widget.Toast.makeText(context, m, android.widget.Toast.LENGTH_LONG).show() }
                }
            }
        }
    }
}
