# GUI humanization audit

- Catalog keys: **270** · Referenced from Java: **247**
- GuiTooltips catalog revision: see `GuiTooltips.CATALOG_REVISION` / `gui-tooltips.json` `_catalogRevision`

## Summary

- No blocking issues
- **0** Java fallback lines still use robotic "Click to…" (use `&8Tap…` or catalog keys)
- **20** catalog warnings

## Icons (materials)

| Pattern | Use |
|---------|-----|
| `LIME_DYE` / `GRAY_DYE` | Personal toggles ON/OFF, staff flags, coin bypass |
| `GOLD_INGOT` | Ancient Coins / economy |
| `BELL` | Chat message toggles ON |
| `DRAGON_EGG` / `GRAY_DYE` | End dragon summon ready / locked |
| `REPEATER` | Staff admin / flag boards |
| Tier mats `COPPER`→`NETHER_STAR` | Difficulty tiers T1–T7 |

## Maintenance

- Edit `gui-tooltips.json` then `/lm admin reload`
- Run `python3 tools/dmz-adaptive-difficulty/sim/humanize_gui_tooltips.py` for batch phrase polish
- Run `python3 tools/dmz-adaptive-difficulty/sim/audit_gui_tooltips.py` before ship
