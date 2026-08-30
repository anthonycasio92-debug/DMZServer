# Deployment policy (effective 2026-08-30)

**From this point forward, do not upload or deploy anything to the live production server.**

All new work in this repo targets the **test server** only.

## Live (production) — frozen

- Former live SFTP host is **off-limits for writes**.
- Do **not** `sftp`/`scp`/`rsync` jars, plugins, kubejs, Fabled configs, or scripts to live.
- Do **not** move files into live `mods/`, `plugins/`, or `kubejs/` from this agent/repo workflow.
- Read-only pulls from live (logs, telemetry snapshots) are allowed only when explicitly requested.
- Live stays at whatever was last deployed before this policy. No further live pushes unless the owner explicitly overrides this doc in writing.

## Test server — default deploy target

| Field | Value |
|-------|-------|
| Protocol | SFTP |
| Host | `node.everal.net` |
| Port | `2022` |
| User | `vamp.a6c38a00` |
| Password | **not stored in git** — use env `TEST_SFTP_PASS` / `SSHPASS` locally |

- Build, commit, and PR as usual, then deploy to this host.
- Prefer moving replaced jars/plugins into `recycle_bin/` on the test server (create the folder if missing).
- Owner restarts from the panel after jar uploads.

## Agent checklist

1. Confirm destination is **`node.everal.net` (test)** — never the old live host.
2. If unsure, **do not upload**.
3. Never commit SFTP passwords into the repository.
