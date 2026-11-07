# Maintaining this fork

Last reviewed: 2026-11-07.

This is the preservation guide for merging changes from [ymusic-io/YMusicV4](https://github.com/ymusic-io/YMusicV4) into [peixotorms/app-ymusic](https://github.com/peixotorms/app-ymusic). [CHANGELOG.md](CHANGELOG.md) records user-facing changes; this guide records implementation boundaries, intentional removals, and regression checks. Update both when the fork changes.

## Baseline and release policy

The fork started from upstream 4.0.16. The currently integrated upstream commit is `80956c5c45ad09def58bb2f50c12329cdbba092d` (upstream's 2026-11-04 merge); the 4.0.16 release commit is `834802d116913150c7e660361de1ab1679dcdf17`. These identify the recorded baseline, not a claim that upstream has stopped changing.

The bundles currently use engine revision 93 and player revision 116 in `buildSrc/src/main/kotlin/Dependencies.kt`. These are protocol/cache identities, separate from the app version; preserve compatibility and increment them when changing the corresponding JavaScript.

Keep `origin` pointing to this fork and `upstream` pointing to the original. Preserve fork commits by merging upstream into a review branch. A clean merge does not establish that the behaviors below still work.

Current user decisions:

- App version remains **4.0.17**, Android code **22**, with the single release **release-v4.0.17**. Do not increase versions without a new request. JavaScript bundle revisions still change whenever their contents change, as required by the engine protocol.
- Publish exactly one Android ARM64 APK, one Windows x64 EXE, and one Linux x64 DEB. No macOS release, extra architectures, standalone JS assets, or checksum files. GitHub's automatic source archives are separate platform-provided links.
- App updates are manual. Do not restore app update checking or installer downloads from upstream.
- Temporary checkouts, probes, caches, emulator files, and build output belong under `/tmp`; keep the working source checkout clean.
- Preserve signing identity and credentials outside the repository. Never add tokens, cookies, keystores, machine-specific configuration, or signed-in test captures to source control.

## Changes to preserve

Paths in this table are repository-relative. They identify ownership and likely conflicts; follow calls into the module documentation before resolving a conflict.

| Area | Fork behavior / merge invariant | Main implementation |
| --- | --- | --- |
| Android identity | Launcher name **YMusic**; release ID `io.github.peixotorms.ymusic`, debug ID with `.debug`. Keep the source namespace `io.ymusic.android`. Authorities and private storage follow the application ID. This permits a separate installation alongside upstream. Preserve the signing key for updates. | `androidApp/build.gradle.kts`, `androidApp/src/main/AndroidManifest.xml`, `androidApp/src/main/res/values/strings.xml`; [identity details](androidApp/README.md#fork-identity-and-upstream-merges) |
| Distribution | Three installers only, signed Android releases, consolidated dated notes, stable release name. ARM64 selection must reach all Android native dependencies, not just APK splitting. Existing shared Apple source stays in place; macOS is excluded from packaging. | `.github/workflows/release.yml`, `buildSrc/src/main/kotlin/FfmpegBuild.kt`, `quickjs/build.gradle.kts`, `mse/build.gradle.kts`, app Gradle files; [building](BUILDING.md) |
| Manual updates | Android and desktop do not create an app updater. No installer permission/receiver or update controls. Engine/player bundle configuration is a separate mechanism; do not accidentally replace the fork's playback changes through an incompatible bundle. | Android/desktop application wiring, deleted `AndroidAppUpdates.kt`, `SettingsScreen.kt`, `AppGraph.kt`; [Android updates](androidApp/README.md#app-updates), [desktop](desktopApp/README.md) |
| Privacy and links | Automatic crash reporting and anonymous usage-stat sending are removed from the shells and menus. Local logs and explicit feedback remain. Source, issues, feedback, and privacy links belong to this fork. Do not reconnect upstream reporting SDKs, mail, Telegram, or donation actions. Functional network services still exist. | `YMusicApplication.kt`, desktop `Main.kt`, app dependency/ProGuard files, `SettingsScreen.kt`, `FeedbackScreen.kt`, `AppLinks.kt`, `OpenSourceLicenses.kt`, `TERMS.md`, build identity/Sentry configuration |
| Android Auto | Media3 discovery and the media browser existed upstream. Preserve the fork identity, early service initialization, library browsing, music-first root order, ratings, and Cast session integration. The host can remember its own tab. Sideload discovery may require Android Auto's Unknown sources setting on the phone; that is not a manifest-only fix. | Android `PlaybackService.kt`, `MediaBrowseTree.kt`, manifest and automotive resources; [car docs](androidApp/README.md#android-auto-discovery-and-testing) |
| YouTube Music default | Phone opens YouTube Music Home; car places YouTube Music first. Discover remains available. | `App.kt`, `YtMusicScreen.kt`, `MediaBrowseTree.kt` |
| Home shelves | Prefetch one section-list continuation before ordering, with a five-second limit and first-page fallback. Favorites/fresh mixes first, then Mixed for you content; library playlists only under Playlists; requested recommendation shelves follow; Listen again last without duplicate likes/supermix. Preserve unknown dynamic shelves and continuation handling. This reorders YouTube's feed; the server still supplies its content. | `MusicHomeLayout.kt`, `YtMusicScreen.kt`, `Feed.kt`, Android browse tree; [shared UI](commonUi/README.md) |
| Ratings | Read/write the signed-in user's song rating through the **YouTube Music WEB_REMIX** client. Share state between phone and car. Unlike/re-like invalidates music lists so the item can return. Reject signed-out writes, roll back failures, and ignore outdated/pending car actions. | `VideoRatings.kt`, `PlaybackCoordinator.kt`, Android `PlaybackService.kt`, `dynamic-engine/.../yt/YtMusicCommands.kt`, `engine-js/src/logic/yt/musicRatings.ts`, `ytmusic.ts`, `login.ts` |
| Car Like action | Like/Unlike precedes Shuffle; use the standard filled/outline thumb and consistent host sizing. Pending/unliked uses the inactive style; stale actions include the video ID. | `PlaybackService.kt`; [car controls](androidApp/README.md#car-controls-and-navigation) |
| SponsorBlock source | SponsorBlock already existed upstream. The fork changes the provider: public `https://sponsor.ajay.app/api/skipSegments/HASH_PREFIX` first, YMusic `https://api.ymix.io/v1/sponsorblock/segments?v=VIDEO_ID` fallback on failure/timeout. A valid empty result is not a failure. Do not fetch both and merge. Preserve hash matching, category preferences, caching, cancellation, and five-second lookup bounds. | `commonUi/.../sponsorblock/PublicSponsorBlockApi.kt`, `FallbackSponsorBlockApi.kt`, `SponsorBlockCoordinator.kt`, both app shells |
| Downloads | Bulk selection/actions in the download manager. Keep three concurrent downloads; do not import the five-concurrent experiment from another fork. Quality selection was already available in upstream 4.0.16 and is not a new fork feature. | `DownloadsScreen.kt`; concurrency remains in `download/.../DownloadService.kt` |
| Audio buffer setting | Choices 30/60/120/180/300/600 seconds, default 120. Start normally with a short initial target; expand after five actual listening seconds. Pauses/seeks do not consume that grace. Short tracks buffer through their end. Video targets 30 seconds. Segment boundaries can exceed a target slightly. These are streaming buffers, not saved offline downloads. | `AudioBufferRow.kt`, `AppSettings.audioBufferAheadSeconds`, `PlayerImpl.kt`, `MsePlayer.kt`, `engine-js/src/logic/player/buffering.ts`, `buffer_sabr.ts`, SABR/HTTP interception |
| Prepare next | Once current coverage reaches the end and at most one minute remains, prepare about 15 seconds of the next queued item. Retain and promote it; cancel obsolete preparation on skips/seeks/queue changes. Metadata paging must not replace that prepared candidate. | `PlayerImpl.kt`, `Queue.kt`, `MsePlayer.kt`, `DynamicEnginePlayer.kt`, `engine-js/src/logic/player.ts`, `buffering.ts`, `buffer_sabr.ts` |
| Track transitions | Keep explicit play/pause intent on prepared-item promotion. A full prefetched buffer must still complete synthetic video/SABR setup. Replacing a media source clears old ended/seeking/stall state and restores native play when appropriate. Startup recovery is bounded and ends only when the playhead advances; it must not become the normal transition path. | `MsePlayer.kt`, `engine-js/src/logic/player/boot.ts`, `buffer_sabr.ts`, `engine-js/src/protocol/mse.ts`, SABR interception |
| Queue size and shuffle | Load up to 1,000 playlist metadata entries, without downloading all their audio. Shuffle visibly permutes the queue and starts at its first item; Next/Previous/automatic advance follow that order. Off restores session order while retaining the current entry; On reshuffles. Stable entry IDs distinguish duplicates. Newly paged items mix into the future tail after the current/prepared next items. Preserve edits, Play next, and resume tokens. | `Queue.kt`, `PlaybackCoordinator.kt`, `NextQueueSource.kt`, `PlayFromSearch.kt`, `DetailScreens.kt`; [player](player/README.md) |
| Cast audio | Google Cast sender plus a phone-hosted HTTP audio server, session routing, correct MIME/range/HEAD behavior, and format preparation. YouTube uses compatible AAC and the phone proxy; no external proxy server is needed. Nest groups and TVs receive **audio**. Video casting to TV is not implemented. | `androidApp/.../cast/`, dependencies/manifest/theme, shared platform seams; [Cast details](androidApp/README.md#google-cast-audio) |
| Cast output picker | Two-line Play on device rows, This device, active route, device icons, configure/hide/unhide, sorting, and saved per-device settings. Returning to phone preserves position and intent and resets a broken destination. | `CastController.kt`, `CastOutputSheet.kt`, `CastDevicePreferences.kt`, `CastRoutingEngine.kt`, `CastSessionPlayer.kt` |
| Cast volume | Foreground volume keys and Media3 remote volume both target the receiver. Default step is 5%, configurable per device, with percentage feedback, slider, and mute. Cast SDK calls stay on main; network/server work moves to IO only after reading SDK state. | `MainActivity.kt`, `PlaybackService.kt`, `CastVolumeController.kt`, Cast controller/preferences |
| Cast boundaries | Current YouTube Cast path fully prepares a track before serving it; receiver transitions can pause. Live YouTube Cast is unsupported. Receiver codecs limit local-file casting. Phone effects do not process remote audio. Preserve failure classification and return-to-phone recovery. | Cast engine/source/format/server/router classes; error ledger |
| Shutdown fixes | Do not let canceled receive cleanup use a closed QuickJS engine. Serialize audio-effects native calls with handle invalidation/destruction so late ExoPlayer flushes cannot use freed native state. | `quickjs/.../QuickJsRuntime.kt`, `player/src/androidMain/.../media3/AudioFxProcessor.kt`, `AndroidLocalPlayer.kt` |
| Shared music design | Artwork-derived upper background fades to black; fixed near-white player controls and default orange active states. Preserve minimal README/product copy and the established native dark music surfaces. Browsing/mini-player theme boundaries differ from player controls. | `ArtworkTheme.kt`, `Theme.kt`, shared surfaces, `DESIGN.md`, `.impeccable/` |
| Now Playing | Fixed viewport, square audio/video region, native-ratio video contained on black, stale-frame loading mask, fixed timeline/centered mode selector, 00:00 and unknown duration placeholders. Title and artist each cap at two lines, with fixed heart/menu and progress positions. Inline viewing actions auto-hide after three seconds of ready playback. | `NowPlaying.kt`, `AudioOnlyHole.kt`, `VideoHole.kt`, `SlimSeekBar.kt`, `PlayerIcons.kt`, `ActionIcon.kt` |
| Queue presentation | Separate neutral screen opened by chevrons/swipe up and closed by Back/swipe down. Larger grips, duration/menu columns, current-row highlight. Repeat/Shuffle/Cast use compact equal visible gaps with distinct 48dp targets, no circular press ripple, and orange active states. Preserve Cast glyph-offset handling without shifting its picker. | `NowPlaying.kt`, `ActionIcon.kt`, `CastController.kt` |
| Playlist presentation | Large art/header, overlay navigation/Cast, consistent bare action icons, repeat and active colors, compact track rows. Share/create-local-playlist header actions and filter-tag row removed; existing feature actions otherwise remain. | `DetailScreens.kt`, `FeedItems.kt`, shared bars/dialogs/theme |
| Orientation | Ordinary phones below 600dp smallest width stay portrait. Larger screens/TV retain rotation; intentional fullscreen video is the phone exception, with rotation lock and restoration on exit. Android Auto is a separate host UI. | `commonUi/src/androidMain/.../Platform.android.kt`, platform seams, `NowPlaying.kt` |
| Fullscreen controls | Keep transport, seeking, rotation, and exit. Keep CC; remove fit/fill/aspect-ratio stretching controls. Always use the natural video proportions. | `NowPlaying.kt`, removal of `AppGraph.videoFillScreen` |
| Playback captions restored | Keep fullscreen CC, overflow language selection, the renderer, style settings, and per-track caption sessions. The temporary removal was reversed after the user confirmed captions worked. Do not force `captionVisible = false` in the facade. Preserve language selection, Off, seeking, cancellation, and track ownership through preloading. | `PlayerImpl.kt`, `NowPlaying.kt`, `SettingsScreen.kt`, `CaptionOverlay.kt`, `CaptionStyleRow.kt`, caption style preference definitions |
| Personal YouTube Music uploads (known limitation) | Browsing uses WEB_REMIX on music.youtube.com, but playback currently resolves every cloud track through the regular YouTube WEB player. There is no authenticated Music-upload playback route or automatic source fallback. A private upload may be listed but fail with YouTube’s Video unavailable verdict even in audio-only mode. Supporting uploads needs a Music-aware resolver and owner-account testing; do not replace private uploads with a similarly named public video. | `PlaybackCoordinator.kt` (`toYtMedia`), `engine-js/src/logic/yt/ytmusic.ts`, `engine-js/src/logic/player/boot.ts`; [YouTube upload behavior](https://support.google.com/youtubemusic/answer/9716522?hl=en) |
| Video availability | Existing explicit `hasVideo = false` disables the selector and keeps audio artwork. Unknown source capability remains usable. A YouTube art track can still advertise a video stream; the user deferred further changes to this audio/video behavior. Do not silently claim that all static-art tracks are detected. | `Capabilities.kt`, `PlaybackStatus.kt`, `MsePlayer.kt`, player catalog in `boot.ts`, `PlayerQuality` in `NowPlaying.kt` |
| Caption/video metadata ownership | Retained protocol code scopes cached responses, pictures, and catalogs to the active video ID, including prepared-item promotion; stale events must not update the next/current track incorrectly. Caption ownership applies to active playback and prefetched responses. | `engine-js/src/logic/player/{boot,captions,push}.ts`, `MsePlayer.kt` |
| Error handling | Preserve deployed error-code identities and the error ledger. Runtime failures remain localized categories or stable codes, with technical detail in local logs. Do not silently swallow cancellation or receiver/startup failures. | `error-codes.txt`, module error classification and local logging |
| Development safeguards | `-Pymusic.strictEngineStints=false` is an explicit software-emulator debug override for slow engine tasks; ordinary debug builds retain strict checks. Keep generated output and temporary investigations ignored. Never turn the override into a general release behavior. | App Gradle files, `buildSrc/src/main/kotlin/BuildIdentity.kt`, `.gitignore`; [build details](BUILDING.md) |
| Localization and documentation | Localize new UI strings across the existing locales. Keep user-facing README concise; dated changes in CHANGELOG, implementation contracts in module docs, visual contracts in DESIGN/.impeccable, and merge decisions here. Do not restore upstream-specific links or turn README into an installation manual. | `commonUi/src/commonMain/composeResources/values*/strings.xml`, documentation and existing test fixtures |

## Resolve behavior, not just text conflicts

Read the fork's full diff against the recorded base, then compare upstream's changes in each affected area:

```sh
git diff --stat 80956c5c45ad09def58bb2f50c12329cdbba092d..master
git log --oneline 80956c5c45ad09def58bb2f50c12329cdbba092d..master
git log --oneline master..upstream/master
```

For high-risk overlaps, use `git log -p -- path` and inspect callers on both sides. In particular:

- Apply playback fixes across Kotlin, JS/SABR, and the native-host boundary together. Restoring one old reset or startup deadline can discard prepared audio even when the UI looks correct.
- Preserve the current/prepared-next identity through shuffled page arrivals and queue edits. Never restore random Next selection on top of a visibly shuffled list.
- Preserve WEB_REMIX rating commands and library invalidation together. A successful write with stale lists is still broken behavior.
- Keep app-ID/signing/provider changes together. Preserve original source namespaces to avoid needless upstream merge conflicts.
- Keep Cast SDK main-thread calls, routing intent, HTTP compatibility, volume routing, and application theme wiring together. Replacing only the picker does not implement playback.
- Treat updater, reporting, aspect-stretching, and release-asset removals as deliberate product decisions. Upstream additions must not silently re-enable them.

If upstream now implements an equivalent fix, compare the behavior and tests before dropping the fork implementation. Record the replacement and the new upstream baseline here. Keep intentional differences rather than applying a blanket `ours`/`theirs` resolution.

## Suggested merge workflow

Create the integration checkout under `/tmp`, leaving the main source checkout clean:

```sh
git fetch upstream
git worktree add -b merge/upstream-review /tmp/ymusic-upstream-review master
git -C /tmp/ymusic-upstream-review merge --no-commit --no-ff upstream/master
```

Use a fresh branch/path if those already exist. Resolve conflicts in the integration checkout, review the preservation table, run checks there, and inspect the final diff before committing. Follow [AGENTS.md](AGENTS.md) and module docs. Keep commits logical. Publishing and version changes follow the user's explicit release instructions; a merge itself is not permission to change the release policy.

After validation, merge the reviewed integration branch into master. Update the recorded upstream SHA, this guide, module contracts, and CHANGELOG in the same work. Do not store machine-specific paths, credentials, generated binaries, or diagnostic account data in the commit.

## Required verification after an upstream merge

Run formatting and relevant module checks from the disposable checkout. The current release workflow is the authoritative full Android/Windows/Linux build and compatibility matrix. Use [BUILDING.md](BUILDING.md) for toolchain and native dependency requirements. At minimum:

```sh
./gradlew spotlessApply
./gradlew :commonUi:jvmTest :player:jvmTest :dynamic-engine:jvmTest spotlessCheck
```

For JS changes, also run the engine's strict TypeScript and Closure build and follow the [backward compatibility checklist](engine-js/README.md). Keep wire fields quoted/optional where required. Bump only the affected bundle revisions; native changes require rebuilding matching native libraries. Do not ship an emulator-only APK or reuse old native binaries after changing native code.

| Check | What must hold |
| --- | --- |
| Installation | APK label/ID/code/ABI and signing certificate match the fork; update installs over this fork and does not overwrite upstream. Inspect merged manifest providers/services. |
| Music navigation | Phone opens YT Music Home; car root order is music-first. Initial section-list expansion, playlist-only shelf, merged mixes, dynamic shelves and Show more continuation remain correct. |
| Ratings | Signed-in Music like/unlike/re-like synchronizes with YouTube Music and both app surfaces. Failure rolls back and late car actions cannot rate another song. |
| Playback transition | Play several queued tracks to completion, shuffled and ordered. Next is already partly prepared, does not stay at zero, and does not routinely reset/reinitialize. Test pause, skip, seek, queue replacement and lost-network recovery. |
| Buffering | All six settings, five listening-second grace, short-track end coverage, unchanged next-track target, cancellation, segment rounding and bounded memory. |
| Queue | 1,000 unique metadata entries, sequential visible shuffle, Off restoration preserving current, re-shuffle, duplicates, edits, paging and preserved prepared next. |
| Cast | Physical speaker/stereo group and TV audio, volume keys/slider/mute, screen-off playback, route failure, pause/seek during transfer, This device return and subsequent local progress. |
| SponsorBlock | Valid segments, empty response, service error/timeout fallback, category filtering, cancellation, and beginning/end skips without a startup stall. |
| UI | Phone portrait, compact/large text, tablet landscape, fullscreen/rotation/Back, queue gestures, natural video proportions and fixed metadata/timeline geometry. CC/language/style controls work; no stretch button. |
| Removed behavior | No automatic app updates, crash/stat sending or upstream project/contact actions. |
| Downloads | Select/bulk actions, quality/format handling, cancel/retry, and concurrency stays three. |
| Release | All platform/compatibility/test jobs succeed. Exactly the APK/EXE/DEB assets exist and match the built commit/digests; no fixtures or diagnostics in release APK. |

Emulator UI with fake media is useful for layout and queue logic, but does not establish live YouTube, a real Cast receiver, account synchronization, or Android Auto behavior. Record the scope of each check. The user has confirmed earlier local transitions and speaker audio; new receiver/account/car changes still require device checks. The user confirmed captions work and requested restoration; the earlier controlled-test limitation was not proof that captions were broken.

## Keep a useful history

The current state is authoritative; reverted experiments are not additional requirements. In particular, the temporary 4.0.18/version-code bump was reverted, five concurrent downloads were not adopted, video-to-TV casting was discussed but not implemented, and fullscreen was subsequently changed to remove stretching. The temporary caption removal was reversed; retain captions and CC. Avoid reintroducing any of these through an old commit or an upstream default.

Use the changelog for dated additions, this preservation map for merge review, module READMEs for detailed contracts, and Git history for exact patch provenance. When dropping or replacing a fork patch, record which upstream commit supersedes it and which regression checks passed.
