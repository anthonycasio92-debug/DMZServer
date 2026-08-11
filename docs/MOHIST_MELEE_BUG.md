# Mohist M1: animation but no damage

## Fix v2.12.18

`mods/dmz_mohist_melee_fix-2.12.18.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/saga-party-progress-guard-c766/mods/dmz_mohist_melee_fix-2.12.18.jar

### Personal saga stuck-completion fix (2.12.18)
- After party merge, on party leave, and on login: **purge** any `SUCCESS` quest the player did not personally earn
- Stops “shows completed / can’t claim / stuck after leave” when joining a friend who already finished the saga
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

1. Only **2.12.18** in server `mods/` (client too if using Precision skip)
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — server log: `v2.12.18`
4. Full reset → race select: `/dmzstats reset` (requires `gameplay.forceCharacterCreation=true`)
5. Soft-reset example: `/dmzstats reset 100 true` (self) or `/dmzstats reset <player> 100 true`
