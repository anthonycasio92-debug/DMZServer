# LegacyMechanics changelog

What changed from **4.5.147** (previous `main`) to **4.5.153**. This is the build on `main` after the meditation and command cleanup.

The live mods folder already has `LegacyMechanics-4.5.153.jar`. The running server keeps the older jar until a Kinetic panel restart.

## Meditation

Meditation is no longer a trainer, a trial, or a slash command. DragonMineZ Meditation goes up while you are **already meditating** in Living World, and only in one of these situations:

- **Two players** are both meditating within 16 blocks of each other. Both gain time.
- **One player** is meditating within 16 blocks of **two living NPCs** that are in a Living World meditation circle.

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

`/progression` is **staff-only**. These commands are gone because they did not do the thing they advertised:

- `/progression meditation`
- `/enddragon spawn`
- `/progression flags_fabled`
- Player `/progression android remove` (it only printed a hint)

Players use:

- `/lm` for the hub, including Prestige and Remove Android
- `/difficulty`, `/rival`, `/spar`
- `/skillcheck` for donators

Staff keep progression boosts, flags, android convert and remove, `/enddragon clear`, and `/enddragon repair`. Players summon the End Dragon from the Difficulty menu.

## Already on main before this merge

4.5.147 and earlier stay as they were. That includes the server-only class and stamina behavior (the mod does not rewrite the client bar), the 10-minute death TP penalty, the spar closeness bonus on the training-point cap, and one Skill Check tab.
