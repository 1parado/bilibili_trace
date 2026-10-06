package dev.paradox.trace.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.paradox.trace.R
import dev.paradox.trace.TraceApplication
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as TraceApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            userPreferences = application.userPreferencesRepository,
            sessionRepository = application.sessionRepository,
        ),
    )

    val goalMinutes by viewModel.dailyGoalMinutes.collectAsStateWithLifecycle()
    var goalInput by rememberSaveable { mutableStateOf("") }
    var goalMessage by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val goalSavedText = stringResource(R.string.settings_goal_saved)
    val scope = rememberCoroutineScope()

    var pendingExport by remember { mutableStateOf<Pair<String, String>?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        val content = pendingExport?.second
        pendingExport = null
        if (uri != null && content != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            }
        }
    }
    fun launchExport(fileName: String, content: String) {
        pendingExport = fileName to content
        exportLauncher.launch(fileName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_goal_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_goal_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        label = { Text(stringResource(R.string.settings_goal_input_label)) },
                        supportingText = {
                            Text(stringResource(R.string.settings_goal_current, goalMinutes))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            val parsed = goalInput.trim().toIntOrNull()
                            if (parsed == null || parsed !in 0..1440) {
                                goalMessage = null
                            } else {
                                viewModel.setDailyGoalMinutes(parsed)
                                goalInput = ""
                                goalMessage = goalSavedText
                            }
                        },
                        enabled = goalInput.trim().toIntOrNull() in 0..1440,
                    ) {
                        Text(stringResource(R.string.record_save))
                    }
                }
                if (goalMessage != null) {
                    Text(
                        text = goalMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_data_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_data_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                launchExport(
                                    "trace-export.csv",
                                    viewModel.buildCsv(),
                                )
                            }
                        },
                    ) {
                        Text(stringResource(R.string.settings_export_csv))
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                launchExport(
                                    "trace-export.json",
                                    viewModel.buildJson(),
                                )
                            }
                        },
                    ) {
                        Text(stringResource(R.string.settings_export_json))
                    }
                }
                Button(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.settings_delete_all))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteAllData { }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_delete_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.record_cancel))
                }
            },
        )
    }
}
