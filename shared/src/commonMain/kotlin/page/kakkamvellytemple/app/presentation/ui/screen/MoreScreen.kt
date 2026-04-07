package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.viewmodel.FestivalViewModel

@Composable
fun MoreScreen(
    onOpenMaps: (String) -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    isEn: Boolean = false
) {
    val festivalVM = remember { FestivalViewModel() }
    var section by remember { mutableStateOf("menu") }

    when (section) {
        "festival" -> FestivalScreen(festivalVM, isEn)
        "about"    -> AboutScreen(isEn)
        "kulam"    -> KulamScreen(onCall, onWhatsApp, isEn)
        else -> MoreMenu(isEn,
            onFestival = { section = "festival" },
            onAbout    = { section = "about" },
            onKulam    = { section = "kulam" },
            onCall     = onCall,
            onOpenUrl  = onOpenUrl
        )
    }
}

@Composable
private fun MoreMenu(
    isEn: Boolean,
    onFestival: () -> Unit,
    onAbout: () -> Unit,
    onKulam: () -> Unit,
    onCall: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(if (isEn) "More" else "കൂടുതൽ",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        MenuCard("🎊", if (isEn) "Festival Calendar" else "ഉത്സവ കലണ്ടർ",
            if (isEn) "23 festivals 2026–2029" else "23 ഉത്സവങ്ങൾ 2026–2029", onFestival)

        MenuCard("🛕", if (isEn) "About the Temple" else "ക്ഷേത്രത്തെ കുറിച്ച്",
            if (isEn) "History, deity, highlights" else "ചരിത്രം, ദേവൻ, പ്രത്യേകതകൾ", onAbout)

        // Kulam/Donation highlighted (#7)
        Card(
            onClick = onKulam,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("💧", fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Text(if (isEn) "Kulam Renovation — Donate" else "കുളം നവീകരണം — ദാനം",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(if (isEn) "₹47 Lakh project · Support now" else "₹47 ലക്ഷം · ഭക്തരുടെ സഹകരണം ആവശ്യം",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f))
                }
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.6f))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // APK Download (#11)
        MenuCard("📱", if (isEn) "Download App (APK)" else "ആപ്പ് ഡൗൺലോഡ്",
            if (isEn) "Get latest APK from GitHub" else "GitHub-ൽ നിന്ന് APK ഡൗൺലോഡ് ചെയ്യൂ") {
            onOpenUrl(StaticData.APK_DOWNLOAD_URL)
        }

        // Website
        MenuCard("🌐", if (isEn) "Temple Website" else "ക്ഷേത്ര വെബ്സൈറ്റ്",
            "kakkamvellytemple.page") {
            onOpenUrl(StaticData.WEBSITE_URL)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Quick contacts
        Text(if (isEn) "Quick Contact" else "ദ്രുത ബന്ധം",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        StaticData.CONTACTS.take(2).forEach { c ->
            ListItem(
                leadingContent = { Icon(Icons.Default.Phone, null,
                    tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(if (isEn) c.roleEn else c.roleMl,
                    style = MaterialTheme.typography.bodyMedium) },
                trailingContent = {
                    TextButton(onClick = { onCall(c.phone) }) {
                        Text(c.phone, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        // Website owner (#8) — very low priority, small text
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { onOpenUrl(StaticData.WEBSITE_OWNER_WA) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (isEn) "Contact Website Owner" else "Website Owner",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun MenuCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 28.sp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}
