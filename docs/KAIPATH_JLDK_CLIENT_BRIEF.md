# JLDK brief — KaiPath Ultra GUI client (LM submenus)

**Status:** DMZServer ships a drop-in patched jar built from this brief:
`mods/kaipath-1.3.1-lm-ultra.jar` (see `tools/kaipath-lm-ultra-client/`).
Merge these changes into upstream KaiPath so official releases no longer need the overlay.

**LM side (already done):** 4.6.104+ sends full 10-field `GuiOpenPacket` on
`legacymechanics:gui` / protocol `"1"`. Spec: `docs/KAIPATH_GUI_PROTOCOL.md`.

---

## Problem

KaiPath **1.3.0** only decodes **1 of 10** `GuiOpenPacket` fields (`screenId`) and
renders a **hardcoded** `LmHubScreen`. Submenus cannot display no matter what LM sends.

## Required work (critical path = 1–2)

### 1. Decode all 10 fields in `GuiOpenPacket`

| # | Field | Type |
|---|--------|------|
| 1 | screenId | string |
| 2 | title | string |
| 3 | pageId | string |
| 4 | tabsJson | string (JSON array of `{id,label}`) |
| 5 | activeTab | string |
| 6 | linesJson | string (JSON array of body lines) |
| 7 | buttonsJson | string (JSON array of button objects) |
| 8 | panelWidth | varint (default 350) |
| 9 | panelHeight | varint (default 220) |
| 10 | layoutJson | string (JSON: `{panelWidth,panelHeight,center,margin,…}`) |

Button object:

```json
{
  "id": "@difficulty:do:activate:1",
  "label": "§aT1 …",
  "style": "normal",
  "x": 20,
  "y": 120,
  "w": 150,
  "h": 18,
  "args": { "screen": "difficulty", "page": "tiers" }
}
```

### 2. Render dynamic UI (not hardcoded hub)

Replace hardcoded `LmHubScreen` buttons with rendering from the packet:

- Panel centered at `((screenW - panelWidth) / 2, (screenH - panelHeight) / 2)` when
  `layoutJson.center` is true; honor `margin` (default 6).
- Draw **title**, **tabsJson** (tab click → send that tab’s `id`), **linesJson**, then
  **buttonsJson** (`x`/`y` relative to panel origin).

### 3. `GuiActionPacket` verbatim echo

On click, send:

- `screenId` — echo received packet `screenId` verbatim  
- `actionId` — echo button/tab `id` verbatim (do **not** strip/modify)  
- `argsJson` — echo button `args` JSON verbatim  

### 4. Screen IDs

Do **not** require a whitelist. Accept any `screenId` with the dynamic screen class.
(Optional: also register `character`, `difficulty`, `progression`, `prestige`, `spar`,
`rival`, `config`, `admin` — unnecessary if open always uses the dynamic class.)

LM’s hub shim already tunnels non-hub ids as wire `hub`; dynamic button rendering alone
unblocks submenus when the packet body is honored.

### 5. Future-proofing — don’t hardcode anything

To avoid a KaiPath update every time LM adds a menu:

1. **Dynamic screen IDs** — no whitelist in `LmScreens`. Any `screenId` → dynamic screen.
   `MissingScreen` only for malformed packets / construction failures, never for unknown id.
2. **Dynamic tabs** — render whatever `tabsJson` contains; tab click sends that tab’s `id`.
3. **Generic action echo** — never validate/filter/transform `actionId` on the client.
4. **Field tolerance** — defaults for missing fields (`panelWidth`/`panelHeight` → 350×220,
   empty JSON arrays, centered layout); **ignore bytes after field 10** so LM can add
   fields 11+ later.
5. **Protocol version** — Forge SimpleChannel protocol string is `PROTOCOL_VERSION` (`"1"`),
   not a field inside `GuiActionPacket`. `GuiActionPacket` wire fields are
   `screenId`, `actionId`, `argsJson`. If the remote channel protocol differs, **log a
   warning and still accept** — do not refuse to open the screen.

**Principle:** the client renders what the server sends, no questions asked. Every
hardcoded list (screens, tabs, actions, buttons) is a future breakage point.

With items **1–5**, LM can add menus/tabs/buttons/actions without touching KaiPath again.

---

## Reference implementation in this repo

| Path | Role |
|------|------|
| `tools/kaipath-lm-ultra-client/` | Source + `build.sh` → `mods/kaipath-1.3.1-lm-ultra.jar` |
| `docs/KAIPATH_GUI_PROTOCOL.md` | Full wire contract (LM + client) |

Build: `bash tools/kaipath-lm-ultra-client/build.sh`  
Deploy: replace live `kaipath-*.jar` with `kaipath-1.3.1-lm-ultra.jar` (clients), keep LM 4.6.104+.
