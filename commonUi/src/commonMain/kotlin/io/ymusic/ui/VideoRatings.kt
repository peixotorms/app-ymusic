package io.ymusic.ui

import io.ymusic.dynamicengine.yt.YtMusicRating
import io.ymusic.dynamicengine.yt.YtVideoDetails
import kmpcommon.log.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import projectcommon.error.errorCode

suspend fun videoRatingErrorMessage(code: String): String = errorText(code).load()

data class VideoRatingSnapshot(
    val videoId: String = "",
    val status: String = YtVideoDetails.INDIFFERENT,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val available: Boolean = false,
    val pending: Boolean = false,
    val errorCode: String? = null,
)

/** Main-thread entry points; requests outlive individual screens. */
class VideoRatings internal constructor(
    private val scope: CoroutineScope,
    private val fetch: suspend (String) -> YtMusicRating,
    private val write: suspend (String, String) -> Unit,
    private val currentVideoId: () -> String?,
    private val logger: Logger?,
) {
    private val entries = LinkedHashMap<String, VideoRating>()
    private val mutableLibraryRevision = MutableStateFlow(0L)
    val libraryRevision: StateFlow<Long> = mutableLibraryRevision.asStateFlow()

    fun forVideo(videoId: String): VideoRating {
        val entry = entries.remove(videoId) ?: VideoRating(videoId, scope, fetch, write, logger) {
            mutableLibraryRevision.value++
        }
        entries[videoId] = entry
        while (entries.size > 32) {
            val oldest = entries.entries.firstOrNull {
                it.key != currentVideoId() && !it.value.observed && !it.value.state.value.pending
            } ?: break
            entries.remove(oldest.key)
        }
        return entry
    }

    internal fun invalidate() = entries.values.forEach { it.invalidate() }
}

class VideoRating internal constructor(
    val videoId: String,
    private val scope: CoroutineScope,
    private val fetch: suspend (String) -> YtMusicRating,
    private val write: suspend (String, String) -> Unit,
    parentLog: Logger?,
    private val onChanged: () -> Unit = {},
) {
    private val log = parentLog?.fork("video-rating", mapOf("videoId" to videoId))
    private val mutableState = MutableStateFlow(VideoRatingSnapshot(videoId))
    val state: StateFlow<VideoRatingSnapshot> = mutableState.asStateFlow()
    internal val observed: Boolean get() = mutableState.subscriptionCount.value > 0
    internal var revision = 0L
        private set
    private var loadJob: Job? = null

    internal fun acceptRating(details: YtMusicRating, readRevision: Long) {
        if (revision != readRevision || state.value.pending) return
        mutableState.value = VideoRatingSnapshot(videoId, details.likeStatus, loaded = true, available = details.canRate)
    }

    fun ensureLoaded(): Job? {
        if (state.value.loaded) return null
        loadJob?.takeIf { it.isActive }?.let { return it }
        mutableState.value = state.value.copy(loading = true, errorCode = null)
        val readRevision = revision
        return scope.launch {
            try {
                acceptRating(fetch(videoId), readRevision)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log?.log(Logger.Level.WARN, "rating details failed", error)
                if (revision == readRevision) mutableState.value = state.value.copy(loading = false, errorCode = error.errorCode() ?: "E20094")
            }
        }.also { loadJob = it }
    }

    /** False when signed out or another rating write is already pending. */
    suspend fun setRating(status: String): Boolean = change { status }

    suspend fun toggleRating(status: String): Boolean = change {
        if (it == status) YtVideoDetails.INDIFFERENT else status
    }

    private suspend fun change(target: (String) -> String): Boolean = withContext(Dispatchers.Main.immediate) {
        if (state.value.pending) return@withContext false
        ensureLoaded()?.join()
        val previous = state.value
        if (previous.pending) return@withContext false
        val status = target(previous.status)
        if (!previous.available) return@withContext false
        if (status == previous.status) return@withContext true
        val writeRevision = ++revision
        mutableState.value = previous.copy(status = status, pending = true, errorCode = null)
        try {
            write(videoId, status)
            if (revision == writeRevision) onChanged()
            true
        } catch (error: Throwable) {
            if (revision == writeRevision) mutableState.value = previous
            throw error
        } finally {
            if (revision == writeRevision) mutableState.value = state.value.copy(pending = false)
        }
    }

    internal fun invalidate() {
        revision++
        loadJob?.cancel()
        loadJob = null
        mutableState.value = VideoRatingSnapshot(videoId)
        if (observed) ensureLoaded()
    }
}
