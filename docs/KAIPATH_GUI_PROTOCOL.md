# Ultra GUI protocol (KaiPath)

**Audience:** any Forge mod that wants KaiPath to render server-driven menus — not only
LegacyMechanics (LM).

| | |
|--|--|
| Channel convention | `<modid>:gui` (e.g. `legacymechanics:gui`, `mymod:gui`) |
| Forge protocol string | `"1"` (`UltraGuiProtocol.PROTOCOL_VERSION`) |
| Message 0 | GuiOpenPacket (server → client) |
| Message 1 | GuiActionPacket (client → server) |
| Message 2 | GuiFeaturesPacket (client → server, **optional**) |
| Client renderer | KaiPath (dynamic — renders the packet, no hardcoded menus) |

**Principle:** the client renders what the server sends. Packet codecs must not embed a
channel namespace or mod id. New menus are added on the **server mod** side only. Optional
fields (`style`, `activeTab`) are **data**, not new hardcoded screens.

**KaiPath 1.3.1** baseline: decodes 10 fields and renders dynamic buttons/tabs/lines, but
**ignores** button `style` and packet `activeTab`. LM 4.6.105+ advertises the gap in logs
until the client reports those features via `GuiFeaturesPacket`.

Implement-yourself kit: [`tools/ultra-gui-kit/`](../tools/ultra-gui-kit/).

---

## Client conformance contract (JLDK)

The client **must** render what the server sends (no hardcoded screen/tab/button lists).

| Capability | Required since | Status on KaiPath 1.3.1 |
|------------|----------------|-------------------------|
| Decode all 10 GuiOpen fields | 1.3.1 | yes |
| Dynamic title / tabs / lines / buttons | 1.3.1 | yes |
| Verbatim GuiAction echo | 1.3.1 | yes |
| Any `screenId` (no whitelist) | 1.3.1 | yes |
| Honor button **`style`** | next | **no** — all buttons look identical |
| Honor **`activeTab`** highlight | next | **no** — active tab not distinguished |
| Send **GuiFeaturesPacket** | next | **no** — treated as 1.3.1 baseline |

### Button JSON schema

Every button object in `buttonsJson`:

| Field | Required | Type | Notes |
|-------|----------|------|--------|
| `id` | yes | string | Action id (echoed verbatim on click) |
| `label` | yes | string | Display text (may include `§` colors) |
| `style` | no | string | Visual role — see table below (default `normal`) |
| `x` | yes | number | Panel-relative X |
| `y` | yes | number | Panel-relative Y |
| `w` | yes | number | Width |
| `h` | yes | number | Height |
| `args` | recommended | object | Opaque JSON echoed to the server |

```json
{
  "id": "@difficulty:do:activate:1",
  "label": "§aActivate T1",
  "style": "confirm",
  "x": 20,
  "y": 100,
  "w": 150,
  "h": 18,
  "args": { "screen": "difficulty", "page": "tiers" }
}
```

#### `style` values (client must implement)

| Value | Meaning | Visual expectation |
|-------|---------|-------------------|
| `normal` | Default button | Neutral / standard themed button |
| `confirm` | Safe / positive action | **Green** (or confirm theme) |
| `danger` | Destructive / irreversible | **Red** (or danger theme) |
| `dim` | Disabled / unavailable look | **Dark gray** / muted (still clickable unless server says otherwise) |

Unknown style values → fall back to `normal`. Missing `style` → `normal`.

KaiPath **1.3.1** ignores `style`: all buttons render identically (no green/red/dim
distinction).

### `activeTab` (GuiOpenPacket field 5)

| | |
|--|--|
| Wire field | #5 `activeTab` (string) |
| Meaning | Id of the **currently open** tab |
| Client must | Visually distinguish that tab from the others (highlight / selected state) |

`tabsJson` entries use `{id,label}`. Match `activeTab` against the tab’s **unqualified**
key when the server sends keys like `tiers`, or against the full `id` when that is what
`activeTab` contains. LM builders set `activeTab` to the short key (e.g. `home`, `tiers`).

KaiPath **1.3.1** ignores `activeTab`: the active tab is not highlighted.

### GuiFeaturesPacket (C→S) — message 2

Optional capability advertisement. **Backward compatible:** a client that never sends it
is treated as supporting **nothing beyond the 1.3.1 baseline** (dynamic UI, but no
`style` / `activeTab`).

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `featuresJson` | string | JSON **array** of feature ids |

Known optional feature ids:

| Id | Means the client honors… |
|----|--------------------------|
| `style` | Button `style` values above |
| `activeTab` | GuiOpenPacket field 5 highlight |

Example (conforming client after implementing both):

```json
["style","activeTab"]
```

Send once after channel registration (client setup / first join). Extra ids are ignored.
Extra wire fields after #1 are ignored.

LM logs a **warning** when a player’s feature set lacks `style` or `activeTab`, naming:

- player
- KaiPath version if known
- missing features
- player-visible impact (e.g. “all buttons render identically…” / “active tab is not highlighted”)

`/lm admin guitest` includes the reported features in staff chat and the open packet body.

---

## For other mod developers

### 1. Channel naming

```java
new ResourceLocation(YOUR_MOD_ID, "gui")  // → "mymod:gui"
```

Protocol `"1"`. Message 0 open, 1 action, 2 features (optional).

### 2. GuiOpenPacket (S→C) — 10 fields

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | Any string (not a whitelist) |
| 2 | `title` | string | May include `§` colors |
| 3 | `pageId` | string | Page within the system |
| 4 | `tabsJson` | string | JSON array of `{id,label}` |
| 5 | `activeTab` | string | **Active tab key — client must highlight** |
| 6 | `linesJson` | string | JSON array of body lines |
| 7 | `buttonsJson` | string | JSON array of button objects (incl. **style**) |
| 8 | `panelWidth` | varint | Default **350** |
| 9 | `panelHeight` | varint | Default **220** |
| 10 | `layoutJson` | string | JSON object |

Missing trailing fields → defaults. Bytes after field 10 ignored.

### 3. GuiActionPacket (C→S) — 3 fields

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | Echo open `screenId` verbatim |
| 2 | `actionId` | string | Echo button/tab `id` verbatim |
| 3 | `argsJson` | string | Echo button `args` verbatim |

### 4. Layout JSON

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

Panel at `((screenW - panelWidth) / 2, (screenH - panelHeight) / 2)` when `center` is true;
honor `margin`. Button `x`/`y` are panel-relative.

### 5. Action verb contract (recommended)

Optional `@<screen>:` prefix, then bare id or `open:` / `do:` / `tab:` / `nav:` / `page:`.
Client never validates action ids — echo verbatim.

LM enforces this at packet **build** time via `UltraScreenIds.requireValidActionId`
(malformed ids never reach the client).

### 6. Hello world

```java
String buttons = "["
    + "{\"id\":\"@myshop:do:ping\",\"label\":\"§aPing\","
    + "\"style\":\"confirm\",\"x\":20,\"y\":80,\"w\":150,\"h\":18,"
    + "\"args\":{\"screen\":\"hello\",\"page\":\"main\"}}"
    + "]";
GuiOpenPacket open = new GuiOpenPacket(
    "hello", "§6My Shop", "main",
    "[{\"id\":\"home\",\"label\":\"Home\"}]",
    "home",
    "[\"Welcome.\"]",
    buttons, 350, 220,
    "{\"center\":true,\"margin\":6}");
CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), open);
```

Client: `UltraGuiConsumerRegistry.registerClientChannel("mymod")` then send
`GuiFeaturesPacket` with `["style","activeTab"]` once implemented.

---

## Shared channel convention (KaiPath)

| Rule | Detail |
|------|--------|
| Format | `<modid>:gui` |
| Registry key | Full channel id |
| Multi-mod | Clicks return on the channel that opened the screen |
| Protocol | `"1"` |

---

## LegacyMechanics notes

LM channel: `legacymechanics:gui`. Startup aborts on catalog errors or encode/decode
round-trip failure (including **style** / **activeTab** survival and `GuiFeaturesPacket`).

With KaiPath **1.3.1+** dynamic-any screen ids, hub wire shim is off
(`UltraScreenIds.KAIPATH_DYNAMIC_ANY_SCREEN_ID`).

Smoke: `/lm admin guitest` → reports client features → click **Ping server**.

---

## Packaging checklist (LM jar)

1. Clean compile output before packaging (`build.sh` does this).
2. Scan jar for missing/referenced classes and inner classes
   (`audit_jar_inner_classes.py`).
3. Scan every `@Mixin` class for non-private helper methods
   (`audit_mixin_visibility.py` — helpers must be `private`).
4. Version pins match: `AdaptiveDifficultyMod.VERSION` == `mods.toml` `version`
   (== `plugin.yml` when a GUI plugin is shipped).
5. Fail-closed audits in `build.sh` (Ultra actions, forge GUI backend, ship manifest, …).
