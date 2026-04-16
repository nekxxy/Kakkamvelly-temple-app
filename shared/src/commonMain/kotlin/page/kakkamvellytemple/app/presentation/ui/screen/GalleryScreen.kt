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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.GalleryPhoto
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.component.NetworkImage
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

// Real image paths from kakkamvellytemple.page/images/
private val GALLERY_IMAGES = listOf(
    GalleryPhoto("temple-entrance-deepam",  "ക്ഷേത്ര പ്രവേശനം — ദീപം",    "Temple Entrance — Deepam"),
    GalleryPhoto("deepam-lamp-interior",    "ദീപം — ഉൾഭാഗം",               "Deepam — Interior"),
    GalleryPhoto("deepam-closeup",          "ദീപ ജ്യോതി",                   "Sacred Flame"),
    GalleryPhoto("hanging-lamps-corridor",  "തൂക്കുദീപം — നടപ്പാത",         "Hanging Lamps"),
    GalleryPhoto("temple-gate-entrance",    "ക്ഷേത്ര ഗോപുരം",               "Temple Gate"),
    GalleryPhoto("temple-front-view",       "ക്ഷേത്ര മുൻഭാഗം",             "Temple Front View"),
    GalleryPhoto("temple-side-grounds",     "ക്ഷേത്ര മൈതാനം",              "Temple Grounds"),
)

private fun imageUrl(name: String) =
    "https://kakkamvellytemple.page/images/$name.jpg"

@Composable
fun GalleryScreen(onOpenUrl: (String) -> Unit = {}, isEn: Boolean = false) {
    var selectedIdx by remember { mutableStateOf<Int?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(if (isEn) "Temple Gallery" else "ക്ഷേത്ര ചിത്രങ്ങൾ",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text("Kakkamvelly Sreekrishna Temple · Purameri",
                style = MaterialTheme.typography.labelMedium, color = Gold.copy(alpha = 0.6f))
        }

        // YouTube card
        AnimatedVisibility(visible, enter = fadeIn(tween(300))) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .clickable {
                        onOpenUrl("https://www.youtube.com/watch?v=${StaticData.YOUTUBE_VIDEO_ID}")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0000))
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // YouTube thumbnail
                    NetworkImage(
                        url = "https://img.youtube.com/vi/${StaticData.YOUTUBE_VIDEO_ID}/mqdefault.jpg",
                        contentDescription = "YouTube thumbnail",
                        modifier = Modifier
                            .size(width = 120.dp, height = 68.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("▶", fontSize = 20.sp, color = Color(0xFFFF0000))
                        Text(
                            if (isEn) "Temple Video" else "ക്ഷേത്ര ദൃശ്യം",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Text(
                            if (isEn) "Watch on YouTube" else "YouTube-ൽ കാണുക",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.6f)
                        )
                    }
                }
            }
        }

        // Photo grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(GALLERY_IMAGES) { idx, photo ->
                AnimatedVisibility(visible, enter = fadeIn(tween(300, idx * 60))) {
                    Card(
                        modifier = Modifier.aspectRatio(4f / 3f)
                            .clickable { selectedIdx = idx },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            NetworkImage(
                                url = imageUrl(photo.resName),
                                contentDescription = if (isEn) photo.captionEn else photo.captionMl,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Caption overlay
                            Surface(
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                            ) {
                                Text(
                                    if (isEn) photo.captionEn else photo.captionMl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
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
    selectedIdx?.let { idx ->
        val photo = GALLERY_IMAGES[idx]
        AlertDialog(
            onDismissRequest = { selectedIdx = null },
            title = {
                Text(if (isEn) photo.captionEn else photo.captionMl,
                    style = MaterialTheme.typography.titleMedium)
            },
            text = {
                NetworkImage(
                    url = imageUrl(photo.resName),
                    contentDescription = photo.captionEn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(8.dp))
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
