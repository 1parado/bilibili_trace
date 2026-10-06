package dev.paradox.trace.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.paradox.trace.R
import dev.paradox.trace.TraceApplication
import dev.paradox.trace.feature.today.TodayScreen
import dev.paradox.trace.feature.today.TodayViewModel
import dev.paradox.trace.feature.today.RecordSessionSheet
import dev.paradox.trace.feature.settings.SettingsScreen
import dev.paradox.trace.feature.timeline.TimelineScreen
import kotlinx.coroutines.launch

private data class TabSpec(val icon: ImageVector, val labelRes: Int)

@Composable
fun TraceApp() {
    val application = LocalContext.current.applicationContext as TraceApplication
    val viewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.factory(
            sessionRepository = application.sessionRepository,
            userPreferencesRepository = application.userPreferencesRepository,
        ),
    )

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showRecordSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val saveFailedText = stringResource(R.string.record_save_failed)

    val tabs = listOf(
        TabSpec(Icons.Filled.Home, R.string.tab_today),
        TabSpec(Icons.Filled.DateRange, R.string.tab_timeline),
        TabSpec(Icons.Filled.Settings, R.string.tab_settings),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                0 -> TodayScreen(
                    viewModel = viewModel,
                    onRecordClick = { showRecordSheet = true },
                )
                1 -> TimelineScreen()
                else -> SettingsScreen()
            }
        }
    }

    if (showRecordSheet) {
        RecordSessionSheet(
            onDismiss = { showRecordSheet = false },
            onConfirm = { command ->
                showRecordSheet = false
                viewModel.addSession(command) { result ->
                    if (result.isFailure) {
                        scope.launch { snackbarHostState.showSnackbar(saveFailedText) }
                    }
                }
            },
        )
    }
}
