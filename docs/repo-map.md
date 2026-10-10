# repo-map

Keep under 5KB. No code. Update by 2 lines after every change.

> First session on this repo: generate this file from the current code (sections, key functions, decisions). Do not modify app code while doing so.

## Files
- (list each file and its purpose)

## Sections (SECTION markers)
- (marker name, one-line purpose)

## Key functions and IDs
- (names that must be preserved)

## Decisions
- v3.6.0: per-camp cache versioning (PersonStore.camp, campVersion) + PQ index queries — a write to one camp must not invalidate another camp's stats
- v3.6.0: family transfer (admin only) = tombstone at source, same IDs at destination, push destination first then source, verify via FamSync.fetchIds

## Solved bugs (never reintroduce)
- swipe tray invisible: .cv-list>* content-visibility:auto clips overflow; fix: content-visibility:visible on .sw-drag cards
- pending-sync banner not refreshed after failed push; fix: SyncDiag._save schedules renderPendingSyncBanner

## Open issues
- (none)
