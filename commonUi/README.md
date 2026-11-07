# `:commonUi`

Module detail for the one-line summary in the root [`AGENTS.md`](../AGENTS.md) — read this before working inside the module.

The shared Compose Multiplatform app UI (jvm + android; iOS later).

- **Screens** — drawer navigation (Discover/YT Music/Library/Downloads/Settings); Discover feed tabs + search
  (live suggestions, paged results) over `:dynamic-engine`'s `yt` task methods; LibraryScreen over `:media-store`
  flows; the persistent mini-player + full Now Playing (transport, sleep timer, capability selectors, queue).

- **Artwork-led player** — `ArtworkTheme` samples a small cached image off the UI thread, derives readable accent/secondary colors, and fades the backdrop to black. Player and playlist surfaces use this dark treatment in either app theme; leaving them restores Android system-bar icon appearance. Desktop rendering cuts the native video region out of the painted backdrop.

  Now Playing is a fixed viewport with one shared 16dp horizontal gutter inside safe drawing insets. The square audio cover shrinks when height or larger text needs room; inline video keeps its native renderer and only rotation and fullscreen viewing controls. Song details, seeking, and transport are rendered once. Fullscreen retains captions, transport, seeking, rotation, and exit controls, using natural video proportions without a fit/fill control. Captions remain available in overflow and fullscreen.

  Repeat and Shuffle flank Previous/Play/Next. The bottom strip contains Sleep timer, Speed, queue chevrons, Equalizer, and Cast. The chevrons or a swipe up open a separate neutral queue; Collapse, Back, or a downward pull close the queue before dismissing the player. Queue scrolling and drag reordering remain independent. Header actions keep 48dp targets while their glyphs move closer together. Download, SponsorBlock inclusion, ±10-second seeks, and existing secondary actions remain in overflow; format sits between the time labels.

  `SlimSeekBar` keeps seek-on-release, keyboard/screen-reader seeking, buffered position, and SponsorBlock marks. Its played segment moves as a wave during playback, straightens when paused, and stops animation when the window is inactive; Compose's motion scale handles disabled system animations. This is presentation only: buffering, preloading, and media caching are unchanged.

- **Playlist presentation** — YouTube and YouTube Music playlist/album pages share an artwork hero with overlay Back/Search/Cast, a bottom title gradient, metadata, and equally sized bare Save/Download/Repeat/Shuffle/Play icons when applicable. Share and Create local playlist are removed from this header; playlist pages hide the feed filter chips. Repeat and Shuffle reflect the player state. Play and Shuffle both retain their paginated queue source. Song rows use square artwork, duration, and the existing overflow actions; non-music feeds keep their existing layouts. The shared mini-player is inset and rounded. Dark app surfaces use quieter near-black grounds; light-theme browsing remains available.

- **Track ratings** — Now Playing exposes a heart toggle in portrait and landscape; a second tap removes the like. The details dialog retains the full rating controls and shares the same state. Writes use the signed-in YouTube Music account through `ytmusic/video/rate`, wait for each request to finish, and roll back with a visible error on failure. Comments load only when details are opened.

  `AppGraph.videoRatings` shares rating state between screens and Android's media service. It guards concurrent writes, discards rating reads started before a newer write, rolls back failed writes, and invalidates rating state on account changes. Rating entries are bounded while retaining the current track and observed states. Car commands use an explicit desired rating and video ID rather than blindly toggling a potentially stale button. Rating reads and writes use the WEB_REMIX music client and a song target; WEB description tokens never enter Music mutations. Successful changes invalidate retained music feeds, which refresh when visible so unlike/re-like can restore the Liked Music list. Failed writes roll back without invalidating it.

- **Opening screen** — a fresh launch opens YouTube Music Home; activity recreation retains saveable navigation state. Back from another root returns to YouTube Music. Discover-specific UI tests select Discover explicitly.

- **Music Home arrangement** — `MusicHomeLayout.kt` combines Fresh finds/old favorites with Mixed for you, prioritizes the named mixes, filters the existing library shelf to playlist cards, and moves Listen again to the end without the liked-music or Supermix shortcuts. The requested named recommendation shelves come next, followed by unrecognized shelves in their supplied order. Recognition uses the known English section names; renamed or localized server headings pass through rather than being dropped. Contents remain YouTube Music's personalized recommendations. `Pager` retains raw sections and arranges the aggregate after pagination so Listen again stays last and removals are not undone by another page. Fresh Home loads and reloads automatically fetch one section-list continuation before arranging the combined result. The extra request has a five-second limit; failure preserves the first page and its continuation for retry. Individual shelves retain their own collapsed/expanded state. The bottom Show more requests subsequent continuation pages. This policy is shared with Android Auto.

- **Privacy and links** — crash-report and anonymous usage-stat controls and senders are removed. Feedback only copies diagnostic text or opens an issue when requested. Project links are fixed to `peixotorms/app-ymusic`, including when a service supplies upstream defaults. No upstream email or Telegram contact is presented.

- **Download selection** — long-press a download to select it, tap others or select all in the current filter, then remove entries or delete completed files after confirming their titles. Removing entries preserves finished files; failed file deletions preserve the entries and selection. Back clears selection, and changing filters starts a fresh selection.

- **SponsorBlock** — Android and desktop use the public SponsorBlock API first, querying a four-character
  SHA-256 video-ID prefix and matching the full ID locally. It requests skip actions for sponsor,
  self-promotion, interaction, intro, outro, and music non-content categories. Network, HTTP, and parse
  failures fall back to the configured YMusic service, or `https://api.ymix.io/v1/sponsorblock/segments`
  when no service is configured. Each source has a five-second lookup limit; successful empty results
  are authoritative and do not trigger fallback. `SponsorBlockService` caches results for six minutes,
  reuses stale cache if both sources fail, and honors the global switch and per-video whitelist.
  Overlapping or adjoining ranges are united before reaching the player; source metadata stays intact.
  Playback continues if lookup fails. Source selection does not enable app updates or other YMusic services.
  Skip data is provided by [SponsorBlock](https://sponsor.ajay.app/) under
  [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/).

- **Playback buffering** — Settings → Playback → Audio buffer ahead offers 30/60/120/180/300/600 seconds,
  default 120. A target longer than the remaining song fills through the song's end. YouTube audio starts normally with a 15-second starter target, then expands after
  five seconds of actual playback. Refill resumes at target minus 15 seconds and stops at target;
  video targets 30 seconds, and live streams keep the existing policy. Queue audio preloading waits
  until the current track is buffered through its effective end and has at most 60 seconds left;
  the next track targets 15 seconds. Skips retire obsolete fetches. Cast buffers separately.

- **Playback cache** — Settings → Playback → Player cache defaults to 2 GB, with Off, 128/256/512 MB,
  and 1/2/5/10 GB choices. Streaming stores media chunks in a persistent least-recently-used cache;
  it does not automatically create offline downloads for the queue. Cache retention is unchanged.
  Cast's full-track buffering is separate; see
  [`androidApp/README.md`](../androidApp/README.md#google-cast).

- **AppGraph** — shells construct `AppGraph` (engines + `Player` facade + optional mediaStore/scan callback + the
  platform `playerSurface(modifier)` composable, which hosts the one shared video container the shell handed the
  engines) — nothing platform-specific in common. Desktop YouTube sign-in is a shell screen (`cookieSignIn`: QR /
  DevTools paste in `:desktopApp`). Mobile exposes Settings → Link devices (`exportSessionCookies` + `scanQrCode`
  + `confirmDeviceOwner`) so the phone can confirm a matching code, unlock, encrypt the jar, and POST it to that
  inbox. The desktop deletes the inbox as soon as it receives the payload.

- **Outside links** — shells hand every url they are opened with to `IncomingLinks.open` (Android VIEW intents for
  youtube.com / youtu.be / ymusic.io / ymusicapp.com / `ymusic://`; desktop macOS + iOS `ymusic://` only — no
  universal links, and Windows/Linux have no protocol registration or single-instance hand-off yet). `AppRoot`
  drains it and asks the engine (`resolveLink` → engine-js `yt/resolve-link`, which owns every YouTube url shape and
  the handle → channel lookup) what it opens: a video plays with its radio (or the link's playlist) in Now Playing,
  a playlist/channel/music page lands on Discover's stack, a search runs there.

- **PlaybackCoordinator** — play actions + display metadata for queue items. Play-now starts the video's
  radio through `NextQueueSource` (the engine's `yt/queue` = /next panel; a video with no radio gets its related videos and stops). Playlist pages use `PlaylistQueueSource` and the existing playlist browse continuations, including WEB_REMIX for Music. They eagerly load metadata for up to 1,000 songs, deduplicate video IDs, keep a resume token, and surface failures for retry. This avoids the shorter watch-panel queue. Shuffle reorders the visible list and starts at its first song; playback then follows that order. Turning it off restores the session's original order while retaining the current song; turning it on again generates a fresh order. New metadata is mixed into the future tail while keeping the prepared next song in place. Playback can begin before background metadata finishes. No audio is downloaded by this metadata paging. An already prepared next song survives later page arrivals.
  Watch-time ranking (Library Recent play / Top tracks, Discover Top tracks) lives in
  `PlayHistoryStore` (its own SQLite file), not `:media-store`.

- **Play again** (legacy "Rewind playlist") — every queue a `Player.setQueue` makes is a session keyed by
  `QueueState.queueId`. `PlayAgainRecorder` (one per `AppGraph`, started when `playHistory` is set)
  writes it to `PlayAgainStore` (the `play_again` table in the play-history DB, newest 50) when it starts,
  when its items or current item change, on pause, every 30s while playing, and when it is replaced.
  `PlayAgainScreen` shows each session as a card (current item, a strip of the queue around it, Resume). A tap calls `PlaybackCoordinator.playAgain`, which restores the queue
  under the same id. Like legacy, it resumes mid-item only when ≥5 min remain; otherwise the item restarts.
  A playlist queue's `QueueSource.resumeToken` (the `NextQueueSource` step: radio, playlist position or continuation) is saved with the
  session, so a resumed playlist keeps loading from where it had reached.

- **Playlist transfer** (`PlaylistTransfer.kt`) — the Playlists tab's ⋮ menu: Import from device (shell
  `DevicePlaylistSource`; Android reads MediaStore playlists and keeps only members matching a Library item, merged
  by name), Back up (`createDocument` → a `ymusic.playlists` v1 JSON of every user playlist's codes) and Restore
  (`openDocument` → Merge by name or Replace all, via `BetterMediaStore.restorePlaylists`). Smart playlists are
  never included. Each action hides when its `AppGraph` seam is null.

- **Thumbnails** — render through `:image-loader` (`LocalImageLoader` provided from `AppGraph.imageLoader`; null
  keeps placeholder boxes).

- **Localization** — **localized** via compose-resources: every user-facing string lives in
  `commonMain/composeResources/values*/strings.xml` (en base + ~50 locales — add every new key to ALL `values-*` files; plurals + `%1$s` args; brand marks
  untranslated by omission — missing keys fall back to base) — never hardcode UI text (dev-only CatalogueScreen
  exempt).
  - The `AppSettings.uiLanguage` setting drives `applyUiLocale` (expect/actual; JVM/Android set the default locale,
    iOS = next launch).
  - `App` rebuilds under `key(tag)` with `LocalLayoutDirection` derived from the effective locale (RTL mirrors the
    whole UI).
  - `SettingsScreen` hosts the picker.
  - Visual check: `:desktopApp:jvmTest` LocaleSnapshotTest renders en/vi/ar PNGs.
