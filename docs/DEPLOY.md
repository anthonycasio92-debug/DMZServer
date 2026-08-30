# Deployment policy (effective 2026-08-30)

**From this point forward, do not upload or deploy anything to the live production server.**

All new work in this repo targets the **test server** only.

## Live (production) — frozen

- Host used previously for live SFTP is **off-limits for writes**.
- Do **not** `sftp`/`scp`/`rsync` jars, plugins, kubejs, Fabled configs, or scripts to live.
- Do **not** move files into live `mods/`, `plugins/`, or `kubejs/` from this agent/repo workflow.
- Read-only pulls from live (logs, telemetry snapshots) are allowed only when explicitly requested.
- Live stays at whatever was last deployed before this policy. No further live pushes unless the owner explicitly overrides this doc in writing.

## Test server — default target

- Build, commit, and PR changes here as usual.
- Deploy **only** to the test server when credentials / host details are provided for that environment.
- Prefer documenting test-server paths under `docs/` when the test host is configured.

## Agent checklist

Before any remote file transfer, confirm the destination is **test**, not live. If unsure, **do not upload**.
