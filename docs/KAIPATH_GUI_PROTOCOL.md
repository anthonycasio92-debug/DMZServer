# KaiPath ↔ LegacyMechanics Ultra GUI protocol

Channel: `legacymechanics:gui` · protocol constant: `LmGuiNetwork.PROTOCOL_VERSION` (`"1"`).
**Bumping the protocol requires a coordinated KaiPath release.**

LM is **server-only** for gameplay logic. KaiPath owns the client screen renderer.
**Future-proofing rule:** the client renders what the server sends — LM must not depend on
hardcoded client screen lists. New menus are added on the LM side; KaiPath consumes the
packet shape below.

## Zero-config new screens (LM)

When adding a system:

1. Add the logical id to `UltraScreenIds.PROTOCOL_IDS` and `UltraScreenIds.BUILDER_IDS`.
2. Implement `Ultra*Screens` that build via **`UltraScreenSpec` only** (never
   `new GuiOpenPacket` / `LmGuiNetwork.sendOpen` from a screen class).
3. Wire `UltraGui.openUltra` to that builder (open entrypoint).

Automatic (no extra switch):

- **Hub shim tunneling** — any id not in `KAIPATH_REGISTERED_IDS` is rewritten to wire
  `screenId=hub` (logical id kept in `layoutJson` / button `args`).
- **Shared encode path** — every open goes `UltraScreenSpec.toPacket` →
  `LmGuiNetwork.sendOpen` → `GuiOpenPacket.encode`.
- **Bare hub action mapping** — `UltraScreenIds.bareOpenTarget` maps every protocol/builder
  id (plus `BARE_ALIASES`) without a per-system verb-parser switch.

Startup aborts on catalog errors, invalid builder packets, or encode/decode round-trip
failures.

### Screen id contract

Protocol / LM builders use logical ids: `hub`, `difficulty`, `rival`, `spar`, `prestige`,
`character`, `progression`, `config`, `admin`.

#### Hub wire shim (KaiPath 1.3.0 stock)

Stock KaiPath 1.3.0 only registers **`hub`**. LM tunnels any logical id not in
`UltraScreenIds.KAIPATH_REGISTERED_IDS` by setting the wire `screenId` to **`hub`**.
Title, tabs, buttons, panel (**350×220** default), and layout stay as built.
The logical system is stored in:

- button `args.screen` / `args.system` (echoed into `GuiActionPacket`)
- `layoutJson.system` / `layoutJson.screen`
- `@<screen>:` / `@<logical>:` prefixes on button/tab action ids

`UltraGuiActions` resolves the logical screen from that metadata (not the wire `hub` id).
Expand `KAIPATH_REGISTERED_IDS` when KaiPath adds handlers — one-line change disables the shim
for those ids.

**Patched client (`kaipath-1.3.1-lm-ultra` / upstream after JLDK merge):** no screenId whitelist —
any id opens `LmDynamicScreen`. Hub shim remains harmless for older clients still on 1.3.0.

## GuiOpenPacket (S→C) — 10 fields

Single shared encoder: `GuiOpenPacket.encode` / `decode`. Field order is the wire contract.

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | Logical/wire system id (`hub`, `difficulty`, …) |
| 2 | `title` | string | Colored title |
| 3 | `pageId` | string | Page within the system |
| 4 | `tabsJson` | string | JSON **array** of `{id,label}` — `id` is a full action (`@difficulty:tab:tiers`) |
| 5 | `activeTab` | string | Active tab key (unqualified, e.g. `tiers`) |
| 6 | `linesJson` | string | JSON **array** of body lines |
| 7 | `buttonsJson` | string | JSON **array** of button objects (below) |
| 8 | `panelWidth` | varint | GUI pixels, default **350** |
| 9 | `panelHeight` | varint | GUI pixels, default **220** (grows up to **320** if needed) |
| 10 | `layoutJson` | string | JSON object (below) |

### Panel layout (do not fullscreen)

Compact Ultra centered dialog — not a framebuffer fill.

**LM always populates fields 8–10** via `UltraScreenSpec` → `GuiOpenPacket`. Defaults are
**350×220**, `center: true`, `margin: 6`. Button height **18**, row gap **4**.

`layoutJson` keys (KaiPath **must** honor these):

```json
{
  "panelWidth": 350,
  "panelHeight": 220,
  "width": 350,
  "height": 220,
  "center": true,
  "margin": 6,
  "system": "difficulty",
  "screen": "difficulty"
}
```

**Expected KaiPath (`LmScreens`) behavior:**

1. Read varints field 8–9; parse field 10.
2. Prefer wire varints; fall back to `layoutJson.panelWidth` / `width` (same for height).
3. When `center` is true, place at `((screenW - panelWidth) / 2, (screenH - panelHeight) / 2)`.
4. Keep at least `margin` GUI pixels from the edge.
5. Button `x`/`y` are **relative to the panel origin**.
6. **Do not** stretch to the framebuffer when panel fields are present.
7. **Render tabs/lines/buttons from the packet** — do not replace them with a hardcoded menu.

### Button JSON schema

```json
{
  "id": "@difficulty:do:activate:1",
  "label": "§aT1 …",
  "style": "normal",
  "x": 16,
  "y": 100,
  "w": 150,
  "h": 18,
  "args": { "screen": "difficulty", "system": "difficulty", "page": "tiers" }
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `id` | yes | Action id (see contract below) |
| `label` | yes | Display text (may include `§` colors) |
| `style` | no | `normal` / `confirm` / `danger` / `dim` … |
| `x`,`y`,`w`,`h` | yes | Panel-relative geometry |
| `args` | yes (LM) | Must echo `screen` / `system` / `page` for hub-shim routing |

## GuiActionPacket (C→S)

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | Echo `GuiOpenPacket.screenId` (wire id; often `hub`) |
| 2 | `actionId` | string | Echo button/tab `id` **verbatim** (including `@screen:` prefix) |
| 3 | `argsJson` | string | Echo button `args` (merge click extras if any) |

## Action ID verb contract

Every button/tab id LM encodes must pass `UltraScreenIds.requireValidActionId` at build time:

- **Non-null / non-empty**
- After optional `@<screen>:` strip, the remainder is either:
  - **bare** — no `:`, e.g. `character` (KaiPath hardcoded hub), or
  - **verb-prefixed** — `open:`, `do:`, `tab:`, `nav:`, or `page:`

Examples:

- `@difficulty:do:activate:1`
- `@hub:open:character`
- `@rival:tab:challenge`
- `@config:nav:refresh`
- bare `character` (from KaiPath 1.3.0 hub)

Server strips `@screen:` first, then handles verbs. On hub, bare protocol ids map via
`UltraScreenIds.bareOpenTarget` → `open:&lt;system&gt;`.

Malformed ids fail at **packet build** (clear error naming screen + button label) — they never
reach the client.

## Startup self-tests (LM)

On `LmGuiNetwork.register()`:

1. Catalog check — `BUILDER_IDS ⊆ PROTOCOL_IDS` (ERROR + abort).
2. Builder probe — every builder produces a valid `GuiOpenPacket` (non-null `screenId`, valid
   JSON arrays for tabs/buttons/lines, sane panel bounds) (ERROR + abort).
3. Encode/decode round-trip — every builder packet’s **10 fields** survive identically;
   `GuiActionPacket` with a verbatim action id round-trips (ERROR + abort). Logs
   `screensTested=N`.

## Failure logging

Unhandled exceptions log `screen`, **`rawActionId`**, resolved action, and stack under
`LegacyMechanics`. Player chat never includes raw action IDs.

## KaiPath client requirements (JLDK)

Stock **1.3.0** only decodes `screenId` and shows a hardcoded hub. Required client work
(decode 10 fields, dynamic render, verbatim action echo, no hardcoding) is specified in
**`docs/KAIPATH_JLDK_CLIENT_BRIEF.md`**.

Reference patch in this repo: `tools/kaipath-lm-ultra-client/` →
`mods/kaipath-1.3.1-lm-ultra.jar` (overlay on 1.3.0). Merge upstream so official KaiPath
ships the dynamic consumer.

`KaiPathLmConsumerBootstrapMixin` (LM) still keeps `registerClient()` alive when the LM jar
is also on the client.
