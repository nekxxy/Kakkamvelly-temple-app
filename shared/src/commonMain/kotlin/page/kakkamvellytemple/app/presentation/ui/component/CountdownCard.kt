package page.kakkamvellytemple.app.presentation.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.Festival
import page.kakkamvellytemple.app.presentation.ui.theme.Gold
import page.kakkamvellytemple.app.util.CountdownParts

@Composable
fun CountdownCard(
    festival: Festival?,
    countdown: CountdownParts,
    modifier: Modifier = Modifier,
    isEn: Boolean = false
) {
    if (festival == null) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Festival icon + name
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(festival.icon, fontSize = 28.sp)
                Column {
                    Text(
                        text = if (isEn) festival.nameEn else festival.nameMl,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEn) festival.nameMl else festival.nameEn,
                        style = MaterialTheme.typography.labelMedium,
                        color = Gold.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            // Countdown digits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountdownUnit(countdown.dStr, if (isEn) "Days" else "ദിവസം")
                CountdownSep()
                CountdownUnit(countdown.hStr, if (isEn) "Hrs" else "മണിക്കൂർ")
                CountdownSep()
                CountdownUnit(countdown.mStr, if (isEn) "Mins" else "മിനിറ്റ്")
                CountdownSep()
                CountdownUnit(countdown.sStr, if (isEn) "Secs" else "സെക്കൻഡ്")
            }
            if (festival.noteMl.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (isEn) festival.noteMl else festival.noteMl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CountdownUnit(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .widthIn(min = 50.dp)
    ) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gold,
            fontFeatureSettings = "tnum", textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CountdownSep() {
    Text(" : ", fontSize = 24.sp, color = Gold.copy(alpha = 0.4f),
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp))
}
