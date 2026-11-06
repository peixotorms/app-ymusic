package io.ymusic.ui.sponsorblock

import io.ymusic.remoteconfig.ApiService
import io.ymusic.remoteconfig.SkipSegment
import kmpcommon.log.Logger
import kmphttp.HttpStatusException
import kmphttp.HttpTimeoutException
import kmphttp.KmpHttpClient
import kmphttp.RequestBuilder
import kmphttp.toHttpUrlOrNull
import kmphttp.utf8String
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okio.use
import projectcommon.json.objects
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FallbackSponsorBlockApi(
    private val primary: SponsorBlockApi,
    private val fallback: SponsorBlockApi,
    logger: Logger? = null,
    private val lookupTimeout: Duration = 5.seconds,
) : SponsorBlockApi {
    private val log = logger?.fork("sponsor-block-sources")

    override suspend fun getSkipSegments(videoId: String): GetSkipSegmentsResponse = try {
        fetch(primary, videoId)
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (primaryError: Exception) {
        log?.fork("primary", mapOf("videoId" to videoId))?.log(Logger.Level.WARN, "Primary lookup failed; trying YMusic", primaryError)
        try {
            fetch(fallback, videoId)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (fallbackError: Exception) {
            fallbackError.addSuppressed(primaryError)
            throw fallbackError
        }
    }

    private suspend fun fetch(api: SponsorBlockApi, videoId: String): GetSkipSegmentsResponse =
        withTimeoutOrNull(lookupTimeout) { api.getSkipSegments(videoId) }
            ?: throw HttpTimeoutException("SponsorBlock lookup timed out after $lookupTimeout")
}

fun networkSponsorBlockApi(client: KmpHttpClient, service: ApiService? = null, logger: Logger? = null): SponsorBlockApi =
    FallbackSponsorBlockApi(
        PublicSponsorBlockApi(client),
        service?.let(::ServiceSponsorBlockApi) ?: YMusicSponsorBlockApi(client),
        logger,
    )

private class YMusicSponsorBlockApi(private val client: KmpHttpClient) : SponsorBlockApi {
    override suspend fun getSkipSegments(videoId: String): GetSkipSegmentsResponse = withContext(Dispatchers.IO) {
        val url = requireNotNull("https://api.ymix.io/v1/sponsorblock/segments".toHttpUrlOrNull())
            .newBuilder().addQueryParameter("v", videoId).build()
        client.execute(RequestBuilder().url(url).get().build(), timeout = 5.seconds).use { response ->
            if (!response.isSuccessful) throw HttpStatusException(response.code)
            val segments = Json.parseToJsonElement(response.body.utf8String()).jsonObject.objects("segments")
            GetSkipSegmentsResponse(
                segments.map(SkipSegment::from).map {
                    SponsorBlockSegment(it.startTime, it.endTime, it.category, it.videoDuration)
                },
            )
        }
    }
}
