package io.ymusic.ui

import io.ymusic.dynamicengine.yt.YtFeedItem
import io.ymusic.dynamicengine.yt.YtSection

private enum class MusicHomeGroup {
    Fresh,
    Mixes,
    Playlists,
    NewReleases,
    Covers,
    Videos,
    Albums,
    Featured,
    Community,
    Other,
    ListenAgain,
}

private fun homeWords(text: String?): String = text.orEmpty().lowercase()
    .replace("&", " and ").replace("favourites", "favorites")
    .replace(Regex("[^a-z0-9]+"), " ").trim()

private fun homeGroup(section: YtSection): MusicHomeGroup = when (homeWords(section.header?.title)) {
    "fresh finds and old favorites", "fresh finds old favorites", "fresh finds", "old favorites" -> MusicHomeGroup.Fresh
    "mixed for you", "made for you" -> MusicHomeGroup.Mixes
    "from your library", "your library", "playlists" -> MusicHomeGroup.Playlists
    "new releases", "new releases for you", "new albums and singles" -> MusicHomeGroup.NewReleases
    "covers and remixes", "covers remixes" -> MusicHomeGroup.Covers
    "music videos for you" -> MusicHomeGroup.Videos
    "albums for you" -> MusicHomeGroup.Albums
    "featured playlists for you", "featured playlists" -> MusicHomeGroup.Featured
    "from the community", "from your community" -> MusicHomeGroup.Community
    "listen again" -> MusicHomeGroup.ListenAgain
    else -> MusicHomeGroup.Other
}

private fun musicItemKey(item: YtFeedItem): String = item.kind + ":" +
    if (item.kind == YtFeedItem.KIND_PLAYLIST) item.id.removePrefix("VL") else item.id

private fun likedMusic(item: YtFeedItem): Boolean = item.kind == YtFeedItem.KIND_PLAYLIST &&
    (item.id.removePrefix("VL") == "LM" || homeWords(item.title) in setOf("liked music", "your likes"))

private fun supermix(item: YtFeedItem): Boolean = homeWords(item.title) in setOf("my supermix", "your supermix", "supermix")

private fun freshRank(item: YtFeedItem): Int = when {
    likedMusic(item) -> 0
    homeWords(item.title) in setOf("new release mix", "new releases mix") -> 1
    homeWords(item.title) == "discover mix" -> 2
    homeWords(item.title) == "replay mix" -> 3
    else -> 4
}

private fun mixRank(item: YtFeedItem): Int = when {
    supermix(item) -> 0
    else -> Regex("my mix ([0-9]+)").matchEntire(homeWords(item.title))?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
}

/** Personalized items stay server-owned; unknown or renamed shelves retain their relative order. */
fun musicHomeSections(sections: List<YtSection>, playlistsTitle: String): List<YtSection> {
    val groups = sections.groupBy(::homeGroup)
    val fresh = groups[MusicHomeGroup.Fresh].orEmpty()
    val mixes = groups[MusicHomeGroup.Mixes].orEmpty()
    val listen = groups[MusicHomeGroup.ListenAgain].orEmpty()
    fun merged(source: List<YtSection>, items: List<YtFeedItem> = source.flatMap { it.items }): YtSection? {
        val first = source.firstOrNull() ?: return null
        val unique = items.distinctBy(::musicItemKey)
        if (unique.isEmpty()) return null
        return first.copy(items = unique, collapsible = source.any { it.collapsible } || unique.size > 12)
    }
    val mixItems = (mixes.flatMap { it.items } + listen.flatMap { it.items }.filter(::supermix)).sortedBy(::mixRank)
    val freshItems = (fresh.flatMap { it.items } + listen.flatMap { it.items }.filter(::likedMusic)).sortedBy(::freshRank)
    val supermixIds = mixItems.filter(::supermix).map(::musicItemKey).toSet()
    return buildList {
        merged(fresh + mixes, freshItems + mixItems)?.let { section ->
            add(section.copy(header = section.header?.copy(ctaText = null, ctaToken = null, ctaDeeplink = null, ctaAction = null)))
        }
        val library = groups[MusicHomeGroup.Playlists].orEmpty()
        merged(library, library.flatMap { it.items }.filter { it.kind == YtFeedItem.KIND_PLAYLIST && !it.album })?.let { section ->
            add(section.copy(header = section.header?.copy(title = playlistsTitle, titleKey = null, ctaText = null, ctaToken = null, ctaDeeplink = null, ctaAction = null)))
        }
        for (group in listOf(MusicHomeGroup.NewReleases, MusicHomeGroup.Covers, MusicHomeGroup.Videos, MusicHomeGroup.Albums, MusicHomeGroup.Featured, MusicHomeGroup.Community)) {
            merged(groups[group].orEmpty())?.let(::add)
        }
        addAll(groups[MusicHomeGroup.Other].orEmpty())
        val hasMixSection = fresh.isNotEmpty() || mixes.isNotEmpty()
        merged(listen, listen.flatMap { it.items }.filterNot { hasMixSection && (likedMusic(it) || supermix(it) || musicItemKey(it) in supermixIds) })?.let(::add)
    }
}
