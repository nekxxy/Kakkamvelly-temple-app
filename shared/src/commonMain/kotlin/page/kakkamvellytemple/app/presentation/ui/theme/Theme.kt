package page.kakkamvellytemple.app.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Temple Palette ─────────────────────────────────────────────
val Gold          = Color(0xFFFFD700)
val GoldDark      = Color(0xFFC8860A)
val GoldDeep      = Color(0xFF8B5E00)
val NavyDeep      = Color(0xFF08041C)
val NavyMid       = Color(0xFF14062A)
val NavyLight     = Color(0xFF1E0F3A)
val SaffronBg     = Color(0xFFFFF3E0)
val SaffronMid    = Color(0xFFFFE0B2)
val OpenGreen     = Color(0xFF22C55E)
val ClosedRed     = Color(0xFFEF4444)
val TempRed       = Color(0xFF8B0000)
val CreamLight    = Color(0xFFFDF6E3)

// ── SAFE COLOURS: readable on both light and dark ──────────────
// Use these instead of hardcoded Gold/White
// onGoldSurface: use when text IS ON a gold/saffron background
val OnGoldText    = Color(0xFF3D2000)   // very dark brown — always readable on gold
// goldOnDark: gold text on dark background — fine
val GoldOnDark    = Gold
// accentOnLight: temple red/deep gold on light background
val AccentOnLight = GoldDeep

private val DarkColorScheme = darkColorScheme(
    primary           = Gold,
    onPrimary         = NavyDeep,
    primaryContainer  = Color(0xFF2A1A00),
    onPrimaryContainer= Gold,
    secondary         = GoldDark,
    onSecondary       = Color.White,
    background        = NavyDeep,
    onBackground      = Color(0xFFF0E8D0),
    surface           = NavyMid,
    onSurface         = Color(0xFFEEE8D5),
    surfaceVariant    = Color(0xFF1A0A2E),
    onSurfaceVariant  = Color(0xFFCCBB88),
    outline           = Color(0x44FFD700),
    error             = ClosedRed
)

private val LightColorScheme = lightColorScheme(
    primary           = GoldDeep,
    onPrimary         = Color.White,
    primaryContainer  = SaffronMid,
    onPrimaryContainer= OnGoldText,    // dark brown — readable on saffron
    secondary         = TempRed,
    onSecondary       = Color.White,
    background        = CreamLight,
    onBackground      = Color(0xFF1A0800),
    surface           = Color.White,
    onSurface         = Color(0xFF1A0800),
    surfaceVariant    = SaffronBg,
    onSurfaceVariant  = Color(0xFF5A3A00),
    outline           = Color(0x44C8860A),
    error             = ClosedRed
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
