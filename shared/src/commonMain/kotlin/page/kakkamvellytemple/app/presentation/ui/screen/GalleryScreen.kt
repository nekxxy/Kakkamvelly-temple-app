package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

@Composable
fun GalleryScreen(isEn: Boolean = false) {
    var selected by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(if (isEn) "Temple Gallery" else "ക്ഷേത്ര ചിത്രങ്ങൾ",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text("Kakkamvelly Sreekrishna Temple · Purameri",
                style = MaterialTheme.typography.labelMedium, color = Gold.copy(alpha = 0.6f))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(StaticData.GALLERY_PHOTOS) { idx, photo ->
                Card(
                    modifier = Modifier.aspectRatio(4f / 3f)
                        .clickable { selected = idx },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = "https://kakkamvellytemple.page/images/${photo.resName}.webp",
                            contentDescription = if (isEn) photo.captionEn else photo.captionMl,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))) {
                            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isEn) photo.captionEn else photo.captionMl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(6.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Lightbox
    selected?.let { idx ->
        val photo = StaticData.GALLERY_PHOTOS[idx]
        Dialog(onDismissRequest = { selected = null }) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = "https://kakkamvellytemple.page/images/${photo.resName}.jpg",
                    contentDescription = if (isEn) photo.captionEn else photo.captionMl,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp))
                )
                IconButton(onClick = { selected = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}
