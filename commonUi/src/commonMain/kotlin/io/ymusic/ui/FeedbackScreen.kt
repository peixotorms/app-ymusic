package io.ymusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ymusic.settings.anonymousDeviceId
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.feedback_app
import io.ymusic.ui.resources.feedback_copied
import io.ymusic.ui.resources.feedback_copy
import io.ymusic.ui.resources.feedback_device_id
import io.ymusic.ui.resources.feedback_engine
import io.ymusic.ui.resources.feedback_github_hint
import io.ymusic.ui.resources.feedback_github_title
import io.ymusic.ui.resources.feedback_info_title
import io.ymusic.ui.resources.feedback_language
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Local diagnostic information with explicit Copy and GitHub actions. */
@Composable
internal fun FeedbackScreen(graph: AppGraph, onDismiss: () -> Unit) {
    val deviceId by produceState("", graph) { value = graph.settings.anonymousDeviceId() }
    val config by remember(graph) { graph.remoteConfig ?: emptyFlow() }.collectAsState(initial = null)
    val links = AppLinks(config)
    val bundles = graph.runningBundles
    bundles?.engineFlow?.collectAsState()
    bundles?.playerFlow?.collectAsState()
    val info = FeedbackInfo(
        deviceId = deviceId,
        app = listOfNotNull(
            "YMusic" + (graph.appVersionName?.let { name -> " $name" + (graph.appVersionCode?.let { " ($it)" } ?: "") } ?: ""),
            graph.platformInfo,
        ).joinToString(" · "),
        engine = bundles?.let { "engine ${it.reportedEngine ?: "-"} · player ${it.reportedPlayer ?: "-"}" },
        language = Locale.current.toLanguageTag(),
        email = links.feedbackEmail,
        issueUrl = links.feedbackIssue,
    )
    val currentInfo by rememberUpdatedState(info)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copy = { scope.launch { clipboard.setClipEntry(plainTextClipEntry(currentInfo.text)) } }
    val scrollState = rememberScrollState()
    TrackScroll { scrollState.canScrollBackward }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val margin = gridMetricsFor(maxWidth).margin
        val centerVertically = maxWidth >= 840.dp && maxHeight >= 600.dp
        val viewportHeight = maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = LocalMiniPlayerInset.current),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = DesignSpacing.FormMaxWidth)
                    .fillMaxWidth()
                    .then(if (centerVertically) Modifier.heightIn(min = viewportHeight) else Modifier)
                    .padding(horizontal = margin, vertical = DesignSpacing.Space16),
                verticalArrangement = if (centerVertically) {
                    Arrangement.spacedBy(DesignSpacing.Space16, Alignment.CenterVertically)
                } else {
                    Arrangement.spacedBy(DesignSpacing.Space16)
                },
            ) {
                var copiedByTap by remember { mutableStateOf(false) }
                LaunchedEffect(copiedByTap) {
                    if (copiedByTap) {
                        delay(COPIED_LABEL_MILLIS)
                        copiedByTap = false
                    }
                }
                InfoCard(info, copied = copiedByTap, onCopy = {
                    copy()
                    copiedByTap = true
                })
                val uriHandler = LocalUriHandler.current
                ContactRow(
                    icon = Icons.Outlined.Public,
                    title = stringResource(Res.string.feedback_github_title),
                    hint = stringResource(Res.string.feedback_github_hint),
                    onClick = {
                        uriHandler.openUri(currentInfo.githubIssueUrl())
                    },
                )
            }
        }
    }
}

@Composable
private fun InfoCard(info: FeedbackInfo, copied: Boolean, onCopy: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(DesignColors.SettingsCard, RoundedCornerShape(DesignSpacing.SettingsCardCorner))
            .padding(start = DesignSpacing.Space16, end = DesignSpacing.Space4, top = DesignSpacing.Space4, bottom = DesignSpacing.Space16),
        verticalArrangement = Arrangement.spacedBy(DesignSpacing.Space8),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.feedback_info_title).uppercase(),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DesignColors.InkMuted),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCopy) {
                Icon(
                    if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = DesignColors.Accent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(if (copied) Res.string.feedback_copied else Res.string.feedback_copy),
                    color = DesignColors.Accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = DesignSpacing.Space8),
                )
            }
        }
        Column(Modifier.padding(end = DesignSpacing.Space16), verticalArrangement = Arrangement.spacedBy(DesignSpacing.Space8)) {
            if (info.deviceId.isNotEmpty()) InfoRow(stringResource(Res.string.feedback_device_id), info.deviceId, monospace = true)
            InfoRow(stringResource(Res.string.feedback_app), info.app)
            info.engine?.let { InfoRow(stringResource(Res.string.feedback_engine), it) }
            InfoRow(stringResource(Res.string.feedback_language), info.language)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, monospace: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(DesignSpacing.Space16)) {
        Text(label, style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, color = DesignColors.InkSoft), modifier = Modifier.width(InfoLabelWidth))
        Text(
            value,
            style = TextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = DesignColors.InkStrong,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ContactRow(icon: ImageVector, title: String, hint: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(DesignSpacing.SettingsCardCorner)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, DesignColors.OutlineSoft, shape)
            .background(DesignColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = DesignSpacing.Space16, vertical = DesignSpacing.DialogRowVertical),
        horizontalArrangement = Arrangement.spacedBy(DesignSpacing.Space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(DesignColors.Accent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = DesignColors.Accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignSpacing.Space2)) {
            Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong))
            Text(hint, style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, color = DesignColors.InkSoft))
        }
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = DesignColors.InkSoft, modifier = Modifier.size(18.dp))
    }
}

private val InfoLabelWidth = 96.dp

private const val COPIED_LABEL_MILLIS = 2_000L
