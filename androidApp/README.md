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
