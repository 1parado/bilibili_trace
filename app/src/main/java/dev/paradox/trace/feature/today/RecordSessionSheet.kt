package dev.paradox.trace.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.paradox.trace.R
import dev.paradox.trace.core.time.TimeTextParser
import dev.paradox.trace.domain.model.BilibiliContentPreview
import dev.paradox.trace.domain.repository.ManualSessionCommand
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * Form for logging one observed viewing session. Inputs are wall-clock times
 * for today; validation errors are shown inline, never silently accepted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSessionSheet(
    initialContentId: String,
    fetchMetadata: suspend (String) -> Result<BilibiliContentPreview>,
    onDismiss: () -> Unit,
    onConfirm: (ManualSessionCommand) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var contentId by remember { mutableStateOf(initialContentId) }
    var creator by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("20:00") }
    var endTime by remember { mutableStateOf("21:00") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var fetching by remember { mutableStateOf(false) }
    var fetchedByApi by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val errorInvalidTime = stringResource(R.string.record_error_time)
    val errorFutureEnd = stringResource(R.string.record_error_future)
    val errorFetch = stringResource(R.string.record_fetch_failed)

    fun performFetch(input: String) {
        scope.launch {
            fetching = true
            errorText = null
            val result = fetchMetadata(input)
            fetching = false
            result.onSuccess { preview ->
                contentId = preview.bvid
                title = preview.title
                creator = preview.creatorName
                fetchedByApi = true
                suggestEndTime(preview.durationSec)
            }.onFailure {
                errorText = errorFetch
            }
        }
    }

    /** Prefills the end time from the video duration when a valid start exists. */
    fun suggestEndTime(durationSec: Long) {
        val startMinutes = TimeTextParser.parseMinutesOfDay(startTime) ?: return
        val endTotal = startMinutes + (durationSec / 60L).toInt()
        if (endTotal in 1 until 24 * 60) {
            endTime = String.format("%02d:%02d", endTotal / 60, endTotal % 60)
        }
    }

    LaunchedEffect(initialContentId) {
        if (initialContentId.isNotBlank()) performFetch(initialContentId)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.record_title),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.record_hint_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = contentId,
                onValueChange = {
                    contentId = it
                    fetchedByApi = false
                },
                label = { Text(stringResource(R.string.record_hint_bvid)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(
                        onClick = { performFetch(contentId) },
                        enabled = contentId.isNotBlank() && !fetching,
                    ) {
                        Text(
                            text = if (fetching) {
                                stringResource(R.string.record_fetching)
                            } else {
                                stringResource(R.string.record_fetch_metadata)
                            },
                        )
                    }
                },
            )
            OutlinedTextField(
                value = creator,
                onValueChange = { creator = it },
                label = { Text(stringResource(R.string.record_hint_creator)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text(stringResource(R.string.record_start)) },
                    supportingText = { Text(stringResource(R.string.record_time_format_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text(stringResource(R.string.record_end)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                )
            }

            if (errorText != null) {
                Text(
                    text = errorText.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.record_cancel))
                }
                Button(
                    onClick = {
                        val command = buildCommand(
                            title = title.trim(),
                            contentId = contentId.trim(),
                            creator = creator.trim(),
                            startTimeText = startTime,
                            endTimeText = endTime,
                        )
                        val validationError = validate(command)
                        if (validationError != null) {
                            errorText = when (validationError) {
                                "future_end" -> errorFutureEnd
                                else -> errorInvalidTime
                            }
                        } else {
                            errorText = null
                            onConfirm(
                                command.copy(
                                    metadataSource = if (fetchedByApi) "PLATFORM_API" else null,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.record_save))
                }
            }
        }
    }
}

internal fun buildCommand(
    title: String,
    contentId: String,
    creator: String,
    startTimeText: String,
    endTimeText: String,
): ManualSessionCommand {
    val zone: ZoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val startMinutes = TimeTextParser.parseMinutesOfDay(startTimeText)
    val endMinutes = TimeTextParser.parseMinutesOfDay(endTimeText)
    val startMs = startMinutes?.let { minutes ->
        today.atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(zone).toInstant().toEpochMilli()
    } ?: Long.MIN_VALUE
    val endMs = endMinutes?.let { minutes ->
        today.atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(zone).toInstant().toEpochMilli()
    } ?: Long.MAX_VALUE
    return ManualSessionCommand(
        platform = "bilibili",
        title = title.ifEmpty { null },
        platformContentId = contentId.ifEmpty { null },
        creatorName = creator.ifEmpty { null },
        startedAtMs = startMs,
        endedAtMs = endMs,
    )
}

internal fun validate(command: ManualSessionCommand): String? {
    if (command.startedAtMs == Long.MIN_VALUE || command.endedAtMs == Long.MAX_VALUE) {
        return "invalid_time"
    }
    if (command.startedAtMs >= command.endedAtMs) {
        return "invalid_order"
    }
    if (command.endedAtMs > System.currentTimeMillis() + 60_000L) {
        return "future_end"
    }
    return null
}
