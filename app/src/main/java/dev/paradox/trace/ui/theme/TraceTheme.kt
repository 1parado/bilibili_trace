package dev.paradox.trace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val TraceDarkColors = darkColorScheme(
    primary = Color(0xFFA5D0C0),
    onPrimary = Color(0xFF17372E),
    primaryContainer = Color(0xFF315C52),
    onPrimaryContainer = Color(0xFFD5E8DF),
    secondary = Color(0xFFB8CBC2),
    onSecondary = Color(0xFF24352E),
    background = Color(0xFF111714),
    onBackground = Color(0xFFE1E7E1),
    surface = Color(0xFF171E1A),
    onSurface = Color(0xFFE1E7E1),
    surfaceContainer = Color(0xFF202923),
    onSurfaceVariant = Color(0xFFB9C4BC),
    outline = Color(0xFF414D45),
)

@Composable
fun TraceTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isSystemInDarkTheme()) TraceDarkColors else TraceLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
