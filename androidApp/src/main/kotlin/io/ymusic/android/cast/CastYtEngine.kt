package io.ymusic.android.cast

import io.ymusic.player.AudioSessionSource
import io.ymusic.player.AudioTrackState
import io.ymusic.player.CaptionState
import io.ymusic.player.PlayableItem
import io.ymusic.player.PlaybackConfig
import io.ymusic.player.PlaybackStatus
import io.ymusic.player.Progress
import io.ymusic.player.VideoQualityState
import io.ymusic.player.YtEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class CastYtEngine(
    private val local: YtEngine,
    private val routed: CastRoutingEngine<PlayableItem.YtMedia>,
) : YtEngine by local, AudioSessionSource {
    override val videoQuality = routed.routeState(
        local.videoQuality,
        VideoQualityState(emptyList(), VideoQualityState.AUDIO_ONLY, VideoQualityState.AUDIO_ONLY),
    )
    override val captionLines = routed.routeState(local.captionLines, emptyList())
    override val captions = routed.routeState(local.captions, CaptionState.DEFAULT)
    override val audioTracks = routed.routeState(local.audioTracks, AudioTrackState.DEFAULT)
    override val audioSessionId = routed.routeState(
        (local as? AudioSessionSource)?.audioSessionId ?: MutableStateFlow(0),
        0,
    )
    override fun selectAudioTrack(id: String) {
        if (!routed.isCasting) local.selectAudioTrack(id)
    }

    override val status: StateFlow<PlaybackStatus> get() = routed.status
    override val progress: StateFlow<Progress> get() = routed.progress
    override fun init() = routed.init()
    override fun load(source: PlayableItem.YtMedia, startPositionMs: Long, playWhenReady: Boolean, config: PlaybackConfig) =
        routed.load(source, startPositionMs, playWhenReady, config)
    override fun preload(source: PlayableItem.YtMedia?) = routed.preload(source)
    override fun play() = routed.play()
    override fun pause() = routed.pause()
    override fun seekTo(positionMs: Long) = routed.seekTo(positionMs)
    override fun stop() = routed.stop()
    override fun reset() = routed.reset()
    override fun reset(reason: String) = routed.reset(reason)
    override fun setConfig(config: PlaybackConfig) = routed.setConfig(config)
    override fun release() = routed.release()
    override fun pcmLevels(out: FloatArray): Int = routed.pcmLevels(out)
    override fun presentedFrames(): Int = routed.presentedFrames()
    override fun droppedFrames(): Int = routed.droppedFrames()
    override fun videoDecoder(): String? = routed.videoDecoder()
}
