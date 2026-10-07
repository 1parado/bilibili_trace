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
import androidx.compose.material3.LinearProgressIndicator
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
import dev.paradox.trace.domain.repository.DailyGoal
import dev.paradox.trace.ui.components.SessionRow
import dev.paradox.trace.ui.components.StatCard
import dev.paradox.trace.ui.theme.TraceThemeExtended
import java.time.ZoneId

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val goalMinutes by viewModel.dailyGoalMinutes.collectAsStateWithLifecycle()
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

        val remaining = DailyGoal.remainingMinutes(goalMinutes, stats.todayMs)
        if (remaining != null) {
            val fraction = if (goalMinutes > 0) {
                (goalMinutes - remaining).toFloat() / goalMinutes.toFloat()
            } else 0f
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = TraceThemeExtended.success,
            )
            Text(
                text = if (remaining > 0) {
                    stringResource(R.string.today_goal_remaining, remaining)
                } else {
                    stringResource(R.string.today_goal_reached)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            val topContent = remember(sessions) { SessionStats.topContent(sessions) }
            if (topContent.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.today_top_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    topContent.forEach { aggregate ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = aggregate.label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.today_top_count, aggregate.count),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = DurationFormatter.format(aggregate.totalMs),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
            val topCreators = remember(sessions) { SessionStats.topCreators(sessions) }
            if (topCreators.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.today_creators_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    topCreators.forEach { creator ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = creator.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.today_top_count, creator.count),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = DurationFormatter.format(creator.totalMs),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.recent_sessions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
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
