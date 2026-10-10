GUI Humanization Audit — LegacyMechanics
4.6.53
Audited: 2026-10-08 against /tmp/LM-4.6.53.jar  (live server jar) Scope: All user-facing strings in
com/dbzlegacy/adaptivedifficulty/gui/  (47 classes) Goal: Every label, button, notice, and empty
state should read like a helpful game guide — not a database admin panel.
How to use this doc: Each entry has the exact class ﬁle, the current string (with §  color codes),
and a drop-in replacement. Search the Java source for the current text and replace.
Color convention (already decided): Notice header §6§lNotice , notice body §e . Status is
conveyed in words, not body color.
P0 — Confusing or Broken
P0-1. Sparring leaderboard tabs lie about what they show
File: gui/cnpc/CnpcGuiStyle.java  Current:
Wins
Win streak
Dojo wars
Problem: The "Wins" and "Win streak" tabs silently display Training Points (there are no
wins/streak categories in SparringSystem.topLines ). The "Wars" tab shows Reputation
( DojoRankings.score()  has no wars category). Players looking for their win record see TP
numbers with no explanation.
Fix:
Wins  →  Training Points
Win streak  →  Top Sessions  (if it shows session count) or remove the tab if it duplicates TP
Dojo wars  →  Reputation
If the underlying data truly can't provide wins/streaks/wars, the tabs must be renamed to match
reality. Do not keep aspirational labels.
P0-2. Prestige requirement doesn't explain scaling

File: gui/cnpc/CnpcLmPrestigeGui.java  (and ProgressionGuiApi.java ) Current:
§7Prestige: §f  (shows count, no requirement context)
§7Requires §e §7power to prestige (next: §e §7)  (format string — verify the "next" value
actually populates)
Problem: Per the 4.6.53 ﬁx, the ﬁrst four prestiges require 20k, 40k, 60k, 80k, then 50k/100k from
the ﬁfth on. If the GUI just says "Requires X power" without explaining the pattern, players will be
confused when the number jumps.
Fix: Add a persistent explainer line on the prestige page:
§7Each prestige costs more than the last:
§71st §f20,000 §8· §72nd §f40,000 §8· §73rd §f60,000 §8· §74th §f80,000
§7From the 5th on: §f50,000 §7(§f100,000 §7if you're holding any)
The existing string §7The first four completed prestiges require §f20,000§7, then
§f40,000§7, then §f60,000§7, then §f80,000§7.  is good — make sure it's visible on the main
prestige page, not buried.
P0-3. "§aYou can prestige now." — green notice body
File: progression/shop/PrestigeSystem.java  (string: §aYou can prestige now. ) Problem: If this
goes through the shared notice path, CnpcMenuFeedback.noticeBody  normalizes to §e . If it's sent
as a raw chat message or inline notice, it bypasses the normalizer and shows green. Per the color
uniformity decision, all notice bodies are §e .
Fix: Change to §eYou can prestige now.  OR route it through CnpcMenuFeedback  so the
normalizer handles it. Audit all §a / §c  preﬁxed strings that are notices (not button labels) —
buttons keep their colors, notice bodies don't.
P1 — Unclear Wording
P1-1. Character Services: "§cPlayers only."
File: gui/CharacterServicesGuiApi.java  Current: §cPlayers only.  Problem: Robotic. Doesn't
explain why or what to do. Fix: §cThis only works in-game — console can't use character
services.

P1-2. "§cUnknown character action: "
File: gui/CharacterServicesGuiApi.java  Current: §cUnknown character action: (with trailing
raw action ID) Problem: Exposes internal action IDs to players. If a player ever sees this, it's a bug
— but the message should still be human. Fix: §cSomething went wrong. Try again, or ask
staff if it keeps happening.
P1-3. "§cInvalid race conﬁrm." / "§cPick a class."
File: gui/CharacterServicesGuiApi.java  Current:
§cInvalid race confirm.
§cPick a class.
Problem: Terse. "Invalid race conﬁrm" is developer-speak. Fix:
§cThat didn't work — try picking your race again.
§cChoose a class first, then confirm.
P1-4. Rival: "§cPick a player to declare." (×7 variants)
File: gui/RivalGuiApi.java  Current:
§cPick a player to declare.
§cPick a player to accept.
§cPick which Mutual to replace.
§cPick a player to decline.
§cPick a player to remove.
§cPick a player for silent rival.
§cPick a player to challenge.
Problem: Repetitive and doesn't explain where to pick. The CNPC GUI has a player list — tell them
to use it. Fix: Use the existing pattern from CnpcGuiStyle : §7Select a player below  as the
primary instruction, then the action-speciﬁc text as a subtitle. E.g.:
§7Select a player below to declare as your rival.
Or keep them short but warmer:
§cChoose who to declare as your rival first.

P1-5. Sparring: "§cPick a player to invite as apprentice." / "§cPick a player to
ask as mentor."
File: gui/SparGuiApi.java  Current:
§cPick a player to invite as apprentice.
§cPick a player to ask as mentor.
§cPick a rival dojo master.
§cPick a banner from the Banner menu.
Problem: Same as P1-4 — doesn't say where. Fix:
§cSelect a player below, then invite them as your apprentice.
§cSelect a player below, then ask them to be your mentor.
P1-6. "§7Unknown entry" / "§7That title could not be found."
File: gui/cnpc/CnpcLmDifficultyGui.java  Current:
§7Unknown entry
§7That title could not be found.
Problem: "Unknown entry" is a database term. The second is better but still cold. Fix:
§7Couldn't find that — try refreshing the list.
P1-7. Diﬃculty: "§cTurn personal diﬃculty ON ﬁrst."
File: gui/cnpc/CnpcLmDifficultyGui.java  Current: §cTurn personal difficulty ON first.
Problem: Doesn't say where the toggle is. Fix: §cTurn on §ePersonal difficulty §con the main
Difficulty page first.
(The existing §7Use §ePersonal difficulty §7on the main menu, then return here to pick a
tier.  is good — use that pattern everywhere.)
P1-8. "§8Tier cost anchor §f §8· T7 target §f copper §8(stock 150000 →  100×
Netherite)"
File: gui/DifficultyChatMenu.java  Current: §7Tier cost anchor §f §8· T7 target §f copper
§8(stock 150000 → 100× Netherite)  Problem: "stock 150000 →  100× Netherite" is a developer

note about default values. Players don't need to see internal defaults. Fix: §7Tier cost anchor §f
§8· T7 target §f copper
(Admin-facing, but still shouldn't leak implementation notes.)
P1-9. Progression: "§8Priced features: Diﬃculty tiers · Character Services ·"
/ "§8End dragon summon · head parts"
File: gui/ProgressionGuiApi.java  Current:
§8Priced features: Difficulty tiers · Character Services ·
§8End dragon summon · head parts
§8New paid LM features should use the same gate.
Problem: "Priced features" and "New paid LM features should use the same gate" are developer-
facing. This is the Ancient Coins staﬀ panel — it should read like admin documentation, not code
comments. Fix:
§8These features charge Ancient Coins:
§8Difficulty tiers · Character Services · End dragon summon · Head parts
Remove §8New paid LM features should use the same gate.  entirely (it's a code comment,
not UI text).
P1-10. "§cLegacyMechanics mod unreachable."
File: gui/ProgressionGuiApi.java  Current: §cLegacyMechanics mod unreachable.  Problem:
Technical. Players don't know what "unreachable" means for a mod. Fix: §cCouldn't reach the
server's systems. Try relogging.
P1-11. Rival: "§8Silent →  Declared →  accept in Declare invites →  Mutual"
File: gui/RivalGuiApi.java  Current: §8Silent → Declared → accept in Declare invites →
Mutual  Problem: This is a state machine diagram, not help text. New players won't understand
the ﬂow. Fix:
§7How rivalries work:
§71. §fDeclare §7someone as your rival (or mark them §fSilent§7)
§72. §fThey accept §7your invite
§73. §fYou're now Mutual §7— bonuses activate

P1-12. Sparring: "§eChoose: §fLeave mentor §8or §fRelease apprentice"
File: gui/SparGuiApi.java  Current: §eChoose: §fLeave mentor §8or §fRelease
apprentice§8GUI: Training bonds → Leave mentor / Release apprentice…  Problem: The trailing
§8GUI: Training bonds → Leave mentor / Release apprentice…  is a developer note about where
the buttons are. The player is already looking at the choice. Fix: §eLeave your mentor, or
release one of your apprentices?
P1-13. "§7Only one at a time · remove the current form before switching"
File: gui/ProgressionGuiApi.java  Current: §7Only one at a time · remove the current form
before switching  Problem: Which form? Remove how? Fix: §7You can only have one special
form at a time. Remove your current one (Majin or Mutant) before picking the other.
P1-14. Character: "§8Ancient Coins are charged when you conﬁrm on the
next screen."
File: gui/CharacterServicesGuiApi.java  Current: §8Ancient Coins are charged when you
confirm on the next screen.  Problem: Passive voice, slightly unclear about when the charge
happens. Fix: §8You'll be charged Ancient Coins when you confirm on the next screen.
P1-15. "§7Fighting class cannot be changed during a reskin."
File: gui/CharacterServicesGuiApi.java  Current: §7Fighting class cannot be changed during
a reskin.  Problem: States a restriction without explaining why or oﬀering an alternative. Fix:
§7Reskin only changes how you look. To change your fighting class, use §fChange class
§7instead.
P2 — Polish
P2-1. Inconsistent: "Head parts" vs "Head Parts"
Files: CharacterServicesGuiApi.java , CnpcLmCharacterGui.java  Current: Both Head parts  and
Head Parts  appear. Also §cHead parts is turned off  (grammatically oﬀ — should be "are").
Fix: Standardize on Head parts  (sentence case, matches other menu items). Fix grammar:
§cHead parts are turned off on this server.

P2-2. Inconsistent: "Reskin" capitalization in buttons
File: gui/cnpc/CnpcLmCharacterGui.java  Current: §dReskin , Confirm reskin , §eReview cost &
continue  Problem: Minor — "Reskin" vs "reskin" mixed. Fix: Button labels use Title Case ( Confirm
Reskin ), descriptive text uses sentence case. Pick one and apply consistently.
P2-3. "§7« Back" vs "§7« Hub" vs "Main menu" vs "Main"
Files: All ChatMenu classes Problem: Back navigation uses four diﬀerent labels: §7« Back , §7«
Hub , Main menu , Main . Players can't build a mental model. Fix: Standardize:
§7« Back  — go up one level
§7« Hub  — go to the main LM hub (only from top-level menus)
Remove Main menu  and Main  as button labels.
P2-4. "§8 ──────────────── " separator overuse
Files: All ChatMenu classes Problem: The 16-dash separator appears 2-3 times per menu. It's
visual noise. Fix: Use once per menu — between the header and the content. Remove duplicates.
P2-5. Rival: "§d[Achs]" abbreviation
File: gui/RivalChatMenu.java  Current: §d[Achs]  →  Achievements  Problem: "Achs" is gamer
slang that not everyone knows. The CNPC GUI uses Achievements  (full word). Fix:
§d[Achievements]  — chat buttons can be wider, clarity beats brevity.
P2-6. "§6[HOF]" abbreviation
File: gui/RivalChatMenu.java  Current: §6[HOF]  →  Hall of Fame  Problem: Same as P2-5. CNPC
uses Hall of fame . Fix: §6[Hall of Fame]
P2-7. Sparring: "§6§lPERFECT TRAINING"
Files: SparChatMenu.java , SparGuiApi.java  Current: §6§lPERFECT TRAINING  Problem: ALL CAPS
+ bold reads as shouting. What does "perfect training" even mean? (It's when you trade hits within
30 blocks with no active spar?) Fix: §6§lSparring  as the header. If "perfect training" is a speciﬁc
mechanic, explain it: §7Trade hits within 30 blocks to start a spar.

P2-8. "§c[Remove Android]" in hub menu
File: gui/MechanicsChatMenu.java  Current: §c[Remove Android]  with description Remove
Android upgrade  Problem: "Remove Android" sounds like you're deleting a phone OS. In context
it's "remove the Android conversion upgrade." Fix: Button: §c[Remove Android Upgrade] .
Description: Undo your Android conversion and restore your previous forms.
P2-9. Diﬃculty: "§e[Diﬃculty OFF]" vs "§a[Diﬃculty ON]"
File: gui/DifficultyChatMenu.java  Current: Toggle buttons use green for ON, yellow for OFF.
Problem: Minor inconsistency — ON/OFF toggles elsewhere use §2§lON  / §8§lOFF
(CnpcGuiSupport). The chat menu should match. Fix: This is a chat menu (not CNPC), so the
existing §a / §e  is acceptable. But ensure the meaning is clear: [Difficulty ON]  should mean
"diﬃculty is currently ON, click to turn OFF" OR "click to turn ON"? Add clarity: §a[Turn Difficulty
ON]  / §e[Turn Difficulty OFF] .
P2-10. "§7No active spar — trade hits within 30 blocks to start."
Files: SparChatMenu.java , SparGuiApi.java  Current: §7No active spar — trade hits within 30
blocks to start.  Problem: Actually pretty good! But "trade hits" is slightly jargon-y. Fix: §7No
sparring session right now — hit each other within 30 blocks to start one.
P2-11. Empty states are good but could be warmer
Files: Various CNPC GUIs Current (good examples):
§7Your list is empty — start from Actions  (Rival)
§7No bond invites waiting.  (Spar)
§7Nobody else is online right now — try again when other players are on.  (Rival)
Problem: These are actually good! The issue is inconsistency — some empty states are helpful,
others are just §7No data  or similar.
Fix: Audit all list pages. Every empty state should follow the pattern: What’s missing  + —  + what
to do next . Examples:
Bad: §7No head parts on this page.  →  Good: §7No head parts here — try the next page
or use search.

Bad: §7This list is empty — there is nothing to choose yet.  →  Good: §7Nothing to
choose yet — check back later.
P2-12. "§8Tap Remove to end this rivalry."
File: gui/RivalGuiApi.java  Current: §8Tap Remove to end this rivalry.  Problem: "Tap" is
mobile language. On PC it's "click." Fix: §8Click Remove to end this rivalry.  (Or use neutral
"Select Remove" if you want to cover both.)
Actually — the codebase uses "Tap" consistently ( §7Tap a name — Accept or Decline , §7Staff:
tap a row ). This is a deliberate style choice. Leave it, but be consistent — don't mix "tap", "click",
and "double-click" randomly. Pick "tap" for CNPC GUI and "click" for chat, or standardize on one.
P2-13. Admin strings leak to players
File: gui/ProgressionGuiApi.java  Current: §8CNPC: Staff Admin → Progression panel → TP
gains → Global TP boost  Problem: This is a navigation hint for staﬀ, but it's in a string that
players might see. The §8  (dark gray) makes it subtle, but it's still confusing. Fix: Gate staﬀ-only
hints behind a permission check so players never see them. If that's too invasive, at least preﬁx
with §8[Staff] .
P2-14. "§7Skill Check is available for you." vs "§8Skill Check is a donator
perk — ask staﬀ if interested."
File: gui/MechanicsGuiApi.java  Current: Two variants depending on access. Problem: The "not
available" version is good. The "available" version is bland. Fix: §7Skill Check is unlocked for
you — open it from the hub.
P2-15. Prestige: "§7Turn in §f1§7, §f2§7, §f3§7, §f6§7, or §f9 §7at a time"
File: gui/ProgressionGuiApi.java  Current: §7Turn in §f1§7, §f2§7, §f3§7, §f6§7, or §f9
§7at a time  Problem: Why these speciﬁc numbers? The bulk bonus ( 3→4, 6→9, 9→15 ) is
explained separately, but the connection isn't obvious. Fix: Combine them:
§7Turn in held prestiges: §f1§7, §f2§7, §f3§7, §f6§7, or §f9 §7at a time.
§7Bigger turn-ins give bonus points: §f3→4 §8· §f6→9 §8· §f9→15

Color Uniformity Check
Status: ✅  The shared notice path ( CnpcMenuFeedback.noticeBody ) normalizes body text to §e .
The header is §6§lNotice  in both CnpcGuiSupport  and CnpcMenuFeedback .
Deviations found (notice-like strings with non- §e  bodies that may bypass the normalizer):
String File Color Action
§aYou can
prestige
now.
PrestigeSystem.java
§a
green
Change to §e  or route through
CnpcMenuFeedback
§cYou need
a higher
level
first.
PrestigeSystem /
ProgressionGuiApi
§c
red
If this is a notice (not an error), use §e .
If it's an error blocking action, §c  is
correct.
§aLogs
flushed.
MechanicsGuiApi.java
§a
green
This is a success conﬁrmation — §a  is
arguably correct for "action
succeeded." The uniformity rule says
notices are §e , but success
conﬁrmations might deserve an
exception. Decision needed: Are
success conﬁrmations §a  or §e ?
Recommendation: Deﬁne three message types:
Notice (informational): §6§lNotice  header + §e  body
Success (action completed): §a  body, no header — e.g., §aLogs flushed.
Error (action blocked/failed): §c  body, no header — e.g., §cYou need a higher level
first.
This is already the de facto pattern. Document it so future code follows it.
Summary for Cursor
Total issues: 3 P0, 15 P1, 15 P2 + color uniformity decision
Highest priority:
1. Fix sparring leaderboard tab labels (P0-1) — they're factually wrong
2. Verify prestige "next requirement" displays correctly (P0-2)

3. Decide on §aYou can prestige now.  color (P0-3)
Biggest win for least eﬀort: The P1 "Pick a player to..." strings (P1-4, P1-5) — 11 strings across
two ﬁles, all ﬁxed with the same pattern.
Tone guidance for all replacements:
Write like you're explaining to a friend who's new to the server
Say what happens when you click, not just what the button is called
If something costs money/coins, say so upfront
If something has a cooldown or is irreversible, warn before the click, not after
Never show raw IDs, action names, or state machine transitions to players
