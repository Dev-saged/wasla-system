# session-handoff

## Where we stopped
- Branch feature/native-foundation: phases 1-7a done (+aid/requests, beneficiary home, quality) (+messages, backups, updates, camps) (writes ported, People screen) (native login + read-only Firestore sync + real Home stats via existing Worker, Keystore session, login screen; debug builds; untested on device). Native app builds (tests + debug OK): cobalt "Compass" palette, "Dawn" adaptive icon (+monochrome tent, legacy PNGs), opening screen, 5-item nav shell with placeholder screens, Lalezar/IBM Plex fonts.
- Prototypes and brand sources live in prototype/ (home-b.html, brand/gen.py, brand/vd.py). Web app untouched.

## Decisions made this session
- Home direction B (Compass); badge/icon model 2 "Dawn", primary colorway; mono icon = tent with door.
- Brand art is original vector (no stock assets from the reference PSDs).
- Repo is public: secrets only in Actions; Release publish stays manual until phases 3-6 are done.

## Open issues
- Opening screen and icon are verified on desktop renders only, not on a device/emulator.
- Name of previous regional admin still present in web files (index.html x5, manifest.json, README.md): separate small branch.

## Next step
- Device test vs web. Then: Excel import, camp create/delete (admin), classification/vulnerability views, super-admin quota panel, family transfer, staff view of beneficiary threads, permissions PDF with QR. No Release until verified.
