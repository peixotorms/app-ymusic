package io.ymusic.android.cast

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import io.ymusic.download.SabrSpecResolver
import io.ymusic.player.LocalPlayer
import io.ymusic.player.YtEngine
import kmpcommon.log.Logger
import kmphttp.KmpHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

internal class CastController(
    private val context: Context,
    private val scope: CoroutineScope,
    resolver: SabrSpecResolver,
    client: KmpHttpClient,
    userAgent: String,
    private val logger: Logger,
) {
    private val connected = MutableStateFlow(false)
    private val remote = CastAudioEngine(scope, CastAudioSource(context, resolver, client, userAgent, logger), logger)
    private var initialized = false
    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = attach(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = attach(session)
        override fun onSessionEnded(session: CastSession, error: Int) = attach(null)
        override fun onSessionSuspended(session: CastSession, reason: Int) = remote.attach(null)
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionStartFailed(session: CastSession, error: Int) = attach(null)
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumeFailed(session: CastSession, error: Int) = attach(null)
    }

    private fun attach(session: CastSession?) {
        remote.attach(session)
        connected.value = session != null
    }

    private fun initialize() {
        if (initialized) return
        val cast = CastContext.getSharedInstance(context)
        cast.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        cast.sessionManager.currentCastSession?.takeIf { it.isConnected }?.let(::attach)
        initialized = true
    }

    fun route(local: YtEngine): YtEngine = CastYtEngine(local, CastRoutingEngine(local, remote, connected, scope, logger))
    fun route(local: LocalPlayer): LocalPlayer = CastLocalPlayer(local, CastRoutingEngine(local, remote, connected, scope, logger))

    @Composable
    fun Button(modifier: Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { viewContext ->
                MediaRouteButton(ContextThemeWrapper(viewContext, androidx.appcompat.R.style.Theme_AppCompat_DayNight)).apply {
                    try {
                        initialize()
                        CastButtonFactory.setUpMediaRouteButton(viewContext, this)
                    } catch (error: Exception) {
                        logger.log(Logger.Level.WARN, "Cast discovery unavailable", error)
                        isEnabled = false
                    }
                }
            },
        )
    }
}
