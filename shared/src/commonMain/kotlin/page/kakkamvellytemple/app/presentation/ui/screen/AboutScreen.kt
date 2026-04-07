package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

@Composable
fun AboutScreen(isEn: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🛕", fontSize = 48.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(
            text = if (isEn) "About the Temple" else "ക്ഷേത്രത്തെ കുറിച്ച്",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (isEn) "Sacred Home of Baby Krishna" else "ഉണ്ണിക്കൃഷ്ണന്റെ പുണ്യ ഭൂമി",
            style = MaterialTheme.typography.titleMedium, color = Gold
        )
        HorizontalDivider(color = Gold.copy(alpha = 0.2f))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEn) {
                    Text("A small but beautiful temple — once destroyed during Tippu Sultan's rampage through Malabar — Kakkamvelly Sreekrishna Temple stands today as a symbol of enduring faith and devotion.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("The presiding deity is Lord Krishna in his childhood form — Little Krishna (ഉണ്ണിക്കൃഷ്ണൻ). Many devotees believe the idol here resembles the sacred form at the famous Guruvayur Temple.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("This is a quiet, intimate temple — there are no vedikkett (fireworks) during festivals, and elephants are not part of the celebrations here. What you will find is pure, peaceful devotion.",
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("ടിപ്പുവിന്റെ ആക്രമണത്തിൽ തകർന്നിട്ടുണ്ടായ ഈ ക്ഷേത്രം ഇന്ന് സമാധാനത്തിന്റെയും ഭക്തിയുടെയും ഒരു പ്രതീകമായി നിലനിൽക്കുന്നു.",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("ഗുരുവായൂരിലെ പ്രതിമയെ ഓർമ്മിപ്പിക്കുന്ന കുട്ടികൃഷ്ണന്റെ (ഉണ്ണിക്കൃഷ്ണൻ) രൂപത്തിൽ പ്രതിഷ്ഠിതനായ ഭഗവാൻ കൃഷ്ണൻ, ഭക്തിയോടെ പ്രാർത്ഥിക്കുന്നവരുടെ ആഗ്രഹങ്ങൾ സാക്ഷാത്കരിക്കുന്നു.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Highlights
        val highlights = listOf(
            Triple("🛕", if (isEn) "Ancient Temple" else "പ്രാചീന ക്ഷേത്രം",
                if (isEn) "Historic temple with rich heritage" else "സമ്പന്നമായ പൈതൃകം"),
            Triple("🪷", if (isEn) "Baby Krishna Idol" else "ഉണ്ണിക്കൃഷ്ണൻ",
                if (isEn) "Same form as Guruvayur idol" else "ഗുരുവായൂർ ഇഡോൾ സമാന രൂപം"),
            Triple("🍛", if (isEn) "Monthly Annadhanam" else "അന്നദാനം",
                if (isEn) "Free prasadam every first Sunday" else "ഓരോ ഒന്നാം ഞായർ"),
            Triple("💧", if (isEn) "Kulam Renovation" else "കുളം നവീകരണം",
                if (isEn) "₹47 Lakhs restoration project" else "₹47 ലക്ഷം നവീകരണ പദ്ധതി")
        )
        highlights.forEach { (icon, title, sub) ->
            ListItem(
                leadingContent = { Text(icon, fontSize = 24.sp) },
                headlineContent = { Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
                supportingContent = { Text(sub, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}
