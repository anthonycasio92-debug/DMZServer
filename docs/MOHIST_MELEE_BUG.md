# Mohist M1: animation but no damage

## Fix v2.12.25 — flow shockwaves leave terrain alone (Oct 2026)

`mods/dmz_mohist_melee_fix-2.12.25.jar`

`FlowShockwaveNoDestroyMixin` cancels `FlowShockwaveService.destroyRing`. The shockwave visuals and knockback still play. This jar also keeps the 2.12.24 priceless-mixin removal and the 2.12.23 grab load-order fix.

Requires a full server restart.

## Fix v2.12.24 — priceless skill mixin unregistered (Oct 2026)

`mods/dmz_mohist_melee_fix-2.12.24.jar`

`UpdateSkillPricelessMixin` injected into Dragon Mine Z `UpdateSkillC2S` and called `com.dbzlegacy.mohistmelee.PricelessPurchaseGuard`. That class lives in this mod, and Dragon Mine Z's classloader cannot see it — the same isolation that broke `MajinAbsorptionStore` when it sat next to a mixin. `SkillsBlockPricelessLevelMixin` and `SkillBlockPricelessAddLevelMixin` call the same class from `Skills` and `Skill`, so those three are not registered. The other melee, stat-reset, and Mohist mixins stay. The priceless menu guard is off until that logic is inlined into the mixin or moved somewhere Dragon Mine Z can load.

Requires a full server restart.

## Fix v2.12.23 — GrabService mixin no longer loads too early (Oct 2026)

`mods/dmz_mohist_melee_fix-2.12.23.jar`

`OptionalNoeaMixinPlugin` was calling `Class.forName("com.butterjaffa.noeabosses.GrabService")` while Mixin was still reading its config. That defines `GrabService` before the transformer is ready, so `NoeaGrabServiceRequestMixin` is skipped and the log says the target was loaded too early. The plugin now checks for the class file and does not define it. The mixin names `GrabService` as a string and its inject method does not reference Noea types. `GrabService.setEnabled` still runs from the server-started event, after mixins have applied.

`NoeaGrabActionPacketMixin` is unchanged. Requires a full server restart.

## Fix v2.12.22 — hard-disable Noea experimental grab (Oct 2026)

`mods/dmz_mohist_melee_fix-2.12.22.jar`

When **Dragon Block Noea** (`noeabosses`) is installed, this jar:

- Cancels all `GrabActionC2SPacket` handling and `GrabService.request` (Z grab/throw).
- Calls `GrabService.setEnabled(server, false)` on start and every ~10s so `/noea experimental grab on` cannot re-enable it.

Requires a **full server restart** after replacing the melee fix jar (Forge mixins).

## Fix v2.12.21 — handwear + Apotheosis crash (Sep 2026)

`mods/dmz_mohist_melee_fix-2.12.21.jar`

Live crashes when attacking with **DMZ Revamp wristbands** (curio handwear) plus **Apotheosis affix** gear (e.g. Simply More **Blood Harvester**, other uniques) were a **recursive `LivingHurtEvent` loop**: `HandwearCombatEvents.runApotheosisHandwearAttackHooks` → `EnchantmentHelper.doPostAttack` → Thunderstruck (etc.) → hurt again → same hook → `StackOverflowError`.

**Mitigation:** `HandwearApotheosisReentrancyMixin` skips nested handwear Apotheosis hooks on the same thread. Restart required after jar swap.

Upstream fix still belongs in **dmzrevamp** (`HandwearCombatEvents` should not call post-attack affixes from inside `LivingHurt` without a reentrancy guard).

## Fix v2.12.19

`mods/dmz_mohist_melee_fix-2.12.19.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/saga-guard-purge-fix-c766/mods/dmz_mohist_melee_fix-2.12.19.jar

### Saga guard safety fix (2.12.19)
- **Removed** wipe-on-leave / wipe-on-login / wipe-on-death purge (was erasing legitimate saga progress)
- Death/respawn now **carries over** personal earn marks (same UUID) instead of re-bootstrapping empty
- Party merge only strips completions **newly borrowed in that merge**, never pre-existing progress
- Reward claims still blocked for quests the player did not personally earn
- True co-op still works if you were `ACCEPTED` on the quest when it completed


### Personal saga earn guard (2.12.17)
- Party sync can no longer copy already-`SUCCESS` quests onto a player who did not earn them and was not actively on the quest
- `QuestRewardClaimEvent` cancels claims unless the player personally earned that quest (solo turn-in or present in party at completion)
- Saga/quest resets clear personal earn marks so reset → rejoin-friend cannot reclaim TP/rewards
- First login bootstraps earn marks from currently completed quests (grandfather existing progress)

### Skill set maxLevel refresh (2.12.11)
- `/dmzskill set` refreshes skill `maxLevel` from `skills.json` costs before applying the level

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

### Priceless skills/forms stay unbuyable (server) — hardened in 2.12.16
- Cancels DMZ `UpdateSkillC2S.lambda$handle$0` for PURCHASE/UPGRADE when cost is `-1`
- **SDU 3.0.5+:** also cancels `DmzSkills.buyStackSkill` (own `BuyStackSkillC2S` path that did `Math.max(0, -1)` and free-unlocked stack forms on spam/double-click)
- Preserves negatives in DMZ `computeTpCost`
- Guards `Skills.setSkillLevel` / `Skill.addLevel` during menu purchase packets only
- Log lines: `blocked priceless …` / `blocked priceless sdu buyStackSkill …`


### Old Kai UltimateChallenge (client)
- Hardcoded stages: Control → Gravity → Memory → **Precision** → Rhythm
- Client mixin drops Precision → Control → Gravity → Memory → Rhythm (still level 5 each)
- **Install this jar on clients** as well as the server, or players still see Precision

### Install

1. Only **2.12.19** in server `mods/` (client too if using Precision skip)
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — server log: `v2.12.19`
4. Full reset → race select: `/dmzstats reset` (requires `gameplay.forceCharacterCreation=true`)
5. Soft-reset example: `/dmzstats reset 100 true` (self) or `/dmzstats reset <player> 100 true`
