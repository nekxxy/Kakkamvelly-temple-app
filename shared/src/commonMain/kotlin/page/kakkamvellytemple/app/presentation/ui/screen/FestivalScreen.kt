import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow
package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.Festival
import page.kakkamvellytemple.app.presentation.ui.component.CountdownCard
import page.kakkamvellytemple.app.presentation.ui.theme.Gold
import page.kakkamvellytemple.app.presentation.viewmodel.FestivalViewModel
import page.kakkamvellytemple.app.util.ISTClock

@Composable
fun FestivalScreen(viewModel: FestivalViewModel, isEn: Boolean = false) {
    val state by viewModel.state.collectAsState()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = if (isEn) "Festival Calendar" else "ഉത്സവ കലണ്ടർ",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isEn) "2026 – 2029" else "2026 – 2029",
                style = MaterialTheme.typography.labelMedium, color = Gold.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
        }

        item {
            CountdownCard(festival = state.nextFestival, countdown = state.countdown, isEn = isEn)
        }

        item {
            Text(
                text = if (isEn) "Upcoming" else "വരാനിരിക്കുന്ന ഉത്സവങ്ങൾ",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Group by year
        val grouped = state.upcomingFestivals.groupBy {
            it.instant.toLocalDateTime(kotlinx.datetime.TimeZone.of("Asia/Kolkata")).year
        }
        grouped.forEach { (year, festivals) ->
            item {
                Text("— $year —", style = MaterialTheme.typography.labelMedium,
                    color = Gold.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
            }
            items(festivals) { fest -> FestivalQueueItem(fest, isEn) }
        }
    }
}

@Composable
private fun FestivalQueueItem(festival: Festival, isEn: Boolean) {
    val now = ISTClock.epochMs()
    val daysAway = ((festival.dateUtcMs - now) / 86_400_000).toInt()
    val daysLabel = when {
        daysAway == 0  -> if (isEn) "Today" else "ഇന്ന്"
        daysAway == 1  -> if (isEn) "Tomorrow" else "നാളെ"
        else           -> "$daysAway ${if (isEn) "days" else "ദിവസം"}"
    }

    ListItem(
        headlineContent = {
            Text(if (isEn) festival.nameEn else festival.nameMl,
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(if (isEn) festival.nameMl else festival.nameEn,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = { Text(festival.icon, fontSize = 24.sp) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(daysLabel, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
