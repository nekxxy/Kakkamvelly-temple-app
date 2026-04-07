package page.kakkamvellytemple.app.presentation.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.DarshanStatus
import page.kakkamvellytemple.app.presentation.ui.theme.*

@Composable
fun DarshanStatusCard(
    status: DarshanStatus?,
    timeStr: String,
    modifier: Modifier = Modifier
) {
    val isOpen = status?.isOpen == true
    val dotColor by animateColorAsState(
        targetValue = if (isOpen) OpenGreen else ClosedRed,
        animationSpec = tween(500), label = "dot"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isOpen) 0.2f else 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "alpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape)
                        .background(dotColor.copy(alpha = if (isOpen) alpha else 1f))
                )
                Column {
                    Text(
                        text = status?.statusText ?: "...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (status != null && !status.isOpen) {
                        Text(
                            text = "Opens at ${status.nextLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelMedium,
                color = Gold.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
