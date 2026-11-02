# Handoff — native GRCS (branch feature/native-ui)
State: v4.1.0 UI-fidelity phase U1–U6 coded; assembleDebug passes. Not tested on device (by request: no tests/screens).
Done: web palette+Cairo, Kit components, role tabs, Home, Settings cards, People, Messages, Accounts (tabs/search), Camps, BenHome, Login, theme toggle.
Decisions: every screen owns its AppHeader; status bar icons always light; Palette API unchanged (fields added).
Open: sub-pages (vuln/quality/query/similar/rapid/trash/audit/import/export/aid) still QChip layout; no nav unread badge; beneficiary settings has no data tools.
Next: user installs APK from CI artifact of this branch and reports on-device issues; then merge to main; then phases 4–5 (camp info, transfer log, sync diagnostics, camp delete/clear, import fingerprints, quota panel).
