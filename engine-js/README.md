# `:engine-js`

Module detail for the one-line summary in the root [`AGENTS.md`](../AGENTS.md) — read this before working inside the module.

- **Scope** — the one home for ALL engine JS (pipe transport, protocol layers, scraping logic, MSE stubs, player commands, test fixtures),
  TypeScript with two ambient contracts (`types.d.ts` = the app-facing surface, `natives.d.ts` = the C FFI boundary).

- **Build pipeline** — tsc strict (the type checker) → esbuild (bundle only, `--target=esnext` — lowering breaks
  Closure's property renaming) → **Closure Compiler ADVANCED** (minify + full mangling, identifiers AND property
  names; type-based passes off — erased-types input).

- **Artifacts** — all ADVANCED:
  - `engine.js` — scrape (feed/search/login/download); remotely replaceable.
  - `player.js` — MSE YouTube player; separate QuickJS runtime.
  - `engine-test.js` — both + pipe fixtures for `:quickjs`/`:dynamic-engine` tests.

- **Unmangled names** — the ONLY ones:
  - `externs.js` (the C natives, the `__js*` transport globals, `versions`, `console`, WHATWG `fetch` /
    `Headers` / `Request` / `Response` / `ReadableStream` / `AbortController`).
  - QUOTED wire keys — all wire objects are written quoted/bracketed; `.prettierrc` has `quoteProps: preserve`
    because prettier's default strips them.

- **Tests** — drive the engine via pipes only — never externally-evaled JS calling API symbols (they're mangled);
  the few remaining `load()` snippets touch externed natives only.

- **Tooling** — via `npx` (Node at build; versions pinned in buildSrc).

- **Runtime limits** — what JS the QuickJS engine supports and what's slow (no Intl/URL/
  timers-from-engine; regex & `+=` are hot-path traps). Engine builtins include native `atob`/`btoa`
  and `TextEncoder`/`TextDecoder`. WHATWG `fetch` is the bundle wrapping `HttpRequest`:
  [`../quickjs/JS_FEATURES.md`](../quickjs/JS_FEATURES.md).

- **`BACKEND_VERSION`** — the bundle's manually-bumped revision: `Versions.engineJsVersion` for engine.js,
  `Versions.playerJsVersion` for player.js (buildSrc `Dependencies.kt`, independent — bump the one you push);
  an esbuild `--define` compile-time const — `declare const`, inlined, never a runtime global.

- **PRODUCTION** — esbuild `--define` from `-Pproduction` (same bake as Kotlin
  `BuildFlags.PRODUCTION`, default false). Chatty logs use `debug(function () { return concat; })`
  so the thunk is not called when true. `console.info` / `warn` always reach the file. There is
  no `MSE_DEBUG` / `SABR_DEBUG` / `BG_DUMP` / `DEBUG_LOGS` define.

- **Versioning / backward compat** — the host installs the frozen, non-writable `versions` global from C (`{app}`:
  the app version identifies the whole host surface — transport, protocols, app ship as one build) before the
  bundle — **backward-compat branching inside the JS** (`versions.app >= 123` / `typeof __x === "function"`),
  not a load handshake: natives may be added AND removed per app version; the always-latest bundle encodes the
  compatibility matrix.

- **Backward compatibility checklist** — any change crossing the JS ↔ host boundary is judged by direction: **args /
  native calls** flow toward the receiver (old host → new JS for command args; new JS → old host for natives and
  protocol heads), **results** flow back. The newer side must tolerate everything the older side sends or expects.

  | Change | Command args (old Kotlin → new JS) | Command result (new JS → old Kotlin) |
  |---|---|---|
  | add optional field | safe — JS defaults it | safe — old decoders skip unknown keys |
  | add required field | **break** — old host never sends it | safe |
  | remove field | safe — JS ignores it | **break** if any old decoder reads it |
  | optional → required, nullable → non-null | **break** | safe |
  | required → optional, non-null → nullable | safe | **break** |
  | narrow type / enum | **break** | safe |
  | widen type / add enum value | safe | **break** — old `when` / enum decode throws |
  | add command | safe | — |
  | remove / rename command | **break** — old app still calls it | — |

  - Calls from JS into the host (C natives, `__mse*`, http/cookie/cache/settings heads) follow the **args** column with
    JS as the sender: a native or head field newer than the support floor needs a `typeof` / `versions.app` guard
    and a fallback.
  - A **rename** is a remove + add — a break to old hosts. Ship both names; drop the old one only once every app
    version reading it is below the support floor.
  - **Semantic changes break silently**: a field keeping its type but changing format or meaning (duration
    `"3:45"` → seconds, app-link or URL shape, units) is a break — add a new field instead.
  - **Errors are wire too**: `WireError` shape and categories; a new category must land in an old host's generic
    "Something went wrong (E…)" path, never a crash.
  - "Required" means **what the shipped Kotlin decoder requires**, not what `wire.ts` declares — read the decoder
    (`:dynamic-engine` `yt/…`, `DynamicEnginePlayer`) before calling a result change safe.
  - Kotlin side, same rules mirrored: a new decoder must tolerate the oldest bundle it may still run with (the
    shell's bundled `engine.js` is the floor), and must fail a job with an E-code, never crash the host.
  - Verify, don't just reason: `compat/run.sh` runs today's bundles against the host of every shipped tag
    ([`../compat/README.md`](../compat/README.md)); the release build runs it per tag before drafting a release.

- **Headless WEB player** — `logic/player/` (raw `orig/env.raw.js` + PO minter `pot.ts` + `boot.ts`). Homepage
  `get_player_config` (live EVENT_ID + `ytAtN` program) → minter on `HttpRequest` → OWL slot → eval `base.js`
  (`fetch`) → KEVLAR_WATCH `Application.create` / `loadVideoById`. Native `:mse` via `protocol/mse.ts`.
  The snapshot env must fill Chrome's 5 PDF `navigator.plugins` (empty `{length:5}` fat-snapshots and no `webPo`).
  Player ads (`/youtubei/v1/player`) and SABR (`/videoplayback`) rewrite as `HttpRequest.addInterceptor`; fetch/XHR only build the request.
  Device decode caps (`media_caps.ts`): `navigator.mediaCapabilities` and the table half of `isTypeSupported` /
  `canPlayType` answer from the host's decoder table (`__mseDecodingInfo`, `:mse` `mse_caps.c`), and
  `getVideoPlaybackQuality` / `webkit*FrameCount` from its frame counts (`__mseVideoQuality`) — installed after
  the BotGuard snapshot, so the snapshot sees the same env on every host. A host without the natives or without a
  table (an Android player whose MediaCodecList walk had not finished by `player/init`) keeps the env it had before.

- **Playback buffer policy** — `logic/player/buffering.ts` manages the current and next VOD track.
  The optional `player/load.audioBufferSeconds` argument enables it; an absent argument preserves
  the original behavior for older hosts. The load result advertises `buffering: true` so the host
  can send `player/preload {cancel:true}` safely. Playback time, excluding pauses and seeks,
  gates expansion after five seconds. `buffer_sabr.ts` reads contiguous buffered ranges from SABR
  requests and returns a local NEXT_REQUEST_POLICY while full. Real responses retain their cookies,
  server backoff, and unknown fields; an appended policy updates only readahead targets. Media
  payloads remain streamed. Request cancellation is internal to HTTP interceptors; the host HTTP
  wire and native surface are unchanged. Target durations are approximate because YouTube sends
  media in segments; an in-flight segment can extend past the target.

  `player/load` explicitly applies play or pause after loading, including when YouTube reuses a prepared item. This keeps the requested playback intent across queue handoffs without changing command arguments or response fields.

  Local buffer-backoff responses run through the same SABR completion as network responses. A prefetched audio-only track can hold its audio before the synthetic video track is initialized; promotion must still supply that setup while further network fetching is held, so the cached audio can start.

- **Media-source handoff** — assigning a different MSE resource clears ended/seeking/stall/loop state. A native pipeline reset stops decoding, so an unpaused element explicitly restores native play intent after attaching the replacement; a paused element remains paused. This uses only existing native calls.

- **YouTube Music ratings** — `logic/yt/musicRatings.ts` uses WEB_REMIX on music.youtube.com for both song status and mutations. Mutations carry only `target.videoId`, matching [ytmusicapi rate_song](https://github.com/sigma67/ytmusicapi/blob/main/ytmusicapi/mixins/library.py). Signed-out writes and rejected responses fail explicitly. Existing WEB rating commands retain their wire contract.
