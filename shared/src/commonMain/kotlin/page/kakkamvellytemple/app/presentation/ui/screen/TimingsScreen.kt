package page.kakkamvellytemple.app.presentation.ui.screen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.PoojaItem
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.theme.Gold
import page.kakkamvellytemple.app.presentation.viewmodel.TimingsViewModel

@Composable
fun TimingsScreen(viewModel: TimingsViewModel, isEn: Boolean = false) {
    val state by viewModel.state.collectAsState()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(if (isEn) "Temple Timings" else "ക്ഷേത്ര സമയക്രമം",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
        }

        // Opening hours summary
        item {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    HoursBlock("☀️", if (isEn) "Morning" else "രാവിലെ", "5:30 – 9:00 AM")
                    Box(Modifier.width(1.dp).height(48.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    HoursBlock("🌙", if (isEn) "Evening" else "വൈകുന്നേരം", "5:45 – 6:45 PM")
                }
            }
        }

        // Morning poojas
        item {
            PoojaSection(
                title = if (isEn) "Morning (Prabhatha Pooja)" else "രാവിലെ (പ്രഭാത പൂജ)",
                icon = "☀️",
                poojas = state.morningPoojas,
                startIndex = 0,
                activeIndex = state.activePoojaIndex,
                isEn = isEn
            )
        }

        // Evening poojas
        item {
            PoojaSection(
                title = if (isEn) "Evening (Sayahna Pooja)" else "വൈകുന്നേരം (സായഹ്ന പൂജ)",
                icon = "🌙",
                poojas = state.eveningPoojas,
                startIndex = state.morningPoojas.size,
                activeIndex = state.activePoojaIndex,
                isEn = isEn
            )
        }

        // Contacts
        item {
            Text(if (isEn) "Temple Contacts" else "ക്ഷേത്ര ഓഫീസ്",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
        }

        items(StaticData.CONTACTS.size) { i ->
            val c = StaticData.CONTACTS[i]
            ListItem(
                headlineContent = { Text(if (isEn) c.roleEn else c.roleMl,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
                trailingContent = { Text(c.phone, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun PoojaSection(title: String, icon: String, poojas: List<PoojaItem>,
    startIndex: Int, activeIndex: Int, isEn: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 16.sp)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
        }
        poojas.forEachIndexed { localIdx, pooja ->
            val globalIdx = startIndex + localIdx
            val isActive = globalIdx == activeIndex
            PoojaRow(pooja, isActive, isEn)
        }
    }
}

@Composable
private fun PoojaRow(pooja: PoojaItem, isActive: Boolean, isEn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            ).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(if (isEn) pooja.nameEn else pooja.nameMl,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        Text(pooja.time, style = MaterialTheme.typography.labelMedium,
            color = Gold, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HoursBlock(icon: String, label: String, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(time, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
