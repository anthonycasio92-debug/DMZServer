# Adaptive Difficulty audit (v1.7.17 / GUI 1.7.10)

Audited Forge mod `tools/dmz-adaptive-difficulty` + Bukkit companion `tools/dmz-adaptive-difficulty-gui`.

## Verdict

Core economy, difficulty math, melee scaling, and GUI step contract are sound.  
**1.7.17 / 1.7.10** fixes the critical combat bugs and the main GUI contract gaps found in this pass.

---

## Fixed in this pass

| Severity | Issue | Fix |
|---|---|---|
| Critical | Ki / projectile damage double-scaled (`KiAttackHelper.baseDamage` × `scaleOutgoingHurt`) | Ki base is tier-only; difficulty applies once via hurt event |
| Critical | Evolved creepers exploded twice on fuse death | Final boom skips `IS_EXPLOSION` deaths |
| High | Titan Creeper HP bypassed `maxScaledHealth` | Cap at config/vanilla 1024 |
| High | Blaze kit ignited target every evolution tick | Removed trailing per-tick ignite |
| High | Kill TP reward multiplier applied twice | Build unscaled bonus, multiply once |
| High | Divine title unreachable (God checked first) | Check Divine before God |
| High | Anti-flight grounded creative (`instabuild`) | Skip creative/spectator builders |
| High | Bukkit ignored `guiBackend=chat/chest` | `openMenuRespectingConfig` honors Forge config |
| High | Double GUI reopen after every click | Bukkit no longer reopens after Forge `handle` |
| High | Failures shown as yellow “success-ish” text | Reflect `Result.ok` → green/red |
| High | `/dmzdiffgui` + `/difficulty do` skipped `dmzdiff.gui` | Permission checks added |
| Medium | Team contribution ignored hardcap | Clamp mate personal to `hardCapDifficulty` |
| Medium | Stale GUI placeholders (`DifficultyCache.get`) | Use `refresh` |
| Medium | Admin session split Bukkit/Forge | Forge `AdminCommandAccess` is source of truth |
| Medium | Non-numeric `do` amounts became +100 | Reject invalid amounts |
| Low | Docs/MANIFEST version + wallet wording drift | Synced to 1.7.17 / inventory-only |

---

## Remaining / accepted

| Severity | Issue | Notes |
|---|---|---|
| Medium | `hardCapDifficulty == 1000000` always migrates to 0 | Intentional legacy cleanup; intentional 1M cap cannot stick |
| Medium | `baseCostIronCoins = 0` makes raise/buy free | Treat as admin “free mode”; don’t set 0 in production |
| Medium | Dual `/difficulty` (Forge brigadier + Bukkit) | Mohist typically prefers Bukkit for players; watch for double-fire on odd setups |
| Medium | Cost formula uses `double` at huge steps | Possible 1-coin rounding drift at extreme amounts |
| Low | Dead hurt mixin (intentional noop) | Left registered; Forge event is the live path |
| Low | Unused legacy config keys (`baseCost`, `purchaseCurrency`) | Harmless; iron-coin path is authoritative |
| Low | High-tier titles (Transcendent–Zenith) not granted | Only Impossible/Divine/God (+ boss/elite titles) |

---

## What works

- Prestige/level theoretical max (Zenith 10M at defaults)
- Inventory-only Lightman's payments; lower/reset free
- Steps `{1,5,25,100,1000,10000,100000}` on Adjust / Buy / chat / placeholders
- Melee: attribute scale + event skip via `dmz_ad_attr_dmg`
- Hostile detection, area modes, team modes, gravity kits, boss-before-scale

---

## Install

1. `mods/dmz_adaptive_difficulty-1.7.17.jar`
2. `plugins/dmz_adaptive_difficulty_gui-1.7.10.jar` (+ CMILib / CMI)
3. Remove older adaptive-difficulty jars
4. Restart (`difficulty=hard`)
