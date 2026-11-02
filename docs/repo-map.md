# repo-map

Keep under 5KB. No code. Update by 2 lines after every change.

> First session on this repo: generate this file from the current code (sections, key functions, decisions). Do not modify app code while doing so.

## Files
- index.html, sw.js, manifest.json, version.json — web app (served by GitHub Pages; unchanged by the native work).
- android/ — native Kotlin + Compose app (applicationId com.w2a.xihi); config/BrandConfig.kt is the only place for brand and OKLCH palette; ui/theme = OKLCH to sRGB + forced RTL.
- .github/workflows/android.yml — tests, signed build, cert fingerprint check; Release only by manual publish from main.
- docs/NATIVE.md — native rebuild decisions and phase plan; docs/releases/vX.Y.Z.md — Arabic release notes.

## Sections (SECTION markers)
- (marker name, one-line purpose)

## Key functions and IDs
- (names that must be preserved)

## Decisions
- v3.6.0: per-camp cache versioning (PersonStore.camp, campVersion) + PQ index queries — a write to one camp must not invalidate another camp's stats
- v3.6.0: family transfer (admin only) = tombstone at source, same IDs at destination, push destination first then source, verify via FamSync.fetchIds

- v4.0.0: repo is public, so signing material lives only in Actions secrets (KEYSTORE_BASE64, SIGNING_STORE_PASSWORD), never in files; cert SHA-256 is checked in CI against the installed app.
- v4.0.0: a published Release becomes "latest" and the old WebToApp build offers it as an update; publish stays manual until cutover.

## Solved bugs (never reintroduce)
- swipe tray invisible: .cv-list>* content-visibility:auto clips overflow; fix: content-visibility:visible on .sw-drag cards
- pending-sync banner not refreshed after failed push; fix: SyncDiag._save schedules renderPendingSyncBanner

## Open issues
- (none)
- prototype/home-directions.html: static prototype (3 home directions, 3 splash, 3 icon concepts); awaiting user choice, no logic.
- prototype/home-b.html: chosen home direction B (Compass) as interactive prototype + 3 icon developments (A compass, B lit door, C embroidered) of the chosen "tent and door" concept; awaiting icon pick.
