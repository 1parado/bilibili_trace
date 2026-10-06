package dev.paradox.trace.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.paradox.trace.ui.theme.TraceThemeExtended
import dev.paradox.trace.domain.model.ContentSession

/**
 * Horizontal 24h band. Each session renders as a rounded segment positioned
 * by its fraction of the day; sessions with known content use the primary
 * chart color, unnamed ones use the secondary color.
 */
@Composable
fun DayBand(
    sessions: List<ContentSession>,
    overlapOf: (ContentSession) -> Pair<Float, Float>?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val extendedColors = TraceThemeExtended
    val chartPrimary = extendedColors.chart.first()
    val chartSecondary = extendedColors.chart[2]

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
    ) {
        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(size.height, size.height),
        )
        sessions.forEach { session ->
            val (from, to) = overlapOf(session) ?: return@forEach
            val span = (to - from) * size.width
            if (span <= 0f) return@forEach
            drawRoundRect(
                color = if (session.content?.title != null) chartPrimary else chartSecondary,
                topLeft = Offset(from * size.width, 0f),
                size = Size(span, size.height),
                cornerRadius = CornerRadius(size.height / 2, size.height / 2),
            )
        }
    }
}
