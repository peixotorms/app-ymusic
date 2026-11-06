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
  body-artist:
    fontSize: "15sp"
  body-metadata:
    fontSize: "13sp"
    lineHeight: "19sp"
  body-list:
    fontSize: "11.5sp"
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
    size: "80dp"
  playlist-play:
    rounded: "{rounded.circle}"
    size: "56dp"
  music-artwork:
    rounded: "{rounded.music-artwork}"
    size: "52dp"
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

YMusic uses real cover artwork as the focal material, with readable native typography and direct playback controls. The player and playlist composition follows the supplied Symfonium references while retaining YMusic's actions and navigation.

This is a Compose Multiplatform system with Android as the primary surface. Measurements are native dp and sp. The tokens describe implemented values; runtime palette roles remain authoritative where colors depend on artwork or user settings.

**Key Characteristics:**

- Large artwork, rounded geometry, and restrained tonal surfaces.
- One prominent circular transport control with compact secondary actions.
- Dark music surfaces alongside browsing that respects the saved theme.
- Native interaction, scalable text, and retained loading and error states.

## Colors

The saved palette is neutral with a configurable accent; music surfaces derive their accent from the current artwork.

### Primary

`accent-default` is the saved-theme fallback. A custom accent is darkened to a maximum luminance of 0.3. `ArtworkTheme` uses its own runtime accent; the fallback artwork tint is a sampling fallback, not a permanent player color.

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

**The Theme Boundary Rule.** Player and playlist surfaces stay dark. Browsing and the production mini-player inherit the saved app palette; the mini-player is outside the per-screen artwork theme.

## Typography

The system uses the platform sans-serif face through Compose and Material defaults: Roboto on Android, with the platform default elsewhere. There is no separate decorative display font. Explicit music roles are recorded above; unspecified weight, tracking, and line height inherit native defaults.

The playlist headline is centered over the hero and permits three lines. The track headline is left aligned and permits two. Compact playlist rows use `title-list` and `body-list`, each limited to one line with ellipsis. Queue rows use a semibold 12.5sp title and 11sp secondary line in their compact form; the mini-player pairs `title-mini` with 12sp secondary text. Section and dialog headings share `title-section`.

**The Native Type Rule.** Keep text in sp and retain the existing wrapping and ellipsis behavior so larger system text remains usable.

## Layout

The shared spacing steps live in `DesignSpacing`. Browsing uses a 16dp edge margin and 8dp gutter, increasing to 24dp and 16dp at 840dp. Standard feeds switch from square rows below 320dp to wide rows below 470dp, then to a grid whose cards stay at most 324dp wide. Compact music lists remain single-column.

The portrait player respects safe drawing insets. Artwork has 20dp horizontal and 12dp vertical outer padding and caps at 520dp; track details and seeking cap at 560dp. Transport caps at 480dp. When width exceeds height, artwork and the scrolling controls/queue occupy equal columns. Audio artwork is square; inline video uses a 16:9 slot with the existing native aspect-fit renderer.

Playlist hero artwork spans the available width with height capped at 440dp. Overlay navigation respects the status bar. Metadata and actions follow the hero; action rows wrap when needed. Lists reserve the measured mini-player inset. Dialogs normally span 280–560dp and constrain their scrolling body to the available window height.

## Elevation & Depth

Depth comes primarily from tonal fills, artwork fades, and scrims. Playlist artwork fades from a dark top scrim through transparency to black; overlay controls use circular black scrims. The mini-player has a thin gradient shadow above it. Queue dragging uses 4dp elevation, dialog bars animate between 0dp and 6dp when content can scroll beneath them, and the seek thumb uses 1dp elevation.

**The Tonal Depth Rule.** Preserve the uninterrupted player ground and reserve elevation for the existing floating, dragging, and scrolling states.

## Shapes

Material's five corner roles use the `material-*` tokens. Music artwork, the mini-player, and queue artwork use their named exceptions. Transport buttons are circular; search and filter controls are pills. Playlist hero artwork remains rectangular and full width. Reuse native vector icons, including the outlined previous/next vectors, with no text-glyph substitutes.

## Components

### Playback controls

The portrait play/pause circle uses `player-play`; landscape reduces it to 72dp. The main icon is 38dp, and outlined skip icons are 32dp inside 48dp targets. Heart, overflow, repeat, shuffle, and Cast have 48dp targets. Active toggles use the artwork accent. The player has no top toolbar; system Back and swipe-down dismissal remain available. Download, sleep timer, and other secondary actions stay in overflow.

### Seek and format

The played track is a wave during playback and straight while paused. Its stroke is 3dp, wavelength 22dp, amplitude 3dp, and cycle 1600 ms with linear easing. The 13dp thumb sits in a 40dp bar. Animation runs only while playback is active and the window is visible, and respects Compose's motion scale. Timestamps flank the centered quality selector. Seeking commits on release; keyboard and accessibility seeking, buffering, and skip markers remain available.

### Playlist and queue rows

Playlist rows use `music-artwork`, 16dp between artwork and text, and `ListRowVertical` insets; duration and overflow follow the text. Compact queue artwork is 48dp with `queue-artwork` corners. The current queue row uses accent at 0.10 alpha and a 12dp corner. The sticky Up next header groups repeat, shuffle, and Cast and locates the current item when tapped. Reordering and removal remain native interactions.

### Mini-player

The mini-player is inset 12dp horizontally and 8dp vertically, with 64dp artwork, a 2dp progress line, title/artist, pause, and next. Its fill is the saved palette's SurfaceElevated, including when displayed above a dark playlist. Tapping expands it; the existing downward dismissal stops playback and clears the queue.

### Browsing controls and dialogs

Search uses a filled pill with an accent cursor and a clear action when nonempty. Filter chips have a 1dp outline; selected chips use the saved accent and white text. Native Material indication supplies interaction feedback. Drawer navigation and section tabs remain the browsing navigation; selected tabs use an accent icon and a 2dp accent indicator. Elevated dialogs use a Surface body, tonal title/action bars, and dividers, with scroll-dependent shadows.

The sidecar includes compact illustrations of these saved-theme primitives. Its preview CSS maps 1dp/sp to 1px only for display; it does not define native layout, font scaling, ripple behavior, or artwork colors. Synthesized tonal ramps are omitted because the implementation supplies role palettes and runtime interpolation instead.

Source of truth: [Theme.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/Theme.kt), [ArtworkTheme.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/ArtworkTheme.kt), [NowPlaying.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/NowPlaying.kt), [SlimSeekBar.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/SlimSeekBar.kt), [DetailScreens.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/DetailScreens.kt), and the shared browsing components referenced by [App.kt](commonUi/src/commonMain/kotlin/io/ymusic/ui/App.kt).

## Do's and Don'ts

### Do:

- **Do** resolve colors through the active palette and preserve the artwork-theme boundary.
- **Do** retain safe insets, scalable text, native Back, and accessible playback actions.
- **Do** use real loaded artwork and preserve placeholders, buffering, and error states.
- **Do** keep the primary transport prominent and secondary controls grouped.

### Don't:

- **Don't** turn one cover's accent into a fixed brand palette.
- **Don't** apply the player's dark theme to all browsing or to AppRoot's mini-player.
- **Don't** replace native video with a cropped cover or remove its captions and fullscreen access.
- **Don't** add decorative containers or typography that compete with the artwork.
