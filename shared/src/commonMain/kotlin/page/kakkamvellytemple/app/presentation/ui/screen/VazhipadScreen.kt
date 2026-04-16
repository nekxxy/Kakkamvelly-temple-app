package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.ContactPerson
import page.kakkamvellytemple.app.data.model.VazhipadItem
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

private val SECTIONS = listOf(
    "🛕 കൃഷ്ണൻ — Krishna"         to (0..18),
    "👶 ജീവിത ചടങ്ങുകൾ"           to (19..27),
    "🌸 ഭഗവതി Special"             to (28..33),
    "🐘 ഗണപതി Special"             to (34..38),
)
private const val PREVIEW = 4  // items shown before "Show more"

@Composable
fun VazhipadScreen(onCall: (String) -> Unit, onWhatsApp: (String) -> Unit, isEn: Boolean = false) {
    val items = StaticData.VAZHIPAD_ITEMS
    // Track expanded state per section
    val expanded = remember { mutableStateMapOf<Int, Boolean>().also { m -> SECTIONS.indices.forEach { m[it] = false } } }
    // Contact dialog
    var contactDialog by remember { mutableStateOf<ContactPerson?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("🙏", fontSize = 28.sp)
                Text(if (isEn) "Vazhipad & Offerings" else "വഴിപാടുകൾ",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(if (isEn) "Reg. 255/94 · Book in advance via Secretary"
                     else "രജി. 255/94 · Secretary വഴി മുൻകൂട്ടി ബുക്ക് ചെയ്യൂ",
                    style = MaterialTheme.typography.labelMedium, color = Gold.copy(0.7f))
                Spacer(Modifier.height(4.dp))
            }
        }

        // ── Sections with collapse (#3) ───────────────────────
        SECTIONS.forEachIndexed { sIdx, (title, range) ->
            val sectionItems = items.subList(range.first, minOf(range.last + 1, items.size))
            val isExpanded = expanded[sIdx] == true
            val visible = if (isExpanded) sectionItems else sectionItems.take(PREVIEW)

            item {
                Spacer(Modifier.height(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            itemsIndexed(visible) { _, item ->
                VazhipadRow(item, isEn)
            }

            // Show more / Show less toggle
            if (sectionItems.size > PREVIEW) {
                item {
                    TextButton(
                        onClick = { expanded[sIdx] = !isExpanded },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isExpanded)
                                (if (isEn) "Show less" else "ചുരുക്കി കാണൂ")
                            else
                                (if (isEn) "+${sectionItems.size - PREVIEW} more" else "+${sectionItems.size - PREVIEW} കൂടുതൽ"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ── Book now — contacts (#4) ──────────────────────────
        item {
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isEn) "Book a Vazhipad" else "വഴിപാട് ബുക്ക് ചെയ്യാൻ",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    StaticData.CONTACTS.take(2).forEach { c ->
                        // Tappable contact card (#4)
                        Card(
                            onClick = { contactDialog = c },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Person, null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if (isEn) c.roleEn else c.roleMl,
                                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(c.phone, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Contact dialog — Call or WhatsApp (#4) ─────────────────
    contactDialog?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactDialog = null },
            title = { Text(if (isEn) contact.roleEn else contact.roleMl) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(contact.phone, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Button(
                        onClick = { onCall(contact.phone); contactDialog = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Phone, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEn) "Call Now" else "വിളിക്കൂ")
                    }
                    if (contact.whatsapp != null) {
                        OutlinedButton(
                            onClick = { onWhatsApp("91${contact.phone.removePrefix("+91")}"); contactDialog = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("💬  WhatsApp")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { contactDialog = null }) {
                    Text(if (isEn) "Cancel" else "റദ്ദാക്കൂ")
                }
            }
        )
    }
}

@Composable
private fun VazhipadRow(item: VazhipadItem, isEn: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (isEn) item.nameEn else item.nameMl,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground)
                if (item.description.isNotEmpty())
                    Text(item.description, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.55f))
            }
            Surface(color = MaterialTheme.colorScheme.primary.copy(0.12f),
                shape = RoundedCornerShape(50)) {
                Text("₹${item.amount}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
