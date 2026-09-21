# Deployment policy

Production is the **Kinetic live** panel server. The old **test server** (`node.everal.net`) is retired — do not deploy there.

## Live production

| Field | Value |
|-------|--------|
| Protocol | SFTP |
| Host | Set in `live-sftp.env` (gitignored) |
| Port | Usually `2022` |

- Build: `bash tools/dmz-adaptive-difficulty/build.sh` then `bash tools/dmz-adaptive-difficulty-gui/build.sh`. Version is **`AdaptiveDifficultyMod.VERSION`** (ship line **4.5.x** → `LegacyMechanics-4.5.0.jar`, etc.). Builds replace only that version’s filenames; older jars can stay in `mods/` / `plugins/` locally. Deploy scripts pick the highest `sort -V` match, or set **`LM_DEPLOY_VERSION=4.5.0`** to pin.
- **LegacyMechanics Forge jar — never hot-swap.** Stop the panel first, then either:
  - `LIVE_SERVER_STOPPED=STOPPED DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-live.sh` (direct install), or
  - While running: `DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-live.sh` stages `mods/LegacyMechanics-*.jar.pending`; after stop: `LIVE_SERVER_STOPPED=STOPPED bash scripts/activate-lm-staged-jar.sh`.
- Cap JSON (no jar): `DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-cap-config.sh` (`LevelingRevamp.json` 100k/150k + remove KubeJS cap shims).
- Other mods (e.g. `dmz_mohist_melee_fix`): upload to `mods/` manually or extend deploy scripts; move replaced jars to `recycle_bin/`.
- **Start** the server after any Forge mixin jar change (`LegacyMechanics`, `dmz_mohist_melee_fix`, …).
- Credentials: `live-sftp.env` from `live-sftp.env.example`; cloud agents may use env secrets + `scripts/ensure-live-sftp-env.sh`.

## Agent checklist

1. Confirm destination is **live** SFTP — not the retired test host.
2. Never commit passwords (`live-sftp.env` is gitignored).
3. Do not upload to live unless the owner asked for a live deploy in chat.
