package io.ymusic.android.cast

import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import io.ymusic.player.MediaEngine
import io.ymusic.player.PlayableItem
import io.ymusic.player.PlaybackConfig
import io.ymusic.player.PlaybackState
import io.ymusic.player.PlaybackStatus
import io.ymusic.player.PlayerError
import io.ymusic.player.Progress
import kmpcommon.log.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import projectcommon.error.errorCode
import java.io.File
import java.net.DatagramSocket

internal class CastAudioEngine(
    private val scope: CoroutineScope,
    private val source: CastAudioSource,
    private val log: Logger,
) : MediaEngine<PlayableItem> {
    override val status = MutableStateFlow(PlaybackStatus(PlaybackState.Idle))
    override val progress = MutableStateFlow(Progress())
    private var session: CastSession? = null
    private var server: CastAudioServer? = null
    private var audioFile: File? = null
    private var job: Job? = null
    private var wantedPlay = false
    private var pendingPosition = 0L
    private var loaded = false
    private var config = PlaybackConfig()
    private val callback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() = update()
    }

    fun attach(session: CastSession?) {
        this.session?.remoteMediaClient?.unregisterCallback(callback)
        this.session = session
        session?.remoteMediaClient?.registerCallback(callback)
    }

    override fun init() {
        if (status.value.state is PlaybackState.Uninitialized) status.value = PlaybackStatus(PlaybackState.Idle)
    }

    override fun load(source: PlayableItem, startPositionMs: Long, playWhenReady: Boolean, config: PlaybackConfig) {
        stop()
        this.config = config
        wantedPlay = playWhenReady
        pendingPosition = startPositionMs
        status.value = PlaybackStatus(PlaybackState.Prepared(playWhenReady, buffering = true, ended = false))
        progress.value = Progress(positionMs = startPositionMs, durationMs = source.durationMs ?: -1L)
        job = scope.launch {
            var failureCode = "E60072"
            try {
                val file = this@CastAudioEngine.source.newFile()
                audioFile = file
                val prepared = this@CastAudioEngine.source.prepare(source, file)
                failureCode = "E60073"
                val cast = session ?: error("Cast session ended")
                val receiverAddress = checkNotNull(cast.castDevice).inetAddress
                val host = withContext(Dispatchers.IO) {
                    DatagramSocket().use {
                        it.connect(receiverAddress, 8009)
                        it.localAddress.hostAddress ?: error("no route to Cast device")
                    }
                }
                val server = CastAudioServer(host, prepared.file, prepared.mimeType)
                this@CastAudioEngine.server = server
                withContext(Dispatchers.IO) {
                    try {
                        server.start()
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    } catch (error: Throwable) {
                        server.stop()
                        throw error
                    }
                }
                val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
                    putString(MediaMetadata.KEY_TITLE, source.displayTitle)
                    source.displaySubtitle?.let { putString(MediaMetadata.KEY_ARTIST, it) }
                }
                val media = MediaInfo.Builder(server.url)
                    .setContentType(prepared.mimeType)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(metadata)
                    .apply { source.durationMs?.let { setStreamDuration(it) } }
                    .build()
                val remote = cast.remoteMediaClient ?: error("Cast media client unavailable")
                failureCode = "E60074"
                val sentAt = System.nanoTime()
                val loadPosition = pendingPosition
                remote.load(
                    MediaLoadRequestData.Builder().setMediaInfo(media)
                        .setCurrentTime(loadPosition).setAutoplay(wantedPlay).build(),
                ).setResultCallback { result ->
                    if (this@CastAudioEngine.server !== server) return@setResultCallback
                    if (!result.status.isSuccess) {
                        fail(IllegalStateException("Cast load failed: ${result.status.statusCode}"), failureCode)
                    } else {
                        loaded = true
                        setConfig(this@CastAudioEngine.config)
                        if (pendingPosition != loadPosition) seekTo(pendingPosition)
                        if (wantedPlay) remote.play() else remote.pause()
                        update()
                    }
                }
                while (true) {
                    delay(500)
                    if (!loaded && System.nanoTime() - sentAt > 15_000_000_000L) {
                        fail(IllegalStateException("Cast load response timed out"), failureCode)
                        break
                    }
                    update()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                fail(error, failureCode)
            }
        }
    }

    private fun fail(error: Throwable, failureCode: String) {
        val savedProgress = progress.value
        val play = wantedPlay
        log.log(Logger.Level.WARN, "Cast audio failed ($failureCode) requests=${server?.requestCount ?: 0} bytes=${server?.bytesRead ?: 0}", error)
        stop()
        progress.value = savedProgress
        status.value = PlaybackStatus(PlaybackState.Error(PlayerError(error.errorCode() ?: failureCode, cause = error)), playWhenReady = play)
    }

    private fun update() {
        if (!loaded) return
        val remote = session?.remoteMediaClient ?: return
        if (remote.mediaInfo?.contentId != server?.url) return
        val mediaStatus = remote.mediaStatus ?: return
        if (mediaStatus.playerState == MediaStatus.PLAYER_STATE_IDLE && mediaStatus.idleReason == MediaStatus.IDLE_REASON_ERROR) {
            fail(IllegalStateException("Cast receiver could not play the audio"), "E60075")
            return
        }
        val ended = mediaStatus.playerState == MediaStatus.PLAYER_STATE_IDLE && mediaStatus.idleReason == MediaStatus.IDLE_REASON_FINISHED
        wantedPlay = remote.isPlaying || remote.isBuffering || ended
        status.value = PlaybackStatus(PlaybackState.Prepared(wantedPlay, remote.isBuffering, ended))
        progress.value = Progress.of(remote.approximateStreamPosition, remote.streamDuration)
    }

    override fun play() {
        wantedPlay = true
        status.value = status.value.copy(
            state = (status.value.state as? PlaybackState.Prepared)?.copy(playWhenReady = true) ?: status.value.state,
            playWhenReady = true,
        )
        if (loaded) session?.remoteMediaClient?.play()
    }

    override fun pause() {
        wantedPlay = false
        status.value = status.value.copy(
            state = (status.value.state as? PlaybackState.Prepared)?.copy(playWhenReady = false) ?: status.value.state,
            playWhenReady = false,
        )
        if (loaded) session?.remoteMediaClient?.pause()
    }

    override fun seekTo(positionMs: Long) {
        pendingPosition = positionMs.coerceAtLeast(0L)
        if (loaded) session?.remoteMediaClient?.seek(MediaSeekOptions.Builder().setPosition(pendingPosition).build())
        else progress.value = progress.value.copy(positionMs = pendingPosition)
    }

    override fun stop() {
        job?.cancel()
        job = null
        if (loaded) session?.remoteMediaClient?.stop()
        loaded = false
        val oldServer = server
        val oldFile = audioFile
        server = null
        audioFile = null
        scope.launch(Dispatchers.IO) {
            oldServer?.stop()
            oldFile?.delete()
        }
        status.value = PlaybackStatus(PlaybackState.Idle)
        progress.value = Progress()
    }

    override fun reset() = stop()
    override fun release() {
        stop()
        attach(null)
    }
    override fun setConfig(config: PlaybackConfig) {
        this.config = config
        if (loaded) {
            session?.remoteMediaClient?.setStreamVolume(config.volume.toDouble())
            session?.remoteMediaClient?.setPlaybackRate(config.speed.toDouble().coerceIn(0.5, 2.0))
        }
    }
}
