package dev.paradox.trace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TraceLightColors = lightColorScheme(
    primary = Color(0xFF315C52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E8DF),
    onPrimaryContainer = Color(0xFF102F28),
    secondary = Color(0xFF52645E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F8F5),
    onBackground = Color(0xFF191D1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191D1A),
    surfaceContainer = Color(0xFFEEF1ED),
    onSurfaceVariant = Color(0xFF5C655F),
    outline = Color(0xFFD4DAD4),
)

@Composable
fun TraceTheme(
    content: @Composable () -> Unit,
) {
    // A calm light-first palette is intentional for the initial product direction.
    // Dark theme support can be introduced with a complete, tested design system.
    val colorScheme = if (isSystemInDarkTheme()) TraceLightColors else TraceLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
