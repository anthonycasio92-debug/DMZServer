# DMZ Adaptive Difficulty (v3.1.0)

**Server-side only** Forge mixin mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

Implements **Developer Specification V3**  
(`DragonMineZ_Adaptive_Difficulty_System_Concept_V3.txt`).

## V3 model

| System | Behavior |
|--------|----------|
| Unlock tiers (1–7) | Unlocked by DMZ level **or** Prestige ≥ tier id |
| Buy tier | Spend inventory Ancient Coins (sets full tier difficulty) |
| Active difficulty | Set only by tier purchase — no +difficulty upgrades |
| Death | Clears active tier + level (unlocks / prestige / coins kept) |
| Combat Rating | Cached `DMZ×w + Prestige×w + Transform + Active` → enemy scale |
| Team modes | Personal / Threshold bonus / Full contribution |
| Kill rewards | Ancient Coins **drop on the ground** at the mob (+ modest XP) |
| Feature gates | T2 evo → T3 AI → T4 elite → T5 mutation → T6 boss → T7 full |

## Removed (pre-V3)

- Training Point grants / TP multipliers
- Per-level +difficulty coin upgrades / set-max
- Titles system
- Iron-coin / Lightman's main-chain difficulty payments
- Free bootstrap coin grants
- Capsule / emerald / diamond kill inventory dumps
- Zenith ladder UI (Awakened→Zenith) for players

## Install

1. `mods/dmz_adaptive_difficulty-3.1.0.jar` (remove older AD jars)
2. `plugins/dmz_adaptive_difficulty_gui-3.1.0.jar`
3. Restart — config regenerates at `config/dmz_adaptive_difficulty.json`
4. `/difficulty` → **Buy Tier**

## Commands

- `/difficulty` — GUI
- `/difficulty admin off|on|toggle|status` — master system switch (ops)
- `/difficulty admin reload|settings|set <key> <value>`
- Actions: `activate <1-7>`, `down`, `team`, `reset`, `character_reset`

When disabled: no mob scaling, kill coins, AI, death reset, or tier purchases.
Config key: `enabled` (also `admin set enabled false`).

## Notes

- The End stays in `disabledDimensions` by default (End Strength script owns it).
- Ancient Coins are real Lightman's `coin_ancient` items (exact type/count, no change).
- Old NBT wallet balances migrate into Copper Ancient coins on login.
