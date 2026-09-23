# Legacy Mechanics menu rules (CNPC + chest/CMI)

Automated checks: `python3 tools/dmz-adaptive-difficulty/sim/audit_cnpc_gui_style.py` (also runs from `build.sh`).

## Layout

- **Header:** `CnpcGuiSupport.paintHeader` — not legacy `title()`.
- **Body start:** `bodyBelowHeader(infoY)` or `bodyBelowInfo(...)` after status blocks.
- **Left column width:** `listWidth()` / `textBandWidth()` — never `W - 2*M` (overlaps preview).
- **Character preview:** `paintSystemMainPreview` only on each **system main** page (`footer` when `parentPage == null`, or hub/skill-check core main).

## Scroll (CustomNPCs single panel)

- Long status on **button-only** pages: `paintInfoBlock` (wheel on status band **only when** lines exceed inline max).
- Status **above a pick list:** `paintInfoBeforePickList` then `scrollPickList` — never `paintInfoBlock` + `scrollPickList` on the same scroll panel.
- Pick lists: `scrollPickList` only (not raw `scrollSearchable` in menu classes).
- **Hints:** `HINT_PICK_LIST` appears only when list rows overflow the visible band; pick lists scroll via **search + scrollbar drag** (not the status-band wheel). Head bones: 14 cards/page, single-click row to unlock/equip.

## Toggles

- On/off controls: `CnpcGuiStyle.toggleOn` / `toggleOff` (`§2§lON` / `§8§lOFF` prefix) so state is obvious at a glance.
- Flag grids: use `flagOnOff` / `startsWith("§a")`, not `contains("ON")`.

## Forge-only GUI (no Bukkit plugin)

- `GuiBackend.fromConfig()` defaults to **CNPC**; legacy `cmi` / `chest` / `bukkit` config values map to CNPC.
- Player `*Menu.java` classes open `CnpcLmGui` — not `CmiGuiBridge`. Audit: `sim/audit_forge_gui_backend.py`.

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
