package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.VazhipadItem
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

// Group labels for the items
private val SECTION_HEADERS = mapOf(
    0  to "🛕 കൃഷ്ണൻ — Krishna",
    19 to "👶 ജീവിത ചടങ്ങുകൾ — Life Events",
    29 to "🌸 ഭഗവതി — Special",
    35 to "🐘 ഗണപതി — Ganapathi"
)

@Composable
fun VazhipadScreen(onCall: (String) -> Unit, onWhatsApp: (String) -> Unit, isEn: Boolean = false) {
    val secretary = StaticData.CONTACTS[1]
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val items = StaticData.VAZHIPAD_ITEMS

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("🙏", fontSize = 28.sp)
                Text(if (isEn) "Vazhipad & Offerings" else "വഴിപാടുകൾ",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(if (isEn) "Reg. No. 255/94 · Book via Secretary"
                     else "രജി. നം. 255/94 · Secretary വഴി ബുക്ക് ചെയ്യൂ",
                    style = MaterialTheme.typography.labelMedium, color = Gold.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))
            }
        }

        itemsIndexed(items) { idx, item ->
            // Section header
            SECTION_HEADERS[idx]?.let { header ->
                Spacer(Modifier.height(8.dp))
                Text(header, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 2.dp))
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(250, idx * 20)) + slideInVertically(tween(250, idx * 20))
            ) {
                VazhipadCard(item, isEn)
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isEn) "Book a Vazhipad" else "വഴിപാട് ബുക്ക് ചെയ്യാൻ",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(if (isEn) "Contact Secretary Nanu.T — payment in advance required"
                         else "Secretary Nanu.T-നെ ബന്ധപ്പെടുക — വഴിപാടുകൾ മുൻകൂട്ടി ശ്ലീറ്റാക്കണം",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onCall(secretary.phone) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📞 ${secretary.phone}", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { onWhatsApp("91${secretary.phone.removePrefix("+91")}") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VazhipadCard(item: VazhipadItem, isEn: Boolean) {
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(if (isEn) item.nameEn else item.nameMl,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground)
                if (item.description.isNotEmpty())
                    Text(item.description, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.65f))
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
