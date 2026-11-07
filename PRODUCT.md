# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Product Purpose

YMusic is a personal fork of YMusic V4 for playing YouTube and YouTube Music, managing a music queue, and playing local media. Android is the primary product; shared Compose code also supports the existing Windows and Linux applications.

## Users and Operating Context

Listeners use the phone, Android Auto, and Google Cast speakers or televisions. Playback controls must remain usable during song changes, when the network is unreliable, and when switching playback devices.

## Capabilities and Constraints

- Preserve authenticated YouTube Music ratings, playlist actions, queue management, shuffle/repeat, downloads, SponsorBlock, sleep timers, audio effects, video quality, and existing error recovery.
- Android uses its own application identity and retains the current release version and signing identity. Updates are installed manually.
- Cast currently relays original AAC audio through the phone. Television video casting remains separate work.
- Reuse Kotlin Multiplatform and Compose. Preserve desktop keyboard controls, window resizing, and the shared native video surface.
- Keep sources free of build output, temporary fixtures, credentials, and personal account data.

## Brand Commitments

The product name is YMusic, with attribution to the upstream project. The requested redesign follows the supplied Symfonium references for player and playlist composition, adapted to this app's existing capabilities.

## Product Principles

- Playback and existing actions take priority over decoration.
- Move controls into clear, reachable groups without silently removing features.
- Use actual artwork and track metadata; preserve loading, unavailable, and error states.
- Keep account data private and do not restore automatic crash or usage reporting.

Playback captions, their menus/style settings, and fullscreen CC/aspect-stretching controls are intentionally removed. Preserve natural video proportions and the remaining playback controls.
