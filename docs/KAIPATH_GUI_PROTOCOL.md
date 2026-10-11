# KaiPath ↔ LegacyMechanics Ultra GUI protocol

Channel: `legacymechanics:gui` · protocol string: `"1"` (append-compatible).

LM is **server-only**. KaiPath owns the client screen (`LmScreens` / DMZUltra widgets).

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

`layoutJson` keys (KaiPath should honor these):

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

- Draw the dialog at `(screenW - panelWidth) / 2`, `(screenH - panelHeight) / 2` when `center` is true.
- Keep at least `margin` GUI pixels from the screen edge.
- Button `x`/`y` are **relative to the panel origin**, not the framebuffer.
- Defaults match CNPC `CnpcGuiSupport` (**430×280**).

If KaiPath ignores fields 8–10, menus will appear fullscreen — that is a client bug; LM already sends the bounds.

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

Unhandled exceptions and “Something went wrong” handler results are logged on the server with `screen`, `actionId`, and stack traces under the `LegacyMechanics` logger.
