# KaiPath ↔ LegacyMechanics Ultra GUI protocol

Channel: `legacymechanics:gui` · protocol string: `"1"` (append-compatible).

LM is **server-only**. KaiPath owns the client screen (`LmScreens` / DMZUltra widgets).

### Screen id contract (KaiPath owns the renderer)

Protocol / LM builders use logical ids: `hub`, `difficulty`, `rival`, `spar`, `prestige`,
`character`, `progression`, `config`, `admin`.

When Ultra is active (`guiBackend=ultra` + client has KaiPath), every `open:<system>` and
submenu/tab navigation builds a `GuiOpenPacket` via the matching `Ultra*Screens` builder.
There is **no CNPC fallback inside the Ultra path**. CNPC is used only when Ultra cannot run
(missing KaiPath or `guiBackend=cnpc`); that fallback is logged.

#### Hub wire shim (KaiPath 1.3.0)

KaiPath 1.3.0 `LmScreens.registerDefaults()` only registers **`hub`**. LM tunnels any logical
id not in `UltraScreenIds.KAIPATH_REGISTERED_IDS` by setting the wire `screenId` to **`hub`**.
Title, tabs, buttons, panel **430×280**, and layout (`center: true`, `margin: 8`) stay as built.
The logical system is stored in:

- button `args.screen` / `args.system` (echoed into `GuiActionPacket`)
- `layoutJson.system` / `layoutJson.screen`
- `@&lt;logical&gt;:` prefixes on button/tab action ids

`UltraGuiActions` resolves the logical screen from that metadata (not the wire `hub` id).
Expand `KAIPATH_REGISTERED_IDS` when KaiPath adds handlers — one-line change disables the shim
for those ids.

At `LmGuiNetwork.register()`: protocol id check, builder self-test, tab + hub-shim self-checks,
and a single info line listing tunneled ids.

## GuiOpenPacket (S→C)

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | System id: `hub`, `difficulty`, `rival`, `spar`, `prestige`, `character`, `progression`, `config`, `admin` |
| 2 | `title` | string | Colored title |
| 3 | `pageId` | string | Page within the system |
| 4 | `tabsJson` | string | JSON **array** of `{id,label}` — `id` is a full action (`@difficulty:tab:tiers`) |
| 5 | `activeTab` | string | Active tab key (unqualified, e.g. `tiers`) |
| 6 | `linesJson` | string | JSON **array** of body lines |
| 7 | `buttonsJson` | string | JSON **array** of button objects (below) |
| 8 | `panelWidth` | varint | **Required for correct layout** — GUI pixels, default **430** |
| 9 | `panelHeight` | varint | **Required for correct layout** — GUI pixels, default **280** |
| 10 | `layoutJson` | string | JSON object (below) |

### Panel layout (do not fullscreen)

Match DMZUltra / CNPC dialogs: a **centered panel**, not a framebuffer fill.

**LM always populates fields 8–10** for every Ultra screen (`hub`, `difficulty`, `rival`, `spar`,
`prestige`, `character`, `progression`, `config`, `admin`) via `UltraScreenSpec` →
`GuiOpenPacket`. Defaults are **430×280**, `center: true`, `margin: 8`. The server logs a warning
if an outgoing packet has `panelWidth <= 0` or `panelHeight <= 0` (should never happen).

`layoutJson` keys (KaiPath **must** honor these):

```json
{
  "panelWidth": 430,
  "panelHeight": 280,
  "width": 430,
  "height": 280,
  "center": true,
  "margin": 8
}
```

**Expected KaiPath (`LmScreens`) behavior:**

1. Read varints field 8 (`panelWidth`) and field 9 (`panelHeight`); parse field 10 (`layoutJson`).
2. Prefer wire varints; fall back to `layoutJson.panelWidth` / `layoutJson.width` (same for height).
3. When `center` is true (default), place the dialog at  
   `((screenW - panelWidth) / 2, (screenH - panelHeight) / 2)`.
4. Keep at least `margin` GUI pixels from the screen edge.
5. Button `x`/`y` are **relative to the panel origin**, not the framebuffer.
6. **Do not** stretch the panel to the framebuffer. Fullscreen when these fields are present is a
   **KaiPath client bug** — fix in `LmScreens`, not by changing LM packet shape.

Defaults match CNPC `CnpcGuiSupport` (**430×280**).

### Button object

```json
{
  "id": "@difficulty:do:activate:1",
  "label": "§aT1 …",
  "style": "normal",
  "x": 20,
  "y": 120,
  "w": 150,
  "h": 20,
  "args": { "screen": "difficulty", "page": "tiers" }
}
```

## GuiActionPacket (C→S)

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | Echo `GuiOpenPacket.screenId` |
| 2 | `actionId` | string | Echo button/tab `id` **verbatim** (includes `@screen:` prefix) |
| 3 | `argsJson` | string | Echo button `args` (merge click extras if any) |

### Action id shapes

- `@<screen>:do:<action>` or `@<screen>:do:<action>:<arg…>`
- `@<screen>:tab:<tabId>`
- `@<screen>:nav:hub` / `nav:home` / `nav:refresh`
- `@<screen>:open:<system>`

Server strips `@screen:` first (so a wrong/empty packet `screenId` still routes). Then `do:` / `tab:` / `nav:` / `open:` are handled.

**Hub compatibility:** if the screen is `hub` and the action has no verb prefix, a bare system name (`character`, `difficulty`, `progression`, `prestige`, `spar`, `rival`, `config`, `admin`) is treated as `open:<system>`. Prefer sending the full `@hub:open:…` form.

## Failure logging

Unhandled exceptions and “Something went wrong” handler results are logged on the server with
`screen`, **`rawActionId`** (as received, before `@screen:` strip), resolved action, and stack
traces under the `LegacyMechanics` logger.

**Player-facing chat must never include raw action IDs.** Messages stay human (e.g.
“Something went wrong. Try again, or ask staff if it keeps happening.”). Debug detail is
server-log only.
