package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.data.model.TodayEvent
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.presentation.ui.component.*
import page.kakkamvellytemple.app.presentation.ui.theme.*
import page.kakkamvellytemple.app.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel, isEn: Boolean = false) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // Animate cards in on load
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // ── Hero: Krishna image ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(listOf(NavyDeep, NavyMid, Color.Transparent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                NetworkImage(
                    url = StaticData.KRISHNA_IMAGE_URL,
                    contentDescription = "Baby Krishna",
                    modifier = Modifier.height(240.dp).padding(top = 16.dp)
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ॐ", fontSize = 20.sp, color = Gold)
                    Text(
                        if (isEn) "Kakkamvelly Sreekrishna Temple" else "കക്കംവെള്ളി ശ്രീകൃഷ്ണ ക്ഷേത്രം",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Darshan Status — MOST PROMINENT ──────────────────
                AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400))) {
                    DarshanBigCard(
                        status = state.darshanStatus,
                        timeStr = state.istTimeStr,
                        isEn = isEn
                    )
                }

                // ── Today's Events ────────────────────────────────────
                AnimatedVisibility(visible, enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100))) {
                    TodayEventsCard(events = StaticData.TODAY_EVENTS, isEn = isEn)
                }

                // ── Annadhanam Countdown ─────────────────────────────
                state.annadhanam?.let { ann ->
                    AnimatedVisibility(visible, enter = fadeIn(tween(500, 200))) {
                        AnnadhanamCard(ann, isEn)
                    }
                }

                // ── Festival Countdown ───────────────────────────────
                AnimatedVisibility(visible, enter = fadeIn(tween(500, 300))) {
                    CountdownCard(festival = state.nextFestival, countdown = state.countdown, isEn = isEn)
                }

                // ── Weather + Sun/Moon ───────────────────────────────
                AnimatedVisibility(visible, enter = fadeIn(tween(500, 400))) {
                    WeatherCard(
                        weather = state.weather,
                        loading = state.weatherLoading,
                        sunTimes = state.sunTimes,
                        moonPhase = state.moonPhase,
                        isEn = isEn
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DarshanBigCard(status: page.kakkamvellytemple.app.data.model.DarshanStatus?, timeStr: String, isEn: Boolean) {
    val isOpen = status?.isOpen == true
    val bgColor = if (isOpen) Color(0xFF0F3D1E) else Color(0xFF3D0F0F)
    val accentColor = if (isOpen) Color(0xFF4ADE80) else Color(0xFFFF6B6B)

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "p"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = pulse)))
                Text(
                    if (isEn) (if (isOpen) "Temple is Open" else "Temple is Closed")
                    else (if (isOpen) "ക്ഷേത്രം തുറന്നിരിക്കുന്നു" else "ക്ഷേത്രം അടഞ്ഞിരിക്കുന്നു"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(Modifier.weight(1f))
                Text(timeStr, style = MaterialTheme.typography.labelSmall,
                    color = Gold.copy(alpha = 0.7f))
            }
            if (status != null) {
                val sub = if (isOpen) {
                    val m = status.minutesUntilChange
                    if (isEn) "Closes in ${m / 60}h ${m % 60}m" else "${m / 60}h ${m % 60}m കൂടി"
                } else {
                    if (isEn) "Opens at ${status.nextLabel}" else "${status.nextLabel}-ന് തുറക്കും"
                }
                Text(sub, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f))
            }
            HorizontalDivider(color = accentColor.copy(alpha = 0.2f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text(if (isEn) "☀️ 5:30–9:00 AM" else "☀️ 5:30–9:00 AM",
                    style = MaterialTheme.typography.labelMedium, color = Gold.copy(alpha = 0.8f))
                Text("·", color = Gold.copy(alpha = 0.4f))
                Text(if (isEn) "🌙 5:45–6:45 PM" else "🌙 5:45–6:45 PM",
                    style = MaterialTheme.typography.labelMedium, color = Gold.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun TodayEventsCard(events: List<TodayEvent>, isEn: Boolean) {
    if (events.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📅", fontSize = 16.sp)
                Text(if (isEn) "Today's Events" else "ഇന്നത്തെ പ്രത്യേക കർമ്മങ്ങൾ",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = Gold)
            }
            events.forEach { event ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(event.icon, fontSize = 18.sp)
                    Column(Modifier.weight(1f)) {
                        Text(if (isEn) event.titleEn else event.titleMl,
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(if (isEn) event.timeEn else event.timeMl,
                        style = MaterialTheme.typography.labelSmall, color = Gold,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AnnadhanamCard(ann: page.kakkamvellytemple.app.data.model.AnnadhanamInfo, isEn: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A00))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🍛", fontSize = 32.sp)
            Column(Modifier.weight(1f)) {
                Text(if (isEn) "Next Annadhanam" else "അടുത്ത അന്നദാനം",
                    style = MaterialTheme.typography.labelMedium, color = Gold.copy(alpha = 0.7f))
                Text(ann.nextDate, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(if (isEn) "Every 1st Sunday · Noon" else "ഓരോ ഒന്നാം ഞായർ · ഉച്ചക്ക് 12:00",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when {
                        ann.isToday -> if (isEn) "Today!" else "ഇന്ന്!"
                        ann.isTomorrow -> if (isEn) "Tomorrow" else "നാളെ"
                        else -> "${ann.daysAway}"
                    },
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Gold
                )
                if (!ann.isToday && !ann.isTomorrow)
                    Text(if (isEn) "days" else "ദിവസം",
                        style = MaterialTheme.typography.labelSmall, color = Gold.copy(alpha = 0.7f))
            }
        }
    }
}
