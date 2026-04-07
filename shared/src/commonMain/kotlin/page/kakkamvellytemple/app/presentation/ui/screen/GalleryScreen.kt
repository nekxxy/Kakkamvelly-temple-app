package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

@Composable
fun GalleryScreen(isEn: Boolean = false) {
    var selectedIdx by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                if (isEn) "Temple Gallery" else "ക്ഷേത്ര ചിത്രങ്ങൾ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Kakkamvelly Sreekrishna Temple · Purameri",
                style = MaterialTheme.typography.labelMedium,
                color = Gold.copy(alpha = 0.6f)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(StaticData.GALLERY_PHOTOS) { idx, photo ->
                Card(
                    modifier = Modifier
                        .aspectRatio(4f / 3f)
                        .clickable { selectedIdx = idx },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = null,
                                tint = Gold.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (isEn) photo.captionEn else photo.captionMl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                        // Photo number badge
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                            shape = RoundedCornerShape(50),
                            color = Gold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail dialog
    selectedIdx?.let { idx ->
        val photo = StaticData.GALLERY_PHOTOS[idx]
        AlertDialog(
            onDismissRequest = { selectedIdx = null },
            title = {
                Text(
                    if (isEn) photo.captionEn else photo.captionMl,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isEn) photo.captionMl else photo.captionEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "📸 kakkamvellytemple.page",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedIdx = null }) {
                    Text(if (isEn) "Close" else "അടയ്ക്കുക")
                }
            }
        )
    }
}
