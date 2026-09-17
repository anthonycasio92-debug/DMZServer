# GUI humanization audit

- Catalog keys: **272** · Referenced from Java: **249**
- Catalog revision: **Java 195** · **JSON 195** · synced

## Summary

- No blocking issues
- **7** Java fallback lines still use robotic "Click to…" (use `&8Tap…` or catalog keys)
- **22** catalog warnings

## Icons (materials)

| Pattern | Use |
|---------|-----|
| `LIME_DYE` / `GRAY_DYE` | Personal toggles ON/OFF, staff flags, coin bypass |
| `GOLD_INGOT` | Ancient Coins / economy |
| `BELL` | Chat message toggles ON |
| `DRAGON_EGG` / `GRAY_DYE` | End dragon summon ready / locked |
| `REPEATER` | Staff admin / flag boards |
| Tier mats `COPPER`→`NETHER_STAR` | Difficulty tiers T1–T7 |

## Policy compliance (`gui_tooltip_policy.py`)

- Preservation checks (lore shrink / placeholder drop / blocked keys): **0 blocking**
- `JAVA_LORE_ONLY_KEYS` (1): catalog must not define `lore`
  - ✓ `difficulty.tiers.tier` — Java-only dynamic lore
- `REQUIRED_PLACEHOLDERS` (2):
  - ✓ `difficulty.titles.item` — has ['perk', 'rarity_line', 'req']
  - ✓ `progression.economy.staff_free` — has ['action']

## Robotic fallback lore (sample)

- ROBOTIC [tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/GuiBoardHelper.java]: &aUnlocked &8· click to purchase
- ROBOTIC [tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/CmiSparGui.java]: &ePending wars — click to respond
- ROBOTIC [tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/SparChestGui.java]: &ePending wars — click to respond
- ROBOTIC [tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/CmiDifficultyGui.java]: &aCurrently equipped &8· click to unequip
- ROBOTIC [tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/CmiDifficultyGui.java]: &aUnlocked &8· click to equip
- ROBOTIC [tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/DifficultyChestGui.java]: &aCurrently equipped &8· click to unequip
- ROBOTIC [tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/DifficultyChestGui.java]: &aUnlocked &8· click to equip

## Humanize safety

- Catalog **lore** replaces Java fallback lore when present — do not add short static lore on dynamic buttons (tier cost, DMZ/Prestige gates, `{action}` toggles).
- `JAVA_LORE_ONLY_KEYS` in `gui_tooltip_policy.py` — catalog must omit `lore` for those keys.
- **Icons** (`Material.*`) are only changed in Java GUIs, not by humanize scripts.

## Maintenance

- Edit `gui-tooltips.json` then `/lm admin reload`
- Run `humanize_gui_tooltips.py` for phrase polish only; then `audit_gui_tooltips.py` (must PASS)
