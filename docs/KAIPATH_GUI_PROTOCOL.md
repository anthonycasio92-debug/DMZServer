# Ultra GUI protocol (KaiPath)

**Audience:** any Forge mod that wants KaiPath to render server-driven menus — not only
LegacyMechanics (LM).

| | |
|--|--|
| Channel convention | `<modid>:gui` (e.g. `legacymechanics:gui`, `mymod:gui`) |
| Forge protocol string | `"1"` (`UltraGuiProtocol.PROTOCOL_VERSION`) |
| Message 0 | GuiOpenPacket (server → client) |
| Message 1 | GuiActionPacket (client → server) |
| Client renderer | KaiPath (dynamic — renders the packet, no hardcoded menus) |

**Principle:** the client renders what the server sends. Packet codecs must not embed a
channel namespace or mod id. New menus are added on the **server mod** side only.

**KaiPath 1.3.1+** ships the dynamic Ultra consumer (10-field decode, `LmDynamicScreen`,
`UltraGuiConsumerRegistry`). LM 4.6.105+ sends logical wire `screenId`s (hub shim off).

Implement-yourself kit (for other mods / older notes): [`tools/ultra-gui-kit/`](../tools/ultra-gui-kit/).
Server codecs: [`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/).

---

## For other mod developers

### 1. Channel naming

Register a Forge `SimpleChannel` on:

```java
new ResourceLocation(YOUR_MOD_ID, "gui")  // → "mymod:gui"
```

Use protocol `"1"`. Accept missing/mismatched remotes if players without KaiPath should
still join (KaiPath is optional for them).

KaiPath keys **client consumers by channel id**. Each partner mod needs its own
`registerClientChannel(modId)` entry — do not share `legacymechanics:gui`.

### 2. Packet field reference

#### GuiOpenPacket (S→C) — 10 fields

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | Your screen / system id (any string; not a whitelist) |
| 2 | `title` | string | Title (may include `§` color codes) |
| 3 | `pageId` | string | Page within the system |
| 4 | `tabsJson` | string | JSON **array** of `{id,label}` |
| 5 | `activeTab` | string | Active tab key |
| 6 | `linesJson` | string | JSON **array** of body text lines |
| 7 | `buttonsJson` | string | JSON **array** of button objects |
| 8 | `panelWidth` | varint | Default **350** |
| 9 | `panelHeight` | varint | Default **220** |
| 10 | `layoutJson` | string | JSON object (below) |

Missing trailing fields → defaults. Bytes after field 10 are ignored (forward-compatible).

#### GuiActionPacket (C→S) — 3 fields

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | Echo open packet `screenId` verbatim |
| 2 | `actionId` | string | Echo button/tab `id` verbatim |
| 3 | `argsJson` | string | Echo button `args` JSON verbatim |

The Forge channel protocol (`"1"`) is **not** a field inside `GuiActionPacket`.

### 3. Button JSON schema

```json
{
  "id": "@myshop:do:buy:starter",
  "label": "§aBuy starter pack",
  "style": "confirm",
  "x": 20,
  "y": 100,
  "w": 150,
  "h": 18,
  "args": { "screen": "shop", "page": "main", "sku": "starter" }
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `id` | yes | Action id (see verb contract) |
| `label` | yes | Display text |
| `style` | no | Hint: `normal` / `confirm` / `danger` / `dim` … |
| `x`,`y`,`w`,`h` | yes | **Panel-relative** geometry |
| `args` | recommended | Opaque JSON echoed to the server |

### 4. Tabs

`tabsJson` is a JSON array. Tab click sends that tab’s `id` as `actionId` (same as buttons).
Do not hardcode tab lists on the client.

```json
[{"id":"@myshop:tab:weapons","label":"Weapons"},{"id":"@myshop:tab:armor","label":"Armor"}]
```

### 5. Layout JSON

```json
{
  "panelWidth": 350,
  "panelHeight": 220,
  "width": 350,
  "height": 220,
  "center": true,
  "margin": 6
}
```

KaiPath places the panel at `((screenW - panelWidth) / 2, (screenH - panelHeight) / 2)` when
`center` is true, clamped by `margin` from the screen edges. Extra keys (e.g. `system`) are
allowed and ignored by the renderer.

### 6. Action verb contract (recommended)

Servers may use any non-empty `actionId`. The recommended shape (used by LM and easy for
KaiPath tooling) is:

- Optional `@<screen>:` prefix
- Then either a **bare** id (no `:`) or a **verb-prefixed** id: `open:`, `do:`, `tab:`,
  `nav:`, `page:`

Examples:

- `@myshop:do:buy:starter`
- `@myshop:tab:weapons`
- `@myshop:open:cart`
- bare `close` (if you choose to support it)

**Client never validates or rewrites action ids** — echo verbatim. The server decides what
is legal.

LM enforces this at packet **build** time via `UltraScreenIds.requireValidActionId` (malformed
ids never reach the client). Other mods may adopt the same check or define their own.

### 7. Hello world (minimal menu)

Server (your mod), after registering `mymod:gui`:

```java
// Build + send a one-button screen
String buttons = "["
    + "{\"id\":\"@myshop:do:ping\",\"label\":\"§aPing server\","
    + "\"style\":\"confirm\",\"x\":20,\"y\":80,\"w\":150,\"h\":18,"
    + "\"args\":{\"screen\":\"hello\",\"page\":\"main\"}}"
    + "]";
GuiOpenPacket open = new GuiOpenPacket(
    "hello",                    // screenId
    "§6My Shop",                // title
    "main",                     // pageId
    "[]",                       // tabsJson
    "",                         // activeTab
    "[\"Welcome to My Shop.\"]",// linesJson
    buttons,
    350, 220,
    "{\"center\":true,\"margin\":6}");
CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), open);
```

Handle the click on the server:

```java
// GuiActionPacket handler (main thread)
if ("@myshop:do:ping".equals(msg.actionId())) {
    player.sendSystemMessage(Component.literal("Pong from My Shop"));
}
```

Client (KaiPath / JLDK): register the consumer once:

```java
UltraGuiConsumerRegistry.registerClientChannel("mymod"); // → mymod:gui
```

No screen-id whitelist, no hardcoded buttons — KaiPath opens a dynamic screen and echoes
clicks on `mymod:gui`.

Copy-paste codecs: [`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/).

---

## Shared channel convention (KaiPath)

| Rule | Detail |
|------|--------|
| Format | `<modid>:gui` |
| Registry key | Full channel id string (`mymod:gui`) |
| Multi-mod | Many channels may be registered; clicks return on the channel that opened the screen |
| Protocol | `"1"` on every channel; bump requires coordinated KaiPath release |

JLDK implementation target: merge `tools/ultra-gui-kit/kaipath-client/` and call
`UltraGuiConsumerRegistry.registerClientChannel(modId)` (see
`docs/KAIPATH_JLDK_CLIENT_BRIEF.md`). **No overlay jar is shipped.**

---

## LegacyMechanics notes (reference implementation)

LM is one consumer of this protocol on channel `legacymechanics:gui`. Its content helpers
(`UltraScreenSpec`, hub shim for stock KaiPath 1.3.0, startup self-tests) are **LM-only**
and are not required for other mods.

### Zero-config new screens (LM)

1. Add the logical id to `UltraScreenIds.PROTOCOL_IDS` and `UltraScreenIds.BUILDER_IDS`.
2. Implement `Ultra*Screens` via **`UltraScreenSpec` only**.
3. Wire `UltraGui.openUltra` to that builder.

Automatic: hub shim for stock 1.3.0, shared encode path, bare hub action mapping, startup
round-trip abort on failure.

#### Hub wire shim (stock KaiPath 1.3.0 only)

Stock 1.3.0 only registers `hub`. LM may rewrite wire `screenId` to `hub` while keeping
title/tabs/buttons/panel. Logical system stays in `args` / `layoutJson` / `@screen:` prefixes.
KaiPath with the kit merged needs no shim — any `screenId` opens the dynamic screen.

LM logical ids today: `hub`, `difficulty`, `rival`, `spar`, `prestige`, `character`,
`progression`, `config`, `admin`.

### Startup self-tests (LM)

On `LmGuiNetwork.register()`: catalog check, builder probe, 10-field encode/decode
round-trip + verbatim `GuiActionPacket` (failures abort startup).

---

## KaiPath client requirements (JLDK)

Stock **1.3.0** only decodes `screenId` and shows a hardcoded hub. Required client work
(items 1–5 dynamic render + items 7–10 multi-mod) is in
**`docs/KAIPATH_JLDK_CLIENT_BRIEF.md`**.

**Implement from source:** [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/)
— merge into KaiPath; do not expect a DMZServer-built KaiPath jar.
