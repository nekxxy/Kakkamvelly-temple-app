package page.kakkamvellytemple.app.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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

        val homeVM     = remember { HomeViewModel() }
        val timingsVM  = remember { TimingsViewModel() }

        DisposableEffect(Unit) {
            onDispose { homeVM.dispose(); timingsVM.dispose() }
        }

        Scaffold(
            topBar = { KVTTopBar(isEn = isEn, onToggleLang = { isEn = !isEn }) },
            bottomBar = { KVTBottomBar(selected = selectedTab, isEn = isEn, onSelect = { selectedTab = it }) }
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun KVTBottomBar(selected: NavTab, isEn: Boolean, onSelect: (NavTab) -> Unit) {
    NavigationBar(tonalElevation = 4.dp) {
        TABS.forEach { tab ->
            val isCenter = tab == NavTab.Vazhipad
            NavigationBarItem(
                selected  = selected == tab,
                onClick   = { onSelect(tab) },
                icon = {
                    if (isCenter) {
                        // Om symbol center button
                        Text("ॐ",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = if (selected == tab) Gold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        Icon(tab.icon, if (isEn) tab.labelEn else tab.labelMl)
                    }
                },
                label = {
                    Text(if (isEn) tab.labelEn else tab.labelMl,
                        style = MaterialTheme.typography.labelSmall)
                }
            )
        }
    }
}
