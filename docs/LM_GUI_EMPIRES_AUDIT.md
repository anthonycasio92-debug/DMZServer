# LM GUI vs Noea Empires (live server audit)

Date: 2026-10-04 · Live mod count: **141** jars under `mods/`.

## Relevant live GUI mods

| Mod | Role |
|-----|------|
| `LegacyMechanics-4.5.116.jar` | Primary LM Forge CNPC menus (`CnpcGuiSupport`, hub, progression, rival, …) |
| `Noea-Empires-0.9.9-…jar` | Planet/destroyer empire CNPC GUIs (`com.dmzlegacy.noeaempires.*`) |
| `Noea_Build-1.1.5.jar` | Dragon Block Noea core (space, bosses, combat) |
| `ProfTools-0.5.0-alpha24-…jar` | In-game studios (progression/forms); preview UX LM already mirrors |
| `CustomNPCs-…20260711.jar` | `ICustomGui` / scroll panel / colored lines |
| `dmzlegacy-legacysaga-0.4.0-alpha-progression-gui.jar` | Saga port helpers; not a full menu skin |
| `LegacyMechanicsGUI` (Bukkit) | **Not on live** — chest/CMI backends retired; live uses **`guiBackend: cnpc`** |

Other live mods (JEI, FTB, Create, etc.) do not define the LM/Empires menu chrome.

## How Noea Empires builds GUIs

All inspected Empires screens use **CustomNPCs `ICustomGui`**, same stack as LM:

- **Sizes:** e.g. `356×250` (team holdings), `382×278` (destroyer office) — slightly **narrower/shorter** than LM’s default `430×(280–460)`.
- **Gui ids:** dedicated range starting ~`910033` (no collision with LM ids in `CnpcLmGui`).
- **Shared helper:** package-local `GuiStyle` (not shipped as a library):
  - `title(gui, width, "Noea: Empires — …")` — centered **gold** label, scale **0.92**, y≈7
  - **Rule line:** `ICustomGui.addColoredLine(…, COLOR_LINE, …)` under the title (not `§8` dash text)
  - `section(…)` — muted/blue caption, scale **0.72**
  - `label(…, color, scale, centered)` with **`brighten(color)`** on `ILabel.setColor` for contrast
  - `applyStandardBackground` — currently a no-op on live jar
- **Copy pattern:** `Noea: Empires — {page}` with em dash; status lines use fixed ARGB palette (`TEXT`, `MUTED`, `GOLD`, `LINE`, …).

Empires does **not** use the right-column **player preview**; LM keeps preview (ProfTools-style) and therefore needs the **wider** window.

## LM before this pass

- `CnpcGuiSupport.paintHeader`: left-aligned `§f§l` title, text subtitle, **ASCII divider**, flash notice band
- `paintSectionTag`: plain `§8…` caption
- Strong accessibility work already: `readableInfoLine`, scroll hints, pick-list overflow, audit script `sim/audit_cnpc_gui_style.py`

## Changes shipped in repo (Empires parity, LM layout preserved)

1. **`CnpcGuiStyle`** — Empires ARGB palette + `brandTitle(page)` → `Legacy Mechanics — {page}`.
2. **`CnpcGuiSupport.title/subtitle/divider`** — gold centered title, colored rule line (fallback to dash divider), brightened subtitle.
3. **`paintSectionTag`** — section color + 0.72 scale like Empires `section()`.
4. **Hub** — uses `brandTitle("Main menu")` and plain subtitle (styled via subtitle color).

## Recommended follow-ups (not all done here)

- ~~Roll `brandTitle` / `brandSubPage` across submenus~~ (done in 4.5.116).
- Optional **compact width** preset (`360×260`) for pages **without** player preview (admin-only screens).
- Align footer nav labels with Empires button spacing where CNPC allows (Empires uses full-width rows at y≈220+).
- Do **not** shrink global `W` on hub — preview column needs `CnpcPlayerPreview.textBandWidth()`.

## Verify after LM deploy

1. `/lm` or CNPC hub — gold centered title, colored line, section captions readable.
2. Run `python3 tools/dmz-adaptive-difficulty/sim/audit_cnpc_gui_style.py`.
3. Compare side-by-side with `/noea` empire menu (if command exists on live) for spacing/title only.
