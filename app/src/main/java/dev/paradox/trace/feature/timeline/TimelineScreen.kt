package dev.paradox.trace.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.paradox.trace.R
import dev.paradox.trace.TraceApplication
import dev.paradox.trace.ui.components.DayBand
import dev.paradox.trace.ui.components.SessionRow
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DAY_LABEL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE")

@Composable
fun TimelineScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as TraceApplication
    val viewModel: TimelineViewModel = viewModel(
        factory = TimelineViewModel.factory(application.sessionRepository),
    )
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val zone: ZoneId = ZoneId.systemDefault()

    var dayOffset by rememberSaveable { mutableIntStateOf(0) }
    val now = remember(sessions) { java.time.ZonedDateTime.now(zone) }
    val date = remember(dayOffset, now) { TimelineMath.dateForOffset(now, dayOffset) }
    val window = remember(date, zone) { TimelineMath.dayWindowMs(date, zone) }
    val daySessions = remember(sessions, window) {
        sessions.filter { it.interval.startInclusiveMs < window.second && it.interval.endExclusiveMs > window.first }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.tab_timeline),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { dayOffset = TimelineMath.clampOffset(dayOffset - 1) },
                enabled = dayOffset > -TimelineMath.MAX_PAST_DAYS,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.timeline_prev_day))
            }
            Text(
                text = DAY_LABEL_FORMAT.format(date),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(
                onClick = { dayOffset = TimelineMath.clampOffset(dayOffset + 1) },
                enabled = dayOffset < 0,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.timeline_next_day))
            }
        }

        if (daySessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.timeline_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            DayBand(
                sessions = daySessions,
                overlapOf = { session ->
                    TimelineMath.overlapWithinWindow(
                        session.interval.startInclusiveMs,
                        session.interval.endExclusiveMs,
                        window.first,
                        window.second,
                    )
                },
            )
            AxisLabels()
            Text(
                text = stringResource(R.string.stats_method_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                daySessions.forEach { session ->
                    SessionRow(
                        session = session,
                        zone = zone,
                        onDelete = { viewModel.deleteSession(session.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AxisLabels() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("0:00", "6:00", "12:00", "18:00", "24:00").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
