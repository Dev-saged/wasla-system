# session-handoff

## Where we stopped
- Branch feature/native-foundation: phase 1 of the native rebuild (android/ skeleton, BrandConfig, theme, contrast test, CI workflow). Web app untouched.
- Signing key verified: p12 cert SHA-256 82:D0:04:7D:...:CD:90 equals the installed APK. Key and password are not in the repo.

## Decisions made this session
- Native app keeps applicationId com.w2a.xihi, minSdk 23, targetSdk 36; installs over the WebToApp build.
- Repo is public: secrets only in Actions (KEYSTORE_BASE64, SIGNING_STORE_PASSWORD). Release publish is manual (workflow_dispatch) until cutover.
- Old app reads releases/latest of this repo and opens the first .apk asset; the native Release must be the only asset.
- Firestore: role in claims, fine permissions read only when needed; people stored in chunked docs (~500) to stay on Spark.
- Name of previous regional admin removed everywhere (web index.html, manifest.json, README.md, native).

## Open issues
- Add the two Actions secrets, then run the workflow to confirm the fingerprint check passes in CI.
- Phase 2: three home directions and three icon directions, stop for choice.

## Next step
- Merge phase 1 after CI is green, then phase 2 (visual prototype).
