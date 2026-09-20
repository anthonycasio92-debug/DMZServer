# Deployment policy

Production is the **Kinetic live** panel server. The old **test server** (`node.everal.net`) is retired — do not deploy there.

## Live production

| Field | Value |
|-------|--------|
| Protocol | SFTP |
| Host | Set in `live-sftp.env` (gitignored) |
| Port | Usually `2022` |

- Build jars locally, then: `DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-live.sh` (LegacyMechanics Forge + GUI).
- Other mods (e.g. `dmz_mohist_melee_fix`): upload to `mods/` manually or extend deploy scripts; move replaced jars to `recycle_bin/`.
- **Restart** after Forge mixin jar changes (`dmz_mohist_melee_fix`, `LegacyMechanics`, etc.).
- Credentials: `live-sftp.env` from `live-sftp.env.example`; cloud agents may use env secrets + `scripts/ensure-live-sftp-env.sh`.

## Agent checklist

1. Confirm destination is **live** SFTP — not the retired test host.
2. Never commit passwords (`live-sftp.env` is gitignored).
3. Do not upload to live unless the owner asked for a live deploy in chat.
