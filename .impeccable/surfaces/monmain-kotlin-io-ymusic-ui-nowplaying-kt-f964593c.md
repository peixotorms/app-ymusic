---
version: 1
slug: "monmain-kotlin-io-ymusic-ui-nowplaying-kt-f964593c"
primary_target: "commonUi/src/commonMain/kotlin/io/ymusic/ui/NowPlaying.kt"
related_targets: ["commonUi/src/commonMain/kotlin/io/ymusic/ui/DetailScreens.kt","commonUi/src/commonMain/kotlin/io/ymusic/ui/YtMusicScreen.kt","commonUi/src/commonMain/kotlin/io/ymusic/ui/Theme.kt"]
---

# Music presentation

Mode: Operate. Android phone first; larger screens retain an adaptive landscape layout. The user's Symfonium screenshots and real-device corrections are the visual authority.

## Direction contract

THESIS: Keep playback on one stable screen, with a distinct queue reached by a direct gesture or button.

OWN-WORLD: Rounded square audio covers, artwork-derived color fading to black, readable native type, a prominent circular Play button, and compact related icons. The queue has a fixed neutral dark ground. Browsing retains the saved theme and existing features.

FIRST VIEWPORT: Artwork, title, seek bar, transport bounds, and footer share a 16dp gutter inside safe insets. Audio artwork is square and bounded by the available height. Left-aligned title/artist sit below it, with closely grouped heart and overflow. The seek bar and format choice follow. Repeat and Shuffle flank Previous/Play/Next. The bottom row is Sleep timer, Speed, double up chevrons, Equalizer, Cast. The player itself does not scroll.

VIDEO: Replace the square cover with the native inline video region. Show viewing controls (fit/fill, rotation, fullscreen), captions, buffering, and errors there, while the page owns title, seeking, and transport. Fullscreen retains its complete video overlay.

QUEUE: Hidden until the chevrons or an upward swipe opens it. Collapse, Back, or a downward pull returns to Now Playing. Scrolling and reordering the list remain independent. Use a fixed near-black ground, a neutral current-row highlight, clear drag grips, readable title/artist text, and separate duration/menu columns. Keep compact Repeat/Shuffle/Cast header actions and current-item location. No filter/search row.

PLAYLIST: Retain hero artwork, navigation/Cast, title, metadata, and compact song rows. Use equally sized bare icons for applicable Save, Download, Repeat, Shuffle, Play actions; remove Share and Create local playlist from this header. Selected modes use accent color. Queue metadata continues through playlist browse pages up to 1,000 songs, and all loaded entries participate in shuffle.

ORIENTATION: Ordinary phone screens remain portrait below 600dp smallest width. Tablets and TV devices retain system rotation. Deliberate fullscreen video is the phone exception; exiting restores the device policy. Android Auto is separate.

FORM: Native Compose adaptation of the pinned references. Preserve existing playback, real ratings, captions, quality, error recovery, queue management, downloads, Cast, accessibility, and desktop keyboard controls. Compact drawn groups retain distinct 48dp targets.

FINISH: Verify actual emulator captures at phone/tablet sizes, normal and enlarged text, audio/video, queue open/closed, playlist modes, and orientation. Exercise queue gestures, reorder, Back, and the 1,000-item shuffle behavior. Finish with independent full review and matching design documentation. Temporary fixtures stay under /tmp; no reference screenshots or probe code ship.
