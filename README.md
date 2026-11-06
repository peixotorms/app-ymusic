# YMusic

A music player for **Android, Windows, and Linux**. Stream from YouTube and YouTube
Music, play local files, and download music for offline listening.

This is a personal fork of [YMusic V4](https://github.com/ymusic-io/YMusicV4), based
on version 4.0.16.

## Features

- Browse and search YouTube and YouTube Music, manage playlists, and listen in the background.
- Sign in to access your library and like or unlike tracks from Now Playing.
- Use Android Auto to browse your library and control playback from the car.
- Cast audio from Android to compatible Google Cast speakers and speaker groups.
- Skip SponsorBlock segments using the public service, with YMusic as a fallback.
- Choose 30 seconds, one minute, or two minutes of audio buffering ahead.
- Download music and select multiple downloads to remove entries or delete files.
  Up to three downloads run at once.

## Install and update

Download the build for your device from [Releases](https://github.com/peixotorms/app-ymusic/releases):
Android APKs, Windows installers, or Linux Debian packages.

Updates are installed manually. The app does not check for or download app updates.

For sideloaded Android builds, Android Auto may require **Unknown sources** in its
developer settings on the phone. Google Cast requires the phone and speakers to
be on the same network.

## About this fork

This fork adds Google Cast audio support on Android, rating controls in Now Playing,
bulk download selection, adjustable audio buffering, and a public SponsorBlock
lookup with YMusic fallback. Android Auto support comes from the original project.

See the [changelog](CHANGELOG.md) for dated additions and changes, and
[build instructions](BUILDING.md) for development setup.

## License

[GPL-3.0](LICENSE), except `kmpCommon/` and `kmpHttp/`, which use
[Apache-2.0](kmpCommon/LICENSE) ([HTTP library license](kmpHttp/LICENSE)).
