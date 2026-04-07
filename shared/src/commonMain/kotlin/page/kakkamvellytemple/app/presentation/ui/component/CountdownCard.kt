package page.kakkamvellytemple.app.presentation.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.Festival
import page.kakkamvellytemple.app.presentation.ui.theme.*
import page.kakkamvellytemple.app.util.CountdownParts

@Composable
fun CountdownCard(festival: Festival?, countdown: CountdownParts, isEn: Boolean = false, modifier: Modifier = Modifier) {
    if (festival == null) return

    // Shimmer animation
    val shimmer by rememberInfiniteTransition(label = "s").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3000), RepeatMode.Restart), label = "sh"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(festival.icon, fontSize = 22.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isEn) festival.nameEn else festival.nameMl,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (isEn) festival.nameMl else festival.nameEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (festival.noteMl.isNotEmpty())
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            festival.noteMl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
            }

            // Countdown units — dark bg so gold text is always readable
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyDeep)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CountUnit(countdown.dStr, if (isEn) "Days" else "ദിവസം")
                    CountSep()
                    CountUnit(countdown.hStr, if (isEn) "Hrs" else "മണിക്കൂർ")
                    CountSep()
                    CountUnit(countdown.mStr, if (isEn) "Mins" else "മിനിറ്റ്")
                    CountSep()
                    CountUnit(countdown.sStr, if (isEn) "Secs" else "സെക്കൻഡ്")
                }
            }
        }
    }
}

@Composable
private fun CountUnit(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(0.05f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Gold on navy dark background — always readable
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Gold,
            textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(0.55f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun CountSep() {
    Text(":", fontSize = 24.sp, color = Gold.copy(0.4f),
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
}
