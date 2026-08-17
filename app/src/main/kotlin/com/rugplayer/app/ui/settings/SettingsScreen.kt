package com.rugplayer.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rugplayer.app.AppGraph
import com.rugplayer.app.R
import com.rugplayer.app.data.prefs.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(graph: AppGraph, onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(graph.settingsRepository) }
        },
    )
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SectionHeader(stringResource(R.string.settings_theme)) }
            items(ThemeMode.entries.toList()) { mode ->
                ListItem(
                    headlineContent = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    trailingContent = {
                        androidx.compose.material3.RadioButton(
                            selected = settings.theme == mode,
                            onClick = { viewModel.setTheme(mode) },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTheme(mode) },
                )
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_gestures)) }
            item {
                SwitchRow(
                    title = "Volume & brightness swipe",
                    checked = settings.gestureVolumeBrightness,
                    onCheckedChange = viewModel::setGestureVolumeBrightness,
                )
            }
            item {
                SwitchRow(
                    title = "Swipe to seek",
                    checked = settings.gestureSeek,
                    onCheckedChange = viewModel::setGestureSeek,
                )
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_resume)) }
            item {
                SwitchRow(
                    title = "Resume where you left off",
                    checked = settings.rememberPosition,
                    onCheckedChange = viewModel::setRememberPosition,
                )
            }

            item { HorizontalDivider() }
            item { SectionHeader("Background playback") }
            item {
                SwitchRow(
                    title = "Keep playing audio when backgrounded",
                    checked = settings.backgroundPlayback,
                    onCheckedChange = viewModel::setBackgroundPlayback,
                )
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_privacy)) }
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.PrivacyTip, contentDescription = null) },
                    headlineContent = { Text("No ads. No tracking. No account.") },
                    supportingContent = { Text(stringResource(R.string.settings_privacy_body)) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
    )
}
