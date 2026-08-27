# LegacyMechanics audit (2026-08-27)

Scope: CNPC scripts in-repo, remote feature branches, and a live-server script pull.
**Canonical branch:** `cursor/legacymechanics-progression-c766` (PR #28) — consolidates
Rival/Spar, progression, Title/Admin inspect, Building TP / End portal guard,
GhostPartyHeal (melee 2.12.20), inventory GUIs, plus KubeJS packs from
`server-fixes-consolidated` and `dmz-dino-food-balance`.
Jars: `LegacyMechanics-2.1.0` / `LegacyMechanicsGUI-2.1.0` / `dmz_mohist_melee_fix-2.12.20`.

## Live server pull

**Done 2026-08-27** (read-only SFTP). Snapshot: `uploads/live-scripts-2026-08-27/`.

| Path on live | Contents |
|--------------|----------|
| `AdventureWorld/customnpcs/scripts/ecmascript/` | 32 active CNPC scripts |
| `…/player_scripts.json` | 30 Global Player tabs enabled |
| `…/forge_scripts.json` | **disabled** (`ScriptEnabled: 0`) |
| `kubejs/` | balance pack + Building TP + weapon scale + Pothala protect |

### Live vs LegacyMechanics

All 30 enabled CNPC tabs have Java ports. Live versions refreshed into `customnpcs/scripts/`.

Notable live bumps synced + ported:

| Script | Live | Action |
|--------|------|--------|
| End Dimension Strength | **2.12.0** (was 2.10.4 in repo) | Java: single-dragon + End ki purge |
| Rival System | **4.7.10** | Already in LM; script dump updated |
| Sparring TP | **3.2.11** | Already in LM |
| SprintJump | **1.1.0** invested STR | Already `InvestedStrength` |

### Live-only KubeJS added to repo

- `kubejs/server_scripts/dmz_pothala_protection.js` (live `pathalafix1.js`)
- `kubejs/startup_scripts/dmzweaponscale.js` (actual base weapon damage)

Building TP / shadow-dummy remain active on **live** KubeJS; LM has Java ports — disable those KubeJS files when LM ships to avoid doubles.

## CNPC scripts → LegacyMechanics

| Result | Count | Notes |
|--------|-------|-------|
| Ported OK | ~48 unique features | Rival, Sparring, natural progression, Fabled bridges, PlayerStatChecker |
| Intentionally not | 1 | `Attr Fabled bonus stats.js` — marked disabled duplicate of Multi bonus |
| Gaps closed this audit | 2 | `Disable End Portals.js` → `EndPortalGuard`; Building TP (KubeJS) → `BuildingTp` |

See [PROGRESSION.md](PROGRESSION.md) for the full disable list and config flags.

### Intentionally superseded splits (still covered by Java)

`Flight.js` covers Fly / Viltrumite / suppression; `Yardrat.js` covers race+skills; `Fabled Sync` / bridges cover prestige trio; `SprintJumpProgression` covers Jump/Sprint (invested STR).

## Other branches

| Branch | Unique vs LM | Status after audit |
|--------|--------------|--------------------|
| `main` | none material | Ancestor; LM ahead |
| `rival-sparring-adaptive` | Rival/Spar | Already in LM |
| `sprintjump-invested-str` | SprintJump 1.1.0 | Already `SprintJumpProgression` + `InvestedStrength` |
| `dmz-dino-food-balance` | `kubejs/.../dmz_food_balance.js` | **OUT_OF_SCOPE** (KubeJS item food) — keep on that PR |
| `server-fixes-consolidated` | Title 1.0.39, Admin inspect, unlock-gate, Building TP, GhostPartyHeal, weapon/Apotheosis KubeJS | **Ported into LM/melee** except KubeJS balance pack (below) |

### Ported from `server-fixes` this audit

- Title mastery / challenge / score (`TitleProgress`, `TitleEffects`, `TitleRarity`, `TitleScoreRewards`, `TitleSense`)
- Admin inspect: `/difficulty admin gui|inspect <player>` + `AdminInspectSessions`
- Unlock gate: `UnlockSystem.gateLevelForEligibility` + Buy GUI level text
- Building TP on place (`BuildingTp`)
- End portal guard (`EndPortalGuard`)
- Ghost party heal → `dmz_mohist_melee_fix` 2.12.20

### Still OUT_OF_SCOPE (stay KubeJS / other packs)

- `dmz_food_balance.js` (dino meat)
- `dmz_weapon_bonus_scale.js` / `dmz_actual_attack_damage.js`
- Apotheosis / lifesteal / heartstop / silk / necrotic / silky / capsule disables
- Dungeon clone ki / Shurui bridges / race_lock_gui_sync client companion

Ship those via `server-fixes-consolidated` / `dmz-dino-food-balance` PRs, not LegacyMechanics.

## Remaining risks

1. **Live script versions unknown** until SFTP or dump works — in-repo CNPC may lag production.
2. On test: disable matching CNPC + KubeJS Building TP when LM flags are on (avoid double TP).
3. Do not deploy LM to live until test pass.
