# Deployment policy

Production is the **Kinetic live** panel server. The old **test server** (`node.everal.net`) is retired — do not deploy there.

## Live production

| Field | Value |
|-------|--------|
| Protocol | SFTP |
| Host | Set in `live-sftp.env` (gitignored) |
| Port | Usually `2022` |

- Build: `bash tools/dmz-adaptive-difficulty/build.sh` then `bash tools/dmz-adaptive-difficulty-gui/build.sh`. Version is **`AdaptiveDifficultyMod.VERSION`** (ship line **4.5.x** → `LegacyMechanics-4.5.0.jar`, etc.). Builds replace only that version’s filenames; older jars can stay in `mods/` / `plugins/` locally. Deploy scripts pick the highest `sort -V` match, or set **`LM_DEPLOY_VERSION=4.5.0`** to pin.
- **LegacyMechanics live upload (default):** `DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-live.sh` — archives **all** existing `LegacyMechanics-*.jar` in live `mods/` to `recycle_bin/` (or deletes if rename fails), then uploads the new Forge jar; optional GUI same pattern in `plugins/`. Uses `scripts/lm-live-sftp-upload.py` (paramiko). Restart when you want new Forge mixin code loaded (`/lm admin reload` only reloads LM config).
  - **Optional cautious mode:** `LM_STAGE_PENDING=1 DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-live.sh` then after stop `LIVE_SERVER_STOPPED=STOPPED bash scripts/activate-lm-staged-jar.sh`.
  - **Cleanup duplicates on live:** `DEPLOY_LIVE_CONFIRM=LIVE bash scripts/cleanup-lm-live.sh` (moves extra LM/melee jars + `.pending` to `recycle_bin/`; default keeps `4.5.49` + melee `2.12.21`).
  - **Refresh local consolidated base after deploy:** `bash scripts/pull-lm-base-jar.sh` or `bash scripts/refresh-lm-consolidated-base.sh`.
- Cap JSON (no jar): `DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-cap-config.sh` (`LevelingRevamp.json` 100k/150k + remove KubeJS cap shims).
- Other mods (e.g. `dmz_mohist_melee_fix`): upload to `mods/` manually or extend deploy scripts; move replaced jars to `recycle_bin/`.
- **Start** the server after any Forge mixin jar change (`LegacyMechanics`, `dmz_mohist_melee_fix`, …).
- Credentials (pick one):
  - **Cursor Cloud (recommended):** In [Environment settings](https://cursor.com/dashboard/cloud-agents/environments) for this repo, add **secrets** (not committed):
    - `LIVE_SFTP_HOST` — e.g. `use-dc-p98-a6-cg.kineticpanel.net`
    - `LIVE_SFTP_PORT` — `2022` (optional; default 2022)
    - `LIVE_SFTP_USER` — Kinetic SFTP username from the panel
    - `LIVE_SFTP_PASS` — same password as the Kinetic panel login
  - On each agent boot, `install` / `start` run `scripts/ensure-live-sftp-env.sh`, which writes gitignored `live-sftp.env` from those variables. Dashboard secrets override an existing `live-sftp.env` when all three of host, user, and pass are set.
  - **Local only:** copy `live-sftp.env.example` → `live-sftp.env` and fill in the password.

## Agent checklist

1. Confirm destination is **live** SFTP — not the retired test host.
2. Never commit passwords (`live-sftp.env` is gitignored). Rotate the panel password if it was pasted in chat.
3. Do not upload to live unless the owner asked for a live deploy in chat.
