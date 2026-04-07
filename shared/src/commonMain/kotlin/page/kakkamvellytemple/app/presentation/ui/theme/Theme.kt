package page.kakkamvellytemple.app.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Temple Colour Palette ──────────────────────────────────
val Gold          = Color(0xFFFFD700)
val GoldDark      = Color(0xFFC8860A)
val GoldDim       = Color(0xFFAD8A3A)
val NavyDeep      = Color(0xFF08041C)
val NavyMid       = Color(0xFF14062A)
val NavyLight     = Color(0xFF1E0F3A)
val CreamLight    = Color(0xFFFDF6E3)
val CreamMid      = Color(0xFFFFF8EC)
val SaffronLight  = Color(0xFFFFF3E0)
val TempleRed     = Color(0xFF8B0000)
val DarkBrown     = Color(0xFF3A2A00)
val OpenGreen     = Color(0xFF22C55E)
val ClosedRed     = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary         = Gold,
    onPrimary       = NavyDeep,
    primaryContainer = NavyLight,
    onPrimaryContainer = Gold,
    secondary       = GoldDark,
    onSecondary     = Color.White,
    background      = NavyDeep,
    onBackground    = Color.White,
    surface         = NavyMid,
    onSurface       = Color.White,
    surfaceVariant  = Color(0xFF1A0A2E),
    onSurfaceVariant = Color(0xFFCCBB88),
    outline         = Color(0x33FFD700),
    error           = ClosedRed
)

private val LightColorScheme = lightColorScheme(
    primary         = GoldDark,
    onPrimary       = Color.White,
    primaryContainer = SaffronLight,
    onPrimaryContainer = DarkBrown,
    secondary       = TempleRed,
    onSecondary     = Color.White,
    background      = CreamLight,
    onBackground    = DarkBrown,
    surface         = Color.White,
    onSurface       = DarkBrown,
    surfaceVariant  = CreamMid,
    onSurfaceVariant = Color(0xFF5A3A00),
    outline         = Color(0x33C8860A),
    error           = ClosedRed
)

@Composable
fun KakkamvellyTempleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = KVTTypography(),
        content     = content
    )
}
