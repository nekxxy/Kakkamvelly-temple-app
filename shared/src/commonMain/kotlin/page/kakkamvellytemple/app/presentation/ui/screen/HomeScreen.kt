import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow
package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.presentation.ui.component.*
import page.kakkamvellytemple.app.presentation.ui.theme.Gold
import page.kakkamvellytemple.app.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel, isEn: Boolean = false) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky darshan bar
        DarshanStatusCard(status = state.darshanStatus, timeStr = state.istTimeStr)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero greeting
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("🕉", fontSize = 48.sp)
                Text(
                    text = if (isEn) "HARE KRISHNA" else "ഹരേ കൃഷ്ണ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isEn) "Kakkamvelly Sreekrishna Temple" else "കക്കംവെള്ളി ശ്രീകൃഷ്ണ ക്ഷേത്രം",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "📍 ${if (isEn) "Purameri, Kozhikode, Kerala" else "പുറമേരി, കോഴിക്കോട്, കേരളം"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Festival countdown
            CountdownCard(
                festival = state.nextFestival,
                countdown = state.countdown,
                isEn = isEn
            )

            // Weather + Sun/Moon
            WeatherCard(
                weather = state.weather,
                loading = state.weatherLoading,
                sunTimes = state.sunTimes,
                moonPhase = state.moonPhase,
                isEn = isEn
            )

            // Annadhanam banner
            state.annadhanam?.let { ann ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("🍛", fontSize = 28.sp)
                        Column {
                            Text(
                                text = if (isEn) "Monthly Annadhanam" else "അന്നദാനം — ഒന്നാം ഞായർ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val label = when {
                                ann.isToday    -> if (isEn) "🎉 Today! Noon 12:00 PM" else "🎉 ഇന്ന്! ഉച്ചക്ക് 12:00"
                                ann.isTomorrow -> if (isEn) "Tomorrow · Noon 12:00 PM" else "നാളെ · ഉച്ചക്ക് 12:00"
                                else           -> "📅 ${ann.nextDate} · ${if (isEn) "Noon 12:00 PM" else "ഉച്ചക്ക് 12:00"}"
                            }
                            Text(label, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}
