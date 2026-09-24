# GUI humanization audit

- Catalog keys: **293** · Referenced from Java: **261**
- Catalog revision: **Java 200** · **JSON 201** · **MISMATCH**

## Summary

- No blocking issues
- **0** Java fallback lines still use robotic "Click to…" (use `&8Tap…` or catalog keys)
- **25** catalog warnings

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

## Humanize safety

- Catalog **lore** replaces Java fallback lore when present — do not add short static lore on dynamic buttons (tier cost, DMZ/Prestige gates, `{action}` toggles).
- `JAVA_LORE_ONLY_KEYS` in `gui_tooltip_policy.py` — catalog must omit `lore` for those keys.
- **Icons** (`Material.*`) are only changed in Java GUIs, not by humanize scripts.

## Maintenance

- Edit `gui-tooltips.json` then `/lm admin reload`
- Run `humanize_gui_tooltips.py` for phrase polish only; then `audit_gui_tooltips.py` (must PASS)
