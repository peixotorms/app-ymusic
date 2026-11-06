package io.ymusic.android.cast

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import io.ymusic.download.SabrSpecResolver
import io.ymusic.player.LocalPlayer
import io.ymusic.player.YtEngine
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.cast_discovery_unavailable
import io.ymusic.ui.resources.cast_play_on
import io.ymusic.ui.resources.cast_searching
import io.ymusic.ui.resources.cast_this_device
import kmpcommon.log.Logger
import kmphttp.KmpHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource

internal class CastController(
    private val context: Context,
    private val scope: CoroutineScope,
    resolver: SabrSpecResolver,
    client: KmpHttpClient,
    userAgent: String,
    private val logger: Logger,
) {
    private val preferences = CastDevicePreferences(context.getSharedPreferences("cast_devices", Context.MODE_PRIVATE))
    val volume = CastVolumeController(context, scope, preferences, logger)
    private val connected = MutableStateFlow(false)
    private val routeRevision = MutableStateFlow(0L)
    private val remote = CastAudioEngine(scope, CastAudioSource(context, resolver, client, userAgent, logger), logger)
    private var castContext: CastContext? = null
    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = attach(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = attach(session)
        override fun onSessionEnded(session: CastSession, error: Int) = attach(null)
        override fun onSessionSuspended(session: CastSession, reason: Int) {
            remote.attach(null)
            volume.suspendSession()
        }
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionStartFailed(session: CastSession, error: Int) = attach(null)
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumeFailed(session: CastSession, error: Int) = attach(null)
    }
    private val routesCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = changed()
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = changed()
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = changed()
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo) = changed()
        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo) = changed()
    }

    private fun changed() { routeRevision.value++ }

    private fun attach(session: CastSession?) {
        volume.attach(session)
        remote.attach(session)
        connected.value = session != null
        changed()
    }

    private fun initialize(): CastContext {
        castContext?.let { return it }
        return CastContext.getSharedInstance(context).also { cast ->
            cast.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
            cast.sessionManager.currentCastSession?.takeIf { it.isConnected }?.let(::attach)
            castContext = cast
        }
    }

    private fun playOnThisDevice() {
        castContext?.sessionManager?.endCurrentSession(true)
        MediaRouter.getInstance(context).unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED)
        attach(null)
    }

    fun route(local: YtEngine): YtEngine = CastYtEngine(local, CastRoutingEngine(local, remote, connected, scope, logger))
    fun route(local: LocalPlayer): LocalPlayer = CastLocalPlayer(local, CastRoutingEngine(local, remote, connected, scope, logger))

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Button(modifier: Modifier) {
        var show by remember { mutableStateOf(false) }
        var discoveryFailed by remember { mutableStateOf(false) }
        val casting by connected.collectAsState()
        LaunchedEffect(Unit) {
            try {
                initialize()
            } catch (error: Exception) {
                logger.log(Logger.Level.WARN, "Cast discovery unavailable", error)
                discoveryFailed = true
            }
        }
        IconButton(onClick = {
            try {
                initialize()
                discoveryFailed = false
            } catch (error: Exception) {
                logger.log(Logger.Level.WARN, "Cast discovery unavailable", error)
                discoveryFailed = true
            }
            show = true
        }, modifier = modifier) {
            Icon(if (casting) Icons.Outlined.CastConnected else Icons.Outlined.Cast, stringResource(Res.string.cast_play_on))
        }
        if (!show) return
        val router = remember { MediaRouter.getInstance(context) }
        val cast = castContext
        val selector = cast?.mergedSelector
        DisposableEffect(selector) {
            if (selector != null) router.addCallback(selector, routesCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
            changed()
            onDispose { router.removeCallback(routesCallback) }
        }
        val revision by routeRevision.collectAsState()
        val routes = remember(revision, selector) {
            router.routes.filter { !it.isDefaultOrBluetooth && selector != null && it.matchesSelector(selector) }
        }
        ModalBottomSheet(onDismissRequest = { show = false }) {
            Text(stringResource(Res.string.cast_play_on), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            ListItem(
                headlineContent = { Text(stringResource(Res.string.cast_this_device)) },
                leadingContent = { Icon(Icons.Outlined.PhoneAndroid, null) },
                trailingContent = { RadioButton(selected = !casting, onClick = null) },
                modifier = Modifier.clickable { playOnThisDevice(); show = false },
            )
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                routes.forEach { route ->
                    ListItem(
                        headlineContent = { Text(route.name) },
                        leadingContent = { Icon(Icons.Outlined.Cast, null) },
                        trailingContent = { RadioButton(selected = casting && route.isSelected, onClick = null) },
                        modifier = Modifier.clickable(enabled = route.isEnabled) { route.select(); show = false },
                    )
                }
            }
            if (routes.isEmpty()) Text(
                stringResource(if (discoveryFailed) Res.string.cast_discovery_unavailable else Res.string.cast_searching),
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
