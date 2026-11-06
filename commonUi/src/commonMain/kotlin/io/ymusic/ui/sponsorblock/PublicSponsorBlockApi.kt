package io.ymusic.ui.sponsorblock

import kmphttp.HttpStatusException
import kmphttp.KmpHttpClient
import kmphttp.RequestBuilder
import kmphttp.toHttpUrlOrNull
import kmphttp.utf8String
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import okio.ByteString.Companion.encodeUtf8
import okio.use
import projectcommon.json.arrayOrNull
import projectcommon.json.doubleOrNull
import projectcommon.json.objects
import projectcommon.json.stringOrNull
import kotlin.time.Duration.Companion.seconds

class PublicSponsorBlockApi(private val client: KmpHttpClient) : SponsorBlockApi {
    override suspend fun getSkipSegments(videoId: String): GetSkipSegmentsResponse = withContext(Dispatchers.IO) {
        val prefix = videoId.encodeUtf8().sha256().hex().take(4)
        val url = requireNotNull("https://sponsor.ajay.app/api/skipSegments/$prefix".toHttpUrlOrNull())
            .newBuilder()
            .addQueryParameter("categories", """["sponsor","selfpromo","interaction","intro","outro","music_offtopic"]""")
            .addQueryParameter("actionTypes", """["skip"]""")
            .build()
        client.execute(RequestBuilder().url(url).get().build(), timeout = 5.seconds).use { response ->
            if (response.code == 404) return@withContext GetSkipSegmentsResponse(emptyList())
            if (!response.isSuccessful) throw HttpStatusException(response.code)
            val entries = Json.parseToJsonElement(response.body.utf8String()).jsonArray
            GetSkipSegmentsResponse(
                entries.filterIsInstance<JsonObject>()
                    .filter { it.stringOrNull("videoID") == videoId }
                    .flatMap { it.objects("segments") }
                    .mapNotNull(::toSegment)
                    .distinct()
                    .sortedBy { it.startTime },
            )
        }
    }

    private fun toSegment(segment: JsonObject): SponsorBlockSegment? {
        if (segment.stringOrNull("actionType") != "skip") return null
        val category = segment.stringOrNull("category")?.takeIf { it in Categories } ?: return null
        val range = segment.arrayOrNull("segment") ?: return null
        val start = (range.getOrNull(0) as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return null
        val end = (range.getOrNull(1) as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return null
        if (!start.isFinite() || !end.isFinite() || start < 0 || end <= start) return null
        return SponsorBlockSegment(start, end, category, segment.doubleOrNull("videoDuration") ?: 0.0)
    }

    private companion object {
        val Categories = setOf("sponsor", "selfpromo", "interaction", "intro", "outro", "music_offtopic")
    }
}
