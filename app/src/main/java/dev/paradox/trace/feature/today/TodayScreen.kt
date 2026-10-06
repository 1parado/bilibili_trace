package dev.paradox.trace.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.paradox.trace.R
import dev.paradox.trace.core.time.DurationFormatter
import dev.paradox.trace.domain.analytics.SessionStats
import dev.paradox.trace.ui.components.SessionRow
import dev.paradox.trace.ui.components.StatCard
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onRecordClick: () -> Unit,
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val zone: ZoneId = ZoneId.systemDefault()

    val stats = remember(sessions) {
        SessionStats.compute(sessions, System.currentTimeMillis(), zone)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.today_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.stat_today_label),
                value = DurationFormatter.format(stats.todayMs),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stat_week_label),
                value = DurationFormatter.format(stats.last7DaysMs),
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.stats_method_note),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onRecordClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.record_button))
        }

        Text(
            text = stringResource(R.string.recent_sessions_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (sessions.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.empty_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sessions.take(10).forEach { session ->
                    SessionRow(
                        session = session,
                        zone = zone,
                        onDelete = { viewModel.deleteSession(session.id) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

internal fun formatSessionRange(
    startMs: Long,
    endMs: Long,
    zone: ZoneId,
): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(startMs), zone)) +
        " – " +
        formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(endMs), zone))
}
