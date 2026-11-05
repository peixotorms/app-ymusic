package io.ymusic.android.cast

import android.content.Context
import android.net.Uri
import io.ymusic.download.SabrSpecResolver
import io.ymusic.player.PlayableItem
import io.ymusic.ytmedia.sabr.DownloadFile
import io.ymusic.ytmedia.sabr.SabrAttestationPending
import io.ymusic.ytmedia.sabr.SabrPoTokenInvalid
import io.ymusic.ytmedia.sabr.SabrSession
import io.ymusic.ytmedia.sabr.SabrTrack
import kmpcommon.log.Logger
import kmphttp.KmpHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.net.URLConnection
import java.util.UUID

internal data class PreparedAudio(val file: File, val mimeType: String)

internal class CastAudioSource(
    private val context: Context,
    private val resolver: SabrSpecResolver,
    private val client: KmpHttpClient,
    private val userAgent: String,
    private val logger: Logger,
) {
    private val cacheDir = context.cacheDir

    fun newFile(): File = File(cacheDir, "cast-${UUID.randomUUID()}.audio")

    suspend fun prepare(item: PlayableItem, file: File): PreparedAudio = withContext(Dispatchers.IO) {
        try {
            val mimeType = when (item) {
                is PlayableItem.YtMedia -> {
                    check(!item.live) { "live Cast playback is unavailable" }
                    for (attempt in 0..2) {
                        val spec = resolver.resolve(item.videoId).spec
                        val format = spec.formats.filter {
                            it.mimeType.startsWith("audio/mp4") && it.audioTrackIsDefault != false
                        }.sortedWith(compareBy({ it.isDrc }, { -it.clen })).firstOrNull()
                            ?: error("no Cast-compatible AAC audio")
                        file.outputStream().close()
                        try {
                            SabrSession(logger = logger).download(
                                client, userAgent, spec,
                                SabrTrack(format, object : DownloadFile {
                                    override fun length(): Long = file.length()
                                    override fun appendSink(): BufferedSink = file.sink(append = true).buffer()
                                    override fun close() = Unit
                                }),
                            )
                            break
                        } catch (error: IOException) {
                            if (attempt == 2 || error !is SabrAttestationPending && error !is SabrPoTokenInvalid) throw error
                            logger.log(Logger.Level.WARN, "refreshing Cast audio attestation", error)
                            delay(3_000)
                        }
                    }
                    "audio/mp4"
                }
                is PlayableItem.LocalMedia -> {
                    val uri = Uri.parse(item.uri)
                    val input = if (uri.scheme == "file" || uri.scheme == null) {
                        File(uri.path ?: item.uri).inputStream()
                    } else {
                        context.contentResolver.openInputStream(uri) ?: error("cannot open Cast audio")
                    }
                    input.use { source -> file.outputStream().use { source.copyTo(it) } }
                    context.contentResolver.getType(uri)
                        ?: URLConnection.guessContentTypeFromName(uri.path ?: item.uri)
                        ?: "application/octet-stream"
                }
            }
            PreparedAudio(file, mimeType)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }
}
