# LegacyMechanics changelog

What changed from **4.5.147** through **4.6.1**.

The live mods folder gets `LegacyMechanics-4.6.1.jar` on deploy. The running server keeps the older jar until a Kinetic panel restart.

## Level cap cache

Stat getters no longer re-read breakthrough NBT or sync the client. The personal cap is cached per player and refreshed on login, logout, and a breakthrough purchase. A sync packet goes out only when that number changes.



## Dead facades

Chest-menu bridges `BukkitGuiBridge` and `CmiGuiBridge` are gone. So are the unused facades `SkillProgression`, `RaceProgression`, `TpProgression`, and `CnpcBridge`. `ProgressionSystem` already calls the race and training pulses directly.

## Hot-path guards

Owner lookup caches a miss for half a second instead of scanning every online player on each regen read. Stats load clears its thread flag if `load` throws. Stamina regen no longer ticks Dragon Mine Z cooldowns while those cooldowns are already moving, and it drops its maps on logout. A failed mob-scaling pulse no longer skips rival, sparring, and progression for that tick. Meditation looks only near players who are already meditating. The repo keeps one LegacyMechanics jar, one melee-fix jar, and one sdu jar. `plugins/` does not ship `LegacyMechanicsGUI`.

## Difficulty mob fight rhythm

Each kit pulse now commits to one action, picked by range, using the moves that mob already had. Close is a slam, a shock, or a short blast while the mob steps back. Mid is a dash, a laser, or a leap. Far is a beam. A pack no longer refreshes slowness, mining fatigue, or wither on the same tick — one nearby mob owns each of those. Melee sidesteps while closing, and a retreat paths back in.

## End Dragon summon

The Difficulty menu can summon the End Dragon again. Personal Adaptive Difficulty must be on, the active tier must be T4–T7, and the summon costs 3 Ancient Netherite unless staff are free. Natural End dragons still do not spawn, and there is still no `/enddragon spawn`. Admins clear dragons with `/enddragon clear`.

## Meditation rate

Training with another player is full time. More players nearby do not add more. Two living NPCs credit 75% of that time. Each held prestige adds 10% on top of whichever rate you are using. One NPC still does not count.

## End return portal

Walking through the End return portal back to the overworld does not start the death penalty. Dying still does.

## Meditation near two NPCs

Time now counts while you are already meditating within 16 blocks of two living NPCs that are themselves meditating. The circle flag is not required. One NPC still does not count, and standing nearby without meditating still does not count. Two players meditating near each other is unchanged.

## Dojo war tracking

While a dojo war is active, the other dojo's members in your world show up in Noea's tracking signature scan. Open Scan / Track and the compass points at the nearest one until you pick a different signature. Members in another dimension do not show. Positions use the same 4-block snap as other signatures.

## Meditation

Meditation is no longer a trainer, a trial, or a slash command. DragonMineZ Meditation goes up while you are **already meditating** in Living World, and only in one of these situations:

- **Two players** are both meditating within 16 blocks of each other. Both gain time.
- **One player** is meditating within 16 blocks of **two living NPCs** that are meditating.

One NPC is not enough. Standing nearby without meditating does not count. The mod does not start meditation for you. If a partner and a circle are both nearby, you still gain one stretch of real time.

Time is saved when you leave, log out, or the server restarts. Extra time carries into the next level. Each breakthrough raises Meditation by 1 and sends one chat message.

Time to leave each level:

| From | To | Time |
|------|----|------|
| 0 | 1 | 1 minute |
| 1 | 2 | 5 minutes |
| 2 | 3 | 15 minutes |
| 3 | 4 | 30 minutes |
| 4 | 5 | 1 hour |
| 5 | 6 | 2 hours |
| 6 | 7 | 4 hours |
| 7 | 8 | 6 hours |
| 8 | 9 | 8 hours |
| 9 | 10 | 10 hours |

That is **31 hours 51 minutes** to reach level 10. Skill Check shows your level, progress, and time remaining.

## Natural skills

Flight, sprint/jump, and the old meditation trainer are **removed**, not turned off with a flag.

- Potential Unlock still levels from sparring.
- Skill Check is still one Skills tab: Potential Unlock, Meditation, and the saga skills.
- Saga rows show the level and Locked, Unlocked, or Max.

## Prestige shop

The shop still sells **permanent** skill unlocks: Potential Unlock, Fly, Meditation, Jump, Sprint, and the saga skills. Buying one sets the skill floor. It does not replace the new meditation timer. Ki attacks and forms are not in that shop list.

## Commands

`/progression meditation`, `/enddragon spawn`, and `/progression flags_fabled` are gone.

**Android remove works.** `/progression android remove` removes your own upgrade. Run it again within 10 seconds to confirm. Staff can target another player. The hub Remove Android button does the same thing.

**Difficulty summon is back on in 4.5.161.** There is still no `/enddragon spawn`. Natural End dragons stay off. Admins clear dragons with `/enddragon clear` (console works too). `/enddragon repair` is still there.

Players also use `/lm`, `/difficulty`, `/rival`, `/spar`, and `/skillcheck` (donators). Staff keep progression boosts, flags, and android convert.

## Skill Check NPC

Any player can open Skill Check by right-clicking the Skill Check NPC, including dialog trigger 21. Paste `customnpcs/scripts/SkillCheckPlayerNpc.js` onto that NPC. Slash `/skillcheck` and the hub button still need the donator permission `legacymechanics.skillcheck`. The running server keeps the previous jar until a panel restart.

## Already on main before this merge

4.5.147 and earlier stay as they were. That includes the server-only class and stamina behavior (the mod does not rewrite the client bar), the 10-minute death TP penalty, the spar closeness bonus on the training-point cap, and one Skill Check tab.
