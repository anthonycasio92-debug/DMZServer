# Mohist “must die to melee” (DMZ combat)

## Symptom

On Mohist hybrid (`1.20.1-46ca7304` / Forge 47.4.x + DragonMineZ 2.1.3), a player’s **melee hits deal no damage** to players or entities until that player **dies and respawns once**. Ki / other systems may still work.

## Why it happens

DragonMineZ does **not** use vanilla left-click damage as the authority path:

1. Client cancels vanilla `Minecraft.startAttack` (`MinecraftMixin`)
2. Client sends `CombatAttackRequestC2S`
3. Server requires `StatsCapability`, then calls `ServerPlayer.attack(target)`
4. `CombatEvent.onLivingHurt` rewrites the hit to `getMeleeDamage()`

Mohist heavily patches the same path:

| Layer | Conflict |
|-------|----------|
| `Player.attack` | Bukkit `EntityDamageByEntityEvent` bridge + DMZ `@ModifyVariable` / `@Redirect` mixins on `attack` |
| `PlayerList.respawn` | Log warning: DMZ `PlayerListMixin#onPlayerRespawn` LVT incompatible with Mohist’s respawn method |
| Forge ↔ Bukkit damage | Known Mohist gaps where `LivingHurtEvent` / entity damage bridging misbehaves until the player entity is recreated |

Respawn recreates the `ServerPlayer` / Craft wrapper and re-runs DMZ clone/cap sync (`PlayerEvent.Clone` + `refreshAttributes` / `m_6210_()`), which is why **dying “fixes” melee for that session**.

Evidence already on the test server log:

```text
Injection warning: LVT in ...PlayerList::respawn... has incompatible changes
  in callback dragonminez.mixins.json:common.PlayerListMixin->@Inject::dragonminez$onPlayerRespawn
This server is running Mohist version 1.20.1-46ca7304 ... Forge version 47.4.13
```

## What actually fixes it (preferred order)

1. **Do not run DMZ combat on Mohist if you can avoid it**
   - Pure Forge (no Bukkit plugins), or
   - A different hybrid with better Forge combat parity (evaluate Arclight / Ketting on a staging copy)
2. **Update Mohist** to the newest 1.20.1 build and retest with only DMZ + GeckoLib + TerraBlender + Curios
3. **Binary-search plugins** that touch damage/PvP (WorldGuard, CMI combat/god, Fabled damage skills, ProtocolLib listeners) — hybrids often cancel Forge damage after the mod applies it
4. **Workaround in this pack**: `kubejs/server_scripts/mohist_melee_combat_fix.js`  
   On login, calls the same `refreshAttributes` DMZ uses after respawn, clears join i-frames, and resets attack strength.  
   **Does not** gamemode-flicker, teleport, kill, or change `keepInventory`.

## How to verify

1. Fresh login (no death yet), punch a mob / dummy / player
2. With workaround loaded, wait ~2s after login, punch again — expect damage without dying
3. Confirm console: `[mohist-melee-fix] Primed melee for <name> via ...`
4. `keepInventory` must remain **false** / off — this script never touches that gamerule

If attribute refresh alone is not enough, the real fix is leaving/updating Mohist; there is no safe kill-based prime while keepInventory stays off.
