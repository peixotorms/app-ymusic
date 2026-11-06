# YMusic — personal fork

A music player for streaming from YouTube and YouTube Music, playing local files,
and saving music for offline listening. It supports playlists, background playback,
YouTube sign-in, and Android Auto.

Based on [YMusic V4](https://github.com/ymusic-io/YMusicV4), starting from version
4.0.16. This fork is maintained for personal use. Release builds target **Android,
Windows, and Linux**. Shared macOS and iOS code remains in the project.

## Changes in this fork

### 2026-11-06

- **Audio buffering:** choose 30 seconds, one minute, or two minutes ahead in
  Settings → Playback. Playback starts normally; after five seconds of listening,
  it fills toward the selected target. Refill pauses between bursts, and skipping
  cancels fetching for the skipped track. Video targets 30 seconds.
- **Next song:** once the current song is buffered to its end and has at most one
  minute left, prepare about 15 seconds of the next queued song. It follows the
  same five-second grace period when playback begins.
- **SponsorBlock:** use the public SponsorBlock service first, with YMusic as a
  fallback if the lookup fails. Existing category controls still apply.

### 2026-11-05

- **Likes and dislikes:** controls in Now Playing use your signed-in YouTube account.
  Tap the active rating again to remove it.
- **Google Cast on Android:** send audio to compatible speakers, including Google
  Nest devices and speaker groups. Phone and speakers must share a network.
- **Download selection:** long-press to select downloads, select all in a filter,
  remove entries, or delete finished files. Up to three downloads run at once.
- **Manual updates:** removed app update checks and installer downloads. Install
  newer builds yourself from [this repository’s Releases](https://github.com/peixotorms/YMusicV4/releases).
- **Android Auto setup:** documented sideloaded-app discovery and checked the
  media-browser service in an Android emulator. Android Auto support was already
  present upstream; sideloaded builds may need Android Auto’s **Unknown sources**
  developer option enabled on the phone.

## A few details

Streaming starts before the whole song has downloaded. The buffer helps through
short network outages; it cannot guarantee playback when the connection stays
unavailable. The existing player cache remains separate from offline downloads.
Cast currently prepares the full track before sending it to the speaker.

Car controls, Google Nest stereo-pair playback, and signed-in rating changes still
need testing on the actual devices and account.

For build instructions, see [Building YMusic](BUILDING.md).
For release history, see [CHANGELOG.md](CHANGELOG.md).

## License

YMusic is open source under the [GNU General Public License v3.0](LICENSE), except the
foundation libraries below, which are Apache-2.0 so any project can reuse them:

| Directory | License |
|---|---|
| `kmpCommon/` | [Apache-2.0](kmpCommon/LICENSE) |
| `kmpHttp/` | [Apache-2.0](kmpHttp/LICENSE) |
| everything else | [GPL-3.0](LICENSE) |

Nothing under `kmpCommon/` or `kmpHttp/` may depend on a GPL module.

By contributing, you agree that your contribution is licensed under the same license as the
directory it modifies.
