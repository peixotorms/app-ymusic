package io.ymusic.android.cast

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.cast_android
import io.ymusic.ui.resources.cast_chromecast
import io.ymusic.ui.resources.cast_configure
import io.ymusic.ui.resources.cast_connecting
import io.ymusic.ui.resources.cast_discovery_unavailable
import io.ymusic.ui.resources.cast_hide_device
import io.ymusic.ui.resources.cast_hide_hidden
import io.ymusic.ui.resources.cast_info
import io.ymusic.ui.resources.cast_info_text
import io.ymusic.ui.resources.cast_local_device
import io.ymusic.ui.resources.cast_mute
import io.ymusic.ui.resources.cast_percent
import io.ymusic.ui.resources.cast_play_on
import io.ymusic.ui.resources.cast_restore_defaults
import io.ymusic.ui.resources.cast_searching
import io.ymusic.ui.resources.cast_select
import io.ymusic.ui.resources.cast_show_device
import io.ymusic.ui.resources.cast_show_hidden
import io.ymusic.ui.resources.cast_sort
import io.ymusic.ui.resources.cast_sort_ascending
import io.ymusic.ui.resources.cast_sort_descending
import io.ymusic.ui.resources.cast_system_steps
import io.ymusic.ui.resources.cast_unmute
import io.ymusic.ui.resources.cast_volume_down
import io.ymusic.ui.resources.cast_volume_steps
import io.ymusic.ui.resources.cast_volume_steps_hint
import io.ymusic.ui.resources.cast_volume_up
import io.ymusic.ui.resources.dialog_ok
import io.ymusic.ui.resources.sound_volume
import org.jetbrains.compose.resources.stringResource
import java.text.Collator
import kotlin.math.roundToInt

internal enum class CastOutputKind { Local, Television, Speaker, Group }

internal data class CastOutputDevice(
    val id: String,
    val name: String,
    val kind: CastOutputKind,
    val selected: Boolean,
    val enabled: Boolean = true,
    val connecting: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CastOutputSheet(
    devices: List<CastOutputDevice>,
    preferences: CastDevicePreferences,
    volume: CastVolumeControl,
    discoveryFailed: Boolean,
    onChoose: (String) -> Unit,
    onVolumeKey: (KeyEvent) -> Boolean,
    onDismiss: () -> Unit,
) {
    val revision by preferences.revision.collectAsState()
    val currentVolume by volume.state.collectAsState()
    var showHidden by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var configure by remember { mutableStateOf<CastOutputDevice?>(null) }
    val visible = remember(devices, revision, showHidden) {
        val collator = Collator.getInstance()
        val comparator = Comparator<CastOutputDevice> { a, b -> collator.compare(a.name, b.name) }
        devices.filter { it.kind == CastOutputKind.Local } + devices
            .filter { it.kind != CastOutputKind.Local && (showHidden || it.selected || !preferences.isHidden(it.id)) }
            .sortedWith(if (preferences.sortDescending) comparator.reversed() else comparator)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
    ) {
        Column(Modifier.onPreviewKeyEvent { onVolumeKey(it.nativeKeyEvent) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Icon(Icons.Outlined.Cast, null, Modifier.padding(horizontal = 12.dp))
                Text(stringResource(Res.string.cast_play_on), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showSort = true }) { Icon(Icons.Outlined.SortByAlpha, stringResource(Res.string.cast_sort)) }
                    DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                        DropdownMenuItem(text = { Text(stringResource(Res.string.cast_sort_ascending)) }, onClick = { preferences.sortDescending = false; showSort = false })
                        DropdownMenuItem(text = { Text(stringResource(Res.string.cast_sort_descending)) }, onClick = { preferences.sortDescending = true; showSort = false })
                    }
                }
                IconButton(onClick = { showInfo = true }) { Icon(Icons.Outlined.Info, stringResource(Res.string.cast_info)) }
            }
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visible.forEach { device ->
                    val hidden = preferences.isHidden(device.id)
                    CastDeviceCard(
                        device = device,
                        hidden = hidden,
                        onChoose = { onChoose(device.id) },
                        onConfigure = { configure = device },
                        onHide = { preferences.setHidden(device.id, !hidden) },
                    )
                }
            }
            if (preferences.hasHiddenDevices) TextButton(onClick = { showHidden = !showHidden }) {
                Icon(if (showHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, Modifier.padding(end = 8.dp).size(20.dp))
                Text(stringResource(if (showHidden) Res.string.cast_hide_hidden else Res.string.cast_show_hidden))
            }
            currentVolume?.let { CastVolumeControls(it, volume) }
            if (devices.none { it.kind != CastOutputKind.Local }) Text(
                stringResource(if (discoveryFailed) Res.string.cast_discovery_unavailable else Res.string.cast_searching),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
    configure?.let { device ->
        CastDeviceSettings(device, preferences, onVolumeKey) { configure = null }
    }
    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        icon = { Icon(Icons.Outlined.Cast, null) },
        title = { Text(stringResource(Res.string.cast_play_on)) },
        text = { Text(stringResource(Res.string.cast_info_text)) },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(Res.string.dialog_ok)) } },
    )
}

@Composable
private fun CastDeviceCard(device: CastOutputDevice, hidden: Boolean, onChoose: () -> Unit, onConfigure: () -> Unit, onHide: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onChoose,
        enabled = device.enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (device.selected) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (device.selected) colors.onPrimaryContainer else colors.onSurface,
        modifier = Modifier.fillMaxWidth().semantics { selected = device.selected },
    ) {
        Row(Modifier.heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (device.kind) {
                    CastOutputKind.Local -> Icons.Outlined.GraphicEq
                    CastOutputKind.Television -> Icons.Outlined.Tv
                    CastOutputKind.Speaker, CastOutputKind.Group -> Icons.Outlined.Speaker
                },
                null,
                Modifier.padding(end = 16.dp).size(24.dp),
            )
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(if (device.kind == CastOutputKind.Local) stringResource(Res.string.cast_local_device) else device.name, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(when {
                            device.connecting -> Res.string.cast_connecting
                            device.kind == CastOutputKind.Local -> Res.string.cast_android
                            else -> Res.string.cast_chromecast
                        }),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (device.selected) colors.onPrimaryContainer.copy(alpha = .75f) else colors.onSurfaceVariant,
                    )
                    if (device.kind != CastOutputKind.Local) Icon(Icons.Outlined.Cast, null, Modifier.size(12.dp))
                }
            }
            val name = if (device.kind == CastOutputKind.Local) stringResource(Res.string.cast_local_device) else device.name
            IconButton(onClick = onConfigure) { Icon(Icons.Outlined.Settings, stringResource(Res.string.cast_configure, name)) }
            if (device.kind != CastOutputKind.Local) IconButton(onClick = onHide, enabled = !device.selected && !device.connecting) {
                Icon(if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, stringResource(if (hidden) Res.string.cast_show_device else Res.string.cast_hide_device, name))
            }
        }
    }
}

@Composable
private fun CastVolumeControls(state: CastVolumeState, control: CastVolumeControl) {
    var dragging by remember(state.deviceId) { mutableStateOf(false) }
    var percent by remember(state.deviceId) { mutableFloatStateOf(state.percent.toFloat()) }
    LaunchedEffect(state.percent) { if (!dragging) percent = state.percent.toFloat() }
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { control.setMuted(!state.muted) }, enabled = state.available) {
                Icon(if (state.muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeMute, stringResource(if (state.muted) Res.string.cast_unmute else Res.string.cast_mute))
            }
            Text(stringResource(Res.string.sound_volume, percent.roundToInt()), style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { control.adjustVolume(-1) }, enabled = state.available) { Icon(Icons.AutoMirrored.Outlined.VolumeDown, stringResource(Res.string.cast_volume_down)) }
            Slider(
                value = percent,
                onValueChange = { dragging = true; percent = it },
                onValueChangeFinished = { dragging = false; control.setVolume(percent.roundToInt()) },
                valueRange = 0f..100f,
                enabled = state.available,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { control.adjustVolume(1) }, enabled = state.available) { Icon(Icons.AutoMirrored.Outlined.VolumeUp, stringResource(Res.string.cast_volume_up)) }
        }
    }
}

@Composable
private fun CastDeviceSettings(device: CastOutputDevice, preferences: CastDevicePreferences, onVolumeKey: (KeyEvent) -> Boolean, onDismiss: () -> Unit) {
    val revision by preferences.revision.collectAsState()
    val step = remember(device.id, revision) { preferences.volumeStep(device.id) }
    var selecting by remember { mutableStateOf(false) }
    val stepLabel = if (step == 0) stringResource(Res.string.cast_system_steps) else stringResource(Res.string.cast_percent, step)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.onPreviewKeyEvent { onVolumeKey(it.nativeKeyEvent) },
        icon = { Icon(Icons.Outlined.Settings, null) },
        title = { Text(if (device.kind == CastOutputKind.Local) stringResource(Res.string.cast_local_device) else device.name) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.cast_volume_steps, stepLabel), modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { selecting = true }) { Text(stringResource(Res.string.cast_select)) }
                        DropdownMenu(expanded = selecting, onDismissRequest = { selecting = false }) {
                            val choices = (if (device.kind == CastOutputKind.Local) listOf(0) else emptyList()) + listOf(1, 2, 3, 5, 10, 15, 20)
                            choices.forEach { choice ->
                                DropdownMenuItem(
                                    text = { Text(if (choice == 0) stringResource(Res.string.cast_system_steps) else stringResource(Res.string.cast_percent, choice)) },
                                    onClick = { preferences.setVolumeStep(device.id, choice); selecting = false },
                                )
                            }
                        }
                    }
                }
                Text(stringResource(Res.string.cast_volume_steps_hint), style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = { TextButton(onClick = { preferences.restore(device.id) }) { Text(stringResource(Res.string.cast_restore_defaults)) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_ok)) } },
    )
}
