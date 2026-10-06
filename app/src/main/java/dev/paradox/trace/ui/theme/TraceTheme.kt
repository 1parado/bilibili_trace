package dev.paradox.trace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Design tokens ported from 知时 (github.com/1parado/zhishi), originally from
 * @paradox/ui (shadcn token system). See docs/UI_DESIGN_REFERENCE.md.
 * Calm near-black primary, lightest borders, no decorative color.
 */

private val TraceLightColors = lightColorScheme(
    primary = Color(0xFF0F172A),
    onPrimary = Color(0xFFF8FAFC),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF020817),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF020817),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFEF4444),
    onError = Color(0xFFF8FAFC),
)

private val TraceDarkColors = darkColorScheme(
    primary = Color(0xFFF8FAFC),
    onPrimary = Color(0xFF0F172A),
    background = Color(0xFF020817),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF020817),
    onSurface = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFF020817),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1E293B),
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFF7F1D1D),
    onError = Color(0xFFF8FAFC),
)

/** Semantic colors beyond the Material palette: success/warning/chart series. */
data class TraceExtendedColors(
    val success: Color,
    val warning: Color,
    val chart: List<Color>,
)

private val LightExtendedColors = TraceExtendedColors(
    success = Color(0xFF16A34A),
    warning = Color(0xFFF59E0B),
    chart = listOf(
        Color(0xFF2463EB),
        Color(0xFF7C3BED),
        Color(0xFF12A594),
        Color(0xFFDB7706),
        Color(0xFFDF2060),
    ),
)

private val DarkExtendedColors = TraceExtendedColors(
    success = Color(0xFF22C55E),
    warning = Color(0xFFD97706),
    chart = listOf(
        Color(0xFF6DA2F8),
        Color(0xFFB68EF6),
        Color(0xFF3CDDC7),
        Color(0xFFFAB047),
        Color(0xFFF47BA3),
    ),
)

val LocalTraceExtendedColors = staticCompositionLocalOf { LightExtendedColors }

@Composable
fun TraceTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) TraceDarkColors else TraceLightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalTraceExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/** Convenience accessor for extended tokens inside composables. */
val TraceThemeExtended: TraceExtendedColors
    @Composable get() = LocalTraceExtendedColors.current
