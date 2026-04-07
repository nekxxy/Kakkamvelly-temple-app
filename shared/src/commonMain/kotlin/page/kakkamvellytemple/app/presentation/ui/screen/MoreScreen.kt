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

@Composable
fun MoreScreen(
    onOpenMaps: (String) -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    isEn: Boolean = false
) {
    var section by remember { mutableStateOf("menu") }

    when (section) {
        "location" -> LocationScreen(onOpenMaps, onCall, isEn)
        "about"    -> AboutScreen(isEn)
        "kulam"    -> KulamScreen(onCall, onWhatsApp, isEn)
        else -> MoreMenu(isEn,
            onLocation = { section = "location" },
            onAbout    = { section = "about" },
            onKulam    = { section = "kulam" },
            onCall     = onCall
        )
    }
}

@Composable
private fun MoreMenu(
    isEn: Boolean,
    onLocation: () -> Unit,
    onAbout: () -> Unit,
    onKulam: () -> Unit,
    onCall: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (isEn) "More" else "കൂടുതൽ",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        MenuCard("📍", if (isEn) "Location & Directions" else "സ്ഥലവും വഴിക്കുറിപ്പും",
            if (isEn) "Temple location, transport, contacts" else "ക്ഷേത്ര സ്ഥലം, ഗതാഗതം", onLocation)
        MenuCard("🛕", if (isEn) "About the Temple" else "ക്ഷേത്രത്തെ കുറിച്ച്",
            if (isEn) "History, deity, highlights" else "ചരിത്രം, ദേവൻ, പ്രത്യേകതകൾ", onAbout)
        MenuCard("💧", if (isEn) "Kulam Renovation" else "കുളം നവീകരണം",
            if (isEn) "₹47L project — donate & support" else "₹47 ലക്ഷം പദ്ധതി — ദാനം", onKulam)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("kakkamvellytemple.page",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun MenuCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
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
