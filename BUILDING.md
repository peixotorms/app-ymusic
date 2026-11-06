# Building YMusic

Every platform needs **JDK 17+** (CI uses 21), **Node.js** (CI uses 22) and **CMake 3.20+**. Packaging
an installer needs a full JDK that ships `jpackage` (e.g. Temurin; not a JBR). The desktop app is
built on, and for, the machine's own OS and CPU.

### FFmpeg

FFmpeg comes from [ffmpeg-build](https://github.com/ymusic-io/ffmpeg-build), checked out next to this
repository (`../ffmpeg-build`) and built locally — nothing is downloaded. Check out the commit CI builds
against (`FFMPEG_BUILD_REF` in [`release.yml`](.github/workflows/release.yml)), then build the targets
you need (its README has the details). A missing build fails with the command that produces it.

```sh
# Windows: first `git config --global core.autocrlf false`, or the build scripts break on CRLF
git clone --recursive --shallow-submodules https://github.com/ymusic-io/ffmpeg-build ../ffmpeg-build
git -C ../ffmpeg-build checkout "$(sed -n 's/^ *FFMPEG_BUILD_REF: //p' .github/workflows/release.yml)"

cd ../ffmpeg-build
scripts/build-desktop.sh            # desktop: macOS, Linux, or Windows from an MSYS2 shell
scripts/build-android.sh arm64-v8a  # Android, once per ABI: armeabi-v7a, arm64-v8a, x86, x86_64
scripts/build-ios.sh                # iOS
```

### Per platform

- **macOS** — Xcode (the iOS app, and the command-line tools for the desktop natives). ffmpeg-build:
  `brew install nasm automake autoconf libtool pkg-config meson ninja`.
- **Linux** (Debian/Ubuntu) — the app: `sudo apt install build-essential cmake libpulse-dev`, plus
  `fakeroot` to package the `.deb`. ffmpeg-build:
  `sudo apt install nasm automake autoconf libtool pkg-config meson ninja-build patchelf`.
- **Windows** — [MSYS2](https://www.msys2.org). The app's native libraries are GCC/Clang-only C, so they
  build with MinGW-w64, not MSVC. The build finds it in `C:/msys64/mingw64` (`C:/msys64/clangarm64` on
  ARM), or wherever `mingw.dir` in `local.properties` or `MINGW_HOME` points.
  - x64, in the MSYS2 MINGW64 shell: `pacman -S git make diffutils autoconf automake libtool patch mingw-w64-x86_64-toolchain mingw-w64-x86_64-make mingw-w64-x86_64-nasm mingw-w64-x86_64-pkgconf mingw-w64-x86_64-meson mingw-w64-x86_64-ninja mingw-w64-x86_64-zlib`
  - ARM64, in the CLANGARM64 shell: the same with `mingw-w64-clang-aarch64-` packages (no nasm), and
    `HOST_CC=clang` when running `scripts/build-desktop.sh`.
  - The installer needs [Inno Setup 6](https://jrsoftware.org/isinfo.php): `inno.iscc=<path to ISCC.exe>`
    in `local.properties`, or ISCC on `PATH`.
- **Android** — the Android SDK with NDK r28 (`28.2.13676358`). ffmpeg-build needs `ANDROID_NDK_ROOT`
  pointing at it, `nasm`, the autotools and `patchelf`.

### Run

```sh
./gradlew :desktopApp:run            # desktop app
./gradlew :androidApp:installDebug   # Android, on a connected device
```

The iOS app builds from `iosApp/iosApp.xcodeproj` in Xcode; its Kotlin framework comes from
`:iosAppKotlin` automatically.


## Personal releases

The release workflow builds Android APKs, Windows installers, and Linux Debian packages.
macOS packaging is disabled. It runs for a `release-v…` tag and creates a draft
in this repository’s GitHub Releases.

Android uses a separate signing identity. Set these repository Actions secrets before
running the workflow: `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Keep the same key for future updates.
For local release builds, the matching environment variables are
`YMUSIC_ANDROID_STORE_FILE`, `YMUSIC_ANDROID_STORE_PASSWORD`,
`YMUSIC_ANDROID_KEY_ALIAS`, and `YMUSIC_ANDROID_KEY_PASSWORD`.

Build from a disposable checkout and keep generated files and caches outside the source
checkout. No app updater configuration is generated; install releases manually.

For reusable build caches, run the workflow from `master` and fill in its `tag` input
with the release tag. App sources are checked out at that exact tag. GitHub isolates
caches between tags, while tagged builds can reuse caches saved on the default branch:
[cache scope documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching#restrictions-for-accessing-a-cache).
