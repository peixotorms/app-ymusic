---
version: 1
slug: "monmain-kotlin-io-ymusic-ui-nowplaying-kt-f964593c"
primary_target: "commonUi/src/commonMain/kotlin/io/ymusic/ui/NowPlaying.kt"
related_targets: ["commonUi/src/commonMain/kotlin/io/ymusic/ui/DetailScreens.kt","commonUi/src/commonMain/kotlin/io/ymusic/ui/YtMusicScreen.kt","commonUi/src/commonMain/kotlin/io/ymusic/ui/Theme.kt"]
---

# Music presentation

Mode: Operate. Android phone first, with usable landscape and desktop layouts. The user supplied explicit player and playlist references and asked to preserve features while adopting their composition.

## Direction contract

THESIS: Artwork leads the music experience; playback controls and the queue remain immediately usable without separate box-like chrome.

OWN-WORLD: Rounded square album art, artwork-derived tonal color fading to black, warm or cool accents taken from the image, readable white text, and Material icons. Other pages retain theme selection while sharing spacing, shapes, and compact music rows.

STORY: See the current track, like it, play or skip, change audio/video quality, and reach the queue and Cast. Secondary actions stay in the existing overflow menu, including download and sleep timer.

FIRST VIEWPORT: Safe status-bar inset, large square cover with generous side margins, left-aligned title/artist, heart and overflow to their right; a moving wavy seek track, timestamps and centered format choice; prominent round play/pause with previous/next; Up next with compact repeat/shuffle/Cast controls. Video occupies the same artwork position using its existing aspect ratio and native renderer. Playlist pages have full-width hero art fading into the title and action row, followed by compact square-art track rows.

FORM: Direct adaptation of the user's pinned screenshots, implemented in native Compose. No alternative visual-world selection is needed; the supplied composition is the design authority. Existing actions, accessibility, queue drag/reorder, errors and paging remain available.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Scope and open decisions

- Remove the player top toolbar while retaining swipe-down and system Back.
- Show one heart for YouTube Music like/unlike; keep ratings synchronization and sign-in/error handling.
- Preserve fast seek actions through accessible controls or the overflow if the primary transport is simplified.
- Move sleep timer and download into overflow, quality below the seek track, and Cast beside repeat/shuffle.
- Add playlist hero artwork, overlay navigation/Cast, playlist actions, and compact track rows without hiding available actions.
- Use a floating rounded mini-player and matching surface colors/spacing elsewhere.
- Player and playlist screens use the requested dark artwork treatment; other pages follow the saved light/dark theme.
- All temporary previews and emulator fixtures stay under /tmp. Runtime artwork comes from the existing image loader; no reference screenshot ships in the application.
