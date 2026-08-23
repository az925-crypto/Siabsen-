package zaaaam.siabsen.com.ui.feature.guru

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import zaaaam.siabsen.com.data.repository.AttendanceRepository
import zaaaam.siabsen.com.data.repository.EarlyWarningItem
import zaaaam.siabsen.com.data.repository.SettingsRepository
import zaaaam.siabsen.com.ui.components.EmptyState
import zaaaam.siabsen.com.ui.feature.student.SubPageScaffold
import zaaaam.siabsen.com.ui.navigation.Routes
import javax.inject.Inject

@HiltViewModel
class EarlyWarningVm @Inject constructor(
    private val attendance: AttendanceRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    data class Ui(
        val loading: Boolean = true,
        val critical: List<EarlyWarningItem> = emptyList(),
        val warning: List<EarlyWarningItem> = emptyList(),
        val normal: List<EarlyWarningItem> = emptyList(),
    )

    val ui = MutableStateFlow(Ui())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val s = settingsRepo.current()
            val items = attendance.earlyWarning(null, 60, s.warnThresholdPercent, s.criticalThresholdPercent)
            ui.value = Ui(
                loading = false,
                critical = items.filter { it.level(s.warnThresholdPercent, s.criticalThresholdPercent) == 2 },
                warning = items.filter { it.level(s.warnThresholdPercent, s.criticalThresholdPercent) == 1 },
                normal = items.filter { it.level(s.warnThresholdPercent, s.criticalThresholdPercent) == 0 },
            )
        }
    }

}

@Composable
fun EarlyWarningScreen(nav: NavController, vm: EarlyWarningVm = hiltViewModel()) {
    val ui by vm.ui.collectAsState()

    SubPageScaffold(title = "Early Warning") { mod ->
        LazyColumn(mod.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    "Monitoring kehadiran 60 hari terakhir. Threshold bisa diatur admin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!ui.loading && ui.critical.isEmpty() && ui.warning.isEmpty()) {
                item { EmptyState("Semua siswa dalam kondisi aman 🎉") }
            }
            if (ui.critical.isNotEmpty()) {
                item { SectionHeader("🔴 Risiko kehadiran rendah", Color(0xFFC62828)) }
                items(ui.critical.size) { i -> WarnCard(ui.critical[i]) { nav.navigate(Routes.studentDetail(it)) } }
            }
            if (ui.warning.isNotEmpty()) {
                item { SectionHeader("🟡 Perlu perhatian", Color(0xFFF9A825)) }
                items(ui.warning.size) { i -> WarnCard(ui.warning[i]) { nav.navigate(Routes.studentDetail(it)) } }
            }
            if (ui.normal.isNotEmpty()) {
                item { SectionHeader("🟢 Normal", Color(0xFF2E7D32)) }
                items(ui.normal.take(5).size) { i -> WarnCard(ui.normal[i]) { nav.navigate(Routes.studentDetail(it)) } }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
}

@Composable
private fun WarnCard(item: EarlyWarningItem, onClick: (String) -> Unit) {
    Card(onClick = { onClick(item.studentId) }) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.studentName, fontWeight = FontWeight.Bold)
                Text(
                    buildList {
                        item.className?.let { add(it) }
                        add("Alpa ${item.absentCnt}")
                        add("Telat ${item.lateCnt}")
                    }.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${item.ratePercent}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
