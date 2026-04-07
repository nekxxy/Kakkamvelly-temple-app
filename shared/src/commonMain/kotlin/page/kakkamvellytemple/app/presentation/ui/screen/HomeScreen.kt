package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Infinite slow rotation for mandala
    val rotation by rememberInfiniteTransition(label = "rot").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "r"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ── Hero Section ───────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().height(340.dp)
                    .background(Brush.verticalGradient(
                        listOf(NavyDeep, Color(0xFF1A062E), Color(0xFF0D0820))
                    ))
            ) {
                // Rotating mandala ring behind image
                Box(
                    modifier = Modifier.size(280.dp).align(Alignment.Center)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Gold.copy(0.0f), Gold.copy(0.08f),
                                    Gold.copy(0.0f), Gold.copy(0.12f),
                                    Gold.copy(0.0f), Gold.copy(0.08f),
                                    Gold.copy(0.0f)
                                )
                            )
                        )
                )
                // Glow circle
                Box(
                    modifier = Modifier.size(220.dp).align(Alignment.Center)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Gold.copy(0.08f), Color.Transparent)
                            )
                        )
                )
                // Krishna image — full height, not cropped
                NetworkImage(
                    url = StaticData.KRISHNA_IMAGE_URL,
                    contentDescription = "Baby Krishna",
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(0.65f)
                        .align(Alignment.Center)
                )
                // Bottom fade
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF0D0820))
                            )
                        )
                )
                // Title text over image
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ॐ", fontSize = 18.sp, color = Gold, fontWeight = FontWeight.Bold)
                    Text(
                        if (isEn) "Kakkamvelly Sreekrishna Temple"
                        else "കക്കംവെള്ളി ശ്രീകൃഷ്ണ ക്ഷേത്രം",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "📍 ${if (isEn) "Purameri, Kozhikode, Kerala" else "പുറമേരി, കോഴിക്കോട്, കേരളം"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.65f)
                    )
                }
            }

            // ── Content cards — long scroll ─────────────────────────
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Darshan — most prominent
                AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400))) {
                    DarshanBigCard(state.darshanStatus, state.istTimeStr, isEn)
                }

                // Today's Events
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 80))) {
                    TodayEventsCard(StaticData.TODAY_EVENTS, isEn)
                }

                // Annadhanam countdown
                state.annadhanam?.let { ann ->
                    AnimatedVisibility(visible, enter = fadeIn(tween(400, 160))) {
                        AnnadhanamCard(ann, isEn)
                    }
                }

                // Festival countdown
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 240))) {
                    CountdownCard(festival = state.nextFestival, countdown = state.countdown, isEn = isEn)
                }

                // Weather
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 320))) {
                    WeatherCard(
                        weather = state.weather,
                        loading = state.weatherLoading,
                        sunTimes = state.sunTimes,
                        moonPhase = state.moonPhase,
                        isEn = isEn
                    )
                }

                // Pool Renovation teaser
                AnimatedVisibility(visible, enter = fadeIn(tween(400, 400))) {
                    KulamTeaser(isEn)
                }
            }
        }
    }
}

// ── Darshan Big Card ──────────────────────────────────────────
@Composable
fun DarshanBigCard(
    status: page.kakkamvellytemple.app.data.model.DarshanStatus?,
    timeStr: String,
    isEn: Boolean
) {
    val isOpen = status?.isOpen == true
    val cardBg     = if (isOpen) Color(0xFF062010) else Color(0xFF200606)
    val accentClr  = if (isOpen) Color(0xFF4ADE80) else Color(0xFFFF6B6B)
    val pulseAlpha by rememberInfiniteTransition(label = "p").animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pa"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Status row
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(13.dp).clip(CircleShape)
                        .background(accentClr.copy(pulseAlpha))
                )
                Text(
                    if (isOpen) (if (isEn) "Temple is Open" else "ക്ഷേത്രം തുറന്നിരിക്കുന്നു")
                    else        (if (isEn) "Temple is Closed" else "ക്ഷേത്രം അടഞ്ഞിരിക്കുന്നു"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentClr
                )
                Spacer(Modifier.weight(1f))
                Text(timeStr, style = MaterialTheme.typography.labelSmall,
                    color = Gold.copy(0.75f))
            }
            // Time until change
            if (status != null) {
                val m = status.minutesUntilChange
                val msg = if (isOpen)
                    (if (isEn) "Closes in ${m/60}h ${m%60}m" else "${m/60}h ${m%60}m കൂടി")
                else
                    (if (isEn) "Opens at ${status.nextLabel}" else "${status.nextLabel}-ന് തുറക്കും")
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f))
            }
            // Divider
            Box(Modifier.fillMaxWidth().height(1.dp).background(accentClr.copy(0.2f)))
            // Hours
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("☀️  5:30–9:00 AM", style = MaterialTheme.typography.labelMedium,
                    color = Gold.copy(0.85f))
                Text("🌙  5:45–6:45 PM", style = MaterialTheme.typography.labelMedium,
                    color = Gold.copy(0.85f))
            }
        }
    }
}

// ── Today's Events ─────────────────────────────────────────────
@Composable
fun TodayEventsCard(events: List<TodayEvent>, isEn: Boolean) {
    if (events.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 16.sp)
                Text(
                    if (isEn) "Today's Events" else "ഇന്നത്തെ കർമ്മങ്ങൾ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            events.forEach { ev ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(0.6f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ev.icon, fontSize = 18.sp)
                    Text(
                        if (isEn) ev.titleEn else ev.titleMl,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (isEn) ev.timeEn else ev.timeMl,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        // Safe on dark AND light: use primary
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ── Annadhanam Card ────────────────────────────────────────────
@Composable
fun AnnadhanamCard(ann: page.kakkamvellytemple.app.data.model.AnnadhanamInfo, isEn: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🍛", fontSize = 32.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    if (isEn) "Next Annadhanam" else "അടുത്ത അന്നദാനം",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(ann.nextDate, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (isEn) "Every 1st Sunday · Noon 12:00" else "ഓരോ ഒന്നാം ഞായർ · ഉച്ചക്ക്",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Day count badge — primary on primaryContainer = always readable
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when { ann.isToday -> "🎉"; ann.isTomorrow -> "1"; else -> "${ann.daysAway}" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        // onPrimaryContainer is ALWAYS readable on primaryContainer
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (!ann.isToday) Text(
                        if (isEn) "days" else "ദിവസം",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.75f)
                    )
                }
            }
        }
    }
}

// ── Pool Renovation Teaser ──────────────────────────────────────
@Composable
fun KulamTeaser(isEn: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("💧", fontSize = 28.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    if (isEn) "Kulam Renovation" else "കുളം നവീകരണം",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    if (isEn) "₹47 Lakh project — Support needed"
                    else "₹47 ലക്ഷം · ഭക്ത സഹകരണം ആവശ്യം",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                )
            }
            Text("→", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
