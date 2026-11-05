# `:androidApp`

Module detail for the one-line summary in the root [`AGENTS.md`](../AGENTS.md) — read this before working inside the module.

- **What it is** — the Android product shell: plain AGP 9 application module booting the same shared `App` over ExoPlayer + MSE YouTube. The WebView is the login browser and the cookie store (`WebViewCookieJar` wraps `CookieManager`). Settings → Link devices scans a desktop QR (Play services code scanner, no CAMERA permission), confirms a matching code, unlocks with PIN/biometric, and POSTs the encrypted jar.
  - **`com.android.application` cannot combine with the KMP plugin since AGP 9** — plain layout `src/main/`, built-in Kotlin, `org.jetbrains.compose` plugin applied for dependency substitution.

- **Home-screen widgets** — `PlayerWidgets` (Small / Compact / Large / Square, plain `RemoteViews`): live from `player.status × queue` once the graph is up; with an empty queue they show the newest "Play again" session and Play resumes it (`PlaybackService.playOrResume`); the play button sends the explicit Play/Pause its icon shows, never a toggle. Blur, palette and rounded crops are baked into bitmaps (RemoteViews can't), and the last track + its art are snapshotted to `filesDir/widget` so a refresh in a dead process still renders. Buttons are foreground-service starts of `PlaybackService` (a tap may revive the process).

- **compileSdk** — 36 (okhttp 5.4 demands it for app modules).

- **Not wired yet** — media store/SAF scanner.

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

The Cast button in Now Playing uses Google's device picker and default media receiver, including
Cast audio devices and speaker groups. The Nest stereo pair should be discovered as its configured
pair name when phone and speakers share a reachable Wi-Fi network; guest-network isolation or a VPN
can prevent the receiver from reaching the phone.

`cast/` wraps both playback engines so the existing queue, transport controls, notifications, sleep
timer, and history use the remote status/progress. Connecting transfers the current position and
play intent; disconnecting restores local playback. Receiver suspension keeps the remote route
selected to avoid starting a second audible copy on the phone. Local playback retains its native
audio-session handle for system effects; casting exposes audio-only capabilities.

YouTube audio uses the existing authenticated SABR resolver and downloads AAC/M4A to app cache,
then an HTTP server bound to the phone's receiver-facing address serves that one file at an
unguessable URL with byte-range support. Cookies and account tokens stay on the phone. The server
and temporary audio are retired on replacement, stop, or disconnect. Cast SDK notifications and
media sessions are disabled because `PlaybackService` already supplies those controls.

Current limits: each track is fully buffered before the receiver starts, so track changes may pause;
live YouTube streams are unsupported; local media is copied without transcoding, so receiver codec
support applies. Receiver speed is limited to 0.5–2×, and phone equalizer/pitch effects do not process
remote audio. The phone must remain reachable while casting. Runtime errors surface through the
existing player error UI. Real Nest playback, pair discovery, reconnect, screen-off playback, and
long queues need hardware validation; HTTP/routing checks and Android compilation alone cannot
establish receiver compatibility.

## App updates

App updates are installed manually by sideloading. The Android shell does not create an app updater,
and declares neither an update-install receiver nor the package-install permission. The shared UI
hides its update check and automatic-download controls when no updater is supplied.

Use the same application ID and signing certificate for subsequent APKs. Remote engine/player bundle
configuration is separate from app installation; it cannot replace the APK.
