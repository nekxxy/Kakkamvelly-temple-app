package page.kakkamvellytemple.app.presentation.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

@Composable
fun UpdateBanner(
    version: String,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(tween(400)) + fadeIn(tween(400))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A3A00),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⬆️", fontSize = 18.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        "Update Available — $version",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80)
                    )
                    Text(
                        "Tap to download & install",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.7f)
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Later", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.5f))
                }
                Button(
                    onClick = onUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Update", style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF052010), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
