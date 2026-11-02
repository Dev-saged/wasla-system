# Session handoff — GRCS native (feature/native-foundation)
## State
- Native app v4.0.0 builds (assembleDebug OK). All stages pushed. No tests run, nothing tested on device (user's instruction). No Release published.
- Done: login, sync, People CRUD, merge/pulse, ben accounts, messages (+outbox, staff->ben threads), backups, update check, camps (admin create/rename), aid + requests, quality, vulnerability (+shelter inference, tier chips), import (flat), transfer, aid report, ben cascade delete.
## Decisions
- Reuse Worker login; REST only; Release only by manual dispatch after user verifies.
- Camp delete/clear not in native (risky) — web only.
## Open
- unread badges only on beneficiary lists (camp rows skipped to save reads); global quota panel (needs Worker); role dashboards (staff accounts page + create done).
- CI status of final commits not re-verified; PR #5 (web name removal) awaits owner merge.
## Plan
- docs/NATIVE-PLAN.md: phases 2-5. Phase 2 (export, trash, audit) DONE on feature/native-phase-2. Next: phase 3 (categories+saved queries, duplicates, rapid entry), then 4 (camp info, transfer log, sync diag), then 5 (camp delete/clear, import fingerprints, settings+quota).
## Next
- Continue gaps above, then user tests: install debug APK from Actions, compare Home numbers with web, CRUD a person and confirm on web, backup/restore, beneficiary login+request.
- Security: move PUSH_ADMIN_KEY from Worker source to a Cloudflare Secret.
