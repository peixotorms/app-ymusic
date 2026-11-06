package io.ymusic.android.cast

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class CastRoutingEngine<S : PlayableItem>(
    private val local: MediaEngine<S>,
    private val remote: MediaEngine<PlayableItem>,
    private val connected: StateFlow<Boolean>,
    private val scope: CoroutineScope,
    private val logger: Logger,
) : MediaEngine<S> {
    override val status = MutableStateFlow(local.status.value)
    override val progress = MutableStateFlow(local.progress.value)
    private val jobs = mutableListOf<Job>()
    private var transfer: Job? = null
    private var transferPosition = 0L
    private var transferPlay = false
    private var source: S? = null
    private var config = PlaybackConfig()
    private var useCast = connected.value
    private val target: MediaEngine<S> get() = if (useCast) remote else local

    init {
        jobs += scope.launch {
            local.status.collect { if (!useCast) status.value = it }
        }
        jobs += scope.launch {
            local.progress.collect { if (!useCast) progress.value = it }
        }
        jobs += scope.launch {
            remote.status.collect { if (useCast && source != null) status.value = it }
        }
        jobs += scope.launch {
            remote.progress.collect { if (useCast && source != null) progress.value = it }
        }
        jobs += scope.launch {
            connected.collectLatest { cast ->
                if (useCast == cast) return@collectLatest
                transfer?.cancel()
                val item = source
                transferPosition = progress.value.positionMs
                // Backends update Prepared's intent; the facade owns the outer playWhenReady flag.
                (status.value.state as? PlaybackState.Prepared)?.let { transferPlay = it.playWhenReady }
                if (item != null) target.stop()
                useCast = cast
                if (item == null) {
                    status.value = target.status.value
                    progress.value = target.progress.value
                } else {
                    transfer = scope.launch {
                        try {
                            val destination = target
                            val state = destination.status.value.state
                            if (state is PlaybackState.Error && state.error.needReset) {
                                destination.reset("Cast route transfer")
                            } else {
                                destination.init()
                            }
                            val ready = withTimeoutOrNull(30_000) {
                                destination.status.first { it.state.readyToLoad || it.state is PlaybackState.Error }
                            } ?: error("Cast route preparation timed out")
                            if (source == item) {
                                if (ready.state is PlaybackState.Error && !ready.state.readyToLoad) {
                                    status.value = ready.copy(playWhenReady = transferPlay)
                                } else {
                                    destination.load(item, transferPosition, transferPlay, config)
                                }
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            logger.log(Logger.Level.WARN, "Cast route transfer failed", error)
                            status.value = PlaybackStatus(PlaybackState.Error(PlayerError("E60071", cause = error)), playWhenReady = transferPlay)
                        }
                    }
                }
            }
        }
    }

    val isCasting: Boolean get() = connected.value

    fun <T> routeState(local: StateFlow<T>, castValue: T): StateFlow<T> {
        val state = MutableStateFlow(if (connected.value) castValue else local.value)
        jobs += scope.launch {
            combine(connected, local) { cast, value -> if (cast) castValue else value }.collect { state.value = it }
        }
        return state
    }

    override fun init() {
        local.init()
        if (useCast) remote.init()
    }
    override fun load(source: S, startPositionMs: Long, playWhenReady: Boolean, config: PlaybackConfig) {
        transfer?.cancel()
        this.source = source
        this.config = config
        transferPosition = startPositionMs
        transferPlay = playWhenReady
        target.load(source, startPositionMs, playWhenReady, config)
        status.value = target.status.value
        progress.value = target.progress.value
    }
    override fun preload(source: S?) {
        if (!useCast) local.preload(source)
    }
    override fun play() {
        transferPlay = true
        target.play()
    }
    override fun pause() {
        transferPlay = false
        target.pause()
    }
    override fun seekTo(positionMs: Long) {
        transferPosition = positionMs
        target.seekTo(positionMs)
    }
    override fun setConfig(config: PlaybackConfig) {
        this.config = config
        target.setConfig(config)
    }
    override fun stop() {
        transfer?.cancel()
        if (source != null) target.stop()
        source = null
        status.value = PlaybackStatus(PlaybackState.Idle)
        progress.value = Progress()
    }
    override fun reset() {
        stop()
        target.reset()
    }
    override fun reset(reason: String) {
        stop()
        target.reset(reason)
    }
    override fun release() {
        stop()
        jobs.forEach { it.cancel() }
        local.release()
    }
    override fun pcmLevels(out: FloatArray): Int = if (useCast) 0 else local.pcmLevels(out)
    override fun presentedFrames(): Int = if (useCast) 0 else local.presentedFrames()
    override fun droppedFrames(): Int = if (useCast) 0 else local.droppedFrames()
    override fun videoDecoder(): String? = if (useCast) null else local.videoDecoder()
}
