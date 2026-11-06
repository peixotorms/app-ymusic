package io.ymusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ymusic.settings.AppSettings
import io.ymusic.settings.flow
import io.ymusic.settings.put
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.a11y_duration_minutes
import io.ymusic.ui.resources.a11y_duration_seconds
import io.ymusic.ui.resources.settings_audio_buffer_ahead
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AudioBufferRow(graph: AppGraph) {
    val selected by remember(graph) { graph.settings.flow(AppSettings.audioBufferAheadSeconds) }.collectAsState()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { expanded = true }
            .padding(horizontal = DesignSpacing.SettingsRowHorizontal, vertical = DesignSpacing.SettingsRowVertical),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.settings_audio_buffer_ahead),
            modifier = Modifier.weight(1f),
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.Ink),
        )
        Box {
            Text(audioBufferLabel(selected.coerceIn(30, 120)), style = TextStyle(fontSize = 13.sp, color = DesignColors.InkSecondary))
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(30, 60, 120).forEach { seconds ->
                    DropdownMenuItem(
                        text = { Text(audioBufferLabel(seconds)) },
                        onClick = {
                            expanded = false
                            scope.launch { graph.settings.put(AppSettings.audioBufferAheadSeconds, seconds) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun audioBufferLabel(seconds: Int): String =
    if (seconds >= 60) pluralStringResource(Res.plurals.a11y_duration_minutes, seconds / 60, seconds / 60)
    else pluralStringResource(Res.plurals.a11y_duration_seconds, seconds, seconds)
