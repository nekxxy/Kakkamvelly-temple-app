package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.GalleryPhoto
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.component.NetworkImage
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

private val GALLERY_IMAGES = listOf(
    GalleryPhoto("temple-entrance-deepam",  "ക്ഷേത്ര പ്രവേശനം", "Temple Entrance"),
    GalleryPhoto("deepam-lamp-interior",    "ദീപം",              "Deepam"),
    GalleryPhoto("deepam-closeup",          "ദീപ ജ്യോതി",        "Sacred Flame"),
    GalleryPhoto("hanging-lamps-corridor",  "തൂക്കുദീപം",        "Hanging Lamps"),
    GalleryPhoto("temple-gate-entrance",    "ഗോപുരം",            "Temple Gate"),
    GalleryPhoto("temple-front-view",       "മുൻഭാഗം",           "Front View"),
    GalleryPhoto("temple-side-grounds",     "മൈതാനം",            "Grounds"),
)
private fun imageUrl(name: String) = "https://kakkamvellytemple.page/images/$name.jpg"

@Composable
fun GalleryScreen(onOpenUrl: (String) -> Unit = {}, isEn: Boolean = false) {
    var selectedIdx by remember { mutableStateOf<Int?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // ── About Temple section ──────────────────────────────
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header
            Text("🛕", fontSize = 36.sp)
            Text(
                if (isEn) "Kakkamvelly Sreekrishna Temple"
                else "കക്കംവെള്ളി ശ്രീകൃഷ്ണ ക്ഷേത്രം",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                if (isEn) "Sacred Home of Baby Krishna · Purameri, Kozhikode"
                else "ഉണ്ണിക്കൃഷ്ണൻ · പുറമേരി, കോഴിക്കോട്",
                style = MaterialTheme.typography.bodyMedium,
                color = Gold
            )

            HorizontalDivider(color = Gold.copy(0.2f))

            // About text
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isEn) {
                        Text(
                            "An ancient temple once destroyed during Tippu Sultan's march through Malabar — " +
                            "Kakkamvelly Sreekrishna Temple stands today as a symbol of enduring faith. " +
                            "The presiding deity is Lord Krishna in his childhood form (ഉണ്ണിക്കൃഷ്ണൻ), " +
                            "believed to resemble the sacred idol at Guruvayur Temple.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "No vedikkett. No elephants. Pure, peaceful devotion.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            "ടിപ്പുവിന്റെ ആക്രമണത്തിൽ തകർന്ന ഈ പ്രാചീന ക്ഷേത്രം ഇന്ന് ഭക്തിയുടെ " +
                            "പ്രതീകമായി നിലനിൽക്കുന്നു. ഗുരുവായൂർ ഇഡോളിനോട് സാദൃശ്യമുള്ള " +
                            "ഉണ്ണിക്കൃഷ്ണൻ ആണ് ഇവിടത്തെ പ്രധാന പ്രതിഷ്ഠ.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "വെടിക്കെട്ടില്ല. ആനയില്ല. ശുദ്ധമായ ഭക്തി മാത്രം.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Highlights row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HighlightChip("🪷", if (isEn) "Baby Krishna" else "ഉണ്ണിക്കൃഷ്ണൻ", Modifier.weight(1f))
                HighlightChip("🍛", if (isEn) "Annadhanam" else "അന്നദാനം", Modifier.weight(1f))
                HighlightChip("💧", if (isEn) "Kulam" else "കുളം", Modifier.weight(1f))
            }

            // YouTube
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    onOpenUrl("https://www.youtube.com/watch?v=${StaticData.YOUTUBE_VIDEO_ID}")
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    NetworkImage(
                        url = "https://img.youtube.com/vi/${StaticData.YOUTUBE_VIDEO_ID}/mqdefault.jpg",
                        contentDescription = null,
                        modifier = Modifier.size(width = 100.dp, height = 56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text("▶", fontSize = 16.sp, color = androidx.compose.ui.graphics.Color(0xFFFF0000))
                        Text(if (isEn) "Temple Video" else "ക്ഷേത്ര ദൃശ്യം",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(if (isEn) "Watch on YouTube" else "YouTube-ൽ കാണുക",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ── Photo grid — no captions ──────────────────────────
        Text(
            if (isEn) "Photos" else "ചിത്രങ്ങൾ",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(400.dp)  // fixed height inside scroll
        ) {
            itemsIndexed(GALLERY_IMAGES) { idx, photo ->
                AnimatedVisibility(visible, enter = fadeIn(tween(300, idx * 50))) {
                    // No caption — just the image
                    NetworkImage(
                        url = imageUrl(photo.resName),
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedIdx = idx }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    // Lightbox — also no caption
    selectedIdx?.let { idx ->
        val photo = GALLERY_IMAGES[idx]
        AlertDialog(
            onDismissRequest = { selectedIdx = null },
            text = {
                NetworkImage(
                    url = imageUrl(photo.resName),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
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

@Composable
private fun HighlightChip(icon: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Text(label, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1)
        }
    }
}
