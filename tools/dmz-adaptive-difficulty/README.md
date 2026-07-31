# DMZ Adaptive Difficulty (v3.0.0)

**Server-side only** Forge mixin mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

Implements **Developer Specification V3**  
(`DragonMineZ_Adaptive_Difficulty_System_Concept_V3.txt`).

## V3 model

| System | Behavior |
|--------|----------|
| Unlock tiers (1–7) | Unlocked by DMZ level **or** Prestige ≥ tier id |
| Activation | Spend **Ancient Coins** to activate an unlocked tier |
| Active difficulty | Spend Ancient Coins to raise level inside the tier ceiling |
| Death | Clears active tier + level (unlocks / prestige / coins kept) |
| Combat Rating | Cached `DMZ×w + Prestige×w + Transform + Active` → enemy scale |
| Team modes | Personal / Threshold bonus / Full contribution |
| Kill rewards | Ancient Coins (tiered quality) + XP/drops/capsules/titles (no TP) |

## Install

1. `mods/dmz_adaptive_difficulty-3.0.0.jar` (remove older AD jars)
2. `plugins/dmz_adaptive_difficulty_gui-3.0.0.jar`
3. Restart — config regenerates with V3 keys at `config/dmz_adaptive_difficulty.json`
4. `/difficulty` → **Tiers** to activate → **Upgrade** to raise level

## Commands

- `/difficulty` — GUI
- `/difficulty admin reload|settings|set <key> <value>`
- GUI actions: `activate <1-7>`, `up`/`upgrade`, `down`, `team`, `reset`

## Notes

- The End stays in `disabledDimensions` by default (End Strength script owns it).
- Legacy Lightman's purchase path is replaced by the Ancient Coin wallet.
- Pre-V3 `purchased`/`active` NBT is migrated best-effort on load.
