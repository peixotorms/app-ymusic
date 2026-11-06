# YMusic Terms of Use & Privacy Policy

_Last updated: November 6, 2026_

## Terms of use

YMusic is free and open source. Use it fairly; it comes with no guarantees.

1. **Free and open source.** YMusic is free. Its code is released under the
   [GNU GPL v3.0](LICENSE).
2. **Not affiliated with YouTube or Google.** YMusic is an independent app. YouTube and Google are
   trademarks of Google LLC.
3. **Your responsibility.** You are responsible for how you use YMusic. That includes following the
   laws of your country and respecting copyright and the rights of creators.
4. **Downloads.** Only download content you have the right to keep, and only for your own personal
   use.
5. **Your Google account.** Signing in is optional. Keep your account safe, and only sign in on a
   trusted copy of this fork from its
   [GitHub Releases](https://github.com/peixotorms/app-ymusic/releases).
6. **No guarantees.** YMusic is provided "as is". Features may break or change at any time, for
   example when YouTube changes, and we may stop the app or its online services.
7. **Liability.** As far as the law allows, the YMusic developers are not responsible for any
   damage or loss that comes from using the app, or for how anyone misuses it.
8. **Privacy.** See the [Privacy](#privacy) section below for what we collect.

## Privacy

This fork does not send automatic crash reports or anonymous usage statistics. Local settings, history, downloads and diagnostic logs stay on your device. Online playback and account actions still require requests to the services described below.

### What stays on your device

- Your local library, playlists, downloads, play history, settings and logs.
- Your YouTube sign-in cookies. Sign-in happens on Google's own page. The app uses the cookies to make authenticated YouTube requests; it does not send them to Cast receivers or feedback services.

### What leaves your device

| What | Why | When |
|---|---|---|
| **YouTube/Google requests:** searches, content IDs, media, artwork and signed-in actions such as likes and playlist changes | To browse, stream and use your account | When you use these online features |
| **YMusic service requests:** OS/architecture, app and engine versions, and a random app-instance ID | To fetch service configuration and support configured service features | When a YMusic service is configured; automatic app-installer checks and downloads are disabled |
| **Engine/player bundle downloads** from URLs supplied by service configuration | To keep the playback engine compatible | When configured; separate from installing a new app version |
| **SponsorBlock lookups:** a short video-hash prefix to the public service, or the video ID to the YMusic fallback | To find segments to skip | When SponsorBlock is enabled in Playback settings |
| **Casting:** audio, its temporary local-network URL, title and artwork | To play on the selected receiver | Only while casting; account cookies stay on the phone |
| **Feedback:** diagnostic text containing the app-instance ID, platform and version details | To help describe an issue | Copy puts it on your clipboard; GitHub opens a prefilled issue for you to review and submit. Nothing is uploaded automatically |
| **Linking devices:** encrypted sign-in data through the configured YMusic relay | To sign in on another device | Only when you use QR device linking |

The random app-instance ID is stored in the app's private settings. It is not a hardware identifier. For QR linking, the key that unlocks the sign-in data is carried in the QR code. Network services can also see the network address making each request.

### Other services

YouTube and Google handle online music and account requests under [Google's Privacy Policy](https://policies.google.com/privacy). SponsorBlock, the configured YMusic service, media/artwork hosts and GitHub handle requests for their respective features. This fork has no Sentry integration or background usage-stat sender.

### Deleting your data

Uninstalling removes the app's private data. Downloaded files saved to a shared folder, changes saved to your Google account and issues posted on GitHub remain until you remove them from those locations.

## Changes

If these terms or this policy change, we'll update this page and the date at the top. If you keep
using YMusic after a change, you accept the new version.

## Contact

Open an issue at [github.com/peixotorms/app-ymusic/issues](https://github.com/peixotorms/app-ymusic/issues).
