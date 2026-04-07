package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.GalleryPhoto
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.component.NetworkImage
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

@Composable
fun GalleryScreen(onOpenUrl: (String) -> Unit = {}, isEn: Boolean = false) {
    var selectedIdx by remember { mutableStateOf<Int?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(if (isEn) "Temple Gallery" else "ക്ഷേത്ര ചിത്രങ്ങൾ",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text("Kakkamvelly Sreekrishna Temple · Purameri",
                style = MaterialTheme.typography.labelMedium, color = Gold.copy(alpha = 0.6f))
        }

        // YouTube video card
        AnimatedVisibility(visible, enter = fadeIn(tween(400))) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        onOpenUrl("https://www.youtube.com/watch?v=${StaticData.YOUTUBE_VIDEO_ID}")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("▶", fontSize = 32.sp, color = Color(0xFFFF0000))
                    Column {
                        Text(if (isEn) "Historic Temple Video" else "ക്ഷേത്ര ചരിത്ര ദൃശ്യം",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(if (isEn) "11 years ago · YouTube" else "11 വർഷം മുൻപ് · YouTube",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(StaticData.GALLERY_PHOTOS) { idx, photo ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(300, idx * 80))
                ) {
                    Card(
                        modifier = Modifier.aspectRatio(4f / 3f).clickable { selectedIdx = idx },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            NetworkImage(
                                url = "https://kakkamvellytemple.page/images/thumbs/${photo.resName}.webp",
                                contentDescription = if (isEn) photo.captionEn else photo.captionMl,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    if (isEn) photo.captionEn else photo.captionMl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(6.dp), maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Lightbox
    selectedIdx?.let { idx ->
        val photo = StaticData.GALLERY_PHOTOS[idx]
        AlertDialog(
            onDismissRequest = { selectedIdx = null },
            title = { Text(if (isEn) photo.captionEn else photo.captionMl) },
            text = {
                NetworkImage(
                    url = "https://kakkamvellytemple.page/images/${photo.resName}.jpg",
                    contentDescription = photo.captionEn,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(8.dp))
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedIdx = null }) {
                    Text(if (isEn) "Close" else "അടയ്ക്കുക")
                }
            }
        )
    }
}
