package io.ymusic.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalActionIndication = staticCompositionLocalOf { true }

val LocalActionGlyphOffset = staticCompositionLocalOf { 0.dp }

/**
 * A small icon action: the glyph centered in a [hitSize] circular hover/press target — the standard
 * is a 24dp glyph with 8dp around it (40dp).
 */
@Composable
fun ActionIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    hitSize: Dp = 40.dp,
    tint: Color = DesignColors.InkMuted,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ActionIconBox(
        modifier,
        Modifier.a11yActionLabel(contentDescription)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = if (LocalActionIndication.current) LocalIndication.current else null, enabled = enabled, role = Role.Button, onClick = onClick),
        hitSize,
    ) {
        Icon(icon, null, Modifier.size(iconSize).offset(x = LocalActionGlyphOffset.current), tint = tint)
    }
}

@Composable
fun ActionIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    hitSize: Dp = 40.dp,
    tint: Color = DesignColors.InkMuted,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ActionIconBox(
        modifier,
        Modifier.a11yActionLabel(contentDescription)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = if (LocalActionIndication.current) LocalIndication.current else null, enabled = enabled, role = Role.Button, onClick = onClick),
        hitSize,
    ) {
        Icon(painter, null, Modifier.size(iconSize).offset(x = LocalActionGlyphOffset.current), tint = tint)
    }
}

/** An on/off [ActionIcon]. Keep [contentDescription] fixed — screen readers announce [checked] themselves. */
@Composable
fun ToggleActionIcon(
    icon: ImageVector,
    contentDescription: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    hitSize: Dp = 40.dp,
    tint: Color = DesignColors.InkMuted,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    ActionIconBox(
        modifier,
        Modifier.a11yActionLabel(contentDescription)
            .toggleable(value = checked, interactionSource = remember { MutableInteractionSource() }, indication = if (LocalActionIndication.current) LocalIndication.current else null, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        hitSize,
    ) {
        Icon(icon, null, Modifier.size(iconSize).offset(x = LocalActionGlyphOffset.current), tint = tint)
    }
}

@Composable
private fun ActionIconBox(
    modifier: Modifier,
    interaction: Modifier,
    hitSize: Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(hitSize)
            .then(if (LocalActionIndication.current) Modifier.clip(CircleShape) else Modifier)
            .then(interaction),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
