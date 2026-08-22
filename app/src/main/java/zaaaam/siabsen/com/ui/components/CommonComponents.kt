package zaaaam.siabsen.com.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import zaaaam.siabsen.com.data.local.entity.AttendanceStatus

fun statusColor(s: AttendanceStatus): Color = when (s) {
    AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
    AttendanceStatus.LATE -> Color(0xFFF9A825)
    AttendanceStatus.EXCUSED -> Color(0xFF1565C0)
    AttendanceStatus.SICK -> Color(0xFF6A1B9A)
    AttendanceStatus.ABSENT -> Color(0xFFC62828)
    AttendanceStatus.DISPENSATION -> Color(0xFF00838F)
    AttendanceStatus.EARLY_LEAVE -> Color(0xFFEF6C00)
    AttendanceStatus.DUTY -> Color(0xFF37474F)
}

@Composable
fun StatusChip(status: AttendanceStatus, modifier: Modifier = Modifier) {
    val bg = statusColor(status)
    Box(
        modifier = modifier
            .background(bg.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = bg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun Avatar(initials: String, size: Int = 44, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier.size(size.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials.take(2).uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ProgressBar(percent: Int, color: Color, modifier: Modifier = Modifier) {
    // bar sederhana 10 segmen gaya fitur.txt
    val filled = (percent / 10).coerceIn(0, 10)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(10) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .size(height = 8.dp, width = 8.dp)
                    .background(if (i < filled) color else Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        }
    }
}
