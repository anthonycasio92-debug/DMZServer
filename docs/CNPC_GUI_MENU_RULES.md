# Legacy Mechanics menu rules (CNPC + chest/CMI)

Automated checks: `python3 tools/dmz-adaptive-difficulty/sim/audit_cnpc_gui_style.py` (also runs from `build.sh`).

## Layout

- **Header:** `CnpcGuiSupport.paintHeader` — not legacy `title()`.
- **Body start:** `bodyBelowHeader(infoY)` or `bodyBelowInfo(...)` after status blocks.
- **Left column width:** `listWidth()` / `textBandWidth()` — never `W - 2*M` (overlaps preview).
- **Character preview:** `paintSystemMainPreview` only on each **system main** page (`footer` when `parentPage == null`, or hub/skill-check core main).

## Scroll (CustomNPCs single panel)

- Long status on **button-only** pages: `paintInfoBlock` (wheel on status band).
- Status **above a pick list:** `paintInfoBeforePickList` then `scrollPickList` — never `paintInfoBlock` + `scrollPickList` on the same scroll panel.
- Pick lists: `scrollPickList` only (not raw `scrollSearchable` in menu classes).

## Copy

- Shared hints: `CnpcGuiStyle.HINT_*` (avoid raw “Click to …” in `paintHeader` subtitles).
- Disabled rivals/spar: `CnpcGuiStyle.MSG_RIVALS_OFF` / `MSG_SPAR_OFF`.
- Separators: `CnpcGuiStyle.SEP` (` §8· `).

## Navigation

- System mains: `navSystemRoot` → **Hub**.
- Subpages: `navSubmenu` with `§7« Back`.

## Bukkit chest / CMI

- Button copy: `gui-tooltips.json` keys referenced from Java (`tipBtn` / `GuiTooltips`).
- Policy: `tools/dmz-adaptive-difficulty-gui/sim/gui_tooltip_policy.py` — no banned jargon (telemetry, deep-link, etc.) in player-facing strings.

## Build

- CNPC Java overlays merge onto `base/LegacyMechanics-4.5.49-consolidated.jar`; ki/stamina mixin wiring stays on the base jar (see `base/README.md`).
