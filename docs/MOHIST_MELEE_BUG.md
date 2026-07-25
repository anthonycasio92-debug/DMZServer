# Mohist M1: animation but no damage

## Fix v2.12.3

`mods/dmz_mohist_melee_fix-2.12.3.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.12.3.jar

Restores the proven **2.12.0** melee rescue path (`ServerMeleeFallback` + conservative strike unlock). **2.12.1** regressed punches by clearing strike maps / charge flags / syncing on every M1 packet.

### Melee (same as working 2.12.0)
- Decode cap raise 64→256 then truncate to 64
- Handle mixin unlocks only **stale** strike locks, then `processAttackRequest`, then server-side AABB rescue
- Persistent-data hit-time gate via `PersistentDataAccess` + `CombatEvent.DMZ_LAST_HIT_TARGET_TIME_TAG`
- No damage redirects / `setHealth` / `ki_damage` rewrites

### Stat reset (hardened)
- Record intentional primary writes (public setters + `setAttributeBaseValue`), including 0
- On `resetPlayerProgress`: cancel delayed teleport follow-ups, clear snapshot, suppress restore (~10s)
- Full resets force a zero snapshot; percentage resets adopt live post-reset values

### Master shadow dummies (from DragonMineZ 2.1.3 jar)
- Only two spawn sites: `NPCActionC2S` (master spar) and `SummonPlayerShadowDummyC2S` (player minigame)
- Every master/quest training UI that offers shadow spar sends `NPCActionC2S("popo", 1)`; server allows it if **any** master is in range, then `handlePopo(1)` creates the entity
- Both the protocol entry (`lambda$handle$0`) and `handlePopo` are cancelled server-side
- Player minigame summons are unchanged

### Install

1. Only **2.12.3** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — log: `v2.12.3`
