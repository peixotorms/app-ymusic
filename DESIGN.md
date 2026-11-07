---
name: YMusic
description: Native music controls with prominent artwork, dark music surfaces, and theme-aware browsing.
colors:
  accent-default: "#E8732A"
  artwork-fallback: "#65513D"
  artwork-ink-strong: "#FFFAF6"
  artwork-ink: "#F2ECE7"
  artwork-on-accent: "#151311"
  black: "#000000"
  white: "#FFFFFF"
  player-ink-strong: "#F5F5F5"
  player-ink: "#EDEDED"
  player-ink-secondary: "#D0D0D0"
  dark-surface: "#101010"
  dark-surface-dim: "#1B1B1B"
  dark-surface-elevated: "#202020"
  dark-field: "#292929"
  dark-ink-strong: "#EDEDED"
  dark-ink: "#E2E2E2"
  dark-ink-secondary: "#9A9A9A"
  dark-ink-soft: "#B2B2B2"
  dark-ink-muted: "#C6C6C6"
  dark-ink-faint: "#7E7E7E"
  dark-icon-dots: "#8E8E8E"
  dark-outline: "#5C5C5C"
  dark-divider: "#2A2A2A"
  light-surface-dim: "#FAFAFA"
  light-surface-elevated: "#F5F5F5"
  light-field: "#F1F1F1"
  light-ink-strong: "#222222"
  light-ink: "#333333"
  light-ink-secondary: "#8A8A8A"
  light-outline: "#CFCFCF"
  light-divider: "#E6E6E6"
  skip-segment: "#FFC107"
typography:
  headline-playlist:
    fontFamily: "sans-serif"
    fontSize: "30sp"
    fontWeight: 700
    lineHeight: "36sp"
  headline-track:
    fontFamily: "sans-serif"
    fontSize: "23sp"
    fontWeight: 700
    lineHeight: "29sp"
  title-section:
    fontSize: "17sp"
    fontWeight: 700
  title-mini:
    fontSize: "14sp"
    fontWeight: 600
  title-list:
    fontSize: "12.5sp"
    fontWeight: 600
    lineHeight: "16sp"
  title-queue:
    fontSize: "15sp"
    fontWeight: 600
  body-artist:
    fontSize: "15sp"
  body-metadata:
    fontSize: "13sp"
    lineHeight: "19sp"
  body-list:
    fontSize: "11.5sp"
  body-queue:
    fontSize: "13sp"
  label-chip:
    fontSize: "13.5sp"
  label-quality:
    fontSize: "12sp"
    fontWeight: 500
  label-time:
    fontSize: "11sp"
    fontWeight: 600
rounded:
  material-extra-small: "12dp"
  material-small: "16dp"
  material-medium: "20dp"
  material-large: "24dp"
  material-extra-large: "28dp"
  player-artwork: "22dp"
  mini-player: "18dp"
  music-artwork: "8dp"
  queue-artwork: "6dp"
  pill: "999dp"
  circle: "50%"
spacing:
  Space2: "2dp"
  Space4: "4dp"
  Space8: "8dp"
  Space16: "16dp"
  Space24: "24dp"
  Space32: "32dp"
  Space48: "48dp"
  ListRowVertical: "10dp"
  SettingsRowVertical: "14dp"
  DialogRowVertical: "12dp"
components:
  player-artwork:
    rounded: "{rounded.player-artwork}"
  player-play:
    rounded: "{rounded.circle}"
    size: "76dp"
  player-play-landscape:
    rounded: "{rounded.circle}"
    size: "72dp"
  player-footer:
    height: "56dp"
  playlist-play:
    rounded: "{rounded.circle}"
    size: "48dp"
  music-action:
    rounded: "{rounded.circle}"
    size: "48dp"
  music-artwork:
    rounded: "{rounded.music-artwork}"
    size: "52dp"
  queue-surface:
    backgroundColor: "{colors.dark-surface}"
    textColor: "{colors.player-ink}"
  queue-current-row:
    backgroundColor: "{colors.dark-surface-elevated}"
    textColor: "{colors.player-ink}"
    typography: "{typography.title-queue}"
  queue-artwork:
    rounded: "{rounded.queue-artwork}"
    size: "56dp"
  queue-grip:
    width: "48dp"
    height: "56dp"
  mini-player-dark:
    backgroundColor: "{colors.dark-surface-elevated}"
    textColor: "{colors.dark-ink}"
    rounded: "{rounded.mini-player}"
  search-input-dark:
    backgroundColor: "{colors.dark-field}"
    textColor: "{colors.dark-ink}"
    typography: "{typography.body-artist}"
    rounded: "{rounded.pill}"
    height: "40dp"
  filter-chip-dark:
    textColor: "{colors.dark-ink-soft}"
    typography: "{typography.label-chip}"
    rounded: "{rounded.pill}"
    padding: "7dp 12dp"
  filter-chip-selected-default:
    backgroundColor: "{colors.accent-default}"
    textColor: "{colors.white}"
    typography: "{typography.label-chip}"
    rounded: "{rounded.pill}"
    padding: "7dp 12dp"
  discover-tabs-default:
    textColor: "{colors.accent-default}"
  elevated-dialog:
    rounded: "{rounded.material-large}"
---

# Design System: YMusic

## Overview

**Creative North Star: "Artwork Leads"**

YMusic uses real cover artwork as the focal material, with readable native typography and direct playback controls. Playback occupies a fixed viewport, with a separate queue reached by a gesture or button. The player and playlist composition follows the supplied Symfonium references and the user's device feedback.

This is a Compose Multiplatform system with Android as the primary surface. Measurements are native dp and sp. The tokens describe implemented values; runtime palette roles remain authoritative where colors depend on artwork or user settings.

**Key Characteristics:**

- Large artwork, rounded geometry, and restrained tonal surfaces.
- One prominent circular transport control, compact secondary actions, and a separate neutral queue.
- Dark music surfaces alongside browsing that respects the saved theme.
- Native interaction, scalable text, and retained loading and error states.

## Colors

The saved palette is neutral with a configurable accent. Player backdrops and playlist surfaces derive color from artwork. Now Playing controls and the separate queue use neutral near-white foregrounds and the fixed default orange for active states.

### Primary

`accent-default` is the saved-theme fallback. A custom accent is darkened to a maximum luminance of 0.3. Playlist `ArtworkTheme` uses its own runtime accent; the fallback artwork tint is a sampling fallback, not a permanent player color.

The artwork loader requests a small sample, rejects transparent and near-black/near-white pixels, and selects a saturation-weighted dominant color bucket. The resulting tint transitions over 600 ms. The following formulas use Compose's `Color.lerp`:

| Artwork role | Derivation |
|---|---|
| Accent | `lerp(tint, White, 0.64)` |
| Secondary / muted text | `lerp(tint, White, 0.72)` |
| Soft / faint text | Secondary at 0.68 / 0.85 alpha |
| SurfaceDim / SettingsCard | `lerp(tint, Black, 0.80)` |
| SurfaceElevated | `lerp(tint, Black, 0.84)` |
| FieldPill | `lerp(tint, Black, 0.70)` |
| OutlineSoft / Divider | Accent at 0.24 / 0.12 alpha |

The player backdrop begins at `lerp(tint, Black, 0.25)`, darkened further until its luminance is at most 0.16. It reaches black at 60% of the background height. Playlist pages use a black ground beneath their separate artwork hero fade. Fixed artwork text and on-accent colors are recorded in the frontmatter.

### Neutral

Saved dark browsing uses `dark-surface`, `dark-surface-dim`, and `dark-surface-elevated`. Light browsing uses white with the corresponding light surfaces. Text, fields, outlines, and dividers resolve through `DesignColors`. AMOLED sets Surface, SurfaceDim, SurfaceElevated, and SettingsCard to black; it retains the other dark roles. `skip-segment` identifies SponsorBlock intervals.

The queue uses the fixed `PlayerControlColors` palette over `DarkDesignColors`: `dark-surface` ground, `dark-surface-elevated` current-row highlight, and neutral text. Selected repeat and shuffle and a connected Cast route use `accent-default`; this palette is independent of artwork and the user's saved accent. Main and header controls remain near-white, with grey for disabled states. Queue row overflow keeps the quieter dark-icon-dots role.

**The Theme Boundary Rule.** Player and playlist surfaces stay dark with artwork-derived backdrops. Player controls use fixed white/orange roles; fullscreen retains its original overlay palette. The queue uses the fixed dark palette. Browsing and the production mini-player inherit the saved app palette; the mini-player is outside the per-screen artwork theme.

## Typography

The system uses the platform sans-serif face through Compose and Material defaults: Roboto on Android, with the platform default elsewhere. There is no separate decorative display font. Explicit music roles are recorded above; unspecified weight, tracking, and line height inherit native defaults.

The playlist headline is centered over the hero and permits three lines. The track metadata block reserves capacity for two title lines and two artist lines. Text flows naturally with a 6dp gap between title and artist; unused capacity stays below it. Both ellipsize at two lines. Heart and overflow align at the top of the fixed block. Compact playlist rows use `title-list` and `body-list`; the separate queue uses the larger `title-queue` and `body-queue`. Both list styles limit each text line to one line with ellipsis. Queue duration sits in its own 12sp column. The mini-player pairs `title-mini` with 12sp secondary text. Section and dialog headings share `title-section`.

**The Native Type Rule.** Keep text in sp and retain the existing wrapping and ellipsis behavior so larger system text remains usable.

## Layout

The shared spacing steps live in `DesignSpacing`. Browsing uses a 16dp edge margin and 8dp gutter, increasing to 24dp and 16dp at 840dp. Standard feeds switch from square rows below 320dp to wide rows below 470dp, then to a grid whose cards stay at most 324dp wide. Compact music lists remain single-column.

The portrait player does not scroll. A shared 16dp horizontal gutter and 8dp vertical padding sit inside safe drawing insets. The centered content column caps at 520dp; track details and seeking fill that column. Transport caps at 480dp without another horizontal inset. Flexible gaps keep the footer at the bottom.

Artwork adapts to both viewport height and font scale. After safe insets and outer padding, controls reserve `250dp + 130dp × fontScale`. Available art height is the remaining height, clamped to zero. Both audio artwork and inline video use the same square slot, no wider than the content column or available art height. The video retains its native aspect ratio within the square black region, centered vertically and horizontally; unknown dimensions use 16:9. The title/artist reservations and the square slot keep seeking and transport fixed across track and mode changes.

The shared player uses two equal columns only when width is at least 600dp and exceeds height: artwork on the left, fixed controls on the right, with 16dp outer padding and a 24dp gap. Android ordinary screens stay portrait below 600dp smallest width, except TV devices. Tablets and TV retain system rotation. Explicit fullscreen video can use sensor rotation or the user's rotation lock; exiting restores the ordinary device policy.

Playlist hero artwork spans the available width with height capped at 440dp. Overlay navigation respects the status bar. Metadata and actions follow the hero; action rows wrap when needed. Lists reserve the measured mini-player inset. Dialogs normally span 280–560dp and constrain their scrolling body to the available window height.

## Elevation & Depth

Depth comes primarily from tonal fills, artwork fades, and scrims. Playlist artwork fades from a dark top scrim through transparency to black; overlay controls use circular black scrims. The mini-player has a thin gradient shadow above it. Queue dragging uses 4dp elevation, dialog bars animate between 0dp and 6dp when content can scroll beneath them, and the seek thumb uses 1dp elevation.

**The Tonal Depth Rule.** Preserve the uninterrupted player ground and reserve elevation for the existing floating, dragging, and scrolling states.

## Shapes

Material's five corner roles use the `material-*` tokens. Music artwork, the mini-player, and queue artwork use their named exceptions. The primary transport is circular; playlist action icons are bare within circular interaction bounds. Search and filter controls remain pills. Playlist hero artwork remains rectangular and full width, and the current queue highlight has no rounded container. Reuse native vector icons, including the outlined previous/next vectors, with no text-glyph substitutes.

## Components

### Playback controls

The transport order is Repeat, Previous, Play/Pause, Next, Shuffle. Its play circle uses `player-play` in portrait and `player-play-landscape` in landscape. The main icon is 38dp, and outlined skip icons are 32dp inside 48dp targets. Active repeat and shuffle use the fixed default orange; the active main playback control also turns orange. Inactive controls are near-white.

The bottom strip has five stable slots: Sleep timer, Speed, queue chevrons, Equalizer, Cast. Each has a 48dp target within `player-footer`; an unavailable Cast action leaves its slot empty. Sleep, speed/pitch, audio effects, and connected Cast tint with the fixed default orange when active; the queue chevrons remain neutral. Download, captions, SponsorBlock, fast seek, and other secondary actions remain in overflow.

Heart and overflow keep separate 48dp targets while their glyphs shift inward by 6dp each. The player has no top toolbar. The chevrons or an upward swipe open the queue; system Back or a downward dismissal returns from the queue before closing the player. Desktop Escape follows the same queue/fullscreen priority.

### Inline video

The native video region shows rotation where supported and fullscreen viewing controls, each with a separate `music-action` target (48dp). Fit/fill controls are absent in inline and fullscreen video. Viewing icons hide after three seconds of ready playback and a tap restores them. Inline loading and paused states do not start this timer; fullscreen keeps its existing timing. The square black region masks the previous picture until the new video begins; a play/pause glyph sits inside the loading ring. Captions, buffering, and errors remain inside the region, and enabled captions also appear over audio artwork. Track details, seeking, and transport appear once on the page. Caption selection is available through overflow and fullscreen. Fullscreen keeps captions, transport, seeking, rotation, and exit controls without aspect-ratio stretching.

### Seek and format

The played track is a wave during playback and straight while paused. Its stroke is 3dp, wavelength 22dp, amplitude 3dp, and cycle 1600 ms with linear easing. The 13dp thumb sits in a 40dp bar. Animation runs only while playback is active and the window is visible, and respects Compose's motion scale. Timestamps occupy the two edges independently of the fixed centered quality selector. The initial elapsed label is `00:00` and unknown duration is `--:--`. Only an explicit no-video capability disables the audio-mode selector for that song; unknown capability leaves it enabled. Seeking commits on release; keyboard and accessibility seeking, buffering, and skip markers remain available.

### Playlist actions and rows

The action row uses equally sized bare 24dp icons in 48dp targets: Save and Download when applicable, then Repeat, Shuffle, and Play. Selected Save, Repeat, and Shuffle use the artwork accent; Play uses Ink without a filled circle. Share and Create local playlist are absent from this header, and playlist pages omit the feed filter strip. Overlay Back, Search, and Cast retain their artwork scrims.

Playlist rows use `music-artwork`, 16dp between artwork and text, and `ListRowVertical` insets; duration and overflow follow the text.

### Queue

The queue is a separate full-size surface with safe insets, a fixed header, and an independently scrolling list. Collapse, Back, Escape, or a downward pull returns to the player. There is no search or filter row. Up next locates the current item; the header groups Repeat, Shuffle, and Cast. Repeat, Shuffle, and Cast retain adjacent 48dp targets. Their glyphs form a compact group: Repeat shifts inward by 12dp, Cast by −12dp, and Shuffle by −1.5dp to balance the different vector widths. These actions show active color without a circular press indication or clipping; their touch areas remain separate. Ordinary Now Playing retains native press indications; fullscreen retains its existing no-indication behavior. Shuffle permutes the visible queue once and starts at the top; playback then follows that order. Turning it off restores the original session order while keeping the current item, and re-enabling it generates a new order.

Rows use `queue-artwork`, 8dp gaps, 6dp vertical insets, and an 8dp end inset. The drag grip is a 26dp vector centered in `queue-grip`. Title and artist have their own column; duration and the 48dp overflow target follow it. The current row uses `queue-current-row` without an accent wash. Reordering, swipe removal, and loading/error/retry rows remain available.

### Mini-player

The mini-player is inset 12dp horizontally and 8dp vertically, with 64dp artwork, a 2dp progress line, title/artist, pause, and next. Its fill is the saved palette's SurfaceElevated, including when displayed above a dark playlist. Tapping expands it; the existing downward dismissal stops playback and clears the queue.

### Browsing controls and dialogs

Search uses a filled pill with an accent cursor and a clear action when nonempty. Filter chips have a 1dp outline; selected chips use the saved accent and white text. Native Material indication supplies interaction feedback. Drawer navigation and section tabs remain the browsing navigation; selected tabs use an accent icon and a 2dp accent indicator. Elevated dialogs use a Surface body, tonal title/action bars, and dividers, with scroll-dependent shadows.

The sidecar includes compact illustrations of the saved-theme primitives and neutral queue row. Its preview CSS maps 1dp/sp to 1px only for display; it does not define native layout, font scaling, ripple behavior, or artwork colors. Synthesized tonal ramps are omitted because the implementation supplies role palettes and runtime interpolation instead.

Source of truth: [Theme.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/Theme.kt), [ArtworkTheme.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/ArtworkTheme.kt), [NowPlaying.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/NowPlaying.kt), [ActionIcon.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/ActionIcon.kt), [SlimSeekBar.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/SlimSeekBar.kt), [DetailScreens.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/DetailScreens.kt), Android orientation in [Platform.android.kt](commonUi/src/androidMain/kotlin/io/ymusic/ui/Platform.android.kt), and the shared browsing components referenced by [App.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/App.kt).

## Do's and Don'ts

### Do:

- **Do** resolve colors through the active palette and preserve the artwork-theme boundary.
- **Do** retain safe insets, scalable text, native Back, and accessible playback actions.
- **Do** use real loaded artwork and preserve placeholders, buffering, and error states.
- **Do** keep the primary transport prominent and secondary controls grouped.
- **Do** keep playback fixed in the viewport, with a shared safe gutter and a separately reachable queue.

### Don't:

- **Don't** turn one cover's accent into a fixed brand palette.
- **Don't** apply the player's dark theme to all browsing or to AppRoot's mini-player.
- **Don't** replace native video with a cropped cover or remove its captions and fullscreen access.
- **Don't** add decorative containers or typography that compete with the artwork.
- **Don't** duplicate title, seek, or transport controls over inline video, or add queue rows and filter chips to the player viewport.
