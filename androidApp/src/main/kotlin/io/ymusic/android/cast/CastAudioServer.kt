package io.ymusic.android.cast

import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal class CastAudioServer(host: String, private val file: File, private val mimeType: String) : NanoHTTPD(host, 0) {
    private val requests = AtomicInteger()
    private val bytes = AtomicLong()
    val requestCount: Int get() = requests.get()
    val bytesRead: Long get() = bytes.get()
    private val path = "/${UUID.randomUUID()}/audio.m4a"
    val url: String get() {
        val host = if (hostname.contains(':')) "[$hostname]" else hostname
        return "http://$host:$listeningPort$path"
    }

    override fun serve(session: IHTTPSession): Response {
        if (session.uri != path) return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "")
        requests.incrementAndGet()
        if (session.method == Method.OPTIONS) return cors(newFixedLengthResponse(""))
        if (session.method != Method.GET && session.method != Method.HEAD) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "")
        }
        val length = file.length()
        val range = session.headers["range"]
        var start = 0L
        var end = length - 1
        if (range != null) {
            val match = Regex("bytes=(\\d*)-(\\d*)").matchEntire(range)
            val first = match?.groupValues?.get(1).orEmpty()
            val last = match?.groupValues?.get(2).orEmpty()
            if (match == null || first.isEmpty() && last.isEmpty()) return unsatisfiable(length)
            if (first.isEmpty()) {
                val suffix = last.toLongOrNull()?.takeIf { it > 0 } ?: return unsatisfiable(length)
                start = (length - suffix).coerceAtLeast(0)
            } else {
                start = first.toLongOrNull() ?: return unsatisfiable(length)
                end = if (last.isEmpty()) end else (last.toLongOrNull() ?: return unsatisfiable(length)).coerceAtMost(end)
            }
            if (start >= length || start > end) return unsatisfiable(length)
        }
        // NanoHTTPD 2.3.1 needs an empty stream to suppress a fixed-length HEAD body.
        val input: InputStream = if (session.method == Method.HEAD) ByteArrayInputStream(ByteArray(0)) else FileInputStream(file)
        try {
            if (input is FileInputStream) input.channel.position(start)
            val monitored = object : FilterInputStream(input) {
                override fun read(): Int = input.read().also { if (it >= 0) bytes.incrementAndGet() }
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int = input.read(buffer, offset, length).also {
                    if (it > 0) bytes.addAndGet(it.toLong())
                }
            }
            return cors(
                newFixedLengthResponse(
                    if (range == null) Response.Status.OK else Response.Status.PARTIAL_CONTENT,
                    mimeType,
                    monitored,
                    end - start + 1,
                ),
            ).apply {
                addHeader("Accept-Ranges", "bytes")
                if (range != null) addHeader("Content-Range", "bytes $start-$end/$length")
            }
        } catch (error: Throwable) {
            input.close()
            throw error
        }
    }

    private fun unsatisfiable(length: Long): Response = cors(
        newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, ""),
    ).apply { addHeader("Content-Range", "bytes */$length") }

    private fun cors(response: Response): Response = response.apply {
        closeConnection(true)
        addHeader("Access-Control-Allow-Origin", "*")
        addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
        addHeader("Access-Control-Allow-Headers", "Range, Content-Type")
        addHeader("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges")
    }
}
