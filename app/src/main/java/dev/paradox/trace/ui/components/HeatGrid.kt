package dev.paradox.trace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
private val CELL_SIZE = 16.dp

/**
 * Calendar heat grid: grid[week][weekday] holding level 0..4, null = no cell.
 * Levels blend the success color into the card color for a calm gradient.
 */
@Composable
fun HeatGrid(
    grid: List<List<Int?>>,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val success = TraceThemeExtended.success
    val card = MaterialTheme.colorScheme.surfaceContainer

    fun levelColor(level: Int?): Color = when (level) {
        null -> Color.Transparent
        0 -> track
        else -> lerpColor(card, success, 0.2f + 0.2f * level)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val columnCount = grid.firstOrNull()?.size ?: 0
        for (dayIndex in 0 until 7) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (weekIndex in 0 until columnCount) {
                    val level = grid.getOrNull(weekIndex)?.getOrNull(dayIndex)
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(CELL_SIZE)
                            .clip(RoundedCornerShape(4.dp))
                            .background(levelColor(level)),
                    )
                }
            }
        }
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = 1f,
)
