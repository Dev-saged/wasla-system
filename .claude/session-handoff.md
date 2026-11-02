# session-handoff

## Where we stopped
- Branch feature/native-foundation: phases 1-2 done. Native app builds (tests + debug OK): cobalt "Compass" palette, "Dawn" adaptive icon (+monochrome tent, legacy PNGs), opening screen, 5-item nav shell with placeholder screens, Lalezar/IBM Plex fonts.
- Prototypes and brand sources live in prototype/ (home-b.html, brand/gen.py, brand/vd.py). Web app untouched.

## Decisions made this session
- Home direction B (Compass); badge/icon model 2 "Dawn", primary colorway; mono icon = tent with door.
- Brand art is original vector (no stock assets from the reference PSDs).
- Repo is public: secrets only in Actions; Release publish stays manual until phases 3-6 are done.

## Open issues
- CI: last re-run showed "Verify signing certificate" skipped; confirm it runs with secrets after this push.
- Opening screen and icon are verified on desktop renders only, not on a device/emulator.
- Name of previous regional admin still present in web files (index.html x5, manifest.json, README.md): separate small branch.

## Next step
- Phase 3: read current Firestore rules and user docs, then plan (Auth user@domain, claims, passwordPlain migration, rules, login, remember-me) and wait for approval before coding (>150 lines).
