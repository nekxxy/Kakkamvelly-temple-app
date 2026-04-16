package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.repository.StaticData

@Composable
fun LocationScreen(
    onOpenMaps: (String) -> Unit,
    onCall: (String) -> Unit,
    isEn: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(if (isEn) "Location & Directions" else "സ്ഥലവും വഴിക്കുറിപ്പും",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Address card
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(if (isEn) "Address" else "വിലാസം",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(if (isEn) "Kakkamvelly Sreekrishna Temple," else "കക്കംവെള്ളി ശ്രീകൃഷ്ണ ക്ഷേത്രം,",
                            style = MaterialTheme.typography.bodyMedium)
                        Text(if (isEn) "Purameri, Kozhikode, Kerala – 673503"
                             else "പുറമേരി, കോഴിക്കോട്, കേരളം – 673503",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Open Maps button
        Button(onClick = { onOpenMaps(StaticData.MAPS_URL) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Map, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isEn) "Open in Google Maps" else "Google Maps-ൽ തുറക്കുക")
        }

        // Transport info
        TransportSection(isEn)

        // Contacts
        Text(if (isEn) "Contact" else "ബന്ധപ്പെടുക",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    var selectedContact by remember { mutableStateOf<page.kakkamvellytemple.app.data.model.ContactPerson?>(null) }

        StaticData.CONTACTS.forEach { contact ->
            ListItem(
                headlineContent = { Text(if (isEn) contact.roleEn else contact.roleMl,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
                trailingContent = {
                    TextButton(onClick = { selectedContact = contact }) {
                        Text(contact.phone, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                    }
                },
                modifier = androidx.compose.foundation.clickable { selectedContact = contact },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }

    selectedContact?.let { contact ->
        AlertDialog(
            onDismissRequest = { selectedContact = null },
            title = { Text(if (isEn) contact.roleEn else contact.roleMl) },
            text = {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Text(contact.phone, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Button(onClick = { onCall(contact.phone); selectedContact = null },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (isEn) "📞 Call" else "📞 വിളിക്കൂ")
                    }
                    OutlinedButton(onClick = { onWhatsApp("91${contact.phone.removePrefix("+91")}"); selectedContact = null },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("💬 WhatsApp")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { selectedContact = null }) { Text(if (isEn) "Cancel" else "റദ്ദ്") } }
        )
    }
    }
}

@Composable
private fun TransportSection(isEn: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (isEn) "How to Reach" else "എങ്ങനെ എത്തിച്ചേരാം",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TransportCard("🚉", if (isEn) "Nearest Railway" else "റെയിൽവേ സ്റ്റേഷൻ",
            if (isEn) "Vadakara (BDJ) — ~15 km\nKozhikode (CLT) — ~45 km"
            else "വടകര (BDJ) — ~15 കി.മീ.\nകോഴിക്കോട് (CLT) — ~45 കി.മീ.")
        TransportCard("🚌", if (isEn) "Nearest Bus Stand" else "ബസ് സ്റ്റാൻഡ്",
            if (isEn) "Nadapuram Bus Stand — ~3-4 km"
            else "നടപ്പുരം ബസ് സ്റ്റാൻഡ് — ~3-4 കി.മീ.")
        TransportCard("📍", if (isEn) "Nearest Bus Stop" else "ഏറ്റവും അടുത്ത ബസ് സ്റ്റോപ്പ്",
            if (isEn) "Purameri Petrol Pump Stop\nWithin walking distance"
            else "Purameri Petrol Pump Stop\nനടന്നെത്താവുന്ന ദൂരം")
    }
}

@Composable
private fun TransportCard(icon: String, title: String, details: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(icon, fontSize = 22.sp)
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text(details, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
