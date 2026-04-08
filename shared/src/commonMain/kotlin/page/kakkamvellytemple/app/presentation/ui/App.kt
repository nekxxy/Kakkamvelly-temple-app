package page.kakkamvellytemple.app.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import page.kakkamvellytemple.app.presentation.ui.screen.*
import page.kakkamvellytemple.app.presentation.ui.theme.*
import page.kakkamvellytemple.app.presentation.viewmodel.*

sealed class NavTab(val route: String, val icon: ImageVector, val labelMl: String, val labelEn: String) {
    object Home     : NavTab("home",     Icons.Default.Home,         "മുഖ്യ",    "Home")
    object Timings  : NavTab("timings",  Icons.Default.AccessTime,   "സമയം",    "Timings")
    object Vazhipad : NavTab("vazhipad", Icons.Default.AutoAwesome,  "വഴിപാട്", "Vazhipad")
    object Location : NavTab("location", Icons.Default.LocationOn,   "സ്ഥലം",   "Location")
    object More     : NavTab("more",     Icons.Default.MoreHoriz,    "കൂടുതൽ", "More")
}

val TABS = listOf(NavTab.Home, NavTab.Timings, NavTab.Vazhipad, NavTab.Location, NavTab.More)

@Composable
fun KVTApp(
    onOpenMaps: (String) -> Unit = {},
    onCall: (String) -> Unit = {},
    onWhatsApp: (String) -> Unit = {},
    onOpenUrl: (String) -> Unit = {}
) {
    KakkamvellyTempleTheme {
        var selectedTab by remember { mutableStateOf<NavTab>(NavTab.Home) }
        var isEn by remember { mutableStateOf(false) }

        val homeVM    = remember { HomeViewModel() }
        val timingsVM = remember { TimingsViewModel() }

        DisposableEffect(Unit) {
            onDispose { homeVM.dispose(); timingsVM.dispose() }
        }

        val homeState by homeVM.state.collectAsState()

        Scaffold(
            topBar = {
                Column {
                    KVTTopBar(isEn = isEn, onToggleLang = { isEn = !isEn })
                    // ── Sticky status bar — visible on ALL screens ──
                    GlobalStatusBar(
                        status   = homeState.darshanStatus,
                        ann      = homeState.annadhanam,
                        timeStr  = homeState.istTimeStr,
                        isEn     = isEn
                    )
                }
            },
            bottomBar = {
                KVTBottomBar(selected = selectedTab, isEn = isEn, onSelect = { selectedTab = it })
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                    label = "tab"
                ) { tab ->
                    when (tab) {
                        NavTab.Home     -> HomeScreen(homeVM, isEn)
                        NavTab.Timings  -> TimingsScreen(timingsVM, isEn)
                        NavTab.Vazhipad -> VazhipadScreen(onCall, onWhatsApp, isEn)
                        NavTab.Location -> LocationScreen(onOpenMaps, onCall, isEn)
                        NavTab.More     -> MoreScreen(onOpenMaps, onCall, onWhatsApp, onOpenUrl, isEn)
                    }
                }
            }
        }
    }
}

// ── Global Status Bar — shown on every screen ─────────────────
@Composable
fun GlobalStatusBar(
    status: page.kakkamvellytemple.app.data.model.DarshanStatus?,
    ann: page.kakkamvellytemple.app.data.model.AnnadhanamInfo?,
    timeStr: String,
    isEn: Boolean
) {
    val isOpen = status?.isOpen == true
    val dotColor = if (isOpen) Color(0xFF4ADE80) else Color(0xFFFF6B6B)
    val pulse by rememberInfiniteTransition(label = "p").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pa"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyDeep,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pulsing open/closed dot
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(dotColor.copy(alpha = if (isOpen) pulse else 1f))
            )

            // Temple status
            Text(
                text = when {
                    status == null -> "..."
                    isOpen -> {
                        val m = status.minutesUntilChange
                        if (isEn) "${m/60}h ${m%60}m left"
                        else "${m/60}h ${m%60}m കൂടി"
                    }
                    else -> {
                        if (isEn) "Opens ${status.nextLabel}"
                        else "${status.nextLabel}-ന് തുറക്കും"
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = dotColor,
                maxLines = 1
            )

            // Divider dot
            Text("·", color = Color.White.copy(0.25f), fontSize = 10.sp)

            // Annadhanam countdown
            if (ann != null) {
                Text("🍛", fontSize = 11.sp)
                Text(
                    text = when {
                        ann.isToday    -> if (isEn) "Annadhanam Today!" else "ഇന്ന് അന്നദാനം!"
                        ann.isTomorrow -> if (isEn) "Annadhanam Tomorrow" else "നാളെ അന്നദാനം"
                        else           -> if (isEn) "${ann.daysAway}d to Annadhanam" else "${ann.daysAway} ദിവസം"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold.copy(0.85f),
                    maxLines = 1
                )
            }

            Spacer(Modifier.weight(1f))

            // IST clock
            Text(
                timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.35f),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KVTTopBar(isEn: Boolean, onToggleLang: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text("കക്കംവെള്ളി ക്ഷേത്രം", style = MaterialTheme.typography.titleMedium)
                Text("ॐ Hare Krishna", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        },
        actions = {
            TextButton(onClick = onToggleLang) {
                Text(if (isEn) "മലയാളം" else "English",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor     = MaterialTheme.colorScheme.surface,
            titleContentColor  = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun KVTBottomBar(selected: NavTab, isEn: Boolean, onSelect: (NavTab) -> Unit) {
    NavigationBar(tonalElevation = 4.dp) {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick  = { onSelect(tab) },
                icon = {
                    if (tab == NavTab.Vazhipad) {
                        Text("ॐ", style = MaterialTheme.typography.titleLarge.copy(
                            color = if (selected == tab) Gold
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        ))
                    } else {
                        Icon(tab.icon, if (isEn) tab.labelEn else tab.labelMl)
                    }
                },
                label = { Text(if (isEn) tab.labelEn else tab.labelMl,
                    style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
