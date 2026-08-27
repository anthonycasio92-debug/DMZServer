# LegacyMechanics audit (2026-08-27)

Scope: CNPC scripts in-repo, remote feature branches, and a live-server script pull.
Branch: `cursor/legacymechanics-progression-c766` · jars `LegacyMechanics-2.0.0` / `LegacyMechanicsGUI-2.0.0` / `dmz_mohist_melee_fix-2.12.20`.

## Live server pull

**Blocked.** SFTP to `use-dc-p98-a6-cg.kineticpanel.net:2022` as `skbxbrhg.9471e9b6` returns `Permission denied`.
Need updated `SFTP_USER` / `SFTP_PASSWORD` (optional host/port) or a zip of live `AdventureWorld/customnpcs/scripts` (+ ecmascript) to finish a live-vs-port content diff.

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
