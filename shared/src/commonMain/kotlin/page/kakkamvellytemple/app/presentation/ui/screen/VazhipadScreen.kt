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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.VazhipadItem
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.theme.Gold

@Composable
fun VazhipadScreen(onCall: (String) -> Unit, onWhatsApp: (String) -> Unit, isEn: Boolean = false) {
    val secretary = StaticData.CONTACTS[1]
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🙏", fontSize = 32.sp)
                Text(if (isEn) "Vazhipad & Offerings" else "വഴിപാട്",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(if (isEn) "Book through Secretary" else "Secretary വഴി ബുക്ക് ചെയ്യൂ",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.75f))
                Spacer(Modifier.height(4.dp))
            }
        }

        itemsIndexed(StaticData.VAZHIPAD_ITEMS) { idx, item ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, idx * 50)) + slideInVertically(tween(300, idx * 50))
            ) {
                VazhipadCard(item, isEn)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isEn) "To Book" else "ബുക്ക് ചെയ്യാൻ",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(if (isEn) "Contact Secretary Nanu.T to book any Vazhipad"
                         else "ഏത് വഴിപാടും ബുക്ക് ചെയ്യാൻ Secretary Nanu.T-നെ ബന്ധപ്പെടുക",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onCall(secretary.phone) }, modifier = Modifier.weight(1f)) {
                            Text(if (isEn) "📞 Call" else "📞 വിളി", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(onClick = { onWhatsApp("91${secretary.phone.removePrefix("+91")}") }, modifier = Modifier.weight(1f)) {
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (isEn) item.nameEn else item.nameMl,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                if (item.description.isNotEmpty())
                    Text(item.description, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(0.15f),
                shape = RoundedCornerShape(50)
            ) {
                Text("₹${item.amount}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
