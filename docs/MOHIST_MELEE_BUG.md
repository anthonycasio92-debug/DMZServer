# Mohist M1: animation but no damage

## Fix v2.12.10

`mods/dmz_mohist_melee_fix-2.12.10.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.12.10.jar

### Instant Transmission to players disabled (2.12.10)
- Blocks `InstantTransmissionTravelToPlayerC2S` (party + external players)
- IT menu only lists masters; master teleport still works

### Stat reset / race selection (2.12.9)
- Vanilla DMZ clears `hasCreatedCharacter` on every reset → client race select (`forceCharacterCreation`)
- Soft `/dmzstats reset 100 true` still keeps stats/% + skills through recreate
- Percentage snapshot math + self `/dmzstats reset <%> [keepSkills]` still patched

### Melee (same as working 2.12.0)
- Decode cap raise 64→256 then truncate to 64
- Handle mixin unlocks only **stale** strike locks, then `processAttackRequest`, then server-side AABB rescue
- Persistent-data hit-time gate via `PersistentDataAccess` + `CombatEvent.DMZ_LAST_HIT_TARGET_TIME_TAG`
- No damage redirects / `setHealth` / `ki_damage` rewrites

### Stat reset (hardened)
- Record intentional primary writes (public setters + `setAttributeBaseValue`), including 0
- On `resetPlayerProgress`: cancel delayed teleport follow-ups, suppress restore (~10s), adopt/zero snapshot after
- Full resets force a zero snapshot; percentage resets adopt live post-reset values

### Disabled master NPC actions (server)
- **Shadow spar:** `NPCActionC2S("popo", 1)` cancelled. Player minigame summons unchanged.
- **Guru potential unlock:** `NPCActionC2S("guru", 1)` cancelled.
- **Dr. Gero android conversion:** `NPCActionC2S("gero", 1)` / `handleGero` cancelled.

### Priceless skills stay unbuyable (server)
- DMZ `UpdateSkillC2S.computeTpCost` does `Math.max(0, cost)`, so config `-1` (UI “Priceless”) became free
- SDU stack-skill double-click (Ultimate / Kaioken) does the same client clamp
- Mixin restores negative costs after compute so purchase/upgrade is rejected

### Old Kai UltimateChallenge (client)
- Hardcoded stages: Control → Gravity → Memory → **Precision** → Rhythm
- Client mixin drops Precision → Control → Gravity → Memory → Rhythm (still level 5 each)
- **Install this jar on clients** as well as the server, or players still see Precision

### Install

1. Only **2.12.10** in server `mods/` (client too if using Precision skip)
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — server log: `v2.12.10`
4. Full reset → race select: `/dmzstats reset` (requires `gameplay.forceCharacterCreation=true`)
5. Soft-reset example: `/dmzstats reset 100 true` (self) or `/dmzstats reset <player> 100 true`
