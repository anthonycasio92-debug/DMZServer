# Legacy Mechanics — owner ship manifest

This file is the **source of truth for requests you already approved**.  
CI/build/deploy run `tools/dmz-adaptive-difficulty/sim/audit_ship_manifest.py` — **deploy is blocked if any item fails**.

When you ask for a behavior change, add a row here and a matching check in `audit_ship_manifest.py` in the **same PR** that implements it.

| ID | Request (summary) | Introduced | Audit |
|----|-------------------|------------|--------|
| prestige-need | Need: C0–4 → (completed+1)×20k cap 100k; C5+ by **held wallet** 50k/100k/145k/150k; **C8 held0 = 50k not 150k** | 4.5.66+ | `audit_prestige_need_ladder.py`, manifest §prestige |
| prestige-gui | Main prestige hub: **Need/Held** from wallet; **no Cap** on main; no cap breakthrough button on main (CNPC/chest/CMI) | 4.5.64+ | `audit_prestige_gui_flow.py`, manifest §prestige |
| skillcheck-gate | **Skill Check** = `legacymechanics.skillcheck` only; **no NPC/session bypass**; staff use **Skills** admin | 4.5.68+ | manifest §access |
| staff-admin-gate | **Staff Admin** / `/skills` / progression staff pages = staff/op only | 4.5.68+ | manifest §access |
| rival-challenge-pending | Rival **Challenge → Pending requests** board; tap row → accept/decline/cancel (CNPC + chest + CMI) | 4.5.59+ (merged 4.5.69) | manifest §rival |
| rival-declare-pending | Rival **Actions → Pending** + `pending_decide:` (declare mutual confirm) | 4.5.55+ | manifest §rival |
| spar-pending | Spar **Mentor Pending** + `pending_decide:`; dojo war pending decide | 2.3.142+ | manifest §spar |
| gui-menus | All LM hub systems wired (CNPC + chest + CMI where applicable) | ongoing | `audit_gui_menus_comprehensive.py` |
| version-handshake | Forge `VERSION`, `mods.toml`, GUI `plugin.yml`, and built jar names **same version** | always | `audit_gui_abi.py`, manifest §version |

## Deploy process (do not skip)

1. `bash tools/dmz-adaptive-difficulty/build.sh` — must pass all audits including **ship manifest**.
2. `bash tools/dmz-adaptive-difficulty-gui/build.sh` — ABI handshake with Forge jar.
3. `LM_DEPLOY_VERSION=x.y.z DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-live.sh` — script re-runs manifest audit before upload.
4. **Full server restart** after Forge jar change (not GUI-only reload).
5. Confirm live: only one `LegacyMechanics-*.jar` and one `LegacyMechanicsGUI-*.jar` in active folders.

## Why regressions happened (2026-09)

- **Rival challenge pending** lived on open PR #150 and was **never merged to `main`** while prestige/skillcheck fixes shipped on other branches → live got 4.5.68 **without** challenge pending UX.
- **Prevention:** manifest audit + deploy gate; merge manifest row when feature ships; do not deploy from a branch that fails `audit_ship_manifest.py`.
