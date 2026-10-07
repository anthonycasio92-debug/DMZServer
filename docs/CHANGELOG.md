# LegacyMechanics changelog

What changed from **4.5.147** through **4.6.24**.

The live mods folder gets `LegacyMechanics-4.6.24.jar` on deploy. The running server keeps the older jar until a Kinetic panel restart.

## List scrolling

Head parts, rival picks, spar picks, and the other player lists scroll with the mouse wheel. The list is the menu's scroll, so the wheel is not stolen by a second scroll region.

## Menus

Spar dojo home, members, hall of fame, and rankings are one Dojo page. Dojo war stays its own page. Progression sections are tabs on Modules. Difficulty, Rival, and Spar toggles live on Settings. The hub no longer shows section labels or a welcome line. Remove Android is on Character Services. Race change asks how much progress to keep on the confirm page. Rival actions stay their own page. Prestige forms stay their own page.

## Rival records

History, stats, season, quests, achievements, hall of fame, journal, and title are one Records page. The section you are reading is highlighted. Older links to those pages open the same page on that section. Leaderboard stays its own page.

## Head part preview

The head-parts menu shows your own character, not a stand-in model. The part is already on you, so the menu uses that same model. Step back and look closes the menu and leaves the preview on so you can look at it. Back puts the old part back.

## Menu colors

Progression titles and its own buttons are blue. Prestige stays pink, including Open Prestige. Forms stays the Prestige color. Confirm summon is green. Inactive team modes are gray. The 2× boost presets match the other presets. Dojo home uses the spar color. The staff progression panel uses the progression color. On and off on the boost line match the toggle colors.

## Android tools

Android tools and convert stay staff-only. Players can use Remove Android, and Back returns them to the main menu.

## Menus

Long menu results that say "check chat for details" are also sent to chat. Screen titles use the same System · page form. Info blocks use three lines. Button rows use one step. Difficulty tiers, prestige forms, and spar leaderboard tabs use the shared button grid. A missing inner class fails the build before the jar is uploaded.

## Confirm clicks

A second click that confirms a paid action no longer crashes the server. The pending-click class is included in the jar.

## Menu wording

Rival chat [Cancel] no longer forfeits a live challenge under a "cancel yours" hover. A live challenge shows Forfeit. A pending request shows Cancel request. Tier buys, permanent tier buys, Majin and Mutant buy and remove, global TP boost start and end, Android convert, title unequip, and clearing the active tier ask you to click again within 10 seconds. Android convert names who it will convert and says Super forms and Legendary forms are deleted. The spectate list is only people in a live challenge. Spar chat [TP], [Sessions], and [Perfect] open those boards. A 0% race wipe says head-part unlocks and coins stay. Player screens use Head parts, Training bonds, Bond invites, Challenge, and Ancient Coins.

## Reskin gender

During a reskin, the gender picked in the editor is saved. Races that do not have a gender stay male.

## Head bone preview

Clicking a head part shows it on you before it charges. Unlock & equip, Equip, or Restore. Back puts the old part back.

## Sparring menus

The player leaderboard no longer has Wins or Win streak tabs. Those boards were showing Training Points. The dojo Wars tab now lists war wins. Button names match the pages they open. Leaving a master asks for confirmation. An invite you sent can be withdrawn. An invite sent to you can be accepted or declined.

## Sparring non-combat damage

Splash potions, lingering potions, TNT, thorns, and magic do not start a spar or grant TP. Ki, empty-hand melee, a registered weapon, and weapon projectiles (arrows, tridents, firework rockets) still do.

## Offline player clear

`/lm admin clear <player>` works when that player is offline. A name is taken from who is online, sparring, rivals, dojos, or the server player cache. A UUID always works. Two people with the same name need a UUID. Spar and rival data clear from their stores, including that player's dojo season row, profile, and wars. Difficulty and progression for someone who is offline are removed from their saved player file. Training points already on the character stay.

## Sparring TP

Sparring pays TP when a player deals damage to another player. A left-click with an ordinary item is not that damage, so it does not start a spar, keep one going, or grant TP. Empty-hand melee, a registered weapon, and ki still do.

## Corpses in protected dimensions

Deaths in space, Namek, Beerus' planet, and the other Noea travel dimensions still clear the dropped items and restore the inventory on respawn. The death event is no longer cancelled, so a corpse can spawn. That corpse is empty.

## Majin absorption bonus

Noea's stored melee and ki absorption bonus applies only while absorption is selected and the player is a Majin. When it is not, the read subtracts that stored bonus back out. The stored number itself is left alone. Battle power is unchanged.

## Race lock removed

No race is blocked, reset, or padlocked. `race-lock.json` is deleted on load. The race-select sync sends an empty lock list so an old padlock clears. The staff Race Lock toggle is gone. Stuck saga difficulty still clears while character creation is incomplete.

## Inflated max reset

4.6.3 saved huge max-ki and max-stamina bases on the player. 4.6.2 does not rewrite them, so both sides keep showing that number. On login, a base above 100 times the registered default (20, so anything above 2,000) is set back to that default. There is no repeating reconciler.

## Class-change resource sync

Paths that change max ki or stamina now send `ResourceSyncS2C` as well as the stats packet. A fighting-class change sends it next to the other class-change packets. Class commands, race creation, prestige count changes, prestige recovery, and rival fusion go through `DmzResourcePoolClamp.syncToClient`, which sends both packets. `DmzSkillUtil.sync` stays stats-only.

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
