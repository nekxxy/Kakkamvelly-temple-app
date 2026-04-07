package page.kakkamvellytemple.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.WeatherData
import page.kakkamvellytemple.app.data.model.SunTimes
import page.kakkamvellytemple.app.data.model.MoonPhase

@Composable
fun WeatherCard(
    weather: WeatherData?,
    loading: Boolean,
    sunTimes: SunTimes?,
    moonPhase: MoonPhase?,
    modifier: Modifier = Modifier,
    isEn: Boolean = false
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Weather
        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(14.dp)) {
                Text(if (isEn) "WEATHER" else "കാലാവസ്ഥ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else if (weather != null) {
                    Text(weather.icon, fontSize = 32.sp)
                    Text("${weather.temperatureC}°C", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text(weather.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("💧${weather.humidityPct}%  💨${weather.windKmh}km/h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        // Sun + Moon
        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(14.dp)) {
                Text(if (isEn) "SUN · MOON" else "സൂര്യൻ · ചന്ദ്രൻ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                if (sunTimes != null) {
                    SunRow("🌅", if (isEn) "Sunrise" else "സൂര്യോദയം", sunTimes.sunrise)
                    SunRow("🌇", if (isEn) "Sunset" else "സൂര്യാസ്തമയം", sunTimes.sunset)
                }
                if (moonPhase != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(moonPhase.icon, fontSize = 18.sp)
                        Text(if (isEn) moonPhase.nameEn else moonPhase.nameMl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SunRow(icon: String, label: String, time: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 14.sp)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(time, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
}
