# DragonMineZ 2.1.3 — Scripting / Addon API Reference

Verified against `dragonminez-2.1.3.jar` (CurseForge file `8469416` / Modrinth `yZ4DgZaE`).

DMZ does **not** ship a KubeJS/CraftTweaker plugin. Custom logic is done by:

1. **Java addon mods** listening on `MinecraftForge.EVENT_BUS` for `DMZEvent.*` (recommended)
2. **Server commands / configs** (`/dmz*`, `config/dragonminez/`)
3. Optional **KubeJS Forge event listeners** if you add KubeJS yourself (same event class names)

Official player docs: https://docs.dragonminez.com/  
Source org: https://github.com/DragonMineZ/dragonminez  
Real-world addon patterns: [dmz-plus](https://github.com/KiziroAkami/dmz-plus)

---

## Package map

| Area | Package |
|------|---------|
| Mod entry | `com.dragonminez.DragonMineZ`, `Reference.MOD_ID = "dragonminez"` |
| Public Forge events | `com.dragonminez.common.events.DMZEvent` |
| Client FX events | `com.dragonminez.client.events.DMZClientEvent` |
| Player capability | `com.dragonminez.common.stats.StatsCapability` / `StatsProvider` / `StatsData` |
| Stats / character | `com.dragonminez.common.stats.character.*` |
| Skills / techniques | `com.dragonminez.common.stats.skills.*`, `...techniques.*` |
| Commands | `com.dragonminez.server.commands.*` |
| Compat hooks | `com.dragonminez.common.compat.*` (JEI, WorldGuard) |

---

## Accessing player data (primary hook)

```java
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

StatsProvider.get(StatsCapability.INSTANCE, player)
    .ifPresent((StatsData data) -> {
        int str = data.getStats().getStrength();
        float energy = data.getResources().getCurrentEnergy();
        boolean alive = data.getStatus().isAlive();
        String race = data.getCharacter().getRaceName();
        float bp = data.getBattlePower();
    });
```

`StatsProvider.get(...)` returns `LazyOptional<StatsData>`.

### `StatsData` entry points

| Method | Returns |
|--------|---------|
| `getStats()` | `Stats` — STR / Strike / Resistance / Vitality / KiPower / Energy |
| `getResources()` | `Resources` — energy, stamina, poise, TP, alignment, attribute points |
| `getStatus()` | `Status` — alive/halo, aura, blocking, fusion, flight mode, etc. |
| `getCharacter()` | `Character` — race, class, forms, appearance, mastery |
| `getSkills()` | `Skills` |
| `getTechniques()` | `Techniques` |
| `getBonusStats()` | `BonusStats` |
| `getBattlePower()` / `getBattlePowerExact()` | BP |
| `getMeleeDamage()` / `getStrikeDamage()` / `getKiDamage()` / `getDefense()` | combat calcs |
| `getMaxHealth()` / `getMaxEnergy()` / `getMaxStamina()` / `getMaxPoise()` | caps |
| `calculateTPGain(int)` / `calculateTPGain(int, TpSource)` | TP math |
| `resetPlayerProgress(ServerPlayer, Integer, boolean, boolean)` | full reset |

### Core mutable methods

**`Stats`**: `get/set/addStrength|StrikePower|Resistance|Vitality|KiPower|Energy`, `setStat/addStat/removeStat(String, int)`

**`Resources`**: `get/set/add/remove` for Energy, Stamina, Poise, Alignment, TrainingPoints; `setPendingAttributePoints`, `setPowerRelease`, `setReleaseLimit`

**`Status`**: `isAlive` / `setAlive`, blocking, charging ki, fusion fields, flight mode, visited dimensions, shadow dummy

**`Character`**: race/class/gender, `setActiveForm` / `clearActiveForm`, stack forms, mastery (`gainMastery`), appearance colors

Capability NBT key (debug): `/data get entity <Player> ForgeCaps.dragonminez:mod`

---

## `DMZEvent` — Forge event bus hooks

Register with:

```java
MinecraftForge.EVENT_BUS.register(this);
// or
MinecraftForge.EVENT_BUS.addListener(this::onDamage);

@SubscribeEvent
public void onDamage(DMZEvent.DamageModifyEvent event) {
    event.setAmount(event.getAmount() * 0.5);
}
```

### Combat

| Event | Cancelable | Key methods |
|-------|------------|-------------|
| `DamageModifyEvent` | yes | `get/setAmount`, `get/setDefensePenetration`, `getSourceType`, attacker/victim |
| `DamageDealtEvent` | no | amount, blocked, parried, `DamageSourceType` |
| `CritChanceEvent` | no | `get/setChance` |
| `PlayerBlockEvent` | yes | `get/setFinalDamage`, `setParry`, `get/setPoiseDamage` |
| `PlayerEvasionEvent` | yes | `get/setKiCost`, original damage |
| `PlayerDashEvent` | yes | `DashType` NORMAL/DOUBLE, `get/setDistance`, `get/setKiCost` |
| `StrikeAttackCastEvent` | no | player, `StatsData`, `StrikeAttackData` |
| `StrikeAttackFireEvent` | no | + `LivingEntity target` |
| `KiAttackCastEvent` | no | player, `StatsData`, `KiAttackData` |
| `KiAttackFireEvent` | no | charge multiplier, `get/setCooldownTicks` |
| `KiChargeEvent` | yes | current/max energy, `isEnergyFull()` |

`DamageSourceType` is nested on `DMZEvent` (melee / strike / ki / etc.).

### Forms / stats / resources

| Event | Cancelable | Key methods |
|-------|------------|-------------|
| `FormChangeEvent` | no | old/new group+form, `isTransform()` / `isUntransform()` |
| `StackFormChangeEvent` | no | same for stack forms |
| `StatChangeEvent` | yes | `StatType` STRENGTH, STRIKE_POWER, RESISTANCE, VITALITY, KI_POWER, ENERGY |
| `TPGainEvent` | yes | `get/setTpGain`, `get/setShareWithParty`, `getNewTpsValue()` |
| `HealthRegenEvent` / `EnergyRegenEvent` / `StaminaRegenEvent` | via parent | extend `ResourceRegenEvent` |
| `ResourceRegenEvent` | yes | `get/setAmount` |

### Quests / story / dragons / fusion

| Event | Cancelable | Notes |
|-------|------------|-------|
| `QuestStartEvent` | yes | `get/setDifficulty` |
| `QuestObjectiveProgressEvent` | yes | `setNewProgress` |
| `QuestTurnInEvent` | yes | `getNpcId` |
| `QuestRewardClaimEvent` | yes | `getRewardIndex` |
| `QuestFailEvent` | yes | `FailureReason`: PLAYER_DEATH, FORCED_RESET, **SCRIPT** |
| `QuestCompletedEvent` | no | |
| `QuestLifecycleEvent` | base | `getPlayer`, `getQuestKey`, `getSaga`, `getQuest`, `getPartyMembers` |
| `DragonSummonedEvent` | no | dragon id, ball set, positions |
| `FusionEvent` | yes | `FusionType`: METAMORU, POTHALA, ABSORPTION, ASSIMILATION |
| `PlayerDataLoadEvent` / `PlayerDataSaveEvent` | no | NBT `CompoundTag` for custom persistence hitchhiking |

---

## Client events (`DMZClientEvent`)

Local-player FX / input timing (not server authority):

- `PlayerAttackStart` / `PlayerAttackHit`
- `KiAttackCast` / `KiAttackRelease`
- `StrikeAttack`

---

## Commands useful from scripts

(See also https://docs.dragonminez.com/wiki/servers/commands/)

| Command | Purpose |
|---------|---------|
| `/dmzstats <set\|add\|remove> <stat\|all> <qty> <player>` | mutate stats |
| `/dmzpoints <set\|add\|remove> <points> <player>` | TP (ZPoints) |
| `/dmzskill <give\|set\|take> <skill> <level> <player>` | skills |
| `/dmzforms <give\|set\|take> <form_id> <level> <player>` | forms |
| `/dmzalignment ...` | alignment |
| `/dmzrevive` / `/dmzrestart` | revive / full character reset |
| `/dmzpermaeffects` / `/dmztempeffects` | effects |
| `/dmzstoryline ...` | saga/quest state |
| `/dmzlocate <structure>` | DMZ structures |
| `/data get entity <p> ForgeCaps.dragonminez:mod` | dump capability |

v2.1.3 also registers: `Bonus`, `Class`, `Config`, `Cooldowns`, `Debug`, `Hair`, `Halo`, `Mastery`, `Party`, `RacialSkill`, `Raid`, `Reload`, `Restore`, `Tech`, `Weight`, etc. under `com.dragonminez.server.commands`.

---

## Minimal addon listener skeleton

```java
@Mod.EventBusSubscriber(modid = "yourmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DmzHooks {
    @SubscribeEvent
    public static void onTp(DMZEvent.TPGainEvent e) {
        // double TP gains
        e.setTpGain(e.getTpGain() * 2);
    }

    @SubscribeEvent
    public static void onForm(DMZEvent.FormChangeEvent e) {
        if (e.isTransform()) {
            // react to transform into e.getNewGroup()/e.getNewForm()
        }
    }
}
```

Gradle dependency (CurseMaven):

```gradle
implementation fg.deobf("curse.maven:dragonminez-1136088:8469416")
```

Or point `files("libs/dragonminez-2.1.3.jar")` at the jar in this repo's `mods/` folder.

---

## Workspace layout for your scripts

| Path | Use |
|------|-----|
| `mods/` | DMZ + deps (+ your addon jars) |
| `scripts/` | drop CraftTweaker / custom script files here |
| `uploads/` | staging drop for anything you send via the agent chat |
| `docs/` | this API reference |

Put jars that your scripts depend on in `mods/` (or `uploads/` first if you are handing them to the agent).
