# Handoff — native GRCS
State: v4.2.0 on feature/native-ui merged to main via PR; UI-fidelity (U1-U6) + phases 4/5 coded; assembleDebug passes. Not tested on a device.
Done: web-style UI everywhere incl. all tool sub-pages; camp info; transfer log; sync diagnostics; camp clear/delete (admin, forced backup+trash+typed name); import mapping + fingerprint templates; theme toggle.
Decisions: template stored at importTemplates/{fp} field nmap (header names) with native fingerprint (not web-compatible); global quota panel needs Worker /usage (not deployed).
Open: on-device verification by user; no unread badge on nav; Release not published (workflow_dispatch publish=true on main after his OK).
Next: user feedback from device; fix issues; then publish Release.
