# `:androidApp`

Module detail for the one-line summary in the root [`AGENTS.md`](../AGENTS.md) — read this before working inside the module.

- **What it is** — the Android product shell: plain AGP 9 application module booting the same shared `App` over ExoPlayer + MSE YouTube. The WebView is the login browser and the cookie store (`WebViewCookieJar` wraps `CookieManager`). Settings → Link devices scans a desktop QR (Play services code scanner, no CAMERA permission), confirms a matching code, unlocks with PIN/biometric, and POSTs the encrypted jar.
  - **`com.android.application` cannot combine with the KMP plugin since AGP 9** — plain layout `src/main/`, built-in Kotlin, `org.jetbrains.compose` plugin applied for dependency substitution.

- **Home-screen widgets** — `PlayerWidgets` (Small / Compact / Large / Square, plain `RemoteViews`): live from `player.status × queue` once the graph is up; with an empty queue they show the newest "Play again" session and Play resumes it (`PlaybackService.playOrResume`); the play button sends the explicit Play/Pause its icon shows, never a toggle. Blur, palette and rounded crops are baked into bitmaps (RemoteViews can't), and the last track + its art are snapshotted to `filesDir/widget` so a refresh in a dead process still renders. Buttons are foreground-service starts of `PlaybackService` (a tap may revive the process).

- **compileSdk** — 36 (okhttp 5.4 demands it for app modules).

- **Not wired yet** — media store/SAF scanner.

## Fork identity and upstream merges

Preserve these settings when merging upstream changes:

| Setting | This fork | Upstream |
| --- | --- | --- |
| Launcher/application label (`src/main/res/values/strings.xml`) | `YMusic` | `YMusic V4` |
| Release application ID (`build.gradle.kts`) | `io.github.peixotorms.ymusic` | `io.ymusic.android` |
| Debug application ID | `io.github.peixotorms.ymusic.debug` | `io.ymusic.android.debug` |
| Kotlin/resource namespace | `io.ymusic.android` | `io.ymusic.android` |

The application ID identifies the installation and isolates Android-managed storage: private files, databases, preferences, WebView cookies, and caches. A typical private data root is `/data/user/0/io.github.peixotorms.ymusic/`; use Android's context APIs rather than hardcoding it. The artwork provider authority follows `${applicationId}.artwork` in the manifest and `context.packageName` at runtime. Dependency providers and the default task affinity must follow the fork's application ID too; inspect the merged release manifest after upstream merges.

Keep the source namespace and Kotlin directory structure unchanged so upstream code can merge without a package-wide rename. Manifest components resolve to their existing `io.ymusic.android` classes inside the separately identified app. Internal action strings are used with explicit app components or media sessions and do not identify the installed package. Both apps may offer to handle the same YouTube links; Android's app preferences choose the handler.

This is a new installation, with no migration or deletion of upstream data. The user signs in again, grants permissions, chooses a download folder, and enables the fork in Android Auto's launcher. Download folders selected through the system picker are shared storage: choose different folders to keep the two apps' downloaded files separate. Keep this fork's application ID and signing key stable for subsequent APK updates; version name and code do not need to differ from upstream when the application IDs differ.

Windows and Linux retain their existing installation identities and data paths; the separate-installation behavior above applies to Android.

## Android Auto discovery and testing

The app already exports `PlaybackService` as a Media3 library service and legacy media browser,
with `automotive_app_desc.xml` declaring media support. It can initialize its session before an
Activity opens; the browse tree supplies local music, YouTube feeds, playlists, and search.

For APKs installed outside Google Play, Android Auto may hide the app until **Unknown sources**
is enabled in **Android Auto's own developer settings** (separate from Android's developer settings):

1. Open Android Auto settings on the phone, expand the Version section, and tap its version info
   ten times to enable developer mode.
2. Open the overflow menu → Developer settings → Unknown sources.
3. Open Customize launcher and ensure YMusic is enabled, then reconnect the car.
4. If it is still missing, record the installed YMusic version, Android/Android Auto versions, APK
   source, and whether the playback service appears in `adb shell dumpsys media_session`.

Google documents the sideload restriction and startup scenarios in
[Testing Android apps for cars](https://developer.android.com/training/cars/testing).
A manifest change cannot override Android Auto's host setting.

For end-to-end testing, install the Android SDK's **Android Auto Desktop Head Unit** package.
Enable Android Auto developer mode on a phone, start its head unit server from the overflow menu,
connect USB debugging, run `adb forward tcp:5277 tcp:5277`, and start `desktop-head-unit`.
Follow [the DHU setup guide](https://developer.android.com/training/cars/testing/dhu).
Check discovery and library browsing before opening YMusic, signed-out browsing, playback from a
playlist, previous/next, pause/resume, seeking, queue display, artwork, and reconnect after process death.
The [Media Controller Test app](https://github.com/android/media-controller-test) can also exercise
the browser/session independently. Compilation and manifest inspection do not replace these tests.

On a software emulator, build with `-Pymusic.strictEngineStints=false` to log slow engine tasks
without the debug watchdog killing the process. The default remains strict for debug builds;
release builds already log without throwing. This flag does not change playback timeouts.

## Google Cast audio

Keep `MainActivity` on `AppCompatActivity` and both activity themes on `Theme.AppCompat.DayNight.NoActionBar` when merging upstream. The earlier SDK picker crashed with the incompatible activity theme. The current output selector uses a Compose sheet; an Android emulator check opened it and selected This device while speaker discovery was unavailable. This checks the local UI path, not a physical Cast connection.

The Cast button in Now Playing opens a Play on sheet with Local device and discovered Cast routes, including
Cast audio devices and speaker groups. Two-line cards show the device name, Android or Chromecast, and a device-specific icon. The current output is highlighted; each device has a configure button, and inactive receivers can be hidden. Sorting and per-device volume steps/visibility are saved using the Cast device ID. Show hidden devices restores access to hidden receivers. Discovery uses the Cast selector and the SDK's default media receiver. Choosing Local device ends the Cast session and transfers the queue item, position, and play/pause intent back to the phone; a receiver failure preserves that intent too. The Nest stereo pair should be discovered as its configured
pair name when phone and speakers share a reachable Wi-Fi network; guest-network isolation or a VPN
can prevent the receiver from reaching the phone.

`cast/` wraps both playback engines so the existing queue, transport controls, notifications, sleep
timer, and history use the remote status/progress. Connecting transfers the current position and
play intent; disconnecting restores local playback. Receiver suspension keeps the remote route
selected to avoid starting a second audible copy on the phone. Local playback retains its native
audio-session handle for system effects; casting exposes audio-only capabilities.

YouTube audio uses the existing authenticated SABR resolver to select default-track AAC-LC/HE-AAC in an MP4 container, then downloads it to app cache.
Then an HTTP server bound to the phone's receiver-facing address serves that one file at an
unguessable URL with byte-range support and body-free HEAD responses. Cookies and account tokens stay on the phone. The server
and temporary audio are retired on replacement, stop, or disconnect. Cast SDK notifications and
media sessions are disabled because `PlaybackService` already supplies those controls. `CastSessionPlayer` wraps that service's Media3 adapter and publishes remote device volume (0–100) while a Cast route is selected. Device-volume commands call `CastSession.setVolume`/`setMute`; receiver callbacks keep the session and picker synchronized. Physical buttons change the receiver by five percentage points by default and display its name and volume. Configure offers 1/2/3/5/10/15/20-point steps and Restore defaults. Local device uses Android's normal steps unless explicitly configured. Returning to local playback restores local volume routing; suspension retains remote routing while volume changes are unavailable.

The picker also has a volume slider, mute and volume buttons. These affect receiver device volume; the player's own audio gain remains separate. Rapid presses accumulate against the pending level, with receiver confirmation or a delayed reconciliation. A failed volume request reports E60076 without stopping playback. Foreground key interception and Media3 remote volume handling must both survive upstream merges. The picker follows the layout in the user's Symfonium reference using Compose and Material icons. [Symfonium's renderer settings](https://docs.symfonium.app/wiki/settings/settings-renderer-settings/) describe volume steps and protocol-specific options; UPnP options do not apply here, and this YouTube source requires the phone proxy rather than an optional proxy toggle.

Current limits: each track is fully buffered before the receiver starts, so track changes may pause;
live YouTube streams are unsupported; local media is copied without transcoding, so receiver codec
support applies. Receiver speed is limited to 0.5–2×, and phone equalizer/pitch effects do not process
remote audio. The phone must remain reachable while casting. Runtime errors surface through the
existing player error UI. The user confirmed speaker playback works. The updated volume controls, reconnect, screen-off playback, and
long queues need hardware validation; HTTP/routing checks and Android compilation alone cannot
establish receiver compatibility.

Cast failures distinguish audio preparation (E60072), local server/address setup (E60073), rejected or unanswered receiver loads (E60074), receiver media errors (E60075), and device-volume changes (E60076). Local logs record receiver HTTP request and served-byte counts to distinguish an unreachable sender from a media decoding failure.

Cast SDK session/device/client access must stay on the main thread. Capture the receiver address before moving socket routing and server startup to IO; `CastSession.getCastDevice()` throws off-main, before the receiver receives a media load. Handoffs read play intent from `PlaybackState.Prepared`, because the native engines update that state while the facade maintains the outer `PlaybackStatus.playWhenReady`. Preserve the last requested intent through errors and initialization, reset a destination that requires it, and surface preparation failure or timeout instead of leaving a pending transfer.

YouTube casting is audio-only in both phone modes and for both speakers and TVs. Video-on-TV casting is not implemented. The phone serves authenticated YouTube audio locally; no external proxy server is required. Symfonium's documented direct URL/proxy choices use the same receiver-fetches-media model ([proxy settings](https://docs.symfonium.app/faq/casting-issues-with-chromecast/)); its registered receiver and transcoding features are separate from this fork's default Google receiver.

Implementation references: [Google Cast sender integration](https://developers.google.com/cast/docs/android_sender/integrate), [supported media](https://developers.google.com/cast/docs/media), [SessionManager](https://developers.google.com/android/reference/com/google/android/gms/cast/framework/SessionManager), [UAMP player switching](https://github.com/android/uamp/blob/main/common/src/main/java/com/example/android/uamp/media/MusicService.kt), and [storage-cast local HTTP serving](https://github.com/ak800i/storage-cast).

## App updates

App updates are installed manually by sideloading. The Android shell does not create an app updater,
and declares neither an update-install receiver nor the package-install permission. The shared UI
hides its update check and automatic-download controls when no updater is supplied.

Use the same application ID and signing certificate for subsequent APKs. Remote engine/player bundle
configuration is separate from app installation; it cannot replace the APK.

- **Release target** — one `arm64-v8a` APK. `FfmpegBuild.androidAbis(project)` keeps APK splits and every native dependency aligned; `-Pymusic.androidAbis=x86_64` is available for development emulators.

## Car controls and navigation

YouTube Music is the first browse root, followed by Library and Discover. Android Auto controls its own remembered tab selection and presentation. Home uses the same initial section-list expansion and section arrangement as the phone, while the car renders the cards in its native layout.

The custom action order starts with Like/Unlike, then Shuffle, then Repeat (or Close while paused). Standard Media3 thumb icons let the host use consistent sizing and tint. The thumb is filled when liked and outlined when unliked or a write is pending; duplicate pending taps are ignored. Actions include the video ID so an outdated control cannot rate the next song. Shared process rating state keeps the phone and car synchronized. These are authenticated YouTube Music ratings, read and written through the WEB_REMIX music client.

## Privacy and project links

The shell has no crash-reporting SDK, telemetry initializer, background usage-stat recorder, or automatic feedback upload. Logs stay in the local log store. Previously saved reporting preferences have no sending code to enable. Project and feedback links belong to this fork; feedback offers explicit Copy and GitHub actions. Upstream email, Telegram, and donation actions are not offered.
