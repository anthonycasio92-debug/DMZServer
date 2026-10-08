# LegacyMechanics changelog

What changed from **4.5.147** through **4.6.56**.

## Fusion mixin target

The unfuse bonus mixin names `FusionLogic` as a string. A class literal loads that class while the mixin config is applied. Both `endFusion` injects use `require = 0`. The mixin plugin does not look the class up at config time.

## Fusion unfuse bonus

Unfusing clears the `FusionBonus` split bonus on the player who unfused and on their partner. Dragon Mine Z only removed that bonus from the fusion leader, and only when the leader was online. Permanent Saiyan zenkai bonuses are left in place. A bonus whose name is both a fusion bonus and a zenkai bonus is removed.

## Menu wording

Sparring leaderboard names match the numbers they show: training points, sessions, perfect spars, combo, and time. Dojo rankings call war wins "War wins" and reputation "Reputation". The prestige page lists 20,000, 40,000, 60,000, and 80,000 for the first four completed prestiges, then 50,000 or 100,000. Notices that only describe status use yellow. Buttons that ask you to pick someone say to select them in the list.

## Prestige requirement follows completed count

The first four completed prestiges require 20,000, then 40,000, then 60,000, then 80,000 power. How many prestige you are holding does not change those four. From the fifth prestige on, holding none requires 50,000 and holding one or more requires 100,000. This build includes the 4.6.51 Hakai helper visibility fix.

## Prestige requirement scaling

4.6.52 keyed those first four amounts off held prestige. 4.6.53 keys them off completed prestige instead. A stored need floor still cannot replace the scaled number with a flat 20,000. The status line and the prestige menu say the current requirement and the one after it.

## Hakai helper visibility

Helpers in `HakaiDestroyerGateMixin` are private. A package-private static method is merged into the Noea class and mixin application fails. The inject handlers stay private and static, because `onKiAttackFire` and `sourceSaysHakai` are static. The build rejects a non-private mixin method that is not an inject, redirect, overwrite, or shadow.

## Mixin cleanup

Default-priority StatsData hooks (load guard, owner lookup, secondary-attribute player, restore clamp, reset wipe) are one mixin, `StatsDataGuardsMixin`. The three default-priority Resources clamps are one mixin, `ResourcesEnergyDrainGuardMixin`. `/dmzstats reset` player block and absorption wipe are one mixin. Every absorption wipe calls `AbsorptionWipeHelper`. The Hakai Destroyer methods are cached after the first successful lookup. A failed lookup is not cached.

These stayed separate because a mixin can only target one class, and a different priority still has to win: the personal stat cap (5000), the absorption bonus (1001), stat scaling (6100), and the death TP slots (6200). `DojoWarSenseMixin`, `PotionEffectHelperTpBonusMixin`, and `DmzRevampPrestigeCapMixin` are still live. This build is not uploaded. Restart onto 4.6.49 until this jar is loaded.

## Hakai mixin applies

The Hakai mixin is no longer skipped during mixin config. Loading `DivineImmortalityEvents` there always failed because Noea is not loaded yet, so the gate returned false and the mixin never applied. The mixin names that class as a string. Each inject uses `require = 0`. Mixin 0.8.5's `@Mixin` annotation has no `require` parameter. Restart onto 4.6.49 to load it.

## Fall health pool

A tall fall logs the player, vanilla current and max health, DMZ max health, fall distance, damage, and whether ki negation consumed the hit. DMZ does not store a current health. When vanilla max health is far below that DMZ max, the vanilla pool is raised to the DMZ max before fall damage is applied. The damage amount is unchanged.

## Hakai damage-message gate

`sourceSaysHakai` reads the projectile technique id, the damage message, and the technique name. A name that contains "hakai" returns false unless the attacker is an apprentice or appointed Destroyer. A missing Destroyer lookup is not a Destroyer. Official Hakai projectiles still go through Noea's own check. Restart onto 4.6.48 to load it.

## Hakai mixin load gate

Destroyer rank is read by reflection when an attack fires, so transforming the Noea listener does not load Noea's Destroyer classes.

## Fall damage log packaging

`FallDamageDiag` is in the Forge jar. 4.6.44 registered the listener and left the class out, which would crash mod init. Do not restart onto 4.6.44.

## Hakai name gate

A ki technique whose id or name contains "hakai" no longer takes Noea's insta-kill path unless the attacker is an apprentice or appointed Destroyer. Official Hakai technique ids are unchanged. They already require that role.

## Fusion cooldown reset

Staff can run `/lm fusionreset <player>` on someone who is online. It sets Noea's `fusionCooldownEnd` to 0, removes Dragon Mine Z's `FusionCooldown`, clears `fusionValidationError`, and syncs both. The player has to be online. The command does not end an active fusion.

## Fall damage log

A tall fall prints one console line, `[LM] Fall damage:`. It shows the entity health, the entity max, DMZ's max health, and the health attribute. It does not change the damage. DMZ does not keep a separate current health.

## Menu notices

Every flash notice uses the same colors. The header is gold, `§6§lNotice`. The body is yellow, `§e`. A screen no longer picks green, gray, or red for that text. On and off are written in the words. The difficulty page's own status lines (personal difficulty, Ancient Coins, scaled mobs) are unchanged.

## Absorption wipe crash

The Majin wipe classes now live outside the mixin package, so the server can load them. Creating a character, changing race, Dende's reset, and prestige can clear the stored absorption bonus. If a wipe throws, the error is printed and the server tick keeps going.

## Stat reset and race change

Players cannot use `/dmzstats reset`, including the percent form. Staff still can. Change race stays in Character Services. Dende's reset still works. Prestige wipes stats on its own, the same full wipe as a 0% reset, and does not go through that command. Class change, reskin, and head parts are unchanged.

## List selection

Highlighting a row no longer previews or opens it. Head parts uses a Preview button on the highlighted part. Race and class use Choose this race and Choose this class. Rival, spar, and difficulty lists use Open, Choose, or Details on the highlighted row. Double-click still equips a title. The wheel scrolls the list.

## Menu layout

The character preview shrinks when you are larger than a normal player, and grows when you are smaller, so a giant form stays inside the preview box. A result notice makes the menu taller instead of squishing the buttons.

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

Death in space, Namek, Beerus' planet, and the other Noea travel dimensions uses the same corpse as everywhere else. Noea was clearing the drop list, and corelib then deleted every inventory item that was not on that list, so the corpse spawned empty and the items came back only on respawn. Stopping that clear leaves the drops in place, and the corpse gets them. They are not returned on respawn. A corpse in the void stays in the void.

## Dragon balls

Left-click a placed dragon ball once with the radar for that set and the ball goes into your inventory. The Earth radar takes Earth balls. The Namek radar takes Namek balls. A different radar leaves the ball where it is. That click still works inside an FTB Chunks claim or a GriefPrevention claim. Other blocks in those claims stay protected. A full inventory drops the leftover at your feet. The radar forgets that spot. Right-click with all seven still summons the dragon.

## Majin absorption bonus

The stored melee and ki bonus applies while absorption power is above zero, scaled by the same power-release percent as melee and ki. The read happens after Noea adds the raw bonus, then replaces that raw add with the scaled one. At zero power the bonus is left out of the damage read and the stored numbers stay, so absorbing again still adds to them. `/dmzstats reset` clears those stored numbers from `StatsCommand.resetStats`. The melee self form clears them from its own executor. Dende's reset (action 2) clears them from `handleDende`. A race change clears them in character services, and finishing a new character clears them again before the new race is applied. The console prints `[LM] wipeAbsorption firing for <name> via <method>` for each of those, then whether Noea's data copy was present, the stored melee before it was zeroed, and the melee read back after save. If `clear()` throws before those lines, the same console prints `[LM] clear() threw` and the stack. Battle power is unchanged.

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
