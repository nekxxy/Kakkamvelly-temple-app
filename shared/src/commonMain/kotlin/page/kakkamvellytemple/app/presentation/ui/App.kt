package page.kakkamvellytemple.app.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import page.kakkamvellytemple.app.presentation.ui.screen.*
import page.kakkamvellytemple.app.presentation.ui.theme.KakkamvellyTempleTheme
import page.kakkamvellytemple.app.presentation.viewmodel.*

sealed class NavTab(val route: String, val iconRes: ImageVector, val labelMl: String, val labelEn: String) {
    object Home     : NavTab("home",     Icons.Default.Home,        "മുഖ്യ",    "Home")
    object Festival : NavTab("festival", Icons.Default.Celebration, "ഉത്സവം",  "Festival")
    object Timings  : NavTab("timings",  Icons.Default.AccessTime,  "സമയം",    "Timings")
    object Gallery  : NavTab("gallery",  Icons.Default.PhotoLibrary,"ഗ്യാലറി", "Gallery")
    object More     : NavTab("more",     Icons.Default.MoreHoriz,   "കൂടുതൽ", "More")
}

val TABS = listOf(NavTab.Home, NavTab.Festival, NavTab.Timings, NavTab.Gallery, NavTab.More)

@Composable
fun KVTApp(
    onOpenMaps: (String) -> Unit = {},
    onCall: (String) -> Unit = {},
    onWhatsApp: (String) -> Unit = {}
) {
    KakkamvellyTempleTheme {
        var selectedTab by remember { mutableStateOf<NavTab>(NavTab.Home) }
        var isEn by remember { mutableStateOf(false) }

        // ViewModels — created once, survive tab changes
        val homeVM     = remember { HomeViewModel() }
        val festivalVM = remember { FestivalViewModel() }
        val timingsVM  = remember { TimingsViewModel() }

        Scaffold(
            topBar = { KVTTopBar(isEn = isEn, onToggleLang = { isEn = !isEn }) },
            bottomBar = {
                KVTBottomBar(selected = selectedTab, isEn = isEn, onSelect = { selectedTab = it })
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180)) },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        NavTab.Home     -> HomeScreen(homeVM, isEn)
                        NavTab.Festival -> FestivalScreen(festivalVM, isEn)
                        NavTab.Timings  -> TimingsScreen(timingsVM, isEn)
                        NavTab.Gallery  -> GalleryScreen(isEn)
                        NavTab.More     -> MoreScreen(
                            onOpenMaps = onOpenMaps,
                            onCall = onCall,
                            onWhatsApp = onWhatsApp,
                            isEn = isEn
                        )
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
                Text(
                    text = "കക്കംവെള്ളി ക്ഷേത്രം",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "ॐ Hare Krishna",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
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
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.iconRes, contentDescription = if (isEn) tab.labelEn else tab.labelMl) },
                label = { Text(if (isEn) tab.labelEn else tab.labelMl,
                    style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
