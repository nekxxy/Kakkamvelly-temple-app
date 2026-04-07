package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

@Composable
fun KulamScreen(onCall: (String) -> Unit, onWhatsApp: (String) -> Unit, isEn: Boolean = false) {
    val secretary = StaticData.CONTACTS[1]

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💧", fontSize = 28.sp)
            Column {
                Text(if (isEn) "Renovation Project" else "നവീകരണ പദ്ധതി",
                    style = MaterialTheme.typography.labelMedium, color = Gold.copy(alpha = 0.7f))
                Text(if (isEn) "Temple Pool Renovation" else "അമ്പലം കുളം നവീകരണം",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        // Stats
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(if (isEn) "Target" else "ലക്ഷ്യം", "₹47,00,000", isEn)
                    StatItem(if (isEn) "Collected" else "ശേഖരിച്ചത്", "₹0", isEn)
                    StatItem(if (isEn) "Status" else "നില", if (isEn) "Planning" else "ആസൂത്രണം", isEn)
                }
                LinearProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Gold,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text("0% — ₹47,00,000 ${if (isEn) "remaining" else "ബാക്കി"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Description
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(
                text = if (isEn) "The historic renovation of the temple pond has begun. " +
                    "This pond, abandoned for years, is now being restored through " +
                    "the collective efforts of thousands of devotees."
                else "ക്ഷേത്ര കുളത്തിൻ്റെ ചരിത്രപരമായ നവീകരണ പ്രവൃത്തി ആരംഭിച്ചിരിക്കുന്നു. " +
                    "വർഷങ്ങളായി ഉപേക്ഷിക്കപ്പെട്ട കുളം ഇപ്പോൾ ഭക്തരുടെ " +
                    "കൂട്ടായ്മ ശ്രമഫലമായി പുനർജനിക്കുകയാണ്.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        Text(
            text = if (isEn) "🙏 Be a part of this sacred cause — ₹47 Lakhs needed"
                   else "🙏 ഈ ദൈവകാര്യത്തിൽ പങ്കാളിയാകൂ — ₹47 ലക്ഷം ആവശ്യമുണ്ട്",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = Gold
        )

        // UPI / Donation info
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isEn) "Donate via UPI / Bank Transfer" else "UPI / ബാങ്ക് ട്രാൻസ്ഫർ",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(if (isEn) "Contact Secretary for UPI ID & account details"
                     else "UPI ID / അക്കൗണ്ട് വിവരങ്ങൾക്ക് Secretary-യെ ബന്ധപ്പെടുക",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onCall(secretary.phone) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Phone, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isEn) "Call" else "വിളിക്കൂ", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(onClick = { onWhatsApp(secretary.phone.removePrefix("+")) }, modifier = Modifier.weight(1f)) {
                        Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, isEn: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
