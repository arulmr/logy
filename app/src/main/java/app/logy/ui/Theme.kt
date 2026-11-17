package app.logy.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.logy.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF6553C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E1FF),
    onPrimaryContainer = Color(0xFF21105E),
    secondary = Color(0xFF006B5C),
    secondaryContainer = Color(0xFF79F8DC),
    background = Color(0xFFF9F7FC),
    surface = Color(0xFFF9F7FC),
    surfaceContainer = Color(0xFFF0EDF4),
    surfaceContainerHigh = Color(0xFFEAE7EE),
    outlineVariant = Color(0xFFCAC6D0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCBBEFF),
    onPrimary = Color(0xFF362479),
    primaryContainer = Color(0xFF4D3B96),
    onPrimaryContainer = Color(0xFFE8E1FF),
    secondary = Color(0xFF58DBC1),
    secondaryContainer = Color(0xFF005045),
    background = Color(0xFF121116),
    surface = Color(0xFF121116),
    surfaceContainer = Color(0xFF201E24),
    surfaceContainerHigh = Color(0xFF2B292F),
    outlineVariant = Color(0xFF48454D)
)

@Composable
fun LogyTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
