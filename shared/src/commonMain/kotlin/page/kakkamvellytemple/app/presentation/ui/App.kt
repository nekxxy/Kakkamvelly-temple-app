package page.kakkamvellytemple.app.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import page.kakkamvellytemple.app.presentation.ui.component.UpdateBanner
import page.kakkamvellytemple.app.presentation.ui.screen.*
import page.kakkamvellytemple.app.presentation.ui.theme.*
import page.kakkamvellytemple.app.presentation.viewmodel.*
import page.kakkamvellytemple.app.util.UpdateChecker

sealed class NavTab(val route: String, val icon: ImageVector, val labelMl: String, val labelEn: String) {
    object Home     : NavTab("home",     Icons.Default.Home,         "മുഖ്യ",    "Home")
    object Timings  : NavTab("timings",  Icons.Default.AccessTime,   "സമയം",    "Timings")
    object Vazhipad : NavTab("vazhipad", Icons.Default.AutoAwesome,  "വഴിപാട്", "Vazhipad")
    object Gallery  : NavTab("gallery",  Icons.Default.PhotoLibrary, "ഗ്യാലറി", "Gallery")  // #5 replaced Location
    object More     : NavTab("more",     Icons.Default.MoreHoriz,    "കൂടുതൽ", "More")
}

val TABS = listOf(NavTab.Home, NavTab.Timings, NavTab.Vazhipad, NavTab.Gallery, NavTab.More)

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

        var updateInfo by remember { mutableStateOf<page.kakkamvellytemple.app.util.UpdateInfo?>(null) }
        var updateDismissed by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            try { updateInfo = UpdateChecker.check() } catch (_: Exception) {}
        }

        DisposableEffect(Unit) { onDispose { homeVM.dispose(); timingsVM.dispose() } }
        val homeState by homeVM.state.collectAsState()

        Scaffold(
            topBar = {
                Column {
                    KVTTopBar(isEn = isEn, onToggleLang = { isEn = !isEn })
                    val upd = updateInfo
                    if (upd != null && upd.available && !updateDismissed) {
                        UpdateBanner(
                            version   = upd.latestVersion,
                            onUpdate  = { onOpenUrl(upd.downloadUrl) },
                            onDismiss = { updateDismissed = true }
                        )
                    }
                    GlobalStatusBar(homeState.darshanStatus, homeState.annadhanam, homeState.istTimeStr, isEn)
                }
            },
            bottomBar = { KVTBottomBar(selectedTab, isEn) { selectedTab = it } }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                    label = "tab"
                ) { tab ->
                    when (tab) {
                        NavTab.Home     -> HomeScreen(homeVM, isEn)
                        NavTab.Timings  -> TimingsScreen(timingsVM, isEn)
                        NavTab.Vazhipad -> VazhipadScreen(onCall, onWhatsApp, isEn)
                        NavTab.Gallery  -> GalleryScreen(onOpenUrl, isEn)
                        NavTab.More     -> MoreScreen(onOpenMaps, onCall, onWhatsApp, onOpenUrl, isEn)
                    }
                }
            }
        }
    }
}

// ── Top App Bar with styled language switcher (#1) ────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KVTTopBar(isEn: Boolean, onToggleLang: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text("കക്കംവെള്ളി ക്ഷേത്രം", style = MaterialTheme.typography.titleMedium)
                Text("ॐ Hare Krishna", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(0.7f))
            }
        },
        actions = {
            // Language toggle — highlighted selected, grayed other (#1)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(0.4f), RoundedCornerShape(50))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                LangButton("മലയാളം", selected = !isEn) { if (isEn) onToggleLang() }
                LangButton("EN", selected = isEn) { if (!isEn) onToggleLang() }
            }
            Spacer(Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor    = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun LangButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickableNoRipple(onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(0.4f)
        )
    }
}

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

// ── Global Status Bar ─────────────────────────────────────────
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
    Surface(modifier = Modifier.fillMaxWidth(), color = NavyDeep) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape)
                .background(dotColor.copy(if (isOpen) pulse else 1f)))
            Text(
                when {
                    status == null -> "..."
                    isOpen -> { val m = status.minutesUntilChange
                        if (isEn) "${m/60}h ${m%60}m left" else "${m/60}h ${m%60}m കൂടി" }
                    else -> if (isEn) "Opens ${status.nextLabel}" else "${status.nextLabel}-ന് തുറക്കും"
                },
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
                color = dotColor, maxLines = 1
            )
            if (ann != null) {
                Text("·", color = Color.White.copy(0.25f), fontSize = 10.sp)
                Text("🍛", fontSize = 11.sp)
                Text(
                    when {
                        ann.isToday    -> if (isEn) "Annadhanam Today!" else "ഇന്ന് അന്നദാനം!"
                        ann.isTomorrow -> if (isEn) "Tomorrow" else "നാളെ"
                        else           -> if (isEn) "${ann.daysAway}d" else "${ann.daysAway} ദിവസം"
                    },
                    style = MaterialTheme.typography.labelSmall, color = Gold.copy(0.85f), maxLines = 1
                )
            }
            Spacer(Modifier.weight(1f))
            Text(timeStr, style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.35f), maxLines = 1)
        }
    }
}

// ── Bottom Nav ────────────────────────────────────────────────
@Composable
private fun KVTBottomBar(selected: NavTab, isEn: Boolean, onSelect: (NavTab) -> Unit) {
    NavigationBar(tonalElevation = 4.dp) {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick  = { onSelect(tab) },
                icon = {
                    if (tab == NavTab.Vazhipad)
                        Text("ॐ", style = MaterialTheme.typography.titleLarge.copy(
                            color = if (selected == tab) Gold
                                    else MaterialTheme.colorScheme.onSurfaceVariant))
                    else Icon(tab.icon, if (isEn) tab.labelEn else tab.labelMl)
                },
                label = { Text(if (isEn) tab.labelEn else tab.labelMl,
                    style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
