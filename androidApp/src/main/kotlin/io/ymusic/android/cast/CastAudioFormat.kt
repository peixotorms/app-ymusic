package io.ymusic.android.cast

import io.ymusic.ytmedia.sabr.AdaptiveFormat

internal fun castAudioFormat(formats: List<AdaptiveFormat>): AdaptiveFormat? = formats.filter {
    val codec = it.mimeType.substringAfter("codecs=", "").trim('"', ' ')
    it.clen > 0 && it.mimeTypeWithoutCodec == "audio/mp4" &&
        codec in setOf("mp4a.40.2", "mp4a.40.5", "mp4a.40.29") && it.audioTrackIsDefault != false
}.sortedWith(compareBy({ it.isDrc }, { -it.clen })).firstOrNull()
